package com.sendal.lighttracker;

import io.dropwizard.Configuration;
import javax.validation.constraints.Max;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sendal.lighttracker.SendalSoftwareServiceConfiguration;

import io.dropwizard.client.JerseyClientConfiguration;

import com.sendal.common.mongo.MongoConfiguration;

public class LightTrackerConfiguration extends Configuration {

    @Valid
    @NotNull
    private MongoConfiguration mongo;

    @JsonProperty
    public MongoConfiguration getMongo() {
        return mongo;
    }

    @JsonProperty
    public void setMongo(MongoConfiguration mongo) {
        this.mongo = mongo;
    }

    @Valid
    @NotNull
    private JerseyClientConfiguration jerseyClient = new JerseyClientConfiguration();

    @JsonProperty("jerseyClient")
    public JerseyClientConfiguration getJerseyClientConfiguration() {
        return jerseyClient;
    }

    public void setJerseyClientConfiguration(JerseyClientConfiguration jerseyClient) {
        this.jerseyClient = jerseyClient;
    }

    @Valid
    @NotNull
    private SendalSoftwareServiceConfiguration sendalSoftwareService;

    @JsonProperty
    public SendalSoftwareServiceConfiguration getSendalSoftwareService() {
        return sendalSoftwareService;
    }

    @JsonProperty
    public void setSendalSoftwareService(SendalSoftwareServiceConfiguration sendalSoftwareService) {
        this.sendalSoftwareService = sendalSoftwareService;
    } 
}
