package com.sendal.externalapicommon.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.sendal.externalapicommon.security.Signer;

public class HmacSignatureGenerator {

    private static final char DELIMITER = ':';
    private static final Logger logger = LoggerFactory.getLogger(HmacSignatureGenerator.class);

    public static String generate(String secretKey, String method, String timestamp, String path, byte[] content) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(method.getBytes());
            out.write(DELIMITER);
            out.write(timestamp.getBytes());
            out.write(DELIMITER);
            out.write(path.getBytes());
            if (content != null && content.length > 0) {
                out.write(DELIMITER);
                out.write(content);
            }

            Signer signer = new Signer(secretKey);
            return signer.createSignature(out.toByteArray());

        } catch (IOException e) {
            logger.error("IO EXCEPTION");
        }
        return null;
    }
}