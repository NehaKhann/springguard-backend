package com.springguard.core;

import com.springguard.model.Severity;

import java.util.List;
import java.util.regex.Pattern;

/**
 * The catalog of Spring Boot security checks. Each rule looks for a known
 * misconfiguration and carries its own plain-language explanation + fix.
 *
 * Adding a new check = adding one entry here. (Future: load from config / per-language packs.)
 */
public final class Rules {

    private static final int FLAGS = Pattern.CASE_INSENSITIVE;

    public static final List<SecurityRule> ALL = List.of(

            new SecurityRule(
                    "csrf-disabled",
                    Severity.HIGH,
                    "CSRF protection is disabled",
                    "Turning off CSRF lets an attacker trick a logged-in user's browser into making unwanted state-changing requests (cross-site request forgery).",
                    "Only disable CSRF for stateless APIs that use tokens. For session-based apps, leave it on. If you truly need it off, scope it narrowly instead of globally.",
                    Pattern.compile("csrf\\s*\\(\\s*\\)\\s*\\.\\s*disable|csrf\\s*\\(\\s*[a-z]+\\s*->\\s*[a-z]+\\s*\\.\\s*disable", FLAGS)
            ),

            new SecurityRule(
                    "permit-all",
                    Severity.HIGH,
                    "All requests are permitted without authentication",
                    "anyRequest().permitAll() leaves every endpoint open to the public, including ones that should require a logged-in user.",
                    "Require authentication by default: .anyRequest().authenticated() and only permitAll() the specific public paths (login, health, static assets).",
                    Pattern.compile("anyRequest\\s*\\(\\s*\\)\\s*\\.\\s*permitAll", FLAGS)
            ),

            new SecurityRule(
                    "wildcard-cors",
                    Severity.MEDIUM,
                    "CORS allows any origin (*)",
                    "Allowing all origins lets any website call your API from a user's browser, which can expose data or enable abuse.",
                    "List the specific front-end origins you trust, e.g. allowedOrigins(\"https://yourapp.com\") instead of \"*\".",
                    Pattern.compile("@CrossOrigin\\s*\\(\\s*origins\\s*=\\s*\"\\*\"|allowedOrigins\\s*\\(\\s*\"\\*\"", FLAGS)
            ),

            new SecurityRule(
                    "actuator-exposed",
                    Severity.HIGH,
                    "All actuator endpoints are exposed",
                    "Exposing every actuator endpoint can leak environment variables, configuration, and internal metrics, and some endpoints can change app state.",
                    "Expose only what you need, e.g. management.endpoints.web.exposure.include=health,info and secure the rest behind authentication.",
                    Pattern.compile("management\\.endpoints\\.web\\.exposure\\.include\\s*=\\s*\\*", FLAGS)
            ),

            new SecurityRule(
                    "hardcoded-db-password",
                    Severity.HIGH,
                    "Database password is hardcoded in properties",
                    "A password committed in application.properties ends up in source control where anyone with repo access can read it.",
                    "Move it to an environment variable or a secrets manager: spring.datasource.password=${DB_PASSWORD}.",
                    Pattern.compile("spring\\.datasource\\.password\\s*=\\s*(?!\\$\\{)\\S+", FLAGS)
            ),

            new SecurityRule(
                    "weak-jwt-secret",
                    Severity.HIGH,
                    "JWT signing secret is weak or hardcoded",
                    "A short or obvious signing secret (like \"secret\") can be brute-forced, letting an attacker forge valid tokens and impersonate any user.",
                    "Use a long, random secret supplied via environment variable: jwt.secret=${JWT_SECRET} (256-bit / 32+ chars).",
                    Pattern.compile("jwt\\.secret\\s*=\\s*(?!\\$\\{)(secret|password|changeme|123\\S*|.{1,15})\\s*$", FLAGS | Pattern.MULTILINE)
            ),

            new SecurityRule(
                    "jwt-no-expiration",
                    Severity.MEDIUM,
                    "JWT tokens do not expire",
                    "A token with no expiry stays valid forever, so a leaked token can never be aged out and keeps working indefinitely.",
                    "Set a sensible lifetime, e.g. jwt.expiration=3600000 (1 hour) and use refresh tokens for longer sessions.",
                    Pattern.compile("jwt\\.expiration\\s*=\\s*0\\b", FLAGS)
            ),

            new SecurityRule(
                    "leaking-exception",
                    Severity.MEDIUM,
                    "Internal error details are exposed to clients",
                    "Returning raw exception messages or stack traces can reveal class names, queries, and paths that help an attacker map your system.",
                    "Log the detail server-side and return a generic message. Use @ControllerAdvice to map exceptions to safe responses.",
                    Pattern.compile("printStackTrace\\s*\\(|throw\\s+new\\s+\\w*Exception\\s*\\(\\s*e\\.getMessage\\s*\\(\\s*\\)", FLAGS)
            ),

            // --- PDPL / GDPR data-handling flavour (your differentiator) ---
            new SecurityRule(
                    "pii-in-logs",
                    Severity.MEDIUM,
                    "Personal data may be written to logs",
                    "Logging emails, phone numbers, or passwords stores personal data in plain text, which conflicts with PDPL/GDPR data-minimisation and security duties.",
                    "Avoid logging personal fields. Mask or omit them, e.g. log a user id instead of an email, and never log credentials.",
                    Pattern.compile("log(ger)?\\.(info|debug|warn|error)\\s*\\([^)]*(password|email|phone|iqama|nationalId)", FLAGS)
            )
    );

    private Rules() {}
}
