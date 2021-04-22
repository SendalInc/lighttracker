package com.sendal.externalapicommon.security;

import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sendal.externalapicommon.security.IDPrincipal;
import com.sendal.externalapicommon.security.APISecurityCredentials;

import com.sendal.externalapicommon.security.HmacSignatureGenerator;
import static com.sendal.externalapicommon.security.TimeUtils.nowInUTC;
import com.sendal.externalapicommon.security.TimeUtils;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.io.IOException;
import java.util.Optional;

import java.security.MessageDigest;

// used by device controllers which have direct access to the API secret 
public class ApiSecretAuthenticatorDC implements Authenticator<APISecurityCredentials, IDPrincipal> {

    private final Logger logger = LoggerFactory.getLogger(ApiSecretAuthenticatorDC.class);

    final static private long allowedTimestampRange = 5*60*1000; // 5 minutes in milliseconds

    private final String deviceControllerId;
    private final String apiKey;
    private final String apiSecret;

    public ApiSecretAuthenticatorDC(String deviceControllerId, String apiKey, String apiSecret) {
        this.deviceControllerId = deviceControllerId;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    public Optional<IDPrincipal> authenticate(APISecurityCredentials credentials) throws AuthenticationException {

        switch(credentials.credentialType) {
            case BYPASS: // test mode
                return Optional.of(new IDPrincipal("", IDPrincipal.PrincipalType.BYPASS));

            case HMAC:
                return authenticateHMAC(credentials);

            default:
                logger.error("Unsupported credential type for Device Controller interface - " + credentials.credentialType);
                return Optional.empty();
        }
    }

    private Optional<IDPrincipal> authenticateHMAC(APISecurityCredentials credentials) throws AuthenticationException {
        if(apiKey != null &&
           apiSecret != null && 
           deviceControllerId != null) {
            // Make sure the timestamp has not expired - this is to protect against replay
            // attacks
            if (!validateTimestamp(credentials.timestamp)) {
                logger.warn("Invalid timestamp");
                return Optional.empty();
            }

            // Get the principal identified by the credentials
            IDPrincipal principal = getPrincipalForHMAC(credentials);
            if (principal == null) {
                logger.warn("Could not get principal");
                return Optional.empty();
            }

            try {
                if (!validateSignature(credentials, apiSecret)) {
                    logger.warn("Invalid signature");
                    return Optional.empty();
                } else {
                    return Optional.of(principal);
                }
            } catch (IOException e) {
                logger.warn("Exception during signature validation");
                return Optional.empty();
            }
        } else {
            logger.warn("HMAC received with no secrets data accessor");
            return Optional.empty();
        }
    }

    private boolean validateTimestamp(String timestamp) {
        DateTime requestTime = TimeUtils.parse(timestamp);
        long difference = Math.abs(new Duration(requestTime, nowInUTC()).getMillis());
        return difference <= allowedTimestampRange;
    }

    private boolean validateSignature(APISecurityCredentials credentials, String secretKey) throws IOException {
        String clientSignature = credentials.signature;
        String serverSignature = createSignature(credentials, secretKey);
        return MessageDigest.isEqual(clientSignature.getBytes(), serverSignature.getBytes());
    }

    private IDPrincipal getPrincipalForHMAC(APISecurityCredentials credentials) {
        if(deviceControllerId != null) {
            return new IDPrincipal(deviceControllerId, IDPrincipal.PrincipalType.HMAC, null);
        } else {
            return null;
        }
    }
    
    private String createSignature(APISecurityCredentials credentials, String secretKey) throws IOException {
        return HmacSignatureGenerator.generate(
                secretKey,
                credentials.method,
                credentials.timestamp,
                credentials.path,
                credentials.content);
    }
}
