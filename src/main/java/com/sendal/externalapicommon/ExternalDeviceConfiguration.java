package com.sendal.externalapicommon;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;
import java.util.Set;

//
// Externalized representation of a home's configuration, often what's displayed here is resticted based
// on permissions for external access.
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExternalDeviceConfiguration implements Serializable {
    public String deviceId;
    public String deviceName;
    public String deviceMake;
    public String deviceModel;
    public Map<String, Map<String, Object>> resourcesConfig; // key is resource string, value is VendorConfig object specific for the resoure type
    public Set<String> assignedRooms; // list of room IDs
}