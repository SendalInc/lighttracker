package com.sendal.externalapicommon.security;

import io.dropwizard.auth.AuthFilter;

import com.sendal.externalapicommon.security.Version;
import com.sendal.externalapicommon.security.APISecurityConstants;

import com.sendal.externalapicommon.security.APISecurityCredentials;

import com.google.common.io.ByteStreams;

import javax.annotation.Nullable;
import javax.annotation.Priority;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Principal;
import java.io.InputStream;
import java.net.URI;

@Priority(Priorities.AUTHENTICATION)
public class APISecurityAuthFilter<P extends Principal> extends AuthFilter<APISecurityCredentials, P> {

    // multiple auth scenes can be used.  Here are the main use cases.
    // - when interacting w/ external software (DC or 3PSS) HMAC is used as authenitcation of 
    //   mess source and authorization to access a resource (when checked against internal permissions
    //   which is done by the resource logic)
    // - when interacting w/ internal software (other SCS services) HMAC is used for service-to-service
    //   authentication and authorization - each sending service is trusted to request only the resources
    //   it should for the user or home context it is operating on.  Requests from IS (which are untrusted)
    //   use a user access or home access token, provede by the user/UI or a trusted SCS service (like scensserver)
    //   respectively.
    //   As a result of the above, HMAC is looked for first and used.  If not, we look for user then home tokens in that order.

    // Alternate Auth mechanisms

    private boolean bypassAuth = false;

    public void setBypassAuth(boolean bypassAuth) {
        this.bypassAuth = bypassAuth;
    }

    // User Access Token
    // if present, it indicates user access tokens are allowed on this interface.
    private boolean userAccessTokensAllowed = false;

    public void setUserAccessTokensAllowed(boolean userAccessTokensAllowed) {
        this.userAccessTokensAllowed = userAccessTokensAllowed;
    }

    // Home Access Token
    // if present, it indicates home access tokens are allowed on this interface.
    private boolean homeAccessTokensAllowed = false;

    public void setHomeAccessTokensAllowed(boolean homeAcessTokensAllowed) {
        this.homeAccessTokensAllowed = homeAcessTokensAllowed;
    }

    // HMAC allowed
    // if present, it indicates HMAC is allowed on this interface
    private boolean hmacAllowed = true;

    public void setHmacAllowed(boolean hmacAllowed) {
        this.hmacAllowed = hmacAllowed;
    }

    private APISecurityAuthFilter() {
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final APISecurityCredentials credentials =
                getCredentials(requestContext);
        if (!authenticate(requestContext, credentials, SecurityContext.BASIC_AUTH)) {
            throw new WebApplicationException(unauthorizedHandler.buildResponse(prefix, realm));
        }
    }


    @Nullable
    private APISecurityCredentials getCredentials(ContainerRequestContext requestContext) {

        final UriInfo uriInfo = requestContext.getUriInfo();
        final URI requestUri = uriInfo.getRequestUri();

        APISecurityCredentials credentials = new APISecurityCredentials();

        final MultivaluedMap<? super String, ? extends String> queryParameters = uriInfo
                .getQueryParameters();

        if(bypassAuth == true) {
            credentials.credentialType = APISecurityCredentials.CredentialType.BYPASS;
            return credentials;
        }
       
        // we test for security mechanisms in the order of preference/their authorative value
        if((userAccessTokensAllowed == true || 
            homeAccessTokensAllowed == true) && 
           requestContext.getHeaderString(APISecurityConstants.DEFAULT_AUTHORIZATION_HTTP_HEADER) != null) {
            //Determine the authorization type.
            String[] tokenComponents = requestContext.getHeaderString(APISecurityConstants.DEFAULT_AUTHORIZATION_HTTP_HEADER).split(" ");

            if(tokenComponents.length == 2) {
                credentials.accessTokenBody = tokenComponents[1];
                String tokenType = tokenComponents[0];

                if(tokenType.equals("SendalHome") == true && 
                   homeAccessTokensAllowed == true) {
                    // home token.
                    credentials.credentialType = APISecurityCredentials.CredentialType.HOMEACCESS;
                } else if(userAccessTokensAllowed == true &&
                          tokenType.equals("SendalHome") == false) {
                    credentials.credentialType = APISecurityCredentials.CredentialType.USERACCESS;
                } else {
                    throw new NotAuthorizedException("Invalid Authorization header format");
                }
            } else {
                throw new NotAuthorizedException("Invalid Authorization header format");
            }
        // the else if here means in concept a message w/ a valid user/home token will be processed even if the HMAC is missing or invalid.
        // not sure if we should also validate the HMAC if we use the above user/home credentials...
        } else if (hmacAllowed 
                && requestContext.getHeaderString(APISecurityConstants.DEFAULT_SIGNATURE_HTTP_HEADER) != null
                && requestContext.getHeaderString(APISecurityConstants.DEFAULT_TIMESTAMP_HTTP_HEADER) != null
                && requestContext.getHeaderString(APISecurityConstants.DEFAULT_VERSION_HTTP_HEADER) != null
                && requestContext.getHeaderString(APISecurityConstants.DEFAULT_API_KEYNAME) != null) {
            // attempt to use HMAC
            credentials.credentialType = APISecurityCredentials.CredentialType.HMAC;
            credentials.apiKey = requestContext.getHeaderString(APISecurityConstants.DEFAULT_API_KEYNAME);
            credentials.signature = requestContext.getHeaderString(APISecurityConstants.DEFAULT_SIGNATURE_HTTP_HEADER);
            credentials.timestamp = requestContext.getHeaderString(APISecurityConstants.DEFAULT_TIMESTAMP_HTTP_HEADER);
            credentials.version = Version.fromValue(requestContext.getHeaderString(APISecurityConstants.DEFAULT_VERSION_HTTP_HEADER));
            credentials.method = requestContext.getMethod();
            credentials.path = requestUri.getPath();

            // Content
            if (requestContext.hasEntity()) {
                try {
                    final InputStream inputStream = requestContext.getEntityStream();
                    try {
                        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        ByteStreams.copy(inputStream, outputStream);

                        final byte[] bytes = outputStream.toByteArray();
                        credentials.content = bytes;
                        requestContext.setEntityStream(new ByteArrayInputStream(bytes));
                    } finally {
                        inputStream.close();
                    }
                } catch (final IOException ioe) {
                    throw new InternalServerErrorException("Error reading content", ioe);
                }
            }
        } else {
            String errorString = "Required auth headers not present: ";

            if(hmacAllowed == true) {
                errorString = errorString + APISecurityConstants.DEFAULT_SIGNATURE_HTTP_HEADER + ", " + APISecurityConstants.DEFAULT_TIMESTAMP_HTTP_HEADER
                    + ", " + APISecurityConstants.DEFAULT_VERSION_HTTP_HEADER + ", " + APISecurityConstants.DEFAULT_API_KEYNAME;
            }

            if(userAccessTokensAllowed == true ||
               homeAccessTokensAllowed == true) {
                if(hmacAllowed == true) {
                    errorString = errorString + " or ";
                }

                errorString = errorString + APISecurityConstants.DEFAULT_AUTHORIZATION_HTTP_HEADER;
            }

            throw new NotAuthorizedException(errorString);            
        }

        return credentials;
    }


    public static class Builder<P extends Principal> extends
            AuthFilterBuilder<APISecurityCredentials, P, APISecurityAuthFilter<P>> {

        @Override
        protected APISecurityAuthFilter<P> newInstance() {
            return new APISecurityAuthFilter<>();
        }
    }
}