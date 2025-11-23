# Currency Exchange Rates Provider Service - Implementation Tasks

## 1. Project Setup and Configuration

### 1.1 Project Initialization
- [ ] Create Spring Boot application with Java 21
- [ ] Configure Maven with required dependencies (pom.xml)
- [ ] Set up project structure following Spring Boot best practices
- [ ] Configure application.properties/application.yml

### 1.2 Docker Configuration
- [ ] Create Dockerfile for main application
- [ ] Create docker-compose.yml for orchestrating all services
- [ ] Configure PostgreSQL container
- [ ] Configure Redis container
- [ ] Set up Docker network for service communication

### 1.3 Database Configuration
- [ ] Configure PostgreSQL connection settings
- [ ] Set up Liquibase for database migrations
- [ ] Create initial database schema migration
- [ ] Configure Spring Data JPA

## 2. Database Schema and Entities

### 2.1 Exchange Rates Schema
- [ ] Create migration for `exchange_rates` table (ID, base_currency, target_currency, rate, timestamp)
- [ ] Create JPA entity for ExchangeRate
- [ ] Create repository interface for ExchangeRate

### 2.2 Currency Management Schema
- [ ] Create migration for `currencies` table
- [ ] Create JPA entity for Currency
- [ ] Create repository interface for Currency

### 2.3 Security Schema
- [ ] Create migration for `users` table with encrypted passwords
- [ ] Create migration for `roles` table
- [ ] Create migration for `user_roles` junction table (many-to-many relationship)
- [ ] Create JPA entities for User and Role
- [ ] Create repositories for User and Role

## 3. External Exchange Rate Providers Integration

### 3.1 Real External APIs
- [ ] Implement client for fixer.io API
- [ ] Implement client for second external API (exchangeratesapi.io/openexchangerates.org/currencylayer.com)
- [ ] Create interface/abstraction for exchange rate providers
- [ ] Implement error handling and retry logic for external APIs

### 3.2 Mock Exchange Services
- [ ] Create first standalone mock service (Spring Boot application)
- [ ] Create second standalone mock service (Spring Boot application)
- [ ] Create Dockerfiles for mock services
- [ ] Add mock services to docker-compose.yml
- [ ] Implement random exchange rate generation logic

## 4. Core Business Logic

### 4.1 Currency Management Service
- [ ] Implement service to get list of currencies
- [ ] Implement service to add new currency
- [ ] Add validation for currency codes (ISO 4217)

### 4.2 Exchange Rate Service
- [ ] Implement service to fetch rates from all providers
- [ ] Implement logic to select best rate from multiple providers
- [ ] Implement service to store rates in PostgreSQL
- [ ] Implement service to store/update rates in Redis cache
- [ ] Implement service to retrieve rates from cache (with database fallback)
- [ ] Implement exchange rate calculation service

### 4.3 Scheduled Tasks
- [ ] Create scheduled job to fetch exchange rates every hour
- [ ] Implement rate fetching on application startup
- [ ] Configure proper scheduling with @Scheduled annotation

### 4.4 Trends Analysis Service
- [ ] Implement service to calculate exchange rate trends
- [ ] Add support for different time periods (12H, 10D, 3M, 1Y)
- [ ] Implement percentage change calculation logic

## 5. REST API Controllers

### 5.1 Currency Controller
- [ ] Implement GET /api/v1/currencies endpoint
- [ ] Implement POST /api/v1/currencies endpoint with currency parameter
- [ ] Add validation annotations (@NotEmpty, @Valid, etc.)
- [ ] Add proper HTTP status codes

### 5.2 Exchange Rate Controller
- [ ] Implement GET /api/v1/currencies/exchange-rates endpoint
- [ ] Add validation for amount, from, and to parameters
- [ ] Implement POST /api/v1/currencies/refresh endpoint
- [ ] Implement GET /api/v1/currencies/trends endpoint
- [ ] Add validation for period parameter format

## 6. Redis Caching

### 6.1 Redis Configuration
- [ ] Add Redis dependencies to pom.xml
- [ ] Configure Redis connection in application.yml
- [ ] Create Redis configuration class
- [ ] Implement RedisTemplate or use Spring Cache abstraction

### 6.2 Caching Strategy
- [ ] Implement caching for exchange rates
- [ ] Implement cache update logic when new rates are fetched
- [ ] Ensure only best/latest rates are stored in cache

## 7. Spring Security Implementation

### 7.1 Security Configuration
- [ ] Configure Spring Security with form login
- [ ] Create login page (HTML/Thymeleaf)
- [ ] Configure password encryption (BCryptPasswordEncoder)
- [ ] Implement UserDetailsService

### 7.2 Authorization Rules
- [ ] Configure public access for GET /api/v1/currencies
- [ ] Configure public access for GET /api/v1/currencies/exchange-rates
- [ ] Configure ADMIN-only access for POST /api/v1/currencies
- [ ] Configure ADMIN-only access for POST /api/v1/currencies/refresh
- [ ] Configure ADMIN and PREMIUM_USER access for GET /api/v1/currencies/trends

### 7.3 Initial Users Setup
- [ ] Create Liquibase migration to insert USER role
- [ ] Create Liquibase migration to insert PREMIUM_USER role
- [ ] Create Liquibase migration to insert ADMIN role
- [ ] Create Liquibase migration to insert test users with encrypted passwords
- [ ] Assign appropriate roles to test users

## 8. Exception Handling

### 8.1 Custom Exceptions
- [ ] Create CurrencyNotFoundException
- [ ] Create ExchangeRateNotFoundException
- [ ] Create InvalidCurrencyException
- [ ] Create InvalidPeriodFormatException
- [ ] Create ExternalApiException

### 8.2 Global Exception Handler
- [ ] Create @RestControllerAdvice class
- [ ] Implement handlers for custom exceptions
- [ ] Implement handler for validation exceptions
- [ ] Implement handler for authentication/authorization exceptions
- [ ] Create standardized error response JSON structure
- [ ] Ensure proper HTTP status codes are returned

## 9. API Documentation

### 9.1 Swagger/OpenAPI Configuration
- [ ] Add SpringDoc OpenAPI dependencies
- [ ] Configure Swagger UI
- [ ] Add @Operation annotations to controller methods
- [ ] Add @Schema annotations to DTOs
- [ ] Document security requirements
- [ ] Add API examples and descriptions

## 10. Testing

### 10.1 Unit Tests
- [ ] Write unit tests for Currency service
- [ ] Write unit tests for ExchangeRate service
- [ ] Write unit tests for Trends service
- [ ] Write unit tests for external API clients
- [ ] Use Mockito for mocking dependencies
- [ ] Aim for high code coverage

### 10.2 Controller Validation Tests
- [ ] Create @WebMvcTest for Currency controller
- [ ] Create @WebMvcTest for ExchangeRate controller
- [ ] Test validation annotations (@NotEmpty, @Valid, etc.)
- [ ] Test invalid input scenarios

### 10.3 Integration Tests with TestContainers
- [ ] Set up TestContainers for PostgreSQL
- [ ] Set up TestContainers for Redis
- [ ] Write integration tests for currency management flow
- [ ] Write integration tests for exchange rate fetching and storage
- [ ] Write integration tests for trends calculation
- [ ] Test scheduled jobs

### 10.4 WireMock Tests
- [ ] Set up WireMock for testing external API integrations
- [ ] Create mock responses for fixer.io
- [ ] Create mock responses for second external API
- [ ] Test error scenarios (API unavailable, invalid response, etc.)

### 10.5 Security Tests
- [ ] Test authentication and authorization
- [ ] Test access control for different roles
- [ ] Test unauthorized access returns 401/403
- [ ] Test CSRF protection

### 10.6 Exception Handling Tests
- [ ] Test all custom exception scenarios
- [ ] Test validation error responses
- [ ] Test proper status codes and error JSON structure

## 11. Code Quality and Static Analysis

### 11.1 Configure Linters and Analyzers
- [ ] Add and configure Jacoco plugin for code coverage
- [ ] Add and configure Checkstyle plugin
- [ ] Add and configure PMD plugin
- [ ] (Optional) Add and configure PiTest for mutation testing
- [ ] Create configuration files for each tool
- [ ] Set up quality thresholds

### 11.2 Code Review
- [ ] Ensure Optional is used appropriately throughout the code
- [ ] Ensure Stream API is used for collections processing
- [ ] Ensure Lombok annotations are used (@Data, @Builder, @Slf4j, etc.)
- [ ] Follow Java naming conventions
- [ ] Ensure proper logging is implemented

## 12. Documentation

### 12.1 Project Documentation
- [ ] Create comprehensive README.md
- [ ] Document how to run the application locally
- [ ] Document how to run with Docker
- [ ] Document API endpoints with examples
- [ ] Document environment variables and configuration
- [ ] Add architecture diagram (optional)

### 12.2 Code Documentation
- [ ] Add JavaDoc comments to public methods
- [ ] Add comments for complex business logic
- [ ] Document external API integration details

## 13. Final Integration and Testing

### 13.1 End-to-End Testing
- [ ] Test complete flow: add currency -> fetch rates -> get exchange rates
- [ ] Test scheduled rate updates work correctly
- [ ] Test Redis caching works as expected
- [ ] Test all security roles and permissions
- [ ] Test trends calculation with various periods

### 13.2 Docker Deployment
- [ ] Build and test all Docker images
- [ ] Test docker-compose up starts all services correctly
- [ ] Test service discovery and communication in Docker network
- [ ] Verify database migrations run automatically on startup
- [ ] Test application health and readiness endpoints

### 13.3 Performance and Load Testing (Optional)
- [ ] Test API performance under load
- [ ] Test Redis cache effectiveness
- [ ] Optimize database queries if needed

## 14. Final Checklist

- [ ] All requirements from specification are implemented
- [ ] All tests are passing (unit, integration, functional)
- [ ] Code coverage meets thresholds (Jacoco)
- [ ] No critical Checkstyle/PMD violations
- [ ] API documentation is complete and accessible
- [ ] Docker setup is working properly
- [ ] README.md is complete with all necessary information
- [ ] Code follows best practices and is production-ready
- [ ] All security requirements are implemented and tested

---

## Notes

- Use @Slf4j for logging throughout the application
- Prefer constructor injection over field injection
- Use DTOs for API requests/responses (separate from entities)
- Implement proper transaction management with @Transactional
- Use environment variables for sensitive configuration (API keys, passwords)
- Follow RESTful conventions for API design
- Ensure all async/scheduled tasks handle exceptions properly
