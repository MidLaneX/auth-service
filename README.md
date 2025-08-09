# Authentication Service

A microservice for user authentication and authorization in the Project Management Tool ecosystem.

## 🏗️ System Design

### Core Responsibilities
- User registration and authentication (email/password + social login)
- JWT token generation and validation with RSA keys
- Email verification workflow
- Role-based access control (USER, ADMIN)
- User event publishing via Kafka

### Architecture
- **Framework**: Spring Boot 3.x with Java 21
- **Database**: PostgreSQL for user data persistence
- **Security**: RSA-based JWT tokens with refresh token mechanism
- **Messaging**: Kafka for user event publishing
- **External APIs**: Google OAuth2 for social authentication

## 📡 API Endpoints

### Authentication APIs
```
POST /api/auth/initial/register       # Register with email/password
POST /api/auth/initial/login          # Login with email/password  
POST /api/auth/initial/social/login   # Social login (Google/Facebook)
POST /api/auth/initial/refresh        # Refresh access token
POST /api/auth/initial/logout         # Logout (revoke refresh token)
POST /api/auth/initial/logout-all     # Logout from all devices
GET  /api/auth/initial/public-key     # Get RSA public key for JWT verification
```

### User Management APIs
```
GET  /api/users                      # Get all users (Admin only)
GET  /api/users/profile              # Get current user profile
PUT  /api/users/profile              # Update user profile
PUT  /api/users/{id}/role            # Update user role (Admin only)
DELETE /api/users/{id}               # Delete user (Admin only)
```

### Email Verification APIs
```
POST /api/email/send-verification    # Send verification email
GET  /api/email/verify               # Verify email with token
POST /api/email/resend-verification  # Resend verification email
```

## 📨 Kafka Events

The service publishes minimal user events to inform other microservices:

### Topic: `user.added`

**Event Structure:**
```json
{
  "userId": 123,
  "email": "user@example.com",
  "eventType": "USER_CREATED" | "USER_UPDATED" | "USER_DELETED"
}
```

**Event Triggers:**
- `USER_CREATED`: New user registration (email/password or social login)
- `USER_UPDATED`: User profile updates, password changes, role changes
- `USER_DELETED`: User account deletion

**Key Design Decision**: Only essential data (userId, email) is published via Kafka. Full user details (name, profile picture, etc.) remain in the auth service database for privacy and data minimization.

## 🔐 Security Model

### JWT Token Strategy
- **Access Tokens**: Short-lived (15 minutes), RSA-signed JWT tokens
- **Refresh Tokens**: Long-lived (7 days), stored securely with device tracking
- **RSA Keys**: Public/private key pair for token signing and verification

### User Data Model
```java
// Essential fields for all authentication types
userId: Long              // Primary identifier
email: String            // Unique, required
phone: String            // Optional
passwordHash: String     // BCrypt hashed (null for social login)
role: Role              // USER or ADMIN
emailVerified: Boolean   // Email verification status
createdAt: LocalDateTime
updatedAt: LocalDateTime

// Social login fields (when applicable)
provider: AuthProvider   // LOCAL, GOOGLE, FACEBOOK
providerId: String       // Provider's user ID
firstName: String        // From social provider
lastName: String         // From social provider
profilePictureUrl: String // From social provider
```

### Authentication Flow
1. **Registration**: Email verification required for local accounts
2. **Login**: Returns access + refresh tokens
3. **Token Refresh**: Exchange refresh token for new access token
4. **Social Login**: Auto-registration if email doesn't exist

## 🔗 Integration Points

### With API Gateway
- Exposes RSA public key at `/api/auth/initial/public-key`
- Gateway validates JWT tokens using this public key
- No direct database access needed by gateway

### With Other Microservices
- Publishes user events via Kafka
- Other services can subscribe to `user.added` topic
- Services receive minimal user data (userId + email only)

### External Dependencies
- **PostgreSQL**: User data persistence
- **Kafka**: Event publishing
- **Gmail SMTP**: Email verification
- **Google OAuth2**: Social authentication
- **Facebook Graph API**: Social authentication (optional)

## 📊 Port & Service Discovery
- **Service Port**: 8081
- **Health Check**: `/actuator/health`
- **API Documentation**: `/swagger-ui.html`
- **Metrics**: `/actuator/metrics`
