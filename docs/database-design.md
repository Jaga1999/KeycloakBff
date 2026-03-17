# Database Design

KeycloakBff uses a relational **PostgreSQL** database to store application-specific data, while delegating identity management to **Keycloak's internal schema**.

## 📊 Auto-Sync Logic: Keycloak to App DB

The "Source of Truth" for users is Keycloak. However, I sync them to my local DB to allow for high-performance relations (e.g., User owns Todo).

### Synchronization Sequence

```mermaid
sequenceDiagram
    participant KC as Keycloak
    participant Auth as AuthService
    participant DB as App Postgres (users table)

    Note over Auth: Triggered on every successful /login
    KC-->>Auth: JWT (contains: sub, email, name, roles)
    
    Auth->>DB: findByKeycloakId(sub)
    
    alt User Does Not Exist
        Auth->>DB: INSERT INTO users (id, keycloak_id, email, roles...)
        Note right of DB: ID is generated locally (UUID)
    else User Exists
        Auth->>DB: UPDATE users SET email = ?, roles = ?, updated_at = now()
        Note right of DB: Ensures local profile matches IAM changes
    end
    
    DB-->>Auth: Persisted UserEntity
```

---

## 🔗 Keycloak to App DB Connection

The connection between the two databases is **virtual and federated**, maintained via the `keycloak_id` field.

```mermaid
flowchart LR
    subgraph "Keycloak JWT"
        sub["sub (UUID)"]
        email["email"]
        roles["roles[]"]
    end
    
    subgraph "App Logic (AuthService)"
        Sync["Synchronization Logic"]
    end
    
    subgraph "Postgres Tables"
        U["users Table"]
        R["user_roles Table"]
    end
    
    sub -- "Mapped to" --> Sync
    Sync -- "UPSERT" --> U
    roles -- "SYNC" --> R
```

The connection between the two databases is **virtual and federated**, maintained via the `keycloak_id` field.

```mermaid
erDiagram
    USERS ||--o{ USER_ROLES : has
    USERS ||--o{ TODOS : owns

    USERS {
        uuid id PK "Local Primary Key"
        string keycloak_id UK "Subject (sub) from Keycloak"
        string username UK
        string email UK
        string first_name
        string last_name
        timestamp_tz created_at
        timestamp_tz updated_at
    }

    USER_ROLES {
        uuid user_id FK
        string role "Calculated from Keycloak"
    }

    TODOS {
        uuid id PK
        string title
        text description
        uuid owner_id FK "References users.id"
    }
```

---

## 🗄️ Detailed Table Reference

### `users`
Synced directly from the ID Token / Access Token claims. The `keycloak_id` is the immutable anchor link.

### `user_roles`
Extracted from the `realm_access.roles` claim in the Keycloak JWT. This table enables standard Spring Security RBAC without needing to re-parse the JWT on every single query.

### `todos`
Our primary business table. It relates back to the `users` table via `owner_id`.

```mermaid
graph TD
    U[User: Jaga]
    T1[Todo: Fix Security]
    T2[Todo: Update Docs]
    T3[User: Guest]
    T4[Todo: Some Task]
    
    U -- "id = owner_id" --> T1
    U -- "id = owner_id" --> T2
    T3 -- "id = owner_id" --> T4
```
