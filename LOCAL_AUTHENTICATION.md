# Local Authentication for OpenVSX

This setup provides a local authentication solution for OpenVSX testing without requiring external OAuth2 providers like GitHub, GitLab, etc.

## Overview

The local authentication system consists of:

1. **Mock OAuth2 Server** - A simple Spring Boot application that simulates an OAuth2 provider
2. **OpenVSX Configuration** - Modified to use the local OAuth2 server
3. **Predefined Test Users** - Hardcoded users with different roles for testing

## Architecture

```
┌─────────────────┐    OAuth2 Flow    ┌──────────────────┐
│   OpenVSX       │ ◄──────────────── │ Mock OAuth2      │
│   (Port 8080)   │                   │ Server           │
│                 │ ──────────────────► │ (Port 9999)      │
└─────────────────┘                   └──────────────────┘
```

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven (for mock server)
- Gradle (for OpenVSX server)

### Starting the Services

1. **Simple Start (Recommended)**:
   ```bash
   ./start-local-auth.sh
   ```

2. **Manual Start**:
   ```bash
   # Terminal 1: Start Mock OAuth2 Server
   cd mock-oauth2-server
   ./mvnw spring-boot:run
   
   # Terminal 2: Start OpenVSX with local auth profile
   cd server
   ./gradlew bootRun --args='--spring.profiles.active=local-auth'
   ```

### Accessing the Application

- **OpenVSX**: http://localhost:8080
- **Mock OAuth2 Server**: http://localhost:9999

## Test Users

The system comes with predefined test users:

| Username  | Password  | Role        | Description                    |
|-----------|-----------|-------------|--------------------------------|
| admin     | admin123  | admin       | Full administrative access     |
| user      | user123   | user        | Regular user permissions       |
| publisher | pub123    | privileged  | Can publish to any namespace   |
| testuser  | test123   | user        | Additional test user           |

## Authentication Flow

1. User visits OpenVSX and clicks "Login"
2. OpenVSX redirects to Mock OAuth2 Server (`http://localhost:9999/oauth/authorize`)
3. User sees a login form with available test users
4. User selects or enters credentials
5. Mock server redirects back to OpenVSX with authorization code
6. OpenVSX exchanges code for access token
7. OpenVSX fetches user info and creates/updates user account

## Configuration Details

### Mock OAuth2 Server

The mock server provides these OAuth2 endpoints:

- **Authorization**: `GET /oauth/authorize`
- **Token Exchange**: `POST /oauth/token`
- **User Info**: `GET /user`
- **Server Info**: `GET /`

### OpenVSX Configuration

The local authentication is configured in `application-local-auth.yml`:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          local-test:
            client-id: local-test
            client-secret: test-secret
            # ... other OAuth2 settings

ovsx:
  oauth2:
    primary-provider: local-test
    attribute-names:
      local-test:
        # Maps OAuth2 user attributes to OpenVSX user fields
```

## Customization

### Adding New Users

Edit `MockUserService.java` in the mock server:

```java
public MockUserService() {
    users.put("newuser", new MockUser("newuser", "password", "email@example.com", "Full Name", "role"));
}
```

### Changing User Roles

Available roles in OpenVSX:
- `admin` - Full administrative access
- `privileged` - Can publish to any namespace
- `user` - Regular user (default)

### Modifying Server Ports

1. **Mock OAuth2 Server**: Edit `mock-oauth2-server/src/main/resources/application.yml`
2. **OpenVSX**: Edit `server/src/main/resources/application-local-auth.yml`

## Security Considerations

⚠️ **This setup is for testing only!**

- Passwords are stored in plain text
- No HTTPS/TLS encryption
- Simple client authentication
- No session management
- No rate limiting

**Never use this in production!**

## Troubleshooting

### Common Issues

1. **Port Already in Use**:
   ```bash
   # Check what's using the port
   lsof -i :9999
   lsof -i :8080
   
   # Kill processes if needed
   kill -9 <PID>
   ```

2. **Mock Server Not Starting**:
   ```bash
   # Check logs
   tail -f mock-oauth2-server.log
   
   # Verify Java version
   java -version
   ```

3. **OpenVSX Can't Connect to Mock Server**:
   - Ensure mock server is running on port 9999
   - Check firewall settings
   - Verify URLs in configuration

4. **Authentication Fails**:
   - Check that usernames/passwords match exactly
   - Verify OAuth2 client credentials
   - Check server logs for errors

### Debug Mode

Enable debug logging by adding to `application-local-auth.yml`:

```yaml
logging:
  level:
    org.eclipse.openvsx.security: DEBUG
    org.springframework.security: DEBUG
```

## Development

### Project Structure

```
openvsx/
├── mock-oauth2-server/          # Mock OAuth2 provider
│   ├── src/main/java/           # Java source code
│   ├── src/main/resources/      # Configuration and templates
│   └── pom.xml                  # Maven configuration
├── server/                      # OpenVSX server
│   └── src/main/resources/
│       └── application-local-auth.yml  # Local auth config
├── start-local-auth.sh          # Startup script
└── LOCAL_AUTHENTICATION.md     # This documentation
```

### Extending the Mock Server

The mock OAuth2 server can be extended to:

- Support additional OAuth2 flows
- Add user management endpoints
- Implement proper session handling
- Add HTTPS support
- Integrate with databases

### Integration with Real OAuth2 Providers

To switch back to real OAuth2 providers:

1. Remove `local-auth` from active profiles
2. Configure real OAuth2 providers in `application.yml`
3. Set appropriate environment variables for client credentials

## API Reference

### Mock OAuth2 Server Endpoints

#### GET /
Returns server information and available users.

#### GET /oauth/authorize
OAuth2 authorization endpoint. Displays login form.

**Parameters**:
- `client_id` - OAuth2 client identifier
- `redirect_uri` - Callback URL
- `response_type` - Should be "code"
- `scope` - Requested permissions
- `state` - Optional state parameter

#### POST /oauth/token
OAuth2 token exchange endpoint.

**Parameters**:
- `grant_type` - Should be "authorization_code"
- `code` - Authorization code from /oauth/authorize
- `client_id` - OAuth2 client identifier
- `client_secret` - OAuth2 client secret

**Response**:
```json
{
  "access_token": "uuid-token",
  "token_type": "Bearer",
  "expires_in": 3600,
  "scope": "read:user"
}
```

#### GET /user
Returns user information for the authenticated user.

**Headers**:
- `Authorization: Bearer <access_token>`

**Response**:
```json
{
  "username": "admin",
  "email": "admin@example.com",
  "name": "Admin User",
  "avatar_url": "https://avatars.githubusercontent.com/u/1?v=4",
  "html_url": "http://localhost:9999/users/admin",
  "role": "admin"
}
```

## License

This local authentication setup follows the same license as the OpenVSX project (Eclipse Public License v. 2.0).