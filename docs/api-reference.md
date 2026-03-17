# 📋 Exhaustive API Reference Dashboard

This document provides a complete reference for all BFF endpoints. For interactive testing, please use the **Swagger UI** at `http://localhost:8080/swagger-ui.html`.

---

## 🔐 Authentication & Session Endpoints (`/api/auth`)

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Registers a new user in Keycloak and syncs to App DB. | No |
| `POST` | `/api/auth/login` | Authenticates via Keycloak and sets `SESSION_ID` cookie. | No (supports Remember Me) |
| `GET` | `/api/auth/me` | Returns professional profile of the logged-in user. | Yes |
| `POST` | `/api/auth/logout` | Revokes tokens and clears BFF session/cookie. | Yes |
| `POST` | `/api/auth/forgot-password` | Triggers a password reset email via Keycloak. | No |

---

## 👤 User Management (`/api/users`)

| Method | Endpoint | Description | Role Required |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/users/me` | Returns the full `UserEntity` for the current user. | `ANY` |
| `GET` | `/api/users` | Lists all users in the system. | `ROLE_ADMIN` |
| `GET` | `/api/users/{id}` | Returns details for a specific user. | `ROLE_USER` / `ADMIN` |

---

## 📝 Todo Management (`/api/todos`)

| Method | Endpoint | Description | Role Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/todos` | Creates a new todo (Owner = Current User). | `ROLE_USER` |
| `GET` | `/api/todos` | List todos (Admin: All, User: Own). | `ROLE_USER` |
| `GET` | `/api/todos/{id}` | Fetch specific todo (Ownership enforced). | `ROLE_USER` / `ADMIN` |
| `PUT` | `/api/todos/{id}` | Update a todo (Ownership enforced). | `ROLE_USER` / `ADMIN` |
| `DELETE` | `/api/todos/{id}` | Delete a todo (Ownership enforced). | `ROLE_USER` / `ADMIN` |

---

## ⚙️ Infrastructure & Monitoring

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `GET` | `/actuator/health` | Service health status. | No |
| `GET` | `/v3/api-docs` | OpenAPI 3.0 specification. | No |

---
> [!IMPORTANT]
> All state-changing requests (`POST`, `PUT`, `DELETE`) require a valid `SESSION_ID` cookie and should include CSRF protection headers in production.
