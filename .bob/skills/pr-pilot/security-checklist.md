# Security Checklist

Use this checklist when reviewing changed files for security issues.
Report every hit as: `[CRITICAL] path/to/File.java:LINE — <description> — <fix>`

## A1 — Injection

- [ ] **SQL/JPQL string concatenation**: look for `"SELECT " + variable`, `entityManager.createQuery(string + input)`, `nativeQuery = true` with unparameterised strings.
  - Fix: use Spring Data method names, `@Query` with `:param` placeholders, or `CriteriaBuilder`.
- [ ] **JPQL named parameters**: verify `@Param` is used and values are not constructed from raw user input.
- [ ] **Shell injection**: look for `Runtime.exec(userInput)`, `ProcessBuilder` with unsanitised values.

## A2 — Broken Authentication

- [ ] **Hardcoded credentials**: grep for `password = "`, `secret = "`, `api_key = "`, `token = "` in source (not test fixtures).
- [ ] **Hardcoded JWT secret**: look for `Keys.hmacShaKeyFor("hardcoded-string".getBytes())`.
- [ ] **Passwords in logs**: `log.info("password: {}", password)` patterns.

## A3 — Sensitive Data Exposure

- [ ] **Entities returned directly from controllers** (should be DTOs).
- [ ] **Sensitive fields without `@JsonIgnore`**: `password`, `ssn`, `creditCard` fields on entities/DTOs.
- [ ] **Stack traces exposed in API responses**: `e.getMessage()` returned directly in `ResponseEntity`.

## A4 — Broken Access Control

- [ ] **New `@RestController` methods without `@PreAuthorize` or `@Secured`** (unless the endpoint is intentionally public — flag for human confirmation).
- [ ] **User-supplied IDs used directly without ownership check**: e.g., `reservationRepo.findById(userId)` where `userId` comes from request body, not the security context.

## A5 — Security Misconfiguration

- [ ] **CORS wildcard**: `allowedOrigins("*")` in production security config.
- [ ] **`@CrossOrigin` without explicit origins** on controllers.
- [ ] **`csrf().disable()`** without justification comment.
- [ ] **`permitAll()` applied to non-public endpoints**.

## A6 — Vulnerable and Outdated Components

- [ ] **Weak hashing for passwords**: `MessageDigest.getInstance("MD5")` or `SHA-1` used for password storage.
  - Fix: use `BCryptPasswordEncoder`.
- [ ] **`new Random()`** used for security-sensitive values (tokens, OTPs).
  - Fix: use `SecureRandom`.

## A7 — Insecure Deserialization

- [ ] **`ObjectInputStream` from untrusted sources.**
- [ ] **Jackson polymorphic deserialization** with `enableDefaultTyping()` (deprecated, dangerous).

## A8 — Logging and Monitoring

- [ ] **No audit log on sensitive operations**: reservation creation/cancellation, user role changes.
- [ ] **Exception swallowed silently**: `catch (Exception e) {}` or `catch (Exception e) { return null; }`.

## A9 — Secrets in configuration files

- [ ] **`application.properties` or `application.yml` committed with real passwords/keys** (not referencing env vars or Vault).
  - Look for `spring.datasource.password=<literal>` that is not `${DB_PASSWORD}`.

## A10 — Concurrency and race conditions

- [ ] **Double-check locking or non-atomic read-modify-write on shared state** (critical for reservation/inventory logic).
- [ ] **Missing `@Transactional` on service methods that do multiple DB writes** (could leave data inconsistent on partial failure).
- [ ] **Optimistic locking**: entities that can be concurrently modified should have `@Version` field.
