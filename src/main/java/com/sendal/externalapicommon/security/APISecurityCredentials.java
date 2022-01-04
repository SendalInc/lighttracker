package com.sendal.externalapicommon.security;
import com.sendal.externalapicommon.security.Version;

public class APISecurityCredentials {

    enum CredentialType {
        BYPASS, // meaning we skip credentially - this is a test-only mode
        HMAC,
        USERACCESS,
        HOMEACCESS
    }

    public CredentialType credentialType;

    // for HMAC
    public Version version;
    public String apiKey;
    public String signature;
    public String path;
    public String timestamp;
    public String method;
    public byte[] content;

    // for user and home tokens
    public String accessTokenBody;

    
}