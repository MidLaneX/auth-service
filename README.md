# Project Management Tool - Authentication Service

## 🎯 Overview

A robust Spring Boot authentication service for the Project Management Tool that provides secure user registration, login, email verification, JWT token management, and role-based access control (RBAC).

## 🚀 Features

- **User Registration & Login** - Secure user authentication with email verification
- **JWT Token Management** - Stateless authentication using JSON Web Tokens
- **Email Verification** - Email-based account verification with HTML templates
- **Role-Based Access Control (RBAC)** - Admin and User role management
- **Password Security** - BCrypt password hashing
- **Email Services** - Welcome emails, password reset, and verification emails
- **RESTful API** - Clean REST endpoints for all authentication operations
- **Environment Configuration** - Secure configuration management with .env files

## 📋 Prerequisites

Before running this service, ensure you have:

- **Java 17+** installed
- **Maven 3.6+** installed
- **PostgreSQL 12+** running on port 5434
- **Gmail App Password** for email services (optional but recommended)

## 🛠️ Technology Stack

- **Spring Boot 3.x** - Main framework
- **Spring Security 6.x** - Authentication and authorization
- **Spring Data JPA** - Database operations
- **PostgreSQL** - Primary database
- **JWT (JSON Web Tokens)** - Stateless authentication
- **JavaMailSender** - Email services
- **Lombok** - Reduce boilerplate code
- **BCrypt** - Password hashing

## 📁 Project Structure

```
src/main/java/com/midlane/project_management_tool_auth_service/
├── config/           # Configuration classes
├── controller/       # REST API controllers
├── dto/             # Data Transfer Objects
├── exception/       # Custom exceptions
├── model/           # JPA entities
├── repository/      # Data access layer
├── security/        # Security configuration & JWT
├── service/         # Business logic
└── util/           # Utility classes
```

## ⚙️ Configuration

### 1. Environment Setup

Copy the `.env.example` to `.env` and configure:

```env
# Database Configuration
DB_URL=jdbc:postgresql://localhost:5434/auth_service_db
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Server Configuration
SERVER_PORT=8081

# JWT Configuration
JWT_SECRET=dGhpcyBpcyBhIDI1NiBiaXQgc2VjdXJlIGtleSBmb3IgSldUIGF1dGhlbnRpY2F0aW9uIQ==
JWT_EXPIRATION=86400000

# Email Configuration
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_FROM=noreply@projectmanagement.com
FRONTEND_URL=http://localhost:5173
```

### 2. Gmail Configuration (Email Services)

To enable email services:

1. Go to [Google Account Settings](https://myaccount.google.com)
2. Enable **2-Factor Authentication**
3. Navigate to **Security > 2-Step Verification > App passwords**
4. Generate a new app password for "Mail"
5. Use the 16-digit code in `MAIL_PASSWORD`
6. Replace `MAIL_USERNAME` with your Gmail address

### 3. Database Setup

Create PostgreSQL database:

```sql
CREATE DATABASE auth_service_db;
```

The application will automatically create tables on startup.

## 🚀 Running the Application

### Method 1: Using Maven

```bash
# Install dependencies
mvn clean install

# Run the application
mvn spring-boot:run
```

### Method 2: Using JAR

```bash
# Build JAR
mvn clean package

# Run JAR
java -jar target/project-management-tool-auth-service-0.0.1-SNAPSHOT.jar
```

The service will start on `http://localhost:8082`

## 📡 API Endpoints

### Authentication Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/auth/register` | User registration | No |
| POST | `/api/auth/login` | User login | No |
| GET | `/api/auth/verify-email` | Email verification | No |
| POST | `/api/auth/resend-verification` | Resend verification email | No |

### User Management Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/users/profile` | Get user profile | Yes |
| GET | `/api/users/all` | Get all users (non-sensitive data) | Yes (USER) |

### Admin Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/admin/users` | Get all users with full details | Yes (ADMIN) |
| DELETE | `/api/admin/users/{id}` | Delete user | Yes (ADMIN) |
| PUT | `/api/admin/users/{id}/role` | Update user role | Yes (ADMIN) |

## 📝 API Usage Examples

### 1. User Registration

```bash
curl -X POST http://localhost:8082/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "securePassword123",
    "phone": "+1234567890"
  }'
```

**Response:**
```json
{
  "message": "Registration successful. Please check your email for verification.",
  "success": true
}
```

### 2. User Login

```bash
curl -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "securePassword123"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "user@example.com",
  "role": "USER",
  "expiresIn": 86400000
}
```

### 3. Get All Users (Non-Sensitive)

```bash
curl -X GET http://localhost:8082/api/users/all \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response:**
```json
[
  {
    "userId": 1,
    "email": "user@example.com",
    "phone": "+1234567890",
    "role": "USER",
    "userCreated": "2025-01-15T10:30:00"
  }
]
```

## 🔐 Security Features

### JWT Token Authentication
- **256-bit secure key** for token signing
- **24-hour expiration** (configurable)
- **Stateless authentication** - no server-side sessions

### Password Security
- **BCrypt hashing** with secure rounds
- **Minimum password requirements** (can be configured)

### Role-Based Access Control (RBAC)
- **USER Role**: Basic user operations
- **ADMIN Role**: Full administrative access
- **Endpoint-level security** with method annotations

### Email Verification
- **Secure token generation** for email verification
- **24-hour expiration** for verification links
- **HTML email templates** with professional styling

#### 2. Email Authentication Failed
- Ensure 2FA is enabled on your Google account
- Use App Password, not your regular Gmail password
- Check that `MAIL_USERNAME` and `MAIL_PASSWORD` are correctly set in `.env`

#### 3. JWT Token Issues
- Ensure `JWT_SECRET` is at least 256-bit (32+ characters)
- Check token expiration time
- Verify Bearer token format in requests

#### 4. Database Connection Issues
- Ensure PostgreSQL is running on port 5434
- Check database credentials in `.env`
- Verify database `auth_service_db` exists

### Logs

Application logs provide detailed information:
- **INFO level**: Normal operations
- **ERROR level**: Exceptions and failures
- **DEBUG level**: Detailed debugging (development only)

## 🧪 Testing

### Manual Testing with Postman

1. Import the API endpoints into Postman
2. Register a new user
3. Check email for verification link
4. Login with verified credentials
5. Test protected endpoints with JWT token

### Health Check

```bash
curl http://localhost:8081/actuator/health
```



**Last Updated**: July 2025
**Version**: 0.0.1-SNAPSHOT
