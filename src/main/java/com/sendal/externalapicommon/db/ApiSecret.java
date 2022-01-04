package com.sendal.externalapicommon.db;

import java.io.Serializable;
import org.bson.types.ObjectId;
import java.util.*;

import org.bson.codecs.pojo.annotations.*;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import com.fasterxml.jackson.annotation.JsonInclude;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiSecret implements Serializable {
    @BsonId
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;
    public ObjectId getId() {
        return id;
    }
    public void setId(ObjectId id) {
        this.id = id;
    }

    private String apiKey;
    public String getApiKey() {
        return apiKey;
    }
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    private String secretKey;
    public String getSecretKey() {
        return secretKey;
    }
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    private String apiEndpointUrl;
    public String getApiEndpointUrl() {
        return apiEndpointUrl;
    }
    public void setApiEndpointUrl(String apiEndpointUrl) {
        this.apiEndpointUrl = apiEndpointUrl;
    }

    private String description;
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    private Long updateStatePollIntervalSeconds;
    public Long getUpdateStatePollIntervalSeconds() {
        return updateStatePollIntervalSeconds;
    }

    public void setUpdateStatePollIntervalSeconds(Long updateStatePollIntervalSeconds) {
        this.updateStatePollIntervalSeconds = updateStatePollIntervalSeconds;
    }

    private boolean disableOnDemandUpdateState;
    public boolean getDisableOnDemandUpdateState() {
        return disableOnDemandUpdateState;
    }

    public void setDisableOnDemandUpdateState(boolean disableOnDemandUpdateState) {
        this.disableOnDemandUpdateState = disableOnDemandUpdateState;
    }
}
