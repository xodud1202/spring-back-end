package com.xodud1202.springbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xodud1202.springbackend.domain.news.*;
import com.xodud1202.springbackend.mapper.NewsDailyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class NewsDailyService {
    public static final List<String> CATEGORIES = List.of("사회", "경제", "국제", "정치", "스포츠", "날씨");
    private final NewsDailyMapper mapper;
    private final ObjectMapper json;
    private final PlatformTransactionManager transactionManager;

    private TransactionTemplate newTransaction() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.setTimeout(30);
        return tx;
    }

    // Short reservation commits independently, even if the caller's transaction later rolls back.
    public NewsDailyResult start(NewsDailyAttemptRequest request, Long actor) {
        requireActor(actor);
        validateAttempt(request);
        return newTransaction().execute(tx -> {
            mapper.ensureDate(request.yyyymmdd());
            int counter = mapper.lockDate(request.yyyymmdd());
            NewsDailyBatch prior = mapper.lockAttempt(request.yyyymmdd(), request.requestId());
            if (prior != null) {
                requireOwner(prior, actor);
                if (!prior.sourceGeneratedAt().equals(request.sourceGeneratedAt()))
                    throw new NewsDailyConflictException("같은 requestId의 원본 생성시각을 변경할 수 없습니다.");
                return result(prior);
            }
            int seq = Math.addExact(Math.max(counter, mapper.existingMax(request.yyyymmdd())), 1);
            mapper.advance(request.yyyymmdd(), seq);
            mapper.reserve(request.yyyymmdd(), seq, request.requestId(), request.sourceGeneratedAt(), actor);
            return result(mapper.lockAttempt(request.yyyymmdd(), request.requestId()));
        });
    }

    // Compatibility API: reserve if needed, then atomically publish the entire article array.
    public NewsDailyResult save(NewsDailySaveRequest input, Long actor) {
        requireActor(actor);
        if (input == null) throw new IllegalArgumentException("요청이 필요합니다.");
        NewsDailyAttemptRequest attempt = new NewsDailyAttemptRequest(input.yyyymmdd(), input.requestId(), input.sourceGeneratedAt());
        NewsDailyResult reserved = start(attempt, actor);
        if ("FAILED".equals(reserved.status())) throw new NewsDailyConflictException("실패한 시도는 다시 저장할 수 없습니다. 새 requestId로 시작하세요.");
        NewsDailySaveRequest request;
        try { request = validate(input); }
        catch (IllegalArgumentException ex) {
            recordFailure(attempt, actor, "VALIDATION_FAILED");
            throw ex;
        }
        String hash = hash(request);
        AtomicBoolean callbackFinished = new AtomicBoolean(false);
        try {
            return newTransaction().execute(tx -> {
                NewsDailyBatch batch = mapper.lockAttempt(request.yyyymmdd(), request.requestId());
                requireOwner(batch, actor);
                if ("FAILED".equals(batch.status())) throw new NewsDailyConflictException("이미 실패 처리된 시도입니다. 새 requestId를 사용하세요.");
                if ("COMPLETED".equals(batch.status())) {
                    if (!hash.equals(batch.payloadHash())) throw new NewsDailyConflictException("완료된 시도의 내용을 변경할 수 없습니다.");
                    NewsDailyResult prior = result(batch);
                    callbackFinished.set(true);
                    return prior;
                }
                for (NewsDailyArticle article : request.articles())
                    mapper.insertArticle(request.yyyymmdd(), batch.seq(), article, actor);
                if (mapper.complete(request.yyyymmdd(), request.requestId(), hash, request.articles().size()) != 1)
                    throw new IllegalStateException("시도 완료 전환 실패");
                NewsDailyResult complete = result(mapper.lockAttempt(request.yyyymmdd(), request.requestId()));
                callbackFinished.set(true);
                return complete;
            });
        } catch (NewsDailyConflictException ex) {
            throw ex; // Never turn a competing completed/failed attempt into a failure.
        } catch (RuntimeException ex) {
            // Commit/rollback transport failures have an uncertain outcome. Never guess FAILED.
            if (callbackFinished.get() || ex instanceof TransactionException)
                throw new IllegalStateException("커밋 결과를 확정할 수 없습니다. 상태를 확인하고 같은 requestId로 재시도하세요.");
            recordFailure(attempt, actor, "SAVE_FAILED");
            // Do not expose/log an underlying SQL exception containing article parameters.
            throw new IllegalStateException("기사 저장이 롤백되었습니다. 시도 상태를 확인하세요.");
        }
    }

    public NewsDailyResult fail(NewsDailyFailRequest request, Long actor) {
        requireActor(actor);
        if (request == null) throw new IllegalArgumentException("요청이 필요합니다.");
        validateDate(request.yyyymmdd());
        validateRequestId(request.requestId());
        if (!Set.of("SUMMARY_FAILED", "CANCELLED", "ABANDONED").contains(Objects.toString(request.reasonCode(), "")))
            throw new IllegalArgumentException("reasonCode는 SUMMARY_FAILED/CANCELLED/ABANDONED 중 하나여야 합니다.");
        return newTransaction().execute(tx -> {
            NewsDailyBatch batch = mapper.lockAttempt(request.yyyymmdd(), request.requestId());
            requireOwner(batch, actor);
            if ("COMPLETED".equals(batch.status())) throw new NewsDailyConflictException("완료된 시도를 실패 처리할 수 없습니다.");
            if ("PENDING".equals(batch.status())) mapper.fail(request.yyyymmdd(), request.requestId(), request.reasonCode());
            return result(mapper.lockAttempt(request.yyyymmdd(), request.requestId()));
        });
    }

    private void recordFailure(NewsDailyAttemptRequest request, Long actor, String code) {
        try {
            newTransaction().executeWithoutResult(tx -> {
                NewsDailyBatch batch = mapper.lockAttempt(request.yyyymmdd(), request.requestId());
                requireOwner(batch, actor);
                // A concurrent successful retry wins. Preserve all terminal states.
                if ("PENDING".equals(batch.status())) mapper.fail(request.yyyymmdd(), request.requestId(), code);
            });
        } catch (RuntimeException ex) {
            throw new IllegalStateException("실패 상태 기록을 확정하지 못했습니다. 네트워크 오류만으로 실패를 단정하지 말고 상태를 확인하세요.");
        }
    }

    private static void requireActor(Long actor) {
        if (actor == null) throw new SecurityException("관리자 인증이 필요합니다.");
    }
    private static void requireOwner(NewsDailyBatch batch, Long actor) {
        if (batch == null) throw new NewsDailyConflictException("예약된 시도를 찾을 수 없습니다.");
        if (!Objects.equals(batch.regNo(), actor)) throw new NewsDailyConflictException("다른 사용자의 시도에 접근할 수 없습니다.");
    }

    @Transactional(readOnly = true)
    public NewsDailyResult latest(String date) {
        validateDate(date);
        NewsDailyBatch batch = mapper.latest(date);
        return batch == null ? new NewsDailyResult("EMPTY", date, null, null, null, List.of()) : result(batch);
    }

    /** 읽기 화면은 시도 이력과 별개로 실제 저장된 뉴스의 최대 시퀀스를 노출합니다. */
    @Transactional(readOnly = true)
    public NewsDailySummary summary(String date) {
        // 기존 날짜 검증과 기사 정렬 쿼리를 재사용해 날짜별 최신 묶음을 조회합니다.
        validateDate(date);
        int seq = mapper.existingMax(date);
        return seq == 0 ? new NewsDailySummary(date, null, List.of())
                : new NewsDailySummary(date, seq, mapper.articles(date, seq));
    }

    /** 한국 시간 기준 최근 7일에서 실제 뉴스가 존재하는 날짜를 조회합니다. */
    @Transactional(readOnly = true)
    public NewsDailySummaryWeek summaryWeek() {
        // 서버 기본 시간대에 영향받지 않도록 날짜 경계를 한국 시간으로 고정합니다.
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        String toDate = today.format(DateTimeFormatter.BASIC_ISO_DATE);
        String fromDate = today.minusDays(6).format(DateTimeFormatter.BASIC_ISO_DATE);
        return new NewsDailySummaryWeek(toDate, mapper.summaryDates(fromDate, toDate));
    }

    @Transactional(readOnly = true)
    public NewsDailyResult status(String date, String requestId, Long actor) {
        validateDate(date);
        validateRequestId(requestId);
        requireActor(actor);
        NewsDailyBatch batch = mapper.find(date, requestId);
        return batch == null || !Objects.equals(batch.regNo(), actor)
                ? new NewsDailyResult("NOT_FOUND", date, null, requestId, null, List.of()) : result(batch);
    }

    private NewsDailyResult result(NewsDailyBatch batch) {
        List<NewsDailyArticle> articles = "COMPLETED".equals(batch.status())
                ? mapper.articles(batch.yyyymmdd(), batch.seq()) : List.of();
        if ("COMPLETED".equals(batch.status()) && articles.size() != batch.articleCount())
            throw new IllegalStateException("저장 묶음 무결성을 확인해야 합니다.");
        return new NewsDailyResult(batch.status(), batch.yyyymmdd(), batch.seq(), batch.requestId(),
                batch.sourceGeneratedAt(), articles, batch.failureCode(), batch.startedAt(), batch.finishedAt());
    }

    public static void validateAttempt(NewsDailyAttemptRequest request) {
        if (request == null) throw new IllegalArgumentException("요청이 필요합니다.");
        validateDate(request.yyyymmdd());
        validateRequestId(request.requestId());
        try {
            if (request.sourceGeneratedAt() == null || !request.sourceGeneratedAt().matches("[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}"))
                throw new IllegalArgumentException();
            LocalDateTime.parse(request.sourceGeneratedAt(),
                    DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT));
        } catch (RuntimeException ex) { throw new IllegalArgumentException("sourceGeneratedAt은 원본 meta의 KST 생성시각이어야 합니다."); }
    }

    public static void validateDate(String date) {
        if (date == null || !date.matches("[0-9]{8}")) throw new IllegalArgumentException("yyyymmdd는 유효한 8자리 날짜여야 합니다.");
        try { LocalDate.parse(date, DateTimeFormatter.BASIC_ISO_DATE); }
        catch (RuntimeException ex) { throw new IllegalArgumentException("존재하지 않는 날짜입니다."); }
    }

    private static void validateRequestId(String id) {
        if (id == null || !id.matches("[A-Za-z0-9_-]{8,64}"))
            throw new IllegalArgumentException("requestId는 영문/숫자/_/- 8~64자여야 합니다.");
    }

    public static NewsDailySaveRequest validate(NewsDailySaveRequest input) {
        if (input == null) throw new IllegalArgumentException("요청이 필요합니다.");
        validateAttempt(new NewsDailyAttemptRequest(input.yyyymmdd(), input.requestId(), input.sourceGeneratedAt()));
        if (input.articles() == null || input.articles().size() < 20 || input.articles().size() > 25)
            throw new IllegalArgumentException("완성된 요약 20~25개를 한 번에 전송하세요.");
        List<NewsDailyArticle> sorted = new ArrayList<>(input.articles());
        if (sorted.contains(null)) throw new IllegalArgumentException("빈 기사는 허용하지 않습니다.");
        sorted.sort(Comparator.comparingInt(NewsDailyArticle::dispOrd));
        int lastCategory = -1;
        int sports = 0, weather = 0;
        for (int i = 0; i < sorted.size(); i++) {
            NewsDailyArticle a = sorted.get(i);
            validateColumn(a.dispOrd(), "categoryNm", a.categoryNm(), 10);
            int category = a.categoryNm() == null ? -1 : CATEGORIES.indexOf(a.categoryNm());
            if (a.dispOrd() != i + 1) throw new IllegalArgumentException("dispOrd는 중복 없이 1부터 연속이어야 합니다.");
            if (category < 0 || category < lastCategory) throw new IllegalArgumentException("카테고리 순서: 사회,경제,국제,정치,스포츠,날씨");
            lastCategory = category;

            validateColumn(a.dispOrd(), "newsContent", a.newsContent(), 800);
            if (category == 4) sports++;
            if (category == 5) weather++;
        }
        if (sports < 1 || sports > 2 || weather != 1)
            throw new IllegalArgumentException("스포츠 1~2개, 날씨 1개를 포함하세요.");
        return new NewsDailySaveRequest(input.yyyymmdd(), input.requestId(), input.sourceGeneratedAt(), List.copyOf(sorted));
    }

    private static void validateColumn(int order, String field, String value, int maximum) {
        int actual = value == null ? 0 : value.codePointCount(0, value.length());
        if (actual > maximum) throw new NewsDailyValidationException(order, field, maximum, actual, "길이 초과. 자동 절단하지 않습니다.");
        if (value == null || value.codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c)))
            throw new NewsDailyValidationException(order, field, maximum, actual, "빈값 또는 공백만 있는 값은 허용하지 않습니다.");
        for (int i=0; i<value.length(); i++) {
            char c=value.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (i+1>=value.length() || !Character.isLowSurrogate(value.charAt(++i)))
                    throw new NewsDailyValidationException(order, field, maximum, actual, "잘못된 Unicode 문자입니다.");
            } else if (Character.isLowSurrogate(c))
                throw new NewsDailyValidationException(order, field, maximum, actual, "잘못된 Unicode 문자입니다.");
        }
    }
    private String hash(NewsDailySaveRequest request) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(json.writeValueAsString(request).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) { throw new IllegalStateException("요청 지문 생성 실패", ex); }
    }
}




