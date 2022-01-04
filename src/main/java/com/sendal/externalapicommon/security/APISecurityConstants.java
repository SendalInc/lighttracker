package com.sendal.externalapicommon.security;


public class APISecurityConstants { 
    public static final String DEFAULT_SIGNATURE_HTTP_HEADER = "X-Auth-Signature";
    public static final String DEFAULT_TIMESTAMP_HTTP_HEADER = "X-Auth-Timestamp";
    public static final String DEFAULT_VERSION_HTTP_HEADER   = "X-Auth-Version";
    public static final String DEFAULT_API_KEYNAME   = "X-Auth-ApiKey";

    // used for non-HMAC authentication/authorization work
    public static final String DEFAULT_AUTHORIZATION_HTTP_HEADER = "Authorization";
}