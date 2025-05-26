package org.eclipse.openvsx.mock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
public class OAuth2Controller {

    @Autowired
    private MockUserService userService;

    @GetMapping("/oauth/authorize")
    public String authorize(@RequestParam String client_id,
                          @RequestParam String redirect_uri,
                          @RequestParam String response_type,
                          @RequestParam(required = false) String scope,
                          @RequestParam(required = false) String state,
                          Model model) {
        
        // Store parameters for later use
        model.addAttribute("client_id", client_id);
        model.addAttribute("redirect_uri", redirect_uri);
        model.addAttribute("response_type", response_type);
        model.addAttribute("scope", scope);
        model.addAttribute("state", state);
        model.addAttribute("users", userService.getAllUsers());
        
        return "login";
    }

    @PostMapping("/oauth/authorize")
    public String handleLogin(@RequestParam String username,
                            @RequestParam String password,
                            @RequestParam String client_id,
                            @RequestParam String redirect_uri,
                            @RequestParam String response_type,
                            @RequestParam(required = false) String scope,
                            @RequestParam(required = false) String state,
                            Model model) {
        
        MockUser user = userService.authenticate(username, password);
        if (user == null) {
            model.addAttribute("error", "Invalid username or password");
            model.addAttribute("client_id", client_id);
            model.addAttribute("redirect_uri", redirect_uri);
            model.addAttribute("response_type", response_type);
            model.addAttribute("scope", scope);
            model.addAttribute("state", state);
            model.addAttribute("users", userService.getAllUsers());
            return "login";
        }

        // Generate authorization code
        String code = userService.generateAuthCode(username);
        
        // Redirect back to client with code
        String redirectUrl = redirect_uri + "?code=" + code;
        if (state != null) {
            redirectUrl += "&state=" + state;
        }
        
        return "redirect:" + redirectUrl;
    }

    @PostMapping("/oauth/token")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> token(@RequestParam String grant_type,
                                                   @RequestParam String code,
                                                   @RequestParam(required = false) String client_id,
                                                   @RequestParam(required = false) String client_secret,
                                                   @RequestParam(required = false) String redirect_uri,
                                                   @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        if (!"authorization_code".equals(grant_type)) {
            return ResponseEntity.badRequest().build();
        }

        // Extract client credentials from Authorization header if not provided as parameters
        String actualClientId = client_id;
        String actualClientSecret = client_secret;
        
        if ((actualClientId == null || actualClientSecret == null) && authorization != null && authorization.startsWith("Basic ")) {
            try {
                String base64Credentials = authorization.substring(6);
                String credentials = new String(java.util.Base64.getDecoder().decode(base64Credentials));
                String[] parts = credentials.split(":", 2);
                if (parts.length == 2) {
                    actualClientId = parts[0];
                    actualClientSecret = parts[1];
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }
        
        if (actualClientId == null || actualClientSecret == null) {
            return ResponseEntity.badRequest().build();
        }

        String accessToken = userService.exchangeCodeForToken(code, actualClientId, actualClientSecret);
        if (accessToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("access_token", accessToken);
        response.put("token_type", "Bearer");
        response.put("expires_in", 3600);
        response.put("scope", "read:user");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/user")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> user(@RequestHeader("Authorization") String authorization) {
        
        if (!authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authorization.substring(7);
        MockUser user = userService.getUserByToken(token);
        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("name", user.getFullName());
        userInfo.put("avatar_url", user.getAvatarUrl());
        userInfo.put("html_url", user.getProfileUrl());
        userInfo.put("role", user.getRole());

        return ResponseEntity.ok(userInfo);
    }

    @GetMapping("/")
    @ResponseBody
    public Map<String, Object> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "Mock OAuth2 Server");
        info.put("version", "1.0.0");
        info.put("description", "Simple OAuth2 server for local testing");
        info.put("endpoints", Map.of(
            "authorize", "/oauth/authorize",
            "token", "/oauth/token",
            "user", "/user"
        ));
        info.put("available_users", userService.getAllUsers().keySet());
        return info;
    }
}