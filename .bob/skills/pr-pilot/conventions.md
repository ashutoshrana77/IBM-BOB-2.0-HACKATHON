# Team Conventions

## Package and class naming

- Package structure: `com.library.<domain>` (e.g., `com.library.reservation`)
- Controllers: suffix `Controller` (e.g., `ReservationController`)
- Services: suffix `Service` with interface + `Impl` implementation (e.g., `ReservationService`, `ReservationServiceImpl`)
- Repositories: suffix `Repository`, extend `JpaRepository` (e.g., `ReservationRepository`)
- DTOs: suffix `Request` for inbound, `Response` for outbound (e.g., `ReservationRequest`, `ReservationResponse`)
- Entities: plain noun, annotated with `@Entity` (e.g., `Reservation`, `Book`, `User`)
- Exceptions: suffix `Exception` (e.g., `BookNotFoundException`)

## Architectural rules (MUST flag violations)

- **No business logic in controllers.** Controllers only validate input, delegate to a service, and map the response.
- **No repository calls from controllers.** Always go through the service layer.
- **All service methods that modify state must be `@Transactional`.**
- **Input validation at the API boundary.** All `@RequestBody` parameters must carry Bean Validation annotations (`@Valid`, `@NotNull`, `@NotBlank`, etc.).
- **No raw SQL strings.** Use JPQL named queries or Spring Data method naming. Parameterised queries only.
- **No `System.out.println`.** Use SLF4J `@Slf4j` logging.
- **DTOs, not entities, are returned from controllers.** Never expose JPA entities directly in REST responses.
- **Global exception handling via `@RestControllerAdvice`.** Do not catch exceptions silently in service methods.

## Method and variable naming

- Boolean variables/methods: prefix `is`, `has`, `can` (e.g., `isAvailable`, `hasExpired`)
- Repository finder methods: `findBy<Field>` or `findBy<Field>And<Field>`
- Constants: `UPPER_SNAKE_CASE` in a dedicated `Constants` class or as `static final` fields

## REST API conventions

- Use `ResponseEntity<T>` for all controller return types.
- HTTP 200 for success, 201 for creation, 204 for deletion, 400 for validation errors, 404 for not found, 409 for conflict.
- All endpoints under `/api/v1/` prefix.
- POST body via `@RequestBody @Valid`, path variables via `@PathVariable`, filters via `@RequestParam`.

## Test conventions

- Unit tests in `src/test/java`, same package as the class under test.
- Test class name: `<ClassName>Test` (e.g., `ReservationServiceTest`)
- Use JUnit 5 (`@ExtendWith(MockitoExtension.class)`) and Mockito.
- At minimum: one happy-path test, one not-found test, one validation/constraint test per service method.
- Integration tests use `@SpringBootTest` + `@AutoConfigureMockMvc`.

## Documentation rules

- All `public` methods in service interfaces must have Javadoc.
- `@Operation` (Swagger/OpenAPI) annotation on all controller methods.
- Changelog entry required for any new endpoint or behaviour change (in `CHANGELOG.md`, under `[Unreleased]`).
