package com.sendal.common.mongo;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

// CLOVER:OFF
public class MongoConfiguration {

    @Valid
    @NotNull
    private String uri;

    @JsonProperty
    public String getUri() {
        return uri;
    }

    @JsonProperty
    public void setUri(String uri) {
        this.uri = uri;
    }

    @Valid
    @NotNull
    private String database;

    @JsonProperty
    public String getDatabase() {
        return database;
    }

    @JsonProperty
    public void setDatabase(String database) {
        this.database = database;
    }
}
// CLOVER:ON