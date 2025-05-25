/** ******************************************************************************
 * Copyright (c) 2025 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class OAuth2AttributesMappingTest {

    @Test
    public void testSimpleAttributeMapping() {
        var mapping = new OAuth2AttributesMapping("avatar_url", "email", "name", "login", "html_url");
        var oauth2User = mock(OAuth2User.class);
        
        when(oauth2User.getName()).thenReturn("user123");
        when(oauth2User.getAttribute("avatar_url")).thenReturn("https://example.com/avatar.jpg");
        when(oauth2User.getAttribute("email")).thenReturn("user@example.com");
        when(oauth2User.getAttribute("name")).thenReturn("John Doe");
        when(oauth2User.getAttribute("login")).thenReturn("johndoe");
        when(oauth2User.getAttribute("html_url")).thenReturn("https://example.com/johndoe");
        
        var userData = mapping.toUserData("github", oauth2User);
        
        assertEquals("user123", userData.getAuthId());
        assertEquals("github", userData.getProvider());
        assertEquals("https://example.com/avatar.jpg", userData.getAvatarUrl());
        assertEquals("user@example.com", userData.getEmail());
        assertEquals("John Doe", userData.getFullName());
        assertEquals("johndoe", userData.getLoginName());
        assertEquals("https://example.com/johndoe", userData.getProviderUrl());
    }

    @Test
    public void testNestedAttributeMapping() {
        var mapping = new OAuth2AttributesMapping("links.avatar.href", "email", "display_name", "username", "links.html.href");
        var oauth2User = mock(OAuth2User.class);
        
        var attributes = Map.of(
            "email", "user@example.com",
            "display_name", "John Doe",
            "username", "johndoe",
            "links", Map.of(
                "avatar", Map.of("href", "https://example.com/avatar.jpg"),
                "html", Map.of("href", "https://example.com/johndoe")
            )
        );
        
        when(oauth2User.getName()).thenReturn("user123");
        when(oauth2User.getAttributes()).thenReturn(attributes);
        when(oauth2User.getAttribute("email")).thenReturn("user@example.com");
        when(oauth2User.getAttribute("display_name")).thenReturn("John Doe");
        when(oauth2User.getAttribute("username")).thenReturn("johndoe");
        
        var userData = mapping.toUserData("bitbucket", oauth2User);
        
        assertEquals("user123", userData.getAuthId());
        assertEquals("bitbucket", userData.getProvider());
        assertEquals("https://example.com/avatar.jpg", userData.getAvatarUrl());
        assertEquals("user@example.com", userData.getEmail());
        assertEquals("John Doe", userData.getFullName());
        assertEquals("johndoe", userData.getLoginName());
        assertEquals("https://example.com/johndoe", userData.getProviderUrl());
    }

    @Test
    public void testNestedAttributeMappingMissingPath() {
        var mapping = new OAuth2AttributesMapping("links.missing.href", "email", "name", "username", "url");
        var oauth2User = mock(OAuth2User.class);
        
        var attributes = Map.of(
            "email", "user@example.com",
            "name", "John Doe",
            "username", "johndoe",
            "url", "https://example.com/johndoe",
            "links", Map.of(
                "avatar", Map.of("href", "https://example.com/avatar.jpg")
                // missing "missing" key
            )
        );
        
        when(oauth2User.getName()).thenReturn("user123");
        when(oauth2User.getAttributes()).thenReturn(attributes);
        when(oauth2User.getAttribute("email")).thenReturn("user@example.com");
        when(oauth2User.getAttribute("name")).thenReturn("John Doe");
        when(oauth2User.getAttribute("username")).thenReturn("johndoe");
        when(oauth2User.getAttribute("url")).thenReturn("https://example.com/johndoe");
        
        var userData = mapping.toUserData("custom", oauth2User);
        
        assertEquals("user123", userData.getAuthId());
        assertEquals("custom", userData.getProvider());
        assertNull(userData.getAvatarUrl()); // Should be null due to missing path
        assertEquals("user@example.com", userData.getEmail());
        assertEquals("John Doe", userData.getFullName());
        assertEquals("johndoe", userData.getLoginName());
        assertEquals("https://example.com/johndoe", userData.getProviderUrl());
    }

    @Test
    public void testNullAttributeMapping() {
        var mapping = new OAuth2AttributesMapping(null, "email", "name", "username", null);
        var oauth2User = mock(OAuth2User.class);
        
        when(oauth2User.getName()).thenReturn("user123");
        when(oauth2User.getAttribute("email")).thenReturn("user@example.com");
        when(oauth2User.getAttribute("name")).thenReturn("John Doe");
        when(oauth2User.getAttribute("username")).thenReturn("johndoe");
        
        var userData = mapping.toUserData("custom", oauth2User);
        
        assertEquals("user123", userData.getAuthId());
        assertEquals("custom", userData.getProvider());
        assertNull(userData.getAvatarUrl());
        assertEquals("user@example.com", userData.getEmail());
        assertEquals("John Doe", userData.getFullName());
        assertEquals("johndoe", userData.getLoginName());
        assertNull(userData.getProviderUrl());
    }
}