package com.xodud1202.springbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.HttpURLConnection;
import java.io.InputStream;

@Service
public class NewsSourceService {
    private static final String BASE = "https://be.xodud1202.kro.kr/api/last/news/";
    private static final int MAX_BYTES = 4 * 1024 * 1024;
    private final ObjectMapper json;
    public NewsSourceService(ObjectMapper json) { this.json = json; }

    public JsonNode read(String pressId, String expectedGeneratedAt) {
        JsonNode meta = fetch("meta.json");
        String generated = generation(meta);
        if (expectedGeneratedAt != null && !expectedGeneratedAt.equals(generated))
            throw new NewsDailyConflictException("원본이 교체되었습니다. meta부터 다시 조회하세요.");
        if (pressId == null) return meta;
        if (!pressId.matches("[0-9]{1,18}") || expectedGeneratedAt == null)
            throw new IllegalArgumentException("기사 조회에는 pressId 및 expectedGeneratedAt이 필요합니다.");
        String file = meta.path("articleFileByPressId").path(pressId).asText();
        if (!file.equals("articles/press-" + pressId + ".json"))
            throw new IllegalArgumentException("meta에 없는 언론사 또는 허용되지 않은 shard 경로입니다.");
        JsonNode shard = fetch(file);
        if (!generated.equals(generation(shard)) || !generated.equals(generation(fetch("meta.json"))))
            throw new NewsDailyConflictException("원본 교체 중입니다. meta부터 다시 조회하세요.");
        return shard;
    }

    private static String generation(JsonNode node) {
        String value = node.path("meta").path("generatedAt").asText("");
        if (value.isBlank()) throw new IllegalStateException("원본 generatedAt이 없습니다.");
        return value;
    }

    // Fixed HTTPS origin, no redirects or arbitrary URL input; timeout and body size are bounded.
    protected JsonNode fetch(String relative) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(BASE + relative).toURL().openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept", "application/json");
            if (connection.getResponseCode() != 200) throw new IllegalStateException("원본 JSON 조회 실패");
            try (InputStream input = connection.getInputStream()) {
                byte[] body = input.readNBytes(MAX_BYTES + 1);
                if (body.length > MAX_BYTES) throw new IllegalStateException("원본 JSON 크기 제한 초과");
                return json.readTree(body);
            }
        } catch (Exception ex) { throw new IllegalStateException("원본 JSON을 안전하게 조회하지 못했습니다.", ex); }
        finally { if (connection != null) connection.disconnect(); }
    }
}

