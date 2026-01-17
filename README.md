# TutorService - Authentication Feature

This branch (`feature/auth`) implements the core security and authentication layer for the TutorService application.

## Overview
The project is built using **Java 25** and **Spring Boot 4.0.1**.

**Key Implementation Details:**
- **Stateless Authentication**: Utilizes JSON Web Tokens (JWT) for secure, stateless client-server communication.
- **Token Management**: Implements Redis for managing refresh tokens and enforcing a logout blocklist.
- **Access Control**: Features a comprehensive Role-Based Access Control (RBAC) schema including Users, Roles, and Permissions.
- **Security Configuration**: Leveraging Spring Security 7 for endpoint protection and filter chains.

## Key Capabilities
1.  **Authentication**: Secure Registration and Login endpoints using BCrypt password hashing.
2.  **Session Management**: Implementation of Refresh Tokens to maintain user sessions securely without long-lived access tokens.
3.  **Logout Mechanism**: Server-side token invalidation using a Redis execution strategy.
4.  **Database Seeding**: Automatic initialization of essential Roles (e.g., USER, ADMIN) upon application startup.

## Testing
We utilize the IntelliJ IDEA **HTTP Client** for API testing.

**Files:**
- `requests.http`: Contains the definitions for all API endpoints (Register, Login, User Details).
- `http-client.env.json`: Stores environment-specific configuration and test credentials.

**Testing Workflow:**
1.  Ensure Docker containers for PostgreSQL and Redis are active: `docker compose up -d`.
2.  Start the Spring Boot application.
3.  Execute the **Login** request in `requests.http`. The script will automatically capture the returned JWT and assign it to the environment variables.
4.  Execute authenticated requests (e.g., Greeting endpoint) to verify access.

## Known Issues
- The `POST /auth/register` endpoint is currently returning `401 Unauthorized` due to a pending configuration adjustment in `SecurityConfig`. The Login flow functions correctly for existing users.
