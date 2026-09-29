package com.dharshu.qaagent.ai;

public class RcaAnalyzer {

    public enum Confidence { HIGH, MEDIUM, LOW, UNKNOWN }

    public static class RcaResult {
        public final String explanation;
        public final Confidence confidence;

        public RcaResult(String explanation, Confidence confidence) {
            this.explanation = explanation;
            this.confidence = confidence;
        }
    }

    private static final String PROMPT_TEMPLATE = """
            A Selenium/Cucumber test just failed. Explain the likely cause in ONE short \
            plain-English paragraph a non-technical reviewer could understand, then on a \
            new line write "Confidence: High" or "Confidence: Medium" or "Confidence: Low".

            Scenario: %s

            Error:
            %s

            Page HTML near the point of failure (truncated):
            %s
            """;

    private final LlmClient llm;

    public RcaAnalyzer() {
        this.llm = new LlmClient();
    }

    public RcaAnalyzer(LlmClient llm) {
        this.llm = llm;
    }

    public RcaResult analyze(String scenarioName, String stackTrace, String htmlSnippet) {
        String trimmedHtml = htmlSnippet.length() > 4000 ? htmlSnippet.substring(0, 4000) : htmlSnippet;
        String prompt = PROMPT_TEMPLATE.formatted(scenarioName, stackTrace, trimmedHtml);
        String raw = llm.complete(prompt);

        Confidence confidence = Confidence.UNKNOWN;
        String lower = raw.toLowerCase();
        if (lower.contains("confidence: high")) confidence = Confidence.HIGH;
        else if (lower.contains("confidence: medium")) confidence = Confidence.MEDIUM;
        else if (lower.contains("confidence: low")) confidence = Confidence.LOW;

        return new RcaResult(raw, confidence);
    }
}
