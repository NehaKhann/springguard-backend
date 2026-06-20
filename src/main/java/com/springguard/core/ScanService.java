package com.springguard.core;

import com.springguard.model.ScanReport;
import org.springframework.stereotype.Service;

import java.util.List;

/** Application service. Validates the input, then runs the rule engine. */
@Service
public class ScanService {

    private static final int MAX_CHARS = 100_000;

    private final RuleEngine engine = new RuleEngine(Rules.ALL);

    public ScanReport scan(String code) {
        if (code == null || code.isBlank()) {
            return new ScanReport(0, "\u2014", "", List.of(),
                    "EMPTY", "Paste some Spring Boot code to scan.");
        }
        if (code.length() > MAX_CHARS) {
            return new ScanReport(0, "\u2014", "", List.of(),
                    "TOO_LARGE", "That's a lot of code. Paste a single config or controller file (under ~100k characters).");
        }
        if (!InputClassifier.looksLikeSpringOrJava(code)) {
            return new ScanReport(0, "\u2014", "", List.of(),
                    "NOT_SPRING", "This doesn't look like Spring Boot or Java code. SpringGuard is built specifically for Spring Boot projects.");
        }
        return engine.scan(code);
    }
}
