package com.sendal.externalapicommon.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Collections;

import java.io.IOException;

import java.lang.IllegalStateException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import javax.ws.rs.client.ClientRequestContext;

import com.sendal.externalapicommon.security.TimeUtils;
import com.sendal.externalapicommon.security.HmacSignatureGenerator;
import com.sendal.externalapicommon.security.APISecurityConstants;
import com.sendal.externalapicommon.db.ApiSecret;

public class ClientFilterSigner {

    private static final Logger logger = LoggerFactory.getLogger(ClientFilterSigner.class);

    public static void signForContext(ClientRequestContext ctx, ApiSecret apiSecret) throws IllegalStateException{
        String timestamp = TimeUtils.getCurrentTimestamp();
        byte[] content = null;
        final Map<String, List<Object>> headers = ctx.getHeaders();

        ObjectMapper mapper = new ObjectMapper();

        if (ctx.hasEntity()) {
            // try to handle ready-made JSON and Jackson translation needed cases
            boolean needsMapping = true;
            try {
                mapper.readTree(ctx.getEntity().toString());
                content = ctx.getEntity().toString().getBytes();
                needsMapping = false;
            } catch (IOException e) {
                
            }

            if(needsMapping == true) {
                try {
                    content = mapper.writeValueAsString(ctx.getEntity()).getBytes();
                }
                catch(JsonProcessingException e) {
                    logger.error("client signing content can't be mapped -" + e + " content is - " + ctx.getEntity());
                }
            }
        }   

        if (apiSecret != null) {
            String signature = HmacSignatureGenerator.generate(
                                    apiSecret.getSecretKey(), 
                                    ctx.getMethod(), 
                                    timestamp,
                                    ctx.getUri().getPath(), 
                                    content);

            // add the headers
            headers.put(APISecurityConstants.DEFAULT_SIGNATURE_HTTP_HEADER, Collections.singletonList(signature));
            headers.put(APISecurityConstants.DEFAULT_TIMESTAMP_HTTP_HEADER, Collections.singletonList(timestamp));
            headers.put(APISecurityConstants.DEFAULT_VERSION_HTTP_HEADER, Collections.singletonList("3"));
            headers.put(APISecurityConstants.DEFAULT_API_KEYNAME, Collections.singletonList(apiSecret.getApiKey()));
        } else {
            throw new IllegalStateException(
                    "ApiSecret missing for SCS software element");
        }
    }
}