# 🚀 Getting Started & Startup Guide

This guide will help you get the **KeycloakBff** project running on your local machine.

> [!IMPORTANT]
> This project is currently in the **Development Phase**. This guide focuses on the `dev` profile. A production-ready guide including TLS/SSL and hardened secrets will be added once the initial project scope is complete.

---

## 🛠️ Prerequisites
- **Docker & Docker Compose**: For running the infrastructure (Keycloak, Postgres, Redis).
- **Java 25 (LTS)**: Required for the BFF application.
- **Gradle**: For building the project.

---

## ⚡ Quick Start (Dev Mode)

### 1. Spin up the Infrastructure
The system requires four core services to be healthy before the BFF can start correctly.
```bash
docker-compose up -d
```
**What this does**:
- Starts **Postgres** for both app and Keycloak data.
- Starts **Redis** for stateful sessions.
- Starts **Keycloak** and imports the `realm.json` automatically.

### 2. Verify Infrastructure Health
Before running the application, ensure Keycloak is accessible:
- **Keycloak Admin**: `http://localhost:8081` (Admin/Admin)
- **API Health**: `http://localhost:8081/health`

### 3. Run the BFF Application
```bash
./gradlew bootRun
```
The application will start on `http://localhost:8080` using the `dev` Spring profile.

---

## 🔗 Accessing the System

| Endpoint | Purpose |
| :--- | :--- |
| `http://localhost:8080/swagger-ui.html` | Explore and test the API endpoints. |
| `http://localhost:8081/admin` | Access the Keycloak Admin Console. |
| `/api/auth/login` | **POST**: Initial authentication (Requires JSON body with username/password). |
| `/api/todos` | **GET**: Access protected todo resources (Requires `SESSION_ID` cookie). |

---

## 🚦 Future Startup Improvements
Once the project transition out of the "Ongoing" status, this guide will be updated with:
- [ ] **Native Image Build**: Instructions for building a lightning-fast GraalVM native binary.
- [ ] **Kubernetes Deployment**: Helm charts for production orchestration.
- [ ] **Vault Integration**: Secure management of Keycloak client secrets.
- [ ] **Automated Integration Tests**: Using Testcontainers during the build process.

---
*Stay tuned for updates as I move toward a feature-complete 1.0 release!*
