# 🧪 Testing Strategy (Blueprint)

> [!NOTE]
> This project is currently in the **Build Phase**. Comprehensive tests are being developed as part of the 1.0 roadmap. This document outlines the technical approach to Quality Assurance.

---

## 🏗️ Technical Test Stack
- **JUnit 5**: Core testing framework.
- **AssertJ**: Fluent assertions for more readable test code.
- **MockMvc**: Integration testing for the REST Controller layer without a full server.
- **Testcontainers**: Using real Docker containers (Postgres, Redis, Keycloak) for high-fidelity integration tests.
- **Mockito**: Mocking service dependencies in unit tests.

---

## 🎯 Test Scenarios

### 1. Security Integration Tests (Key)
- **Session Resolution**: Verify that a valid `SESSION_ID` cookie correctly resolves to a `UserEntity` in the Security Context.
- **Token Refresh**: Simulate an expired Access Token and verify that the `SessionAuthenticationFilter` triggers a transparent refresh via the Refresh Token.
- **RBAC Enforcement**: Verify that `ROLE_USER` cannot access `DELETE` endpoints for resources they do not own.

### 2. Service Layer Unit Tests
- **User Sync Logic**: Test that the `UserService` correctly updates local database profiles when a new JWT is presented.
- **Todo Ownership**: Verify that business logic correctly prevents non-owners from modifying specific todos.

### 3. Database Layer Tests
- **Flyway Migrations**: Verify that the schema is correctly applied to a clean Postgres container.
- **JPA Queries**: Validate complex repository queries against a real database instance.

---

## 📈 Code Coverage Target
I aim for **>80% line coverage** across the `service` and `security` modules, ensuring that the mission-critical auth logic is fully exercised.

---
*Testing code will be pushed to the `main` branch soon!*
