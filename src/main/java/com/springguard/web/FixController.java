package com.springguard.web;

import com.springguard.core.AiReviewService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class FixController {

    private final AiReviewService ai;

    public FixController(AiReviewService ai) {
        this.ai = ai;
    }

    @PostMapping("/fix")
    public FixResponse fix(@RequestBody FixRequest request) {
        if (!ai.isEnabled()) {
            return new FixResponse("AI_OFF", "AI auto-fix isn't available right now.", null);
        }
        String fixed = ai.fix(request.code());
        if (fixed == null || fixed.isBlank()) {
            return new FixResponse("ERROR", "Couldn't generate a fix. Please try again.", null);
        }
        return new FixResponse("OK", null, fixed);
    }
}
