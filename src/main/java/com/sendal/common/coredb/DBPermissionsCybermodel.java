package com.sendal.common.coredb;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Set;
import java.util.HashSet;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DBPermissionsCybermodel implements Serializable {

    private boolean userPresenceModel; // relates to the configuration setting for individual vs. grouped user view
    public boolean getUserPresenceModel() {
        return userPresenceModel;
    }

    public void setUserPresenceModel(boolean userPresenceModel) {
        this.userPresenceModel = userPresenceModel;
    }

    private Set<String> deviceClasses; // controllable allowed

    public Set<String> getDeviceClasses() {
        return deviceClasses;
    }

    public void setDeviceClasses(Set<String> deviceClasses) {
        this.deviceClasses = deviceClasses;
    }

    public void addDeviceClass(String deviceClass) {
        if (deviceClasses == null) {
            deviceClasses = new HashSet<String>();
        }

        this.deviceClasses.add(deviceClass);
    }

    public void removeDeviceClass(String deviceClass) {
        if (deviceClasses != null) {
            deviceClasses.remove(deviceClass);
        }
    }

    private boolean roomModel;
    public boolean getRoomModel() {
        return roomModel;
    }

    public void setRoomModel(boolean roomModel) {
        this.roomModel = roomModel;
    }


    private boolean homeModel;
    public boolean getHomeModel() {
        return homeModel;
    }

    public void setHomeModel(boolean homeModel) {
        this.homeModel = homeModel;
    }
}
