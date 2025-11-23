# AI Assistant Instructions: Currency Exchange Rates Provider Service

> Optimized for Claude Sonnet 4.5 and GitHub Copilot

## Project Overview
Spring Boot 3.x service with Java 21 that aggregates exchange rates from multiple providers (2+ external APIs + 2 mock services), stores best rates in PostgreSQL + Redis cache, and exposes REST API with role-based access control.

## Architecture & Data Flow
- **Multi-provider aggregation**: Fetch rates from fixer.io, exchangeratesapi.io/similar, plus 2 Docker mock services → select best rate → store in PostgreSQL + Redis
- **Dual storage strategy**: PostgreSQL for historical data (`exchange_rates` table: id, base_currency, target_currency, rate, timestamp); Redis for hot cache (latest best rates only)
- **Scheduled updates**: @Scheduled job runs hourly + on startup to refresh rates from all providers
- **Microservices in Docker**: Main app, 2 mock services, PostgreSQL, Redis orchestrated via docker-compose with shared network

## Critical Patterns & Conventions

### Code Style (Required)
- **Lombok**: Use `@Data`, `@Builder`, `@Slf4j` on all entities/DTOs/services
- **Optional**: Wrap nullable returns (e.g., `Optional<ExchangeRate> findByCurrencyPair(...)`)
- **Stream API**: Use for collections processing (e.g., `providers.stream().map(...).filter(...).findFirst()`)
- **Constructor injection**: Always prefer over `@Autowired` field injection
- **DTOs**: Separate request/response DTOs from JPA entities (e.g., `ExchangeRateDto` vs `ExchangeRate` entity)

### Database Management
- **Liquibase**: All schema changes via migrations in `src/main/resources/db/changelog/` (XML/YAML/SQL/JSON)
- **Security data migrations**: Create roles (USER, PREMIUM_USER, ADMIN), users with BCrypt passwords, and user_roles mappings via Liquibase
- **JPA repositories**: Extend `JpaRepository<Entity, ID>` with custom query methods using method naming conventions

### Security Configuration
- **Authorization rules**:
  - Public: `GET /api/v1/currencies`, `GET /api/v1/currencies/exchange-rates`
  - ADMIN only: `POST /api/v1/currencies`, `POST /api/v1/currencies/refresh`
  - ADMIN + PREMIUM_USER: `GET /api/v1/currencies/trends`
- **UserDetailsService**: Implement custom service loading User entity with roles
- **BCryptPasswordEncoder**: Use for password hashing in migrations and authentication

### External API Integration
- **Provider interface**: Create abstraction (e.g., `ExchangeRateProvider`) implemented by fixer.io, second API, and mock clients
- **Error handling**: Wrap API calls with try-catch, log failures, continue with available providers
- **Best rate selection**: Compare rates from all providers, select optimal (implementation-defined logic)

### Exception Handling
- **@RestControllerAdvice**: Global handler returning standardized JSON: `{ "timestamp", "status", "error", "message", "path" }`
- **Custom exceptions**: `CurrencyNotFoundException`, `ExchangeRateNotFoundException`, `InvalidCurrencyException`, `InvalidPeriodFormatException`, `ExternalApiException`
- **HTTP status codes**: 404 for not found, 400 for validation errors, 401/403 for auth, 500 for server errors

### Validation
- Use `@NotEmpty`, `@Valid`, `@Pattern`, etc. on controller method parameters and DTO fields
- Validate currency codes against ISO 4217 standard
- Period format validation: `12H` (min), `10D`, `3M`, `1Y` patterns

## Testing Strategy

### Unit Tests (JUnit 5 + Mockito)
- Mock dependencies with `@Mock` and `@InjectMocks`
- Test services in isolation (e.g., mock repositories, external clients)
- Target high coverage for Jacoco thresholds

### Controller Tests (@WebMvcTest)
- Test validation annotations with invalid inputs
- Mock service layer with `@MockBean`
- Verify HTTP status codes and response structure

### Integration Tests (TestContainers + Spring Boot Test)
- Spin up PostgreSQL and Redis containers with `@Testcontainers`
- Test full flows: add currency → fetch rates → retrieve from cache/DB
- Test scheduled jobs execution

### WireMock Tests
- Mock external API responses (fixer.io, second API)
- Test retry logic and error handling when APIs fail
- Verify request parameters sent to external services

### Security Tests
- Test role-based access with `@WithMockUser(roles = {"ADMIN", "USER"})`
- Verify 401/403 for unauthorized access
- Test CSRF protection on POST endpoints

## Build & Run Commands

### Local Development
```bash
mvn clean install                          # Build with tests
mvn spring-boot:run                        # Run locally (requires local PostgreSQL/Redis)
mvn test                                   # Run all tests
mvn jacoco:report                          # Generate coverage report
mvn checkstyle:check pmd:check            # Run static analysis
```

### Docker Deployment
```bash
docker-compose up --build                  # Start all services (main app, mocks, PostgreSQL, Redis)
docker-compose down -v                     # Stop and remove volumes
```

### Mock Services
Create separate Spring Boot apps in `mock-services/` subdirectories with:
- Random rate generation endpoint (e.g., `GET /rates?base=USD&target=EUR`)
- Dockerfile for each mock
- Added to docker-compose.yml with service names

## API Endpoints Reference
- `GET /api/v1/currencies` - List supported currencies (public)
- `POST /api/v1/currencies?currency=USD` - Add currency (ADMIN)
- `GET /api/v1/currencies/exchange-rates?amount=15&from=USD&to=EUR` - Calculate exchange (public)
- `POST /api/v1/currencies/refresh` - Force rate update (ADMIN)
- `GET /api/v1/currencies/trends?from=USD&to=EUR&period=12H` - Trend analysis (ADMIN, PREMIUM_USER)

## Code Quality Gates
- **Jacoco**: Set minimum coverage thresholds (e.g., 80% line coverage)
- **Checkstyle**: Enforce Google/Sun Java Style Guide
- **PMD**: Detect code smells and potential bugs
- **PiTest** (optional): Mutation testing for test quality

## Configuration Management
- Store sensitive data (API keys, DB passwords) in environment variables
- Reference in `application.yml` with `${ENV_VAR:default_value}`
- Docker secrets for production deployments
- Separate profiles: `application-dev.yml`, `application-prod.yml`

## Swagger/OpenAPI Documentation
- Access at `http://localhost:8080/swagger-ui.html` after startup
- Annotate controllers with `@Operation(summary = "...")`, `@ApiResponse`
- Document security requirements with `@SecurityRequirement(name = "bearerAuth")`
- Add DTO examples with `@Schema(example = "...")`

## AI Assistant Workflow Guidelines

### When Implementing Features
1. **Read before writing**: Always check existing code patterns in similar files before creating new components
2. **Parallel file creation**: Create related files (entity + repository + service + controller + tests) in batches using multiple tool calls
3. **Test immediately**: Run tests after implementing each feature to catch issues early
4. **Incremental commits**: Suggest logical commit points after completing cohesive units of work

### Common Implementation Patterns
- **Service layer**: Return `Optional<T>` for single results, use Stream API for filtering/mapping collections
- **Controllers**: Keep thin - delegate all logic to services, use `@Valid` on request bodies
- **Repositories**: Use Spring Data JPA method naming (`findByBaseCurrencyAndTargetCurrency`), add `@Query` only for complex queries
- **Exception flow**: Throw custom exceptions from services, let @RestControllerAdvice handle HTTP responses

### Code Generation Best Practices
- Generate complete, runnable code - avoid placeholders like `// TODO` or `// existing code`
- Include all imports in generated classes
- Add `@Slf4j` and use `log.info/debug/error` instead of System.out
- Create both entity and DTO with mapping logic (manually or via MapStruct)
- Generate corresponding test class when creating production class

### Docker & Environment Setup
- Use environment variables for all external configs (API keys, DB credentials)
- Default to sensible local development values in `application.yml`
- Document required env vars in docker-compose.yml comments
- Mock services should expose same interface as real APIs for seamless switching

### Multi-File Changes
When making related changes across multiple files (e.g., adding new endpoint):
1. Update entity/repository if DB changes needed
2. Modify service layer with business logic
3. Add/update controller endpoint
4. Create/update DTOs
5. Add Liquibase migration if schema changes
6. Generate unit + integration tests
7. Update Swagger annotations

### Debugging Assistance
- Check Liquibase changelog order and format when DB issues occur
- Verify Redis connection and serialization for caching problems
- Review SecurityConfig for 401/403 issues
- Check @Scheduled cron expressions for timing problems
- Validate ISO 4217 currency codes in test data
