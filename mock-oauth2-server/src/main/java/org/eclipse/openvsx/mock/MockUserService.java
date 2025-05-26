package org.eclipse.openvsx.mock;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class MockUserService {
    
    private final Map<String, MockUser> users = new HashMap<>();
    private final Map<String, String> authCodes = new HashMap<>();
    private final Map<String, String> accessTokens = new HashMap<>();

    public MockUserService() {
        // Initialize hardcoded test users
        users.put("admin", new MockUser("admin", "admin123", "admin@example.com", "Admin User", "admin"));
        users.put("user", new MockUser("user", "user123", "user@example.com", "Regular User", "user"));
        users.put("publisher", new MockUser("publisher", "pub123", "publisher@example.com", "Publisher User", "privileged"));
        users.put("testuser", new MockUser("testuser", "test123", "test@example.com", "Test User", "user"));
    }

    public MockUser authenticate(String username, String password) {
        MockUser user = users.get(username);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    public String generateAuthCode(String username) {
        String code = UUID.randomUUID().toString();
        authCodes.put(code, username);
        return code;
    }

    public String exchangeCodeForToken(String code, String clientId, String clientSecret) {
        // Simple validation - in real implementation you'd validate client credentials
        if (!"local-test".equals(clientId) || !"test-secret".equals(clientSecret)) {
            return null;
        }
        
        String username = authCodes.get(code);
        if (username != null) {
            authCodes.remove(code); // One-time use
            String token = UUID.randomUUID().toString();
            accessTokens.put(token, username);
            return token;
        }
        return null;
    }

    public MockUser getUserByToken(String token) {
        String username = accessTokens.get(token);
        if (username != null) {
            return users.get(username);
        }
        return null;
    }

    public Map<String, MockUser> getAllUsers() {
        return new HashMap<>(users);
    }
}