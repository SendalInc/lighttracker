package com.sendal.common.pss;

import javax.validation.constraints.Max;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.*;


public class SendalSoftwareServiceConfiguration {
    @JsonProperty(required = true)
    private String scs3pssId;

    public String getScs3pssId() {
        return scs3pssId;
    }

    public void setScs3pssId(String scs3pssId) {
        this.scs3pssId = scs3pssId;
    }

    @JsonProperty(required = true)
    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    @JsonProperty(required = true)
    private String secretKey;

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    @JsonProperty(required = true)
    private String scsUrl;

    public String getScsUrl() {
        return scsUrl;
    }

    public void setScsUrl(String scsUrl) {
        this.scsUrl = scsUrl;
    }
}