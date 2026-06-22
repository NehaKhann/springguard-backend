package com.springguard.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springguard.model.Finding;
import com.springguard.model.Severity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Second-pass reviewer. Sends the code to an LLM to catch context-dependent issues
 * that pattern rules miss (missing authorization, unsafe input flow, logic flaws).
 * Best-effort: if no API key is set or the call fails, it returns no findings and
 * the rule-based scan is unaffected.
 */
@Service
public class AiReviewService {

    private static final String SYSTEM_PROMPT = """
        You are a senior Spring Boot security reviewer. Review the user's code for SECURITY issues
        that simple pattern rules typically miss: missing authorization or ownership checks (IDOR),
        user input reaching sensitive operations, authentication bypasses, and unsafe logic.
        Do NOT report generic config issues like disabled CSRF, wildcard CORS, or hardcoded secrets —
        those are handled separately. Respond ONLY as a json object of the form
        {"findings":[{"title":"...","why":"...","fix":"...","severity":"HIGH|MEDIUM|LOW"}]}.
        If you find nothing, return {"findings":[]}. Keep each field to one or two sentences.
        """;

    private static final int MAX_CHARS = 8000;

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final ObjectMapper mapper;
    private final RestClient http;

    public AiReviewService(
            @Value("${ai.api-key:}") String apiKey,
            @Value("${ai.model:llama-3.3-70b-versatile}") String model,
            @Value("${ai.base-url:https://api.groq.com/openai/v1}") String baseUrl,
            ObjectMapper mapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.mapper = mapper;

        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(8000);
        rf.setReadTimeout(20000);
        this.http = RestClient.builder().requestFactory(rf).build();
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public List<Finding> review(String code) {
        if (!isEnabled() || code == null || code.isBlank()) {
            return List.of();
        }
        String snippet = code.length() > MAX_CHARS ? code.substring(0, MAX_CHARS) : code;

        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "temperature", 0.2,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", snippet)
                    )
            );

            String response = http.post()
                    .uri(baseUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return parse(response);
        } catch (Exception e) {
            // Never break the scan if the AI call fails.
            return List.of();
        }
    }

    private List<Finding> parse(String response) throws Exception {
        JsonNode root = mapper.readTree(response);
        String content = root.path("choices").path(0).path("message").path("content").asText("");
        if (content.isBlank()) return List.of();

        JsonNode parsed = mapper.readTree(content);
        JsonNode arr = parsed.path("findings");
        List<Finding> out = new ArrayList<>();
        if (arr.isArray()) {
            int i = 1;
            for (JsonNode n : arr) {
                String title = n.path("title").asText("").trim();
                if (title.isEmpty()) continue;
                String why = n.path("why").asText("");
                String fix = n.path("fix").asText("");
                Severity sev = parseSeverity(n.path("severity").asText("MEDIUM"));
                out.add(new Finding("ai-" + (i++), sev, title, why, fix, "AI"));
            }
        }
        return out;
    }

    private Severity parseSeverity(String s) {
        if (s == null) return Severity.MEDIUM;
        return switch (s.trim().toUpperCase()) {
            case "HIGH" -> Severity.HIGH;
            case "LOW" -> Severity.LOW;
            default -> Severity.MEDIUM;
        };
    }
}
