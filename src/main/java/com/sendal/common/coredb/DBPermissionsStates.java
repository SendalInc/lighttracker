package com.sendal.common.coredb;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class DBPermissionsStates implements Serializable {

    private boolean userPresenceStates; // relates to the configuration setting for individual vs. grouped user view
    public boolean getUserPresenceStates() {
        return userPresenceStates;
    }

    public void setUserPresenceStates(boolean userPresenceStates) {
        this.userPresenceStates = userPresenceStates;
    }


    private Map<String, String> deviceClasses; // key is device class, value is PERMISSION_READ*
    
    public Map<String, String> getDeviceClasses() {
        if(deviceClasses == null) {
            deviceClasses = new HashMap<String, String>();
        }

        return deviceClasses;
    }

    public void setDeviceClasses(Map<String, String> deviceClasses) {
        if(deviceClasses == null) {
            deviceClasses = new HashMap<String, String>();
        }

        this.deviceClasses = deviceClasses;
    }

    public void addDeviceClassPermission(String deviceClass, String permission) {
        if(deviceClasses == null) {
            deviceClasses = new HashMap<String, String>();
        }

        deviceClasses.put(deviceClass, permission);
    }

    public void removeDeviceClassPermission(String deviceClass) {
        if(deviceClasses != null) {
            deviceClasses.remove(deviceClass);
        }
    }


    private Set<String> roomStates; // statenames allowed
    public Set<String> getRoomStates() {
        return roomStates;
    }

    public void setRoomStates(Set<String> roomStates) {
        this.roomStates = roomStates;
    }

    public void addRoomStates(String roomStates) {
        if(this.roomStates == null) {
            this.roomStates = new HashSet<String>();
        }

        this.roomStates.add(roomStates);
    }

    public void removeRoomStates(String roomStates) {
        if(this.roomStates != null) {
            this.roomStates.remove(roomStates);
        }
    }


    private Set<String> homeStates; // statenames allowed
    public Set<String> getHomeStates() {
        return homeStates;
    }

    public void setHomeStates(Set<String> homeStates) {
        this.homeStates = homeStates;
    }

    public void addHomeStates(String homeStates) {
        if(this.homeStates == null) {
            this.homeStates = new HashSet<String>();
        }

        this.homeStates.add(homeStates);
    }

    public void removeHomeStates(String homeStates) {
        if(this.homeStates != null) {
            this.homeStates.remove(homeStates);
        }
    }
}
