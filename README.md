# Ecommerce API Gateway

The API Gateway is built using Spring Cloud Gateway, serving as the single entry point for the microservices architecture. It handles authentication (OAuth2/Keycloak), rate limiting, session management, and a token blacklist system.

##  Key Features

- **Unified Routing**: Forwards requests to downstream services (e.g., ecommerce-service).
- **Security**: Integrated with Keycloak OAuth2 Login (Authorization Code Flow with PKCE).
- **Session Management**: Centralized session storage using Redis.
- **Token Blacklist**: A proactive mechanism to invalidate sessions immediately when user roles are changed in Keycloak (via Webhooks).
- **CORS Configuration**: Secure resource sharing for Frontend applications.
- **Global Error Handling**: Centralized exception processing with consistent JSON error responses.

##  Tech Stack

- **Java 17**
- **Spring Boot 3.x**
- **Spring Cloud Gateway**
- **Spring Security (Reactive/WebFlux)**
- **Spring Data Redis (Reactive)**
- **Keycloak** (Identity Provider)

## Configuration

Create a `.env` file in the root directory and configure the following variables:

```env
# Server Config
SERVER_PORT=
FRONTEND_URL=

# Redis Config
REDIS_HOST=
REDIS_PORT=
REDIS_PASSWORD=

# Keycloak Config
KEYCLOAK_URL=
KEYCLOAK_REALM=

# Blacklist Config
ACCESS_TOKEN_LIFESPAN=
BLACKLIST_PREFIX=
```

##  How to Run

1. **Start Redis**: Ensure Redis is running locally or via Docker.
2. **Setup Keycloak**: Ensure the `Ecommerce-Realm` and `client-springboot` are properly configured.
3. **Run the Application**:
   ```bash
   mvn spring-boot:run
   ```

## 🛡 Important API Endpoints

| Endpoint | Description | Access |
|----------|-------------|--------|
| `/oauth2/authorization/keycloak` | Initiates Keycloak login flow | Public |
| `/api/v1/**` | Proxies requests to Resource Servers | Authenticated |
| `/api/v1/auth/webhook` | Receives events from Keycloak (Role Change) | Public |
| `/api/v1/auth/logout-all` | Invalidates all user sessions | Authenticated |

## Project Structure

- `config/`: Security configurations and Bean definitions.
- `controller/`: Auth and Webhook API controllers.
- `dto/`: Data Transfer Objects for error responses.
- `exception/`: Global error handling and custom exceptions.
- `security/`: WebFilters (e.g., TokenBlacklistFilter).
- `services/`: Business logic implementations.
