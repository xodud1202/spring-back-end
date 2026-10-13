package com.xodud1202.springbackend.controller.news;

import com.fasterxml.jackson.databind.JsonNode;
import com.xodud1202.springbackend.domain.news.*;
import com.xodud1202.springbackend.entity.UserBaseEntity;
import com.xodud1202.springbackend.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "news.daily.enabled", havingValue = "true")
@RequestMapping("/api/news/daily")
public class NewsDailyController {
    private final NewsDailyService daily;
    private final NewsSourceService source;

    @PostMapping("/batches/start")
    public NewsDailyResult start(@RequestBody NewsDailyAttemptRequest request, Authentication authentication) {
        return daily.start(request, actor(authentication));
    }

    @PostMapping("/batches/fail")
    public NewsDailyResult fail(@RequestBody NewsDailyFailRequest request, Authentication authentication) {
        return daily.fail(request, actor(authentication));
    }

    @ExceptionHandler(NewsDailyValidationException.class)
    public ResponseEntity<?> validation(NewsDailyValidationException ex) {
        return ResponseEntity.badRequest().body(ex.details());
    }
    @PostMapping("/batches")
    public NewsDailyResult save(@RequestBody NewsDailySaveRequest request, Authentication authentication) {
        return daily.save(request, actor(authentication));
    }

    @GetMapping("/batches/status")
    public NewsDailyResult status(@RequestParam String yyyymmdd, @RequestParam String requestId,
                                  Authentication authentication) {
        return daily.status(yyyymmdd, requestId, actor(authentication));
    }

    @GetMapping("/latest")
    public NewsDailyResult latest(@RequestParam String yyyymmdd) { return daily.latest(yyyymmdd); }

    @GetMapping("/source")
    public JsonNode source(@RequestParam(required = false) String pressId,
                           @RequestParam(required = false) String expectedGeneratedAt,
                           Authentication authentication) {
        actor(authentication);
        return source.read(pressId, expectedGeneratedAt);
    }

    static Long actor(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserBaseEntity user) || user.getUsrNo() == null)
            throw new SecurityException("기존 관리자 Bearer JWT가 필요합니다.");
        return ((UserBaseEntity) authentication.getPrincipal()).getUsrNo();
    }

    @ExceptionHandler(NewsDailyConflictException.class)
    public ResponseEntity<?> conflict(NewsDailyConflictException ex) {
        return ResponseEntity.status(409).body(Map.of("message", ex.getMessage()));
    }
}


