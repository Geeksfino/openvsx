# OAuth2 Provider Configuration

OpenVSX supports multiple OAuth2 authentication providers. This document explains how to configure different providers.

## Default Supported Providers

OpenVSX comes with built-in support for the following OAuth2 providers:

- **GitHub** (default)
- **GitLab**
- **Google**
- **Microsoft Azure AD**
- **Bitbucket**

## Configuration

### Primary Provider

You can configure which provider should be the primary authentication provider:

```yaml
ovsx:
  oauth2:
    primary-provider: github  # or gitlab, google, microsoft, bitbucket
```

The primary provider is used for Eclipse integration and error messages.

### Spring Security OAuth2 Configuration

Configure your OAuth2 providers in the Spring Security section:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: ${GITHUB_CLIENT_ID}
            client-secret: ${GITHUB_CLIENT_SECRET}
            scope: user:email
            
          gitlab:
            client-id: ${GITLAB_CLIENT_ID}
            client-secret: ${GITLAB_CLIENT_SECRET}
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope: read_user
            
        provider:
          gitlab:
            authorization-uri: https://gitlab.com/oauth/authorize
            token-uri: https://gitlab.com/oauth/token
            user-info-uri: https://gitlab.com/api/v4/user
            user-name-attribute: username
```

### Custom Attribute Mappings

If you need to customize how user attributes are mapped from the OAuth2 provider, you can override the default mappings:

```yaml
ovsx:
  oauth2:
    attribute-names:
      my-provider:
        avatar-url: profile.picture
        email: contact.email
        full-name: profile.name
        login-name: username
        provider-url: profile.url
```

#### Nested Attributes

The attribute mapping supports nested attributes using dot notation:

```yaml
ovsx:
  oauth2:
    attribute-names:
      bitbucket:
        avatar-url: links.avatar.href
        provider-url: links.html.href
```

## Provider-Specific Setup

### GitHub

1. Create a GitHub OAuth App at https://github.com/settings/applications/new
2. Set the authorization callback URL to: `https://your-domain.com/login/oauth2/code/github`
3. Configure the client ID and secret:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: ${GITHUB_CLIENT_ID}
            client-secret: ${GITHUB_CLIENT_SECRET}
            scope: user:email
```

### GitLab

1. Create a GitLab application at https://gitlab.com/-/profile/applications
2. Set the redirect URI to: `https://your-domain.com/login/oauth2/code/gitlab`
3. Configure the client ID and secret:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          gitlab:
            client-id: ${GITLAB_CLIENT_ID}
            client-secret: ${GITLAB_CLIENT_SECRET}
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope: read_user
        provider:
          gitlab:
            authorization-uri: https://gitlab.com/oauth/authorize
            token-uri: https://gitlab.com/oauth/token
            user-info-uri: https://gitlab.com/api/v4/user
            user-name-attribute: username
```

### Google

1. Create a Google OAuth2 client at https://console.developers.google.com/
2. Set the authorized redirect URI to: `https://your-domain.com/login/oauth2/code/google`
3. Configure the client ID and secret:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: openid,profile,email
```

### Microsoft Azure AD

1. Register an application in Azure AD at https://portal.azure.com/
2. Set the redirect URI to: `https://your-domain.com/login/oauth2/code/microsoft`
3. Configure the client ID and secret:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          microsoft:
            client-id: ${MICROSOFT_CLIENT_ID}
            client-secret: ${MICROSOFT_CLIENT_SECRET}
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope: openid,profile,email
        provider:
          microsoft:
            authorization-uri: https://login.microsoftonline.com/common/oauth2/v2.0/authorize
            token-uri: https://login.microsoftonline.com/common/oauth2/v2.0/token
            user-info-uri: https://graph.microsoft.com/v1.0/me
            user-name-attribute: userPrincipalName
```

### Bitbucket

1. Create a Bitbucket OAuth consumer at https://bitbucket.org/account/settings/app-passwords/
2. Set the callback URL to: `https://your-domain.com/login/oauth2/code/bitbucket`
3. Configure the client ID and secret:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          bitbucket:
            client-id: ${BITBUCKET_CLIENT_ID}
            client-secret: ${BITBUCKET_CLIENT_SECRET}
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope: account
        provider:
          bitbucket:
            authorization-uri: https://bitbucket.org/site/oauth2/authorize
            token-uri: https://bitbucket.org/site/oauth2/access_token
            user-info-uri: https://api.bitbucket.org/2.0/user
            user-name-attribute: username
```

## Eclipse Integration

When using Eclipse integration with non-GitHub providers, note that:

1. The Eclipse profile verification will be relaxed for non-GitHub primary providers
2. GitHub handle verification in Eclipse profiles only applies when GitHub is the primary provider
3. For other providers, Eclipse integration will work but won't verify GitHub handles

## Environment Variables

It's recommended to use environment variables for sensitive information:

```bash
export GITHUB_CLIENT_ID=your_github_client_id
export GITHUB_CLIENT_SECRET=your_github_client_secret
export GITLAB_CLIENT_ID=your_gitlab_client_id
export GITLAB_CLIENT_SECRET=your_gitlab_client_secret
# ... etc
```

## Multiple Providers

You can configure multiple providers simultaneously. Users will see all configured providers on the login page and can choose which one to use.

## Custom Providers

For providers not listed above, you can create custom configurations by:

1. Adding the Spring Security OAuth2 client configuration
2. Creating custom attribute mappings in the `ovsx.oauth2.attribute-names` section

Example for a custom provider:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          custom-provider:
            client-id: ${CUSTOM_CLIENT_ID}
            client-secret: ${CUSTOM_CLIENT_SECRET}
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope: read:user
        provider:
          custom-provider:
            authorization-uri: https://auth.example.com/oauth/authorize
            token-uri: https://auth.example.com/oauth/token
            user-info-uri: https://api.example.com/user
            user-name-attribute: username

ovsx:
  oauth2:
    attribute-names:
      custom-provider:
        avatar-url: avatar_url
        email: email
        full-name: display_name
        login-name: username
        provider-url: profile_url
```