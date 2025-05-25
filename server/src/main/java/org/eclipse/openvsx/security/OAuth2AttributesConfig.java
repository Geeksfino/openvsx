/********************************************************************************
 * Copyright (c) 2023 Ericsson and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.*;

/**
 * Configuration example:
 * <pre><code>
 *ovsx:
 *  oauth2:
 *    primary-provider: github
 *    attribute-names:
 *      [provider-name]:
 *        avatar-url: string
 *        email: string
 *        full-name: string
 *        login-name: string
 *        provider-url: string
 * </code></pre>
 */
@ConfigurationProperties(prefix = "ovsx.oauth2")
public record OAuth2AttributesConfig(
        String primaryProvider,
        Map<String, OAuth2AttributesMapping> attributeNames) {
    private static final Map<String, OAuth2AttributesMapping> DEFAULT_MAPPINGS = Map.of(
            "github", new OAuth2AttributesMapping("avatar_url", "email", "name", "login", "html_url"),
            "gitlab", new OAuth2AttributesMapping("avatar_url", "email", "name", "username", "web_url"),
            "google", new OAuth2AttributesMapping("picture", "email", "name", "email", "profile"),
            "microsoft", new OAuth2AttributesMapping("picture", "mail", "displayName", "userPrincipalName", "profileUrl"),
            "bitbucket", new OAuth2AttributesMapping("links.avatar.href", "email", "display_name", "username", "links.html.href")
    );

    public OAuth2AttributesMapping getAttributeMapping(String provider) {
        return Optional.ofNullable(attributeNames).map(a -> a.get(provider)).orElse(DEFAULT_MAPPINGS.get(provider));
    }

    public List<String> getProviders() {
        var providers = new ArrayList<>(DEFAULT_MAPPINGS.keySet());
        if(attributeNames != null) {
            providers.addAll(attributeNames.keySet());
        }

        return providers;
    }

    public String getPrimaryProvider() {
        return Optional.ofNullable(primaryProvider).orElse("github");
    }
}
