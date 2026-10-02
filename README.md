# Autonomous QA Agent

![CI](https://github.com/dharshuadhi/autonomous-qa-agent/actions/workflows/ci.yml/badge.svg)

An AI-augmented test automation agent: it runs a Java/Selenium/Cucumber suite
against [SauceDemo](https://www.saucedemo.com), **heals its own locators**
when the UI changes, **explains failures in plain English** using an LLM, and
**automatically files a bug** in Azure DevOps when something's genuinely
broken.

## Why this exists

Most test automation portfolios stop at "here's a Selenium framework."
The two most expensive parts of real automation work are locators breaking
on every UI change, and triaging *why* a test failed before anyone can act on
it. This project automates both.

## Architecture

```
Ticket / Story --> Test Generation (LLM) --> Self-Healing Execution --> Tests pass?
                                                                          |     \
                                                                         yes    no
                                                                          |      \
                                                                       Passed   AI Root Cause Analysis
                                                                                        |
                                                                                 Auto Bug Filing (Azure DevOps)
```

## Tech stack

| Layer | Choice |
| --- | --- |
| Test execution | Java 17, Selenium 4, Cucumber, JUnit 4 |
| Build / CI | Maven, GitHub Actions |
| Self-healing | Custom `SmartLocator` (primary locator + fallback chain) |
| AI layer | LLM API (free tier available, no credit card) -- test generation + root cause analysis |
| Bug filing | Azure DevOps REST API (Work Items) |
| Reporting | Allure |

## Running it locally

1. Install Java 17, Maven, and Google Chrome.
2. Copy `.env.example` to `.env`, fill in your own `LLM_API_KEY` (see
   `.env.example` for where to get one) and, if you want bug filing to work,
   `ADO_ORG` / `ADO_PROJECT` / `ADO_PAT`.
   Then export them: `export $(cat .env | xargs)` (Mac/Linux).
3. Run the suite:
   ```
   mvn test
   ```
4. View the report:
   ```
   allure serve target/allure-results
   ```

Without an `LLM_API_KEY` set, the suite still runs and passes/fails normally
— it just skips the AI root cause analysis and bug filing steps on failure,
and logs that it skipped them.

## Generating new test scenarios with AI

```
mvn compile exec:java \
  -Dexec.mainClass=com.dharshu.qaagent.ai.TestGenerator \
  -Dexec.args="requirements/checkout.txt src/test/resources/features/checkout_generated.feature"
```

Review the generated file before moving any scenarios into the real suite.

## Self-healing, demonstrated

`SmartLocator` tries a primary locator first, then an ordered list of
fallbacks (CSS attribute, then structural/XPath match). Every resolution is
logged and counted by `HealingReport`, printed at the end of each run:

```
==== Self-Healing Summary ====
Total lookups: 42
Resolved via primary locator: 38
Self-healed via fallback: 4
Failed (no locator worked): 0
===============================
```

## Project structure

```
src/main/java/com/dharshu/qaagent/
├── core/            SmartLocator, HealingReport, DriverManager
├── ai/               LlmClient, TestGenerator, RcaAnalyzer
└── integrations/    AzureDevOpsClient

src/test/java/com/dharshu/qaagent/
├── runners/         Cucumber TestRunner
├── steps/           Step definitions + page objects usage
└── hooks/           Wires RCA + bug filing into @After

src/test/resources/features/   Gherkin feature files
requirements/                  Plain-English requirements fed to TestGenerator
.github/workflows/ci.yml       CI pipeline
```

## Results

- [x] Self-healed 1 of 1 intentionally broken locators (verified with real headless Chrome against live SauceDemo -- broken primary healed via fallback, `SelfHealingVerificationTest`)
- [x] 13/13 unit tests pass (`HealingReportTest`, `SmartLocatorBuilderTest`, `RcaAnalyzerTest`)
- [ ] AI-generated scenario caught a real edge case the hand-written suite missed
- [ ] A real Azure DevOps work item was auto-created from a real failure

*(The last two need `LLM_API_KEY` / `ADO_*` secrets -- they run in CI once those are configured.)*
