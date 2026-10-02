package com.dharshu.qaagent.core;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tracks, per test run, how many elements resolved via their primary locator
 * versus a fallback ("healed"), and how many failed outright. Printed at the
 * end of the run so the README's self-healing numbers come from a real count,
 * not an estimate.
 */
public final class HealingReport {

    private static final AtomicInteger primaryHits = new AtomicInteger(0);
    private static final AtomicInteger healedHits = new AtomicInteger(0);
    private static final AtomicInteger failures = new AtomicInteger(0);
    private static final CopyOnWriteArrayList<String> healEvents = new CopyOnWriteArrayList<>();

    private HealingReport() {
    }

    /** Resets all counters. Package-visible for unit tests. */
    static void reset() {
        primaryHits.set(0);
        healedHits.set(0);
        failures.set(0);
        healEvents.clear();
    }

    static void recordPrimary(String elementName) {
        primaryHits.incrementAndGet();
    }

    static void recordHealed(String elementName, String fallbackUsed) {
        healedHits.incrementAndGet();
        String event = elementName + " -> healed via " + fallbackUsed;
        healEvents.add(event);
        System.out.println("[SELF-HEALED] " + event);
    }

    static void recordFailure(String elementName) {
        failures.incrementAndGet();
    }

    public static void printSummary() {
        int total = primaryHits.get() + healedHits.get() + failures.get();
        System.out.println("==== Self-Healing Summary ====");
        System.out.println("Total lookups: " + total);
        System.out.println("Resolved via primary locator: " + primaryHits.get());
        System.out.println("Self-healed via fallback: " + healedHits.get());
        System.out.println("Failed (no locator worked): " + failures.get());
        healEvents.forEach(e -> System.out.println("  - " + e));
        System.out.println("===============================");
    }

    public static int getHealedCount() {
        return healedHits.get();
    }
}
