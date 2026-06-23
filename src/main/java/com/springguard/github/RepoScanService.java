package com.springguard.github;

import com.springguard.core.RuleEngine;
import com.springguard.core.Rules;
import com.springguard.model.Finding;
import com.springguard.model.RepoScanReport;
import com.springguard.model.ScanReport;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;

@Service
public class RepoScanService {

    private static final int MAX_FILE_BYTES = GitHubClient.MAX_FILE_BYTES;

    private final GitHubClient github;
    private final RuleEngine engine = new RuleEngine(Rules.ALL);

    public RepoScanService(GitHubClient github) {
        this.github = github;
    }

    public RepoScanReport scan(String repoUrl, String token, String requestedBranch) {
        GitHubClient.Repo repo = github.parse(repoUrl);
        if (repo == null) {
            return msg("BAD_URL", "Enter a GitHub repo URL like https://github.com/owner/repo.", null);
        }
        String target = repo.owner() + "/" + repo.name();

        try {
            String defaultBranch = github.defaultBranch(repo, token); // also validates the repo exists

            // Branch precedence: explicit field > branch in URL > repo default.
            String chosen = (requestedBranch != null && !requestedBranch.isBlank())
                    ? requestedBranch.trim()
                    : (repo.branch() != null && !repo.branch().isBlank() ? repo.branch() : null);

            String branch;   // the branch name (used for fetching file contents)
            String treeish;  // what we hand to the trees API (default name, or a validated SHA)

            if (chosen == null) {
                // Default-branch scan: lean path, no extra branch-list call.
                branch = defaultBranch;
                treeish = defaultBranch;
            } else {
                // Explicit branch: validate it genuinely exists, then list by its commit SHA.
                String sha = github.branchSha(repo, chosen, token);
                if (sha == null) {
                    return msg("BRANCH_NOT_FOUND",
                            "Branch '" + chosen + "' was not found in " + target + ".", target);
                }
                branch = chosen;
                treeish = sha;
            }

            List<String> files = github.listRelevantFiles(repo, treeish, token);
            if (files.isEmpty()) {
                return new RepoScanReport(0, "\u2014", "", List.of(),
                        "NO_FILES", "No Java or Spring config files found on branch '" + branch + "'.", 0, target);
            }

            List<Finding> all = new ArrayList<>();
            int scanned = 0;
            for (String path : files) {
                String content;
                try {
                    content = github.fetchFile(repo, branch, path, token);
                } catch (Exception e) {
                    continue;
                }
                if (content == null || content.isBlank()) continue;
                if (content.length() > MAX_FILE_BYTES) content = content.substring(0, MAX_FILE_BYTES);
                for (Finding f : engine.detect(content)) {
                    all.add(f.withFile(path));
                }
                scanned++;
            }

            ScanReport rep = engine.reportFrom(all);
            String summary = target + " (" + branch + ") \u2014 scanned " + scanned + " file(s). " + rep.summary();
            return new RepoScanReport(rep.score(), rep.grade(), summary, rep.findings(),
                    "ANALYZED", null, scanned, target);

        } catch (RestClientResponseException e) {
            int code = e.getStatusCode().value();
            String message = switch (code) {
                case 401 -> "The token was rejected. Check it has Contents read access.";
                case 403 -> "GitHub rate limit reached. Add a GitHub token (it raises the limit from 60 to 5,000 per hour), or try again in a little while.";
                case 404 -> "Repo not found. If it's private, provide a token with Contents read access.";
                default -> "GitHub request failed (" + code + "). Please try again.";
            };
            return msg("ERROR", message, target);
        } catch (Exception e) {
            return msg("ERROR", "Could not scan that repo. Please check the URL and try again.", target);
        }
    }

    private RepoScanReport msg(String status, String message, String target) {
        return new RepoScanReport(0, "\u2014", "", List.of(), status, message, 0, target);
    }
}
