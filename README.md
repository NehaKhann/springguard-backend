# SpringGuard — Backend (Level 1: Find & Explain)

A security auditor for Spring Boot. Send it Spring code/config, get back a graded
report: each issue, why it's dangerous, and the fix. Built to extend to whole repos
(zip / GitHub) and to other languages later.

This is **Level 1**: the detection engine + REST API. No database, no external services,
runs offline. (Next: the grade/report-card UI, then repo scanning, then auto-fix PRs.)

## Requirements
- Java 17+
- Maven 3.9+

## Run it
```bash
cd springguard-backend
mvn spring-boot:run
```
The API starts on http://localhost:8080

## Try it

Health check:
```bash
curl http://localhost:8080/api/health
```

Scan some deliberately insecure code:
```bash
curl -X POST http://localhost:8080/api/scan \
  -H "Content-Type: application/json" \
  -d '{"code":"http.csrf().disable().authorizeHttpRequests(x -> x.anyRequest().permitAll());\nmanagement.endpoints.web.exposure.include=*\nspring.datasource.password=admin123\njwt.secret=secret\njwt.expiration=0"}'
```

You'll get JSON like:
```json
{
  "score": 0,
  "grade": "F",
  "summary": "Grade F — found 5 issue(s): 4 high, 1 medium, 0 low.",
  "findings": [
    {"id":"csrf-disabled","severity":"HIGH","title":"CSRF protection is disabled","why":"...","fix":"..."}
  ]
}
```

## What it checks (Level 1 rule set)
| Rule | Severity |
|---|---|
| CSRF disabled | High |
| All requests permitted (permitAll) | High |
| Wildcard CORS (`*`) | Medium |
| All actuator endpoints exposed | High |
| Hardcoded DB password in properties | High |
| Weak / hardcoded JWT secret | High |
| JWT never expires | Medium |
| Internal error details leaked to clients | Medium |
| Personal data written to logs (PDPL/GDPR) | Medium |

Adding a check = one entry in `core/Rules.java`.

## How it's structured
```
web/ScanController.java   REST endpoints (/api/health, /api/scan)
web/WebConfig.java        CORS for the local front-end (specific origins, not *)
core/ScanService.java     Spring service
core/RuleEngine.java      runs rules -> score (0-100) -> grade (A-F)
core/Rules.java           the catalog of checks (edit here to add rules)
core/SecurityRule.java    one rule (pattern + explanation + fix)
model/                    Finding, ScanReport, ScanRequest, Severity
```

## Scoring
Start at 100. Subtract per finding: High −25, Medium −12, Low −5 (floored at 0).
Grade: A ≥90, B ≥75, C ≥60, D ≥40, else F.

## Roadmap
- **Level 1 (done):** detection engine + API + grade
- **Level 2:** report-card UI, rescan-to-improve, score history
- **Level 3:** scan a whole repo (GitHub URL / zip), then open a fix PR
