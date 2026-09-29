package com.dharshu.qaagent.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Thin wrapper around an LLM API. Reads the API key from the LLM_API_KEY
 * environment variable (set as a GitHub Actions secret in CI, or exported
 * locally) -- the key is never hardcoded or committed.
 *
 * Reused by:
 *  - TestGenerator (Step 7: requirement text -> Gherkin scenarios)
 *  - RcaAnalyzer   (Step 8: failure details -> plain-English root cause)
 */
public class LlmClient {

    private static final String MODEL = "gemini-2.5-flash";
    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent";

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
     * Sends a single prompt and returns the model's plain-text reply.
     */
    public String complete(String prompt) {
        try {
            ObjectNode body = mapper.createObjectNode();
            ArrayNode contents = body.putArray("contents");
            ObjectNode content = contents.addObject();
            ArrayNode parts = content.putArray("parts");
            parts.addObject().put("text", prompt);

            RequestBody requestBody = RequestBody.create(
                    mapper.writeValueAsString(body), MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(API_URL)
                    .header("x-goog-api-key", apiKey)
                    .header("content-type", "application/json")
                    .post(requestBody)
                    .build();

            try (Response response = http.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new IOException("LLM API call failed: HTTP " + response.code());
                }
                JsonNode json = mapper.readTree(response.body().string());
                return json.path("candidates").get(0)
                        .path("content").path("parts").get(0)
                        .path("text").asText();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to call the LLM API", e);
        }
    }
}
