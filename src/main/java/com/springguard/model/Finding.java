package com.springguard.model;

/** One detected security issue. {@code source} is "RULE" (deterministic) or "AI" (reviewer). */
public final class Finding {
    public final String id;
    public final Severity severity;
    public final String title;
    public final String why;
    public final String fix;
    public final String source;

    public Finding(String id, Severity severity, String title, String why, String fix) {
        this(id, severity, title, why, fix, "RULE");
    }

    public Finding(String id, Severity severity, String title, String why, String fix, String source) {
        this.id = id;
        this.severity = severity;
        this.title = title;
        this.why = why;
        this.fix = fix;
        this.source = source;
    }

    public String id() { return id; }
    public Severity severity() { return severity; }
    public String title() { return title; }
    public String why() { return why; }
    public String fix() { return fix; }
    public String source() { return source; }
}
