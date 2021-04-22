package com.sendal.common;

import java.io.Serializable;
import com.sendal.common.StateValue;
import com.sendal.common.StateIdentifier;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.bson.codecs.pojo.annotations.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StateReport extends StateValue implements Serializable {

    @BsonId
    protected StateIdentifier stateIdentifier;
    public StateIdentifier getStateIdentifier() {
        return stateIdentifier;
    }
    public void setStateIdentifier(StateIdentifier stateIdentifier) {
        this.stateIdentifier = stateIdentifier;
    }
}
