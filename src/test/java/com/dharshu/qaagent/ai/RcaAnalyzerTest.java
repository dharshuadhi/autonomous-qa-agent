package com.dharshu.qaagent.ai;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the RCA confidence parsing. The LLM itself is stubbed out --
 * these pin down how the analyzer interprets the model's reply, without
 * spending an API call.
 */
public class RcaAnalyzerTest {

    private static RcaAnalyzer analyzerWithReply(String reply) {
        LlmClient stub = new LlmClient("test-key") {
            @Override
            public String complete(String prompt) {
                return reply;
            }
        };
        return new RcaAnalyzer(stub);
    }

    @Test
    public void highConfidenceIsParsed() {
        RcaAnalyzer analyzer = analyzerWithReply(
                "The login button was covered by a cookie banner, so the click never landed.\n"
                        + "Confidence: High");
        RcaAnalyzer.RcaResult result = analyzer.analyze("Login", "ElementClickInterceptedException", "<html/>");
        assertEquals(RcaAnalyzer.Confidence.HIGH, result.confidence);
        assertTrue(result.explanation.contains("cookie banner"));
    }

    @Test
    public void mediumConfidenceIsParsedCaseInsensitively() {
        RcaAnalyzer analyzer = analyzerWithReply(
                "The page may have loaded slowly and the wait was too short.\nconfidence: medium");
        RcaAnalyzer.RcaResult result = analyzer.analyze("Checkout", "TimeoutException", "<html/>");
        assertEquals(RcaAnalyzer.Confidence.MEDIUM, result.confidence);
    }

    @Test
    public void lowConfidenceIsParsed() {
        RcaAnalyzer analyzer = analyzerWithReply(
                "Not enough information to say for sure.\nConfidence: Low");
        RcaAnalyzer.RcaResult result = analyzer.analyze("Cart", "AssertionError", "<html/>");
        assertEquals(RcaAnalyzer.Confidence.LOW, result.confidence);
    }

    @Test
    public void missingConfidenceLineYieldsUnknown() {
        RcaAnalyzer analyzer = analyzerWithReply("Something broke, details unclear.");
        RcaAnalyzer.RcaResult result = analyzer.analyze("Cart", "AssertionError", "<html/>");
        assertEquals(RcaAnalyzer.Confidence.UNKNOWN, result.confidence);
    }

    @Test
    public void longHtmlIsTruncatedBeforePrompting() {
        StringBuilder html = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            html.append('x');
        }
        final String[] seenPrompt = new String[1];
        LlmClient capturing = new LlmClient("test-key") {
            @Override
            public String complete(String prompt) {
                seenPrompt[0] = prompt;
                return "Confidence: Low";
            }
        };
        new RcaAnalyzer(capturing).analyze("Cart", "boom", html.toString());
        assertTrue("Prompt should stay bounded, was " + seenPrompt[0].length(),
                seenPrompt[0].length() < 6000);
    }
}
