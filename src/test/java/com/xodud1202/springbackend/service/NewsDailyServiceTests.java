package com.xodud1202.springbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xodud1202.springbackend.domain.news.*;
import com.xodud1202.springbackend.mapper.NewsDailyMapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;

class NewsDailyServiceTests {
    private AnnotationConfigApplicationContext context;
    private NewsDailyService service;
    private JdbcTemplate jdbc;
    @Configuration @EnableTransactionManagement
    static class TestConfig {
        @Bean DataSource dataSource() {
            JdbcDataSource ds=new JdbcDataSource();
            ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000"); return ds;
        }
        @Bean PlatformTransactionManager transactionManager(DataSource ds) { return new DataSourceTransactionManager(ds); }
        @Bean SqlSessionFactory sqlSessionFactory(DataSource ds) throws Exception {
            SqlSessionFactoryBean bean=new SqlSessionFactoryBean(); bean.setDataSource(ds);
            org.apache.ibatis.session.Configuration config=new org.apache.ibatis.session.Configuration();
            config.setMapUnderscoreToCamelCase(true); config.addMapper(NewsDailyMapper.class);
            bean.setConfiguration(config); return bean.getObject();
        }
        @Bean NewsDailyMapper mapper(SqlSessionFactory factory) { return new SqlSessionTemplate(factory).getMapper(NewsDailyMapper.class); }
        @Bean NewsDailyService service(NewsDailyMapper mapper, PlatformTransactionManager manager) {
            return new NewsDailyService(mapper,new ObjectMapper(),manager);
        }
    }
    @BeforeEach void setup() {
        context=new AnnotationConfigApplicationContext(TestConfig.class);
        jdbc=new JdbcTemplate(context.getBean(DataSource.class));
        jdbc.execute("CREATE SCHEMA news");
        jdbc.execute("CREATE TABLE news.NEWS_DAILY_SEQUENCE(YYYYMMDD VARCHAR(8) PRIMARY KEY,LAST_SEQ INT NOT NULL)");
        jdbc.execute("CREATE TABLE news.NEWS_DAILY_BATCH(YYYYMMDD VARCHAR(8),SEQ INT,REQUEST_ID VARCHAR(64),PAYLOAD_HASH CHAR(64),"
            +"SOURCE_GENERATED_AT VARCHAR(19),ARTICLE_COUNT INT,REG_NO BIGINT,STATUS VARCHAR(16),FAILURE_CODE VARCHAR(32),"
            +"REG_DT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,FINISHED_DT TIMESTAMP,PRIMARY KEY(YYYYMMDD,SEQ),UNIQUE(YYYYMMDD,REQUEST_ID))");
        jdbc.execute("CREATE TABLE news.NEWS_DAILY(NEWS_NO BIGINT AUTO_INCREMENT PRIMARY KEY,YYYYMMDD VARCHAR(8),SEQ INT,"
            +"CATEGORY_NM VARCHAR(10),DISP_ORD INT,NEWS_CONTENT VARCHAR(800) CHECK(NEWS_CONTENT <> 'FAIL'),REG_NO BIGINT,UDT_NO BIGINT)");
        service=context.getBean(NewsDailyService.class);
    }
    @AfterEach void close() { jdbc.execute("DROP ALL OBJECTS"); context.close(); }
    static NewsDailySaveRequest request(String id) {
        List<NewsDailyArticle> articles=new ArrayList<>();
        for(int i=1;i<=20;i++) articles.add(new NewsDailyArticle(i<=18?"사회":i==19?"스포츠":"날씨",i,"검증된 종합 요약 "+i));
        return new NewsDailySaveRequest("20261013",id,"2026-10-13 09:12:00",articles);
    }
    static NewsDailyAttemptRequest attempt(String id) {
        return new NewsDailyAttemptRequest("20261013",id,"2026-10-13 09:12:00");
    }
    static NewsDailySaveRequest replace(NewsDailySaveRequest r,int index,NewsDailyArticle a) {
        List<NewsDailyArticle> list=new ArrayList<>(r.articles());list.set(index,a);
        return new NewsDailySaveRequest(r.yyyymmdd(),r.requestId(),r.sourceGeneratedAt(),list);
    }
    int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM news."+table,Integer.class); }

    /** 읽기 화면은 완료 이력 유무와 무관하게 해당 날짜의 최대 시퀀스만 순서대로 반환합니다. */
    @Test void summaryReadsHighestStoredSequenceInDisplayOrder() {
        // 다른 날짜의 큰 시퀀스와 이전 묶음을 섞어 날짜·시퀀스 경계를 확인합니다.
        jdbc.update("INSERT INTO news.NEWS_DAILY(YYYYMMDD,SEQ,DISP_ORD,NEWS_CONTENT) VALUES('20261016',3,1,'이전 요약')");
        jdbc.update("INSERT INTO news.NEWS_DAILY(YYYYMMDD,SEQ,DISP_ORD,NEWS_CONTENT) VALUES('20261015',99,1,'다른 날짜')");
        jdbc.update("INSERT INTO news.NEWS_DAILY(YYYYMMDD,SEQ,DISP_ORD,NEWS_CONTENT) VALUES('20261016',4,2,'두 번째')");
        jdbc.update("INSERT INTO news.NEWS_DAILY(YYYYMMDD,SEQ,DISP_ORD,NEWS_CONTENT) VALUES('20261016',4,1,'첫 번째')");
        NewsDailySummary summary = service.summary("20261016");
        assertThat(summary.seq()).isEqualTo(4);
        assertThat(summary.articles()).extracting(NewsDailyArticle::newsContent).containsExactly("첫 번째", "두 번째");
        assertThat(service.summary("20261014").articles()).isEmpty();
        assertThat(service.summary("20261014").seq()).isNull();
        assertThatThrownBy(() -> service.summary("20260230")).isInstanceOf(IllegalArgumentException.class);
    }

    /** 날짜 선택 목록은 한국 시간 기준 오늘부터 6일 전까지만 실제 데이터로 구성합니다. */
    @Test void summaryWeekIncludesOnlySevenDaysWithNews() {
        java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.BASIC_ISO_DATE;
        // 경계 전후의 날짜를 삽입해 오래된 날짜와 미래 날짜가 제외되는지 확인합니다.
        for (int offset : new int[]{-7, -6, -1, 0, 1}) {
            jdbc.update("INSERT INTO news.NEWS_DAILY(YYYYMMDD,SEQ,DISP_ORD,NEWS_CONTENT) VALUES(?,1,1,'날짜 검증')",
                    today.plusDays(offset).format(formatter));
        }
        NewsDailySummaryWeek week = service.summaryWeek();
        assertThat(week.today()).isEqualTo(today.format(formatter));
        assertThat(week.dates()).extracting(NewsDailySummaryDate::yyyymmdd).containsExactly(
                today.minusDays(6).format(formatter), today.minusDays(1).format(formatter), today.format(formatter));
    }

    // 검증된 뉴스 전용 계정이 모든 기사와 시도에 기록되는지 실제 INSERT로 확인합니다.
    @Test void dedicatedActorThreeIsPersistedForEveryArticle() {
        service.save(request("actor-three"),3L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM news.NEWS_DAILY WHERE REG_NO=3 AND UDT_NO=3",Integer.class)).isEqualTo(20);
        assertThat(jdbc.queryForObject("SELECT REG_NO FROM news.NEWS_DAILY_BATCH",Long.class)).isEqualTo(3L);
    }
    // 오늘 오후 원본도 허용하며 실패/진행 중 시도보다 최신 완료만 조회합니다.
    @Test void latestTodaySelectsCompletedSequenceAndIgnoresPendingAndFailed() {
        var today=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        String date=today.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        String generated=today+" 17:30:00";
        assertThat(service.latest(date).status()).isEqualTo("EMPTY");
        service.start(new NewsDailyAttemptRequest(date,"manual-today-01",generated),3L);
        assertThat(service.latest(date).status()).isEqualTo("EMPTY");
        service.save(new NewsDailySaveRequest(date,"manual-today-01",generated,request("fixture-key").articles()),3L);
        service.save(new NewsDailySaveRequest(date,"manual-today-02",generated,request("fixture-key").articles()),3L);
        service.start(new NewsDailyAttemptRequest(date,"manual-today-03",generated),3L);
        service.fail(new NewsDailyFailRequest(date,"manual-today-03","SUMMARY_FAILED"),3L);
        service.start(new NewsDailyAttemptRequest(date,"manual-today-04",generated),3L);
        assertThat(service.latest(date).status()).isEqualTo("COMPLETED");
        assertThat(service.latest(date).seq()).isEqualTo(2);
        assertThat(service.latest(date).articles()).hasSize(20);
    }

    @Test void startIsDurableIdempotentAndOwned() {
        NewsDailyResult first=service.start(attempt("start-001"),7L);
        assertThat(first.status()).isEqualTo("PENDING"); assertThat(first.seq()).isEqualTo(1);
        assertThat(first.startedAt()).isNotNull(); assertThat(first.finishedAt()).isNull();
        assertThat(service.start(attempt("start-001"),7L).seq()).isEqualTo(1);
        assertThat(service.start(attempt("start-002"),7L).seq()).isEqualTo(2);
        assertThatThrownBy(()->service.start(attempt("start-001"),8L)).isInstanceOf(NewsDailyConflictException.class);
        assertThatThrownBy(()->service.start(new NewsDailyAttemptRequest("20261013","start-001","2026-10-13 10:00:00"),7L))
            .isInstanceOf(NewsDailyConflictException.class);
        assertThat(count("NEWS_DAILY")).isZero();
        assertThat(service.status("20261013","start-001",8L).status()).isEqualTo("NOT_FOUND");
    }
    @Test void reservationSurvivesOuterRollback() {
        new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).executeWithoutResult(tx->{
            service.start(attempt("outer-rollback"),1L); tx.setRollbackOnly();
        });
        assertThat(service.status("20261013","outer-rollback",1L).status()).isEqualTo("PENDING");
    }
    @Test void emptyInvalidDateAndAnonymous() {
        assertThat(service.latest("20261013").status()).isEqualTo("EMPTY");
        for(String date:List.of("20260230","20261301","2026101","2026-10-13"))
            assertThatThrownBy(()->service.latest(date)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.save(request("anonymous"),null)).isInstanceOf(SecurityException.class);
        assertThatThrownBy(()->service.start(attempt("anonymous"),null)).isInstanceOf(SecurityException.class);
        assertThat(count("NEWS_DAILY_BATCH")).isZero();
    }
    @Test void partialSqlFailurePreservesSeqAndHistoryButNoArticles() {
        service.save(request("success-1"),1L);
        NewsDailySaveRequest failed=replace(request("failure-1"),10,new NewsDailyArticle("사회",11,"FAIL"));
        assertThatThrownBy(()->service.save(failed,1L)).isInstanceOf(IllegalStateException.class);
        NewsDailyResult state=service.status("20261013","failure-1",1L);
        assertThat(state.status()).isEqualTo("FAILED"); assertThat(state.seq()).isEqualTo(2);
        assertThat(state.failureCode()).isEqualTo("SAVE_FAILED"); assertThat(state.finishedAt()).isNotNull();
        assertThat(state.articles()).isEmpty(); assertThat(count("NEWS_DAILY")).isEqualTo(20);
        assertThat(service.latest("20261013").seq()).isEqualTo(1);
        assertThat(service.save(request("success-3"),1L).seq()).isEqualTo(3);
        assertThatThrownBy(()->service.save(request("failure-1"),1L)).isInstanceOf(NewsDailyConflictException.class);
    }
    @Test void validationFailurePreservesAttemptWithSafeLengthMetadata() {
        NewsDailySaveRequest bad=replace(request("long-body"),5,new NewsDailyArticle("사회",6,"가".repeat(801)));
        Throwable error=catchThrowable(()->service.save(bad,1L));
        assertThat(error).isInstanceOf(NewsDailyValidationException.class);
        assertThat(((NewsDailyValidationException)error).details()).containsEntry("articleDispOrd",6)
            .containsEntry("field","newsContent").containsEntry("maxLength",800).containsEntry("actualLength",801);
        assertThat(error.getMessage()).doesNotContain("가가");
        assertThat(service.status("20261013","long-body",1L).failureCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(count("NEWS_DAILY")).isZero();
        assertThat(service.start(attempt("after-long"),1L).seq()).isEqualTo(2);
    }
    @Test void explicitFailureIsTerminalAndIdempotent() {
        service.start(attempt("cancel-01"),1L);
        NewsDailyFailRequest fail=new NewsDailyFailRequest("20261013","cancel-01","SUMMARY_FAILED");
        assertThat(service.fail(fail,1L).status()).isEqualTo("FAILED");
        assertThat(service.fail(new NewsDailyFailRequest("20261013","cancel-01","ABANDONED"),1L).failureCode()).isEqualTo("SUMMARY_FAILED");
        assertThat(service.start(attempt("cancel-01"),1L).seq()).isEqualTo(1);
        assertThatThrownBy(()->service.save(request("cancel-01"),1L)).isInstanceOf(NewsDailyConflictException.class);
        assertThatThrownBy(()->service.fail(fail,2L)).isInstanceOf(NewsDailyConflictException.class);
        service.save(request("complete-2"),1L);
        assertThatThrownBy(()->service.fail(new NewsDailyFailRequest("20261013","complete-2","CANCELLED"),1L))
            .isInstanceOf(NewsDailyConflictException.class);
    }
    @Test void oldPendingCanCompleteButCannotDisplaceNewerCompletedSeq() {
        service.start(attempt("old-pending"),1L);
        jdbc.update("UPDATE news.NEWS_DAILY_BATCH SET REG_DT=TIMESTAMP '2020-01-01 00:00:00'");
        service.save(request("new-complete"),1L);
        service.start(attempt("new-pending"),1L);
        service.start(attempt("new-failure"),1L);
        service.fail(new NewsDailyFailRequest("20261013","new-failure","ABANDONED"),1L);
        assertThat(service.latest("20261013").seq()).isEqualTo(2);
        assertThat(service.save(request("old-pending"),1L).seq()).isEqualTo(1);
        assertThat(service.latest("20261013").seq()).isEqualTo(2);
    }
    @Test void completionReplayIsImmutableAndInputOrderCanonical() {
        NewsDailySaveRequest r=request("retry-123");service.save(r,7L);
        List<NewsDailyArticle> reversed=new ArrayList<>(r.articles());Collections.reverse(reversed);
        assertThat(service.save(new NewsDailySaveRequest(r.yyyymmdd(),r.requestId(),r.sourceGeneratedAt(),reversed),7L).seq()).isEqualTo(1);
        assertThatThrownBy(()->service.save(replace(r,0,new NewsDailyArticle("사회",1,"다른 내용")),7L))
            .isInstanceOf(NewsDailyConflictException.class);
        assertThat(service.status(r.yyyymmdd(),r.requestId(),7L).status()).isEqualTo("COMPLETED");
        assertThat(count("NEWS_DAILY")).isEqualTo(20);
        assertThat(jdbc.queryForObject("SELECT MIN(REG_NO) FROM news.NEWS_DAILY",Long.class)).isEqualTo(7L);
    }
    @Test void unicodeLengthBoundariesBlankAndMalformedSurrogates() {
        NewsDailySaveRequest r=request("unicode-1");
        for(String text:List.of("가".repeat(800),"가".repeat(799)+"\n","😀".repeat(800),"e\u0301".repeat(400))) {
            NewsDailySaveRequest valid=replace(r,0,new NewsDailyArticle("사회",1,text));
            assertThat(NewsDailyService.validate(valid).articles().getFirst().newsContent()).isEqualTo(text);
            assertThatThrownBy(()->NewsDailyService.validate(replace(r,0,new NewsDailyArticle("사회",1,text+"가"))))
                .isInstanceOf(NewsDailyValidationException.class)
                .satisfies(ex->assertThat(((NewsDailyValidationException)ex).details()).containsEntry("actualLength",801));
        }
        for(String blank:Arrays.asList(null,""," ","\n\t","\u00a0\u3000"))
            assertThatThrownBy(()->NewsDailyService.validate(replace(r,0,new NewsDailyArticle("사회",1,blank))))
                .isInstanceOf(NewsDailyValidationException.class);
        assertThatThrownBy(()->NewsDailyService.validate(replace(r,0,new NewsDailyArticle("사회",1,String.valueOf((char)0xd800)))))
            .isInstanceOf(NewsDailyValidationException.class);
        assertThat(service.save(replace(r,0,new NewsDailyArticle("사회",1,"가".repeat(800))),1L).articles().getFirst().newsContent()).hasSize(800);
    }
    @Test void categoryColumnLimitOrderAndDuplicateOrder() {
        NewsDailySaveRequest r=request("validation-1");
        assertThatThrownBy(()->NewsDailyService.validate(replace(r,0,new NewsDailyArticle("가".repeat(11),1,"내용"))))
            .isInstanceOf(NewsDailyValidationException.class)
            .satisfies(ex->assertThat(((NewsDailyValidationException)ex).details()).containsEntry("field","categoryNm")
                .containsEntry("maxLength",10).containsEntry("actualLength",11));
        for(NewsDailyArticle article:List.of(new NewsDailyArticle("정치",1,"내용"),new NewsDailyArticle(null,1,"내용"),new NewsDailyArticle("사회",2,"내용")))
            assertThatThrownBy(()->NewsDailyService.validate(replace(r,0,article))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->NewsDailyService.validate(replace(r,19,new NewsDailyArticle("스포츠",20,"내용"))))
            .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void legacyRowsNotPublishedAndCounterDoesNotCollide() {
        jdbc.update("INSERT INTO news.NEWS_DAILY(YYYYMMDD,SEQ,DISP_ORD,NEWS_CONTENT) VALUES('20261013',9,1,'legacy')");
        assertThat(service.latest("20261013").status()).isEqualTo("EMPTY");
        assertThat(service.start(attempt("legacy-123"),1L).seq()).isEqualTo(10);
    }
    @Test void corruptedLatestFallsBack() {
        service.save(request("complete-one"),1L);service.save(request("complete-two"),1L);
        jdbc.update("DELETE FROM news.NEWS_DAILY WHERE SEQ=2 AND DISP_ORD=20");
        assertThat(service.latest("20261013").seq()).isEqualTo(1);
    }
    @Test void concurrentFirstReservationsHaveDistinctSeq() throws Exception {
        assertThat(new HashSet<>(concurrent(false,true))).containsExactlyInAnyOrder(1,2,3,4,5,6);
        assertThat(count("NEWS_DAILY_BATCH")).isEqualTo(6);assertThat(count("NEWS_DAILY")).isZero();
    }
    @Test void concurrentStartRetriesReuseOneSeq() throws Exception {
        assertThat(concurrent(true,true)).containsOnly(1);assertThat(count("NEWS_DAILY_BATCH")).isEqualTo(1);
    }
    @Test void concurrentSaveRetriesPublishOnce() throws Exception {
        assertThat(concurrent(true,false)).containsOnly(1);assertThat(count("NEWS_DAILY")).isEqualTo(20);
    }
    @Test void concurrentDifferentSavesPublishDistinctAttempts() throws Exception {
        assertThat(new HashSet<>(concurrent(false,false))).containsExactlyInAnyOrder(1,2,3,4,5,6);
        assertThat(count("NEWS_DAILY")).isEqualTo(120);assertThat(service.latest("20261013").seq()).isEqualTo(6);
    }
    @Test void sameIdDifferentPayloadOneWinnerOneConflict() throws Exception {
        NewsDailySaveRequest a=request("same-conflict"),b=replace(a,0,new NewsDailyArticle("사회",1,"다른 내용"));
        ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
        try {
            List<Future<String>> futures=new ArrayList<>();
            for(NewsDailySaveRequest request:List.of(a,b)) futures.add(pool.submit(()->{
                start.await();try{service.save(request,1L);return "COMPLETED";}
                catch(NewsDailyConflictException ex){return "CONFLICT";}
            }));
            start.countDown();
            assertThat(List.of(futures.get(0).get(20,TimeUnit.SECONDS),futures.get(1).get(20,TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder("COMPLETED","CONFLICT");
            assertThat(count("NEWS_DAILY")).isEqualTo(20);
        } finally {pool.shutdownNow();pool.awaitTermination(5,TimeUnit.SECONDS);}
    }
    @Test void lostCommitResponseDoesNotMarkCompletedAttemptFailed() {
        PlatformTransactionManager delegate=context.getBean(PlatformTransactionManager.class);
        AtomicInteger commits=new AtomicInteger();
        PlatformTransactionManager uncertain=new PlatformTransactionManager(){
            public TransactionStatus getTransaction(TransactionDefinition d){return delegate.getTransaction(d);}
            public void rollback(TransactionStatus s){delegate.rollback(s);}
            public void commit(TransactionStatus s){
                delegate.commit(s);
                if(commits.incrementAndGet()==2)throw new TransactionSystemException("fixture lost commit response");
            }
        };
        NewsDailyService target=new NewsDailyService(context.getBean(NewsDailyMapper.class),new ObjectMapper(),uncertain);
        assertThatThrownBy(()->target.save(request("lost-response"),1L)).isInstanceOf(IllegalStateException.class);
        assertThat(service.status("20261013","lost-response",1L).status()).isEqualTo("COMPLETED");
        assertThat(service.save(request("lost-response"),1L).seq()).isEqualTo(1);
        assertThat(count("NEWS_DAILY")).isEqualTo(20);
    }
    @Test void uncommittedArticleTransactionLeavesVisiblePendingState() throws Exception {
        service.save(request("visible-1"),1L);service.start(attempt("visible-2"),1L);
        NewsDailyMapper mapper=context.getBean(NewsDailyMapper.class);
        ExecutorService pool=Executors.newSingleThreadExecutor();
        CountDownLatch ready=new CountDownLatch(1),release=new CountDownLatch(1);
        try {
            Future<?> writer=pool.submit(()->new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).execute(tx->{
                mapper.lockAttempt("20261013","visible-2");
                for(NewsDailyArticle a:request("visible-2").articles())mapper.insertArticle("20261013",2,a,1L);
                mapper.complete("20261013","visible-2","fixture",20);ready.countDown();
                try{if(!release.await(10,TimeUnit.SECONDS))throw new IllegalStateException();}
                catch(InterruptedException ex){Thread.currentThread().interrupt();throw new IllegalStateException();}
                return null;
            }));
            assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue();
            assertThat(service.latest("20261013").seq()).isEqualTo(1);
            assertThat(service.status("20261013","visible-2",1L).status()).isEqualTo("PENDING");
            release.countDown();writer.get(10,TimeUnit.SECONDS);
            assertThat(service.latest("20261013").seq()).isEqualTo(2);
        } finally{release.countDown();pool.shutdownNow();pool.awaitTermination(5,TimeUnit.SECONDS);}
    }
    @Test void explicitFailureRacingSaveHasExactlyOneTerminalOutcome() throws Exception {
        service.start(attempt("race-terminal"),1L);
        ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch start=new CountDownLatch(1);
        try {
            Future<String> save=pool.submit(()->{start.await();try{return service.save(request("race-terminal"),1L).status();}
                catch(NewsDailyConflictException ex){return "CONFLICT";}});
            Future<String> fail=pool.submit(()->{start.await();try{return service.fail(new NewsDailyFailRequest("20261013","race-terminal","CANCELLED"),1L).status();}
                catch(NewsDailyConflictException ex){return "CONFLICT";}});
            start.countDown(); List<String> outcomes=List.of(save.get(20,TimeUnit.SECONDS),fail.get(20,TimeUnit.SECONDS));
            String state=service.status("20261013","race-terminal",1L).status();
            assertThat(outcomes).containsExactlyInAnyOrder(state,"CONFLICT");
            assertThat(count("NEWS_DAILY")).isEqualTo("COMPLETED".equals(state)?20:0);
            assertThat(count("NEWS_DAILY_BATCH")).isEqualTo(1);
        } finally{pool.shutdownNow();pool.awaitTermination(5,TimeUnit.SECONDS);}
    }
    @Test void invalidAttemptMetadataDoesNotAllocateAndCategoryTenIsNotAnOverflow() {
        assertThatThrownBy(()->service.start(new NewsDailyAttemptRequest("20261013","valid-key","2026-02-30 09:00:00"),1L))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(count("NEWS_DAILY_BATCH")).isZero();
        assertThatThrownBy(()->NewsDailyService.validate(replace(request("category-ten"),0,new NewsDailyArticle("가".repeat(10),1,"내용"))))
            .isInstanceOf(IllegalArgumentException.class).isNotInstanceOf(NewsDailyValidationException.class);
    }
    private List<Integer> concurrent(boolean same,boolean reserve) throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(6);CountDownLatch start=new CountDownLatch(1);
        try{
            List<Future<Integer>> futures=new ArrayList<>();
            for(int i=0;i<6;i++){String id=same?"same-request":"different-"+i;
                futures.add(pool.submit(()->{start.await();return (reserve?service.start(attempt(id),1L):service.save(request(id),1L)).seq();}));}
            start.countDown();List<Integer> result=new ArrayList<>();
            for(Future<Integer> future:futures)result.add(future.get(25,TimeUnit.SECONDS));
            return result;
        }finally{pool.shutdownNow();pool.awaitTermination(5,TimeUnit.SECONDS);}
    }
}
