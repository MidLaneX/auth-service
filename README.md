# Project Management Tool - Authentication Service

A comprehensive authentication microservice built with Spring Boot, providing secure user authentication, email verification, social login, and JWT token management.

## 🚀 Features

- **User Registration & Login** with email verification
- **JWT Authentication** with secure token management
- **Social Login** (Google OAuth2)
- **Password Reset** functionality
- **Role-Based Access Control** (RBAC) - Admin and User roles
- **Email Verification** with HTML templates
- **Secure Password Encryption** using BCrypt
- **API Documentation** with Swagger/OpenAPI
- **Docker Support** for containerization

## 📋 Prerequisites

- Java 21 or higher
- Maven 3.6+
- PostgreSQL database
- Gmail account for email services (with App Password)
- Google OAuth2 credentials (for social login)

## 🛠️ Quick Setup

### 1. Clone and Navigate
```bash
git clone <repository-url>
cd auth-service
```

### 2. Environment Configuration
Create a `.env` file in the root directory:

```env
# Database Configuration
DB_URL=jdbc:postgresql://your-db-host:5432/auth_service_db
DB_USERNAME=your-db-username
DB_PASSWORD=your-db-password

# Server Configuration
SERVER_PORT=8081

# JWT Configuration (Generate a secure 256-bit key)
JWT_SECRET=your-secure-jwt-secret-key-here
JWT_EXPIRATION=86400000

# Email Configuration (Gmail)
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-gmail-app-password
MAIL_FROM=noreply@projectmanagement.com

# Frontend Configuration
FRONTEND_URL=http://localhost:5173

# Google OAuth Configuration
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
```

### 3. Build and Run
```bash
# Install dependencies
mvn clean install

# Run the application
mvn spring-boot:run
```

The service will start on `http://localhost:8081`

## 📚 API Documentation

Once running, access the Swagger UI at:
- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **API Docs**: http://localhost:8081/v3/api-docs

## 🔗 API Endpoints

### Authentication Endpoints
```
POST /api/auth/register          # User registration
POST /api/auth/login             # User login
POST /api/auth/social-login      # Social login (Google)
POST /api/auth/refresh           # Refresh JWT token
POST /api/auth/forgot-password   # Request password reset
POST /api/auth/reset-password    # Reset password
```

### User Management
```
GET  /api/users                  # Get all users (Admin only)
GET  /api/users/profile          # Get current user profile
PUT  /api/users/profile          # Update user profile
```

### Email Verification
```
POST /api/email/send-verification    # Send verification email
GET  /api/email/verify              # Verify email with token
POST /api/email/resend-verification  # Resend verification email
```

### Admin Endpoints
```
GET  /api/admin/users           # Get all users
PUT  /api/admin/users/{id}/role # Update user role
DELETE /api/admin/users/{id}    # Delete user
```

## 📧 Email Configuration

### Gmail Setup
1. Enable 2-Factor Authentication on your Gmail account
2. Generate an App Password:
   - Go to Google Account settings
   - Security → App passwords
   - Generate a password for "Mail"
3. Use this App Password in your `.env` file

## 🔐 Security Features

- **JWT Authentication** with secure token generation
- **Password Encryption** using BCrypt
- **Email Verification** mandatory for account activation
- **Role-Based Access Control** (ADMIN, USER)
- **Rate Limiting** for authentication endpoints
- **CORS Configuration** for frontend integration

## 🏗️ Project Structure

```
src/main/java/com/midlane/project_management_tool_auth_service/
├── config/          # Configuration classes
├── controller/      # REST controllers
├── dto/            # Data Transfer Objects
├── exception/      # Custom exceptions
├── model/          # JPA entities
├── repository/     # JPA repositories
├── security/       # Security configurations
├── service/        # Business logic
└── util/           # Utility classes
```

## 🐳 Docker Support

### Build Docker Image
```bash
docker build -t auth-service:latest .
```

### Run with Docker Compose
```bash
docker-compose up -d
```

This will start:
- PostgreSQL database on port 5432
- Auth service on port 8081

## 🔧 Development

### Running Tests
```bash
mvn test
```

### Building for Production
```bash
mvn clean package -DskipTests
```

### Environment Profiles
- `dev` - Development with H2 database
- `prod` - Production with PostgreSQL

## 🚨 Troubleshooting

### Common Issues

1. **Database Connection Failed**
   - Verify PostgreSQL is running
   - Check database URL and credentials in `.env`
   - Ensure database exists

2. **JWT Token Issues**
   - Ensure JWT_SECRET is at least 256 bits (32 characters)
   - Check token expiration settings

3. **Email Not Sending**
   - Verify Gmail App Password is correct
   - Check SMTP settings
   - Ensure 2FA is enabled on Gmail

4. **Port Already in Use**
   - Change SERVER_PORT in `.env`
   - Kill process using the port: `netstat -ano | findstr :8081`

## 📝 Example API Calls

### Register User
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "password": "securePassword123"
  }'
```

### Login
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "securePassword123"
  }'
```

### Get Users (with JWT)
```bash
curl -X GET http://localhost:8081/api/users \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📄 License

This project is licensed under the MIT License.

## 📞 Support

For issues and questions:
- Create an issue in the repository
- Check the troubleshooting section
- Review API documentation

---

**Note**: Make sure to keep your `.env` file secure and never commit it to version control. Use `.env.example` for sharing configuration templates.
