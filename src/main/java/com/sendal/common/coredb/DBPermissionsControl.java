package com.sendal.common.coredb;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Set;
import java.util.HashSet;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class DBPermissionsControl implements Serializable {

    private Set<String> deviceClasses; // controllable allowed
    public Set<String> getDeviceClasses() {
        return deviceClasses;
    }

    public void setDeviceClasses(Set<String> deviceClasses) {
        this.deviceClasses = deviceClasses;
    }

    public void addDeviceClass(String deviceClass) {
        if(deviceClasses == null) {
            deviceClasses = new HashSet<String>();
        }

        this.deviceClasses.add(deviceClass);
    }

    public void removeDeviceClass(String deviceClass) {
        if(deviceClasses != null) {
            deviceClasses.remove(deviceClass);
        }
    }
}
