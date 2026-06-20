package com.springguard.core;

import com.springguard.model.Finding;
import com.springguard.model.ScanReport;
import com.springguard.model.Severity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Runs every rule against the submitted code, then turns the findings into a
 * 0-100 score and an A-F grade. This is the heart of Level 1.
 */
public class RuleEngine {

    private final List<SecurityRule> rules;

    public RuleEngine(List<SecurityRule> rules) {
        this.rules = rules;
    }

    public ScanReport scan(String code) {
        String safe = code == null ? "" : code;

        List<Finding> findings = new ArrayList<>();
        for (SecurityRule rule : rules) {
            if (rule.matches(safe)) {
                findings.add(new Finding(rule.id(), rule.severity(), rule.title(), rule.why(), rule.fix()));
            }
        }

        // Highest severity first so the report reads worst-to-least.
        findings.sort(Comparator.comparingInt(f -> f.severity().ordinal()));

        int score = computeScore(findings);
        String grade = grade(score);
        String summary = summarise(findings, grade);

        return new ScanReport(score, grade, summary, findings);
    }

    private int computeScore(List<Finding> findings) {
        int score = 100;
        for (Finding f : findings) {
            score -= f.severity().weight();
        }
        return Math.max(0, score);
    }

    private String grade(int score) {
        if (score >= 90) return "A";
        if (score >= 75) return "B";
        if (score >= 60) return "C";
        if (score >= 40) return "D";
        return "F";
    }

    private String summarise(List<Finding> findings, String grade) {
        if (findings.isEmpty()) {
            return "No known issues found in this snippet. Grade " + grade + ".";
        }
        long high = findings.stream().filter(f -> f.severity() == Severity.HIGH).count();
        long medium = findings.stream().filter(f -> f.severity() == Severity.MEDIUM).count();
        long low = findings.stream().filter(f -> f.severity() == Severity.LOW).count();
        return "Grade " + grade + " \u2014 found " + findings.size() + " issue(s): "
                + high + " high, " + medium + " medium, " + low + " low.";
    }
}
