package com.sendal.common;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bson.types.ObjectId;

import com.sendal.common.StateIdentifier;

import org.bson.codecs.pojo.annotations.*;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Set;
import java.util.HashSet;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StateRegistration implements Serializable {

    @BsonId
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;
    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    StateIdentifier stateIdentifier;
    public StateIdentifier getStateIdentifier() {
        return stateIdentifier;
    }

    public void setStateIdentifier(StateIdentifier stateIdentifier) {
        this.stateIdentifier = stateIdentifier;
    }

    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId registeredEntityId;

    public ObjectId getRegisteredEntityId() {
        return registeredEntityId;
    }

    public void setRegisteredEntityId(ObjectId registeredEntityId) {
        this.registeredEntityId = registeredEntityId;
    }

    private String registeredEntityType;

    public String getRegisteredEntityType() {
        return registeredEntityType;
    }

    public void setRegisteredEntityType(String registeredEntityType) {
        this.registeredEntityType = registeredEntityType;
    }

    // used to tell the state server we don't need an initial syncing/reporting of the current state
    // in-effect tells state server to report only changes
    @BsonIgnore
    private boolean ignoreInitialStateSync = false;

    @BsonIgnore
    public boolean getIgnoreInitialStateSync() {
        return ignoreInitialStateSync;
    }

    @BsonIgnore
    public void setIgnoreInitialStateSync(boolean ignoreInitialStateSync) {
        this.ignoreInitialStateSync = ignoreInitialStateSync;
    }
}
