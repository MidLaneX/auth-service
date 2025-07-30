# Project Management Tool - Auth Service

A comprehensive authentication service for the Project Management Tool with JWT-based authentication, email verification, and social login support (Google & Facebook).

## Features

- **User Registration & Login** - Traditional email/password authentication
- **JWT Token Authentication** - Secure stateless authentication
- **Email Verification** - Email verification system with secure tokens
- **Social Login** - Google and Facebook OAuth integration
- **Role-Based Access Control (RBAC)** - Admin and User roles
- **Password Reset** - Secure password reset functionality
- **User Management** - Admin endpoints for user management

## API Endpoints

### Authentication Endpoints

#### Traditional Authentication
- `POST /api/auth/initial/register` - User registration
- `POST /api/auth/initial/login` - User login

#### Social Authentication
- `POST /api/auth/initial/social/login` - Generic social login (specify provider in request)
- `POST /api/auth/initial/social/google` - Google OAuth login
- `POST /api/auth/initial/social/facebook` - Facebook OAuth login

#### Email Verification
- `POST /api/auth/email/send-verification` - Send verification email
- `GET /api/auth/email/verify` - Verify email with token
- `POST /api/auth/email/resend-verification` - Resend verification email

#### Password Management
- `POST /api/auth/password/forgot` - Request password reset
- `POST /api/auth/password/reset` - Reset password with token

### User Management Endpoints

#### User Endpoints
- `GET /api/user/profile` - Get user profile
- `PUT /api/user/profile` - Update user profile

#### Admin Endpoints
- `GET /api/admin/users` - Get all users (Admin only)
- `PUT /api/admin/users/{userId}/role` - Update user role (Admin only)

## Social Login Implementation

### Google Login Flow

1. **Frontend Integration**
   - Use Google Sign-In JavaScript library or OAuth2 flow
   - Obtain access token from Google
   - Send token to backend

2. **API Request**
   ```json
   POST /api/auth/initial/social/google
   {
     "accessToken": "google_access_token_here"
   }
   ```

3. **Response**
   ```json
   {
     "token": "jwt_token",
     "userId": 123,
     "email": "user@example.com",
     "role": "USER",
     "emailVerified": true,
     "message": "Login successful via google!"
   }
   ```

### Facebook Login Flow

1. **Frontend Integration**
   - Use Facebook SDK for JavaScript or OAuth2 flow
   - Obtain access token from Facebook
   - Send token to backend

2. **API Request**
   ```json
   POST /api/auth/initial/social/facebook
   {
     "accessToken": "facebook_access_token_here"
   }
   ```

3. **Response**
   ```json
   {
     "token": "jwt_token",
     "userId": 124,
     "email": "user@facebook.com",
     "role": "USER",
     "emailVerified": true,
     "message": "Login successful via facebook!"
   }
   ```

### Generic Social Login

For flexibility, you can also use the generic endpoint:

```json
POST /api/auth/initial/social/login
{
  "accessToken": "provider_access_token_here",
  "provider": "google" // or "facebook"
}
```

## Request/Response Examples

### User Registration
```json
POST /api/auth/initial/register
{
  "email": "user@example.com",
  "password": "securePassword123",
  "phone": "+1234567890"
}
```

### User Login
```json
POST /api/auth/initial/login
{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

### Successful Authentication Response
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 123,
  "email": "user@example.com",
  "role": "USER",
  "emailVerified": true,
  "message": "Login successful!"
}
```

### Error Response
```json
{
  "error": "INVALID_CREDENTIALS",
  "message": "Invalid email or password"
}
```

## Social Login User Flow

### New User (First-time Social Login)
1. User authenticates with Google/Facebook
2. System creates new user account with social provider data
3. Email is automatically verified (if provided by social provider)
4. User gets JWT token and can access the application

### Existing User (Linking Social Account)
1. User with existing local account logs in via social provider
2. System links social provider to existing account
3. User can now login using either method (local or social)

### Existing Social User (Subsequent Logins)
1. User logs in with same social provider
2. System recognizes existing account
3. User gets JWT token immediately

## Environment Configuration

### Required Environment Variables

```env
# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=auth_service_db
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password

# JWT Configuration
JWT_SECRET=your_256_bit_secret_key_here
JWT_EXPIRATION=86400000

# Email Configuration (Gmail SMTP)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
MAIL_FROM=your_email@gmail.com

# Application Configuration
SERVER_PORT=8080
APP_BASE_URL=http://localhost:8080
```

### Gmail Setup for Email Verification

1. Enable 2-Factor Authentication on your Gmail account
2. Generate an App Password:
   - Go to Google Account settings
   - Security → 2-Step Verification → App passwords
   - Generate password for "Mail"
3. Use the generated app password as `MAIL_PASSWORD`

## Running the Application

### Using Docker Compose (Recommended)

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f auth-service

# Stop services
docker-compose down
```

### Using Maven

```bash
# Install dependencies
mvn clean install

# Run application
mvn spring-boot:run

# Or run jar file
java -jar target/project-management-tool-auth-service-0.0.1-SNAPSHOT.jar
```

## Security Features

- **JWT Authentication** - Stateless token-based authentication
- **Password Encoding** - BCrypt password hashing
- **CORS Configuration** - Cross-origin resource sharing setup
- **Rate Limiting** - Protection against brute force attacks
- **Input Validation** - Request validation with proper error handling
- **Social OAuth** - Secure integration with Google and Facebook

## Database Schema

The service uses PostgreSQL with the following key tables:

- **users** - User account information
- **email_verification_tokens** - Email verification tokens
- **password_reset_tokens** - Password reset tokens

Key user fields for social login:
- `provider` - Authentication provider (LOCAL, GOOGLE, FACEBOOK)
- `provider_id` - Social provider user ID
- `first_name`, `last_name` - User names from social providers
- `profile_picture_url` - Profile picture from social providers

## API Documentation

When the application is running, you can access:
- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **API Docs**: http://localhost:8080/v3/api-docs

## Error Handling

The API provides consistent error responses with appropriate HTTP status codes:

- `400 Bad Request` - Invalid request data
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `409 Conflict` - Resource already exists
- `500 Internal Server Error` - Server error

## Development Notes

### Adding New Social Providers

To add a new social provider:

1. Add provider to `AuthProvider` enum
2. Implement provider-specific user info fetching in `SocialAuthService`
3. Add endpoint in `AuthController`
4. Update documentation

### Testing Social Login

For testing social login integration:

1. Set up Google/Facebook developer applications
2. Configure OAuth callback URLs
3. Use provider SDKs in frontend to obtain access tokens
4. Test with real tokens from provider APIs

## Support

For issues and questions:
- Check application logs
- Verify environment variables
- Ensure database connectivity
- Validate social provider tokens


cml-insight
omobio