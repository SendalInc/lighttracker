package com.sendal.externalapicommon;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;
import java.util.List;

//
// Externalized representation of a home's configuration, often what's displayed here is resticted based
// on permissions for external access.
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExternalHomeConfiguration implements Serializable {

    // home info
    public String timeZone; // mapped to a configuration-related setting
    public String postalCode; // mapped to a configuration-related setting
    public String homeLongitude; // mapped to a configuration-related setting
    public String homeLatitude; // mapped to a configuration-related setting

    // device class info
    // this organization means if a device shows up with multiple resources it will be listed multiple times here.
    public Map<String, List<ExternalDeviceConfiguration>> deviceConfigurations; // key is device class

    // room information
    public Map<String, ExternalRoomConfiguration> roomConfigurations; // key is room ID
}