package com.dharshu.qaagent.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Thin wrapper around the Anthropic Messages API. Reads the API key from the
 * LLM_API_KEY environment variable (set as a GitHub Actions secret in CI, or
 * exported locally) -- the key is never hardcoded or committed.
 *
 * Reused by:
 *  - TestGenerator (Step 7: requirement text -> Gherkin scenarios)
 *  - RcaAnalyzer   (Step 8: failure details -> plain-English root cause)
 */
public class LlmClient {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-sonnet-4-5";

    private final OkHttpClient http;
    private final String apiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public LlmClient() {
        this(System.getenv("LLM_API_KEY"));
    }

    public LlmClient(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "LLM_API_KEY is not set. Export it locally or add it as a GitHub Actions secret.");
        }
        this.apiKey = apiKey;
        this.http = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Sends a single user-turn prompt and returns the model's plain-text reply.
     */
    public String complete(String prompt) {
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", MODEL);
            body.put("max_tokens", 1500);

            ArrayNode messages = body.putArray("messages");
            ObjectNode userMessage = messages.addObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);

            RequestBody requestBody = RequestBody.create(
                    mapper.writeValueAsString(body), MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(API_URL)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("content-type", "application/json")
                    .post(requestBody)
                    .build();

            try (Response response = http.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new IOException("LLM API call failed: HTTP " + response.code());
                }
                JsonNode json = mapper.readTree(response.body().string());
                return json.path("content").get(0).path("text").asText();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to call the LLM API", e);
        }
    }
}
