package com.sendal.externalapicommon;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Set;

//
// Externalized representation of a home's configuration, often what's displayed here is resticted based
// on permissions for external access.
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExternalRoomConfiguration implements Serializable {
    public String roomId;
    public String roomName;
    public Set<String> assignedDevices; // list of devices object IDs
}