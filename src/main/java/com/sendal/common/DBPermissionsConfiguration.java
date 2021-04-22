package com.sendal.common.coredb;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;
     

// CLOVER:OFF
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DBPermissionsConfiguration implements Serializable {

    public static final String USERIDENTITY_ANONYMOUS_INDIVIDUAL = "anonymousIndividual";
    public static final String USERIDENTITY_ANONYMOUS_GENERALIZED = "anonymousGeneralized";

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


    private boolean roomDeviceMap; // can the app get a mapping of which devices relate to which rooms?  alternative view - can they get info on which devices are in a room

    public boolean getRoomDeviceMap() {
        return roomDeviceMap;
    }

    public void setRoomDeviceMap(boolean roomDeviceMap) {
        this.roomDeviceMap = roomDeviceMap;
    }


    private boolean roomInformation; // Independent of devices, can information on rooms be read?

    public boolean getRoomInformation() {
        return roomInformation;
    }

    public void setRoomInformation(boolean roomInformation) {
        this.roomInformation = roomInformation;
    }


    private boolean individualAnonymousUserIdentities; // value is USERIDENTITY_*

    public boolean getIndividualAnonymousUserIdentities() {
        return individualAnonymousUserIdentities;
    }

    public void setIndividualAnonymousUserIdentities(boolean individualAnonymousUserIdentities) {
        this.individualAnonymousUserIdentities = individualAnonymousUserIdentities;
    }

    private boolean homeTimeZone;

    public boolean getHomeTimeZone() {
        return homeTimeZone;
    }

    public void setHomeTimeZone(boolean homeTimeZone) {
        this.homeTimeZone = homeTimeZone;
    }

    private boolean homePostalCode;

    public boolean getHomePostalCode() {
        return homePostalCode;
    }

    public void setHomePostalCode(boolean homePostalCode) {
        this.homePostalCode = homePostalCode;
    }

    private boolean homeGeoLocation;

    public boolean getHomeGeoLocation() {
        return homeGeoLocation;
    }

    public void setHomeGeoLocation(boolean homeGeoLocation) {
        this.homeGeoLocation = homeGeoLocation;
    }
}
// CLOVER:ON