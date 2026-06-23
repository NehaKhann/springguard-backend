package com.springguard.web;

import com.springguard.core.AiReviewService;
import com.springguard.github.GitHubClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class FixController {

    private final AiReviewService ai;
    private final GitHubClient github;

    public FixController(AiReviewService ai, GitHubClient github) {
        this.ai = ai;
        this.github = github;
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

    @PostMapping("/fix-repo-file")
    public RepoFixResponse fixRepoFile(@RequestBody RepoFixRequest request) {
        if (!ai.isEnabled()) {
            return new RepoFixResponse("AI_OFF", "AI auto-fix isn't available right now.", request.path(), null, null);
        }
        GitHubClient.Repo repo = github.parse(request.repoUrl());
        if (repo == null) {
            return new RepoFixResponse("ERROR", "Invalid repository URL.", request.path(), null, null);
        }
        try {
            String branch = (request.branch() != null && !request.branch().isBlank())
                    ? request.branch().trim()
                    : github.defaultBranch(repo, request.token());

            String original = github.fetchFile(repo, branch, request.path(), request.token());
            if (original == null || original.isBlank()) {
                return new RepoFixResponse("ERROR", "Could not fetch that file from GitHub.", request.path(), null, null);
            }
            String fixed = ai.fix(original);
            if (fixed == null || fixed.isBlank()) {
                return new RepoFixResponse("ERROR", "Couldn't generate a fix. Please try again.", request.path(), null, null);
            }
            return new RepoFixResponse("OK", null, request.path(), original, fixed);
        } catch (Exception e) {
            return new RepoFixResponse("ERROR", "Could not fix that file. Please try again.", request.path(), null, null);
        }
    }
}
