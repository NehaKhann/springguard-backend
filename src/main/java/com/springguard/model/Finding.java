package com.springguard.model;

/** One detected security issue, with a plain-language reason and a concrete fix. */
public final class Finding {
    public final String id;
    public final Severity severity;
    public final String title;
    public final String why;
    public final String fix;

    public Finding(String id, Severity severity, String title, String why, String fix) {
        this.id = id;
        this.severity = severity;
        this.title = title;
        this.why = why;
        this.fix = fix;
    }

    public String id() { return id; }
    public Severity severity() { return severity; }
    public String title() { return title; }
    public String why() { return why; }
    public String fix() { return fix; }
}
