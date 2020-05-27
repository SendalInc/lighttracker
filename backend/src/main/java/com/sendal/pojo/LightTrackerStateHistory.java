package com.sendal.lighttracker.db;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Representation of the data returned from BQ
// we identify only the data we are interested in
@JsonIgnoreProperties(ignoreUnknown = true)
public class LightTrackerStateHistory implements Serializable {

    public String home;
    public String device;

    public String resource;
    public String stateName;
    public String stateValue;

    public String timestamp; // in the form yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
    
}
