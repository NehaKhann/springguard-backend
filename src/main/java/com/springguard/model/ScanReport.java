package com.springguard.model;

import java.util.List;

/** The graded result: a score, a letter grade, a one-line summary, and the findings. */
public final class ScanReport {
    public final int score;
    public final String grade;
    public final String summary;
    public final List<Finding> findings;

    public ScanReport(int score, String grade, String summary, List<Finding> findings) {
        this.score = score;
        this.grade = grade;
        this.summary = summary;
        this.findings = findings;
    }

    public int score() { return score; }
    public String grade() { return grade; }
    public String summary() { return summary; }
    public List<Finding> findings() { return findings; }
}
