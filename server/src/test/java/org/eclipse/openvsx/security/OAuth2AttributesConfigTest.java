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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class OAuth2AttributesConfigTest {

    @Test
    public void testDefaultPrimaryProvider() {
        var config = new OAuth2AttributesConfig(null, null);
        assertEquals("github", config.getPrimaryProvider());
    }

    @Test
    public void testCustomPrimaryProvider() {
        var config = new OAuth2AttributesConfig("gitlab", null);
        assertEquals("gitlab", config.getPrimaryProvider());
    }

    @Test
    public void testDefaultProviders() {
        var config = new OAuth2AttributesConfig(null, null);
        var providers = config.getProviders();
        
        assertTrue(providers.contains("github"));
        assertTrue(providers.contains("gitlab"));
        assertTrue(providers.contains("google"));
        assertTrue(providers.contains("microsoft"));
        assertTrue(providers.contains("bitbucket"));
    }

    @Test
    public void testCustomProviders() {
        var customMappings = Map.of(
            "custom-provider", new OAuth2AttributesMapping("avatar", "email", "name", "username", "url")
        );
        var config = new OAuth2AttributesConfig(null, customMappings);
        var providers = config.getProviders();
        
        assertTrue(providers.contains("github")); // Default provider
        assertTrue(providers.contains("custom-provider")); // Custom provider
    }

    @Test
    public void testDefaultAttributeMapping() {
        var config = new OAuth2AttributesConfig(null, null);
        var mapping = config.getAttributeMapping("github");
        
        assertNotNull(mapping);
        assertEquals("avatar_url", mapping.avatarUrl());
        assertEquals("email", mapping.email());
        assertEquals("name", mapping.fullName());
        assertEquals("login", mapping.loginName());
        assertEquals("html_url", mapping.providerUrl());
    }

    @Test
    public void testCustomAttributeMapping() {
        var customMapping = new OAuth2AttributesMapping("custom_avatar", "custom_email", "custom_name", "custom_login", "custom_url");
        var customMappings = Map.of("custom-provider", customMapping);
        var config = new OAuth2AttributesConfig(null, customMappings);
        
        var mapping = config.getAttributeMapping("custom-provider");
        assertEquals(customMapping, mapping);
    }

    @Test
    public void testGitLabDefaultMapping() {
        var config = new OAuth2AttributesConfig(null, null);
        var mapping = config.getAttributeMapping("gitlab");
        
        assertNotNull(mapping);
        assertEquals("avatar_url", mapping.avatarUrl());
        assertEquals("email", mapping.email());
        assertEquals("name", mapping.fullName());
        assertEquals("username", mapping.loginName());
        assertEquals("web_url", mapping.providerUrl());
    }
}