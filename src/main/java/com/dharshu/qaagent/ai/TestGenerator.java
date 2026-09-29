package com.dharshu.qaagent.ai;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Turns a short plain-English requirement into draft Gherkin scenarios.
 * Output is written to a *_generated.feature file, kept separate from
 * hand-written features so a reviewer can see what the AI drafted before
 * it's folded into the real suite (see Step 7 of the build guide).
 *
 * Run directly: mvn compile exec:java
 *   -Dexec.mainClass=com.dharshu.qaagent.ai.TestGenerator
 *   -Dexec.args="requirements/checkout.txt src/test/resources/features/checkout_generated.feature"
 */
public class TestGenerator {

    private static final String PROMPT_TEMPLATE = """
            You write Cucumber/Gherkin feature files for a Selenium test suite \
            covering the SauceDemo practice site (https://www.saucedemo.com).

            Given this requirement:
            "%s"

            Write Gherkin scenarios that cover:
            1. The normal / happy-path case
            2. At least one realistic edge case
            3. At least one case that should fail validation (negative path)

            Match this existing style (Given/When/Then, one Feature block):

            Feature: Checkout
              Scenario: <name>
                Given ...
                When ...
                Then ...

            Reply with ONLY the Gherkin text, no explanation before or after.
            """;

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: TestGenerator <requirement-file> <output-feature-file>");
            System.exit(1);
        }

        String requirement = Files.readString(Path.of(args[0])).strip();
        LlmClient llm = new LlmClient();
        String generated = llm.complete(PROMPT_TEMPLATE.formatted(requirement));

        Files.writeString(Path.of(args[1]), generated);
        System.out.println("Wrote generated scenarios to " + args[1]);
        System.out.println("Review them, then move the good ones into the real feature file.");
    }
}
