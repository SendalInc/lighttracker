package com.sendal.lighttracker.pojo;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sendal.lighttracker.db.LightTrackerAnomaly;

// Representation of the data returned from BQ
// we identify only the data we are interested in
@JsonIgnoreProperties(ignoreUnknown = true)
public class LightTrackerReportedAnomaly extends LightTrackerAnomaly implements Serializable {

    public LightTrackerReportedAnomaly() {
        super();
    }
    
    public LightTrackerReportedAnomaly(LightTrackerAnomaly parentAnomaly, String deviceName) {
        super(parentAnomaly);
        this.deviceName = deviceName;
    }

    private String deviceName;
    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
}
