# Project Management Tool - Authentication Service

A secure authentication microservice built with Spring Boot 3.5.3 and Java 21, providing JWT-based authentication and user management for a project management system.

## 🚀 Features

- **User Registration** - Secure user account creation with email validation
- **JWT Authentication** - Stateless authentication using JSON Web Tokens
- **Password Security** - BCrypt password hashing for enhanced security
- **User Management** - Retrieve user information (excluding sensitive data)
- **Database Integration** - PostgreSQL database with JPA/Hibernate
- **Security Configuration** - Spring Security with custom filters
- **RESTful API** - Clean REST endpoints following best practices

## 🛠️ Tech Stack

- **Java 21** - Latest LTS version of Java
- **Spring Boot 3.5.3** - Modern Spring framework
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Data persistence layer
- **PostgreSQL** - Primary database
- **JWT (JSON Web Tokens)** - Stateless authentication
- **Lombok** - Reducing boilerplate code
- **Maven** - Dependency management and build tool

## 📋 Prerequisites

Before running this application, make sure you have:

- Java 21 or higher installed
- Maven 3.6+ installed
- PostgreSQL database server running
- Git for version control

## ⚙️ Installation & Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd auth-service
```

### 2. Database Setup
Create a PostgreSQL database:
```sql
CREATE DATABASE auth_service_db;
```

### 3. Configure Application Properties
Update `src/main/resources/application.properties`:
```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5433/auth_service_db
spring.datasource.username=your_username
spring.datasource.password=your_password

# Server Configuration
server.port=8081

# JWT Configuration (Update for production)
jwt.secret=yourSecretKeyHereShouldBeLongAndComplexForProductionEnvironment
jwt.expiration=86400000
```

### 4. Build and Run
```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The service will start on `http://localhost:8081`

## 📚 API Documentation

### Base URL
```
http://localhost:8081/api/auth
```

### Endpoints

#### 1. User Registration
**POST** `/register`

Register a new user account.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "securePassword123",
  "phone": "+1234567890"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "email": "user@example.com"
}
```

#### 2. Get All Users
**GET** `/users`

Retrieve all registered users (non-sensitive information only).

**Response:**
```json
[
  {
    "userId": 1,
    "email": "user1@example.com",
    "phone": "+1234567890",
    "userCreated": "2025-07-17T15:30:45.123"
  },
  {
    "userId": 2,
    "email": "user2@example.com",
    "phone": "+0987654321",
    "userCreated": "2025-07-17T16:45:22.456"
  }
]
```

## 🏗️ Project Structure

```
src/
├── main/
│   ├── java/com/midlane/project_management_tool_auth_service/
│   │   ├── config/          # Security and JWT configuration
│   │   ├── controller/      # REST API controllers
│   │   ├── dto/            # Data Transfer Objects
│   │   ├── model/          # JPA entities
│   │   ├── repository/     # Data access layer
│   │   ├── service/        # Business logic
│   │   └── util/           # Utility classes
│   └── resources/
│       └── application.properties
└── test/                   # Unit and integration tests
```

## 🔒 Security Features

- **JWT Authentication** - Secure stateless authentication
- **Password Hashing** - BCrypt encryption for password storage
- **CORS Configuration** - Cross-origin resource sharing setup
- **Input Validation** - Request data validation
- **Secure Headers** - Security headers configuration

## 📝 Environment Variables

For production deployment, set these environment variables:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://your-db-host:5432/auth_service_db
SPRING_DATASOURCE_USERNAME=your_username
SPRING_DATASOURCE_PASSWORD=your_password
JWT_SECRET=your-super-secure-jwt-secret-key
JWT_EXPIRATION=86400000
SERVER_PORT=8081
```




---

## 🔮 Future Enhancements

- [ ] User login functionality
- [ ] Password reset via email
- [ ] Multi-factor authentication (MFA)
- [ ] OAuth2 integration
- [ ] User role management
- [ ] Account verification
- [ ] Audit logging
- [ ] Rate limiting
- [ ] API documentation with Swagger
- [ ] Docker containerization

## 📊 Status

- ✅ User Registration
- ✅ JWT Token Generation
- ✅ User Data Retrieval
- ✅ Security Configuration
- 🔄 Login Functionality (In Progress)
- 🔄 Authentication Middleware (In Progress)

---

**Note**: This is a development version. For production use, ensure to update JWT secrets, database credentials, and other security configurations.

** To create a PostgreSQL container for development, you can use the following command:**

```bash
docker run -d -p 5434:5432 --name pg-auth-service -v postgres-data:/var/lib/postgresql/data -e POSTGRES_PASSWORD=pasindu postgres
```
