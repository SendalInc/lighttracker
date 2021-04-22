
package com.sendal.common;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.util.Date;

public class StateValue implements Serializable{
    protected String stateValue;
    public String getStateValue() {
        return stateValue;
    }
    public void setStateValue(String stateValue) {
        this.stateValue = stateValue;
    }

    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    protected Date lastUpdate;
    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    // experation means after this time the state will not be reported to state consumers
    // who poll for the state.  Also the state will not be considered as a previous value
    // when a new report arrives.
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    protected Date expiration;

    public Date getExpiration() {
        return expiration;
    }

    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }
}