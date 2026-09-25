# Test Coverage Rules

Use these rules when reviewing a PR's test coverage.
Report every gap as: `[WARNING] path/to/File.java — <gap description> — <suggested test>`

## Mandatory coverage per layer

### Service layer (`*ServiceImpl.java`)

For every `public` method added or modified:
- [ ] **Happy path test**: normal input, verify return value and interactions.
- [ ] **Not-found test**: when a required entity doesn't exist, verify the correct exception is thrown (e.g., `BookNotFoundException`).
- [ ] **Validation/constraint test**: invalid input (null, empty, boundary values) throws the appropriate exception.
- [ ] **Concurrent/conflict test**: if the method handles a resource that can be concurrently modified (e.g., reserving a book that's already reserved), verify the conflict case.
- [ ] **Transaction rollback test**: if the method is `@Transactional`, verify that a failure mid-method does not partially commit.

### Controller layer (`*Controller.java`)

For every new endpoint:
- [ ] **200/201 success test** using `MockMvc`.
- [ ] **400 validation test**: send invalid body, verify `400 Bad Request` with an error message.
- [ ] **404 not-found test**: request non-existent resource, verify `404`.
- [ ] **401/403 test**: if endpoint is secured, verify unauthenticated/unauthorised access is rejected.

### Repository layer (`*Repository.java`)

For every custom `@Query` method:
- [ ] **At least one integration test** using `@DataJpaTest` with an H2 in-memory DB.
- [ ] **Empty result test**: query returns empty list/Optional when no data matches.

## Naming convention for test methods

Use: `methodName_scenario_expectedBehaviour`  
Example: `reserveBook_whenBookAlreadyReserved_throwsConflictException`

## What counts as "missing tests" (always flag)

1. A new service class with zero test class counterpart.
2. A new `public` method in an existing service with no corresponding test method.
3. A new `@Query` repository method with no `@DataJpaTest` coverage.
4. A new controller endpoint with no `MockMvc` test.
5. A modified method whose existing tests no longer cover the modified branch (look for new `if`/`else` branches with no test exercising the new branch).

## Test quality anti-patterns (flag as SUGGESTION)

- `@Test` method with no assertions (empty test or `assertTrue(true)`).
- Tests that only verify `assertNotNull(result)` with no further assertion.
- Mocking the class under test itself (`@InjectMocks` + `@Spy` on the same class).
- Magic numbers in assertions without explanation (`assertEquals(42, result)`).
