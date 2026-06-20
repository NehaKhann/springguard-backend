package com.springguard.core;

import com.springguard.model.ScanReport;
import org.springframework.stereotype.Service;

/** Application service. Holds the engine and exposes a single scan operation. */
@Service
public class ScanService {

    private final RuleEngine engine = new RuleEngine(Rules.ALL);

    public ScanReport scan(String code) {
        return engine.scan(code);
    }
}
