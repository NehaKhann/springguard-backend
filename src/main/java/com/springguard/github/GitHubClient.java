package com.springguard.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Talks to the GitHub REST API. Token is optional (needed only for private repos). */
@Component
public class GitHubClient {

    public static final int MAX_FILES = 40;
    public static final int MAX_FILE_BYTES = 200_000;

    private final ObjectMapper mapper;
    private final RestClient http;

    public GitHubClient(ObjectMapper mapper) {
        this.mapper = mapper;
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(8000);
        rf.setReadTimeout(15000);
        this.http = RestClient.builder().requestFactory(rf).build();
    }

    /** branch is the branch named in the URL (.../tree/<branch>), or null if none. */
    public record Repo(String owner, String name, String branch) {}

    public Repo parse(String url) {
        if (url == null) return null;
        String u = url.trim().replaceFirst("^https?://", "").replaceFirst("^www\\.", "");
        if (!u.startsWith("github.com/")) return null;
        String[] parts = u.substring("github.com/".length()).split("/");
        if (parts.length < 2) return null;
        String owner = parts[0];
        String repo = parts[1];
        if (repo.endsWith(".git")) repo = repo.substring(0, repo.length() - 4);
        if (owner.isBlank() || repo.isBlank()) return null;

        String branch = null;
        // .../tree/<branch>[/...]
        if (parts.length >= 4 && "tree".equals(parts[2]) && !parts[3].isBlank()) {
            branch = parts[3];
        }
        return new Repo(owner, repo, branch);
    }

    public String defaultBranch(Repo repo, String token) throws Exception {
        String json = apiGet("https://api.github.com/repos/" + repo.owner() + "/" + repo.name(), token, false);
        return mapper.readTree(json).path("default_branch").asText("main");
    }

    /** All branch names that actually exist in the repo (paged, capped). */
    public java.util.List<String> listBranchNames(Repo repo, String token) throws Exception {
        java.util.List<String> names = new ArrayList<>();
        for (int page = 1; page <= 5; page++) { // up to 500 branches
            String url = "https://api.github.com/repos/" + repo.owner() + "/" + repo.name()
                    + "/branches?per_page=100&page=" + page;
            String json = apiGet(url, token, false);
            JsonNode arr = mapper.readTree(json);
            if (!arr.isArray() || arr.isEmpty()) break;
            for (JsonNode b : arr) {
                String name = b.path("name").asText("");
                if (!name.isBlank()) names.add(name);
            }
            if (arr.size() < 100) break;
        }
        return names;
    }

    /** Exact commit SHA for a branch that is confirmed to exist, else null. */
    public String branchSha(Repo repo, String branch, String token) {
        try {
            // Confirm the branch is genuinely in the repo's branch list (no loose fallback).
            if (!listBranchNames(repo, token).contains(branch)) {
                return null;
            }
            String json = apiGet("https://api.github.com/repos/" + repo.owner() + "/" + repo.name()
                    + "/branches/" + branch, token, false);
            String sha = mapper.readTree(json).path("commit").path("sha").asText("");
            return sha.isBlank() ? null : sha;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) return null;
            throw e;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<String> listRelevantFiles(Repo repo, String treeish, String token) throws Exception {
        String url = "https://api.github.com/repos/" + repo.owner() + "/" + repo.name()
                + "/git/trees/" + treeish + "?recursive=1";
        String json = apiGet(url, token, false);
        JsonNode tree = mapper.readTree(json).path("tree");

        List<String> files = new ArrayList<>();
        if (tree.isArray()) {
            for (JsonNode node : tree) {
                if (!"blob".equals(node.path("type").asText())) continue;
                String path = node.path("path").asText("");
                long size = node.path("size").asLong(0);
                if (isRelevant(path) && size <= MAX_FILE_BYTES) {
                    files.add(path);
                }
            }
        }
        files.sort(Comparator.comparingInt(p -> p.contains("/test/") ? 1 : 0));
        return files.size() > MAX_FILES ? files.subList(0, MAX_FILES) : files;
    }

    private boolean isRelevant(String path) {
        String p = path.toLowerCase();
        return p.endsWith(".java") || p.endsWith(".properties") || p.endsWith(".yml") || p.endsWith(".yaml");
    }

    public String fetchFile(Repo repo, String branch, String path, String token) throws Exception {
        if (token != null && !token.isBlank()) {
            String url = "https://api.github.com/repos/" + repo.owner() + "/" + repo.name()
                    + "/contents/" + path + "?ref=" + branch;
            return apiGet(url, token, true);
        }
        String rawUrl = "https://raw.githubusercontent.com/" + repo.owner() + "/" + repo.name()
                + "/" + branch + "/" + path;
        return http.get().uri(rawUrl).header("User-Agent", "SpringGuard").retrieve().body(String.class);
    }

    private String apiGet(String url, String token, boolean raw) {
        var req = http.get().uri(url)
                .header("User-Agent", "SpringGuard")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("Accept", raw ? "application/vnd.github.raw" : "application/vnd.github+json");
        if (token != null && !token.isBlank()) {
            req = req.header("Authorization", "Bearer " + token);
        }
        return req.retrieve().body(String.class);
    }
}
