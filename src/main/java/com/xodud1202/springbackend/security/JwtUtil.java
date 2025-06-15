package com.xodud1202.springbackend.security;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class JwtUtil {
    private static final String SECRET_KEY = "test-secret-key";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String generateToken(String subject) {
        try {
            String headerJson = objectMapper.writeValueAsString(Map.of("alg", "HS256", "typ", "JWT"));
            String header = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));

            long now = System.currentTimeMillis() / 1000L;
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("sub", subject);
            payloadMap.put("iat", now);
            payloadMap.put("exp", now + 3600); // 1 hour expiry
            String payloadJson = objectMapper.writeValueAsString(payloadMap);
            String payload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            String signature = hmacSha256(header + "." + payload, SECRET_KEY);
            return header + "." + payload + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String validateTokenAndGetSubject(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            String header = parts[0];
            String payload = parts[1];
            String signature = parts[2];
            if (!hmacSha256(header + "." + payload, SECRET_KEY).equals(signature)) {
                return null;
            }
            String json = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
            Map<?, ?> map = objectMapper.readValue(json, Map.class);
            long exp = ((Number) map.get("exp")).longValue();
            long now = System.currentTimeMillis() / 1000L;
            if (now > exp) {
                return null;
            }
            return (String) map.get("sub");
        } catch (Exception e) {
            return null;
        }
    }

    private static String hmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}