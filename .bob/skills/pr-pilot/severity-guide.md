# Severity Guide

## CRITICAL 🔴

Use for findings that **must be fixed before merge** — they represent security vulnerabilities, data corruption risks, or system correctness failures.

Examples:
- SQL injection / JPQL injection vulnerability
- Hardcoded secret or credential
- Missing `@Transactional` on a multi-step write operation
- Race condition that can cause double-booking or data loss
- Entity exposed directly in REST response (leaks internal structure/sensitive fields)
- New endpoint with no access control

## WARNING 🟡

Use for findings that **should be fixed** but do not block deployment if there is a documented reason to defer.

Examples:
- Missing unit tests for a new service method
- Method lacks `@Valid` on `@RequestBody` (validation not enforced)
- Business logic placed in controller instead of service
- `System.out.println` or missing structured logging
- Missing Javadoc on a public service interface method
- Design doc specifies behaviour not implemented in the PR

## SUGGESTION 🔵

Use for findings that represent **best-practice improvements** — code quality, maintainability, or minor convention violations that don't affect correctness.

Examples:
- Rename a variable or method to match naming conventions
- Extract a magic number into a named constant
- Suggest a more idiomatic Spring pattern
- Minor Javadoc improvement
- Changelog entry missing

## Confidence scoring

| Score | Meaning |
|-------|---------|
| 90–100% | Diff is small, context is clear, design doc aligns |
| 70–89% | Diff is medium, some ambiguity in design intent |
| 50–69% | Large diff, limited test coverage, design doc missing |
| < 50% | Very large diff or no context — flag to human reviewer |
