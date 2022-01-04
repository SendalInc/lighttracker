package com.sendal.common.state;

import java.io.Serializable;
import org.bson.types.ObjectId;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnore;

import org.bson.codecs.pojo.annotations.*;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

import org.bson.conversions.Bson;
import static com.mongodb.client.model.Filters.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StateIdentifier implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId home;
    public ObjectId getHome() {
        return home;
    }
    public void setHome(ObjectId home) {
        this.home = home;
    }

    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId application;
    public ObjectId getApplication() {
        return application;
    }
    public void setApplication(ObjectId applicationId) {
        this.application = applicationId;
    }

    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId room;
    public ObjectId getRoom() {
        return room;
    }
    public void setRoom(ObjectId room) {
        this.room = room;
    }

    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId roomSet;
    public ObjectId getRoomSet() {
        return roomSet;
    }
    public void setRoomSet(ObjectId roomSet) {
        this.roomSet = roomSet;
    }

    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId outdoorArea;
    public ObjectId getOutdoorArea() {
        return outdoorArea;
    }
    public void setOutdoorArea(ObjectId outdoorArea) {
        this.outdoorArea = outdoorArea;
    }

    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId deviceController;
    public ObjectId getDeviceController() {
        return deviceController;
    }
    public void setDeviceController(ObjectId deviceController) {
        this.deviceController = deviceController;
    }

    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId device;
    public ObjectId getDevice() {
        return device;
    }
    public void setDevice(ObjectId device) {
        this.device = device;
    }

    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId user;
    public ObjectId getUser() {
        return user;
    }
    public void setUser(ObjectId user) {
        this.user = user;
    }

    private String mobileDevice;
    public String getMobileDevice() {
        return mobileDevice;
    }
    public void setMobileDevice(String mobileDevice) {
        this.mobileDevice = mobileDevice;
    }

    private String resource;
    public String getResource() {
        return resource;
    }
    public void setResource(String resource) {
        this.resource = resource;
    }

    private String stateName;
    public String getStateName() {
        return stateName;
    }
    public void setStateName(String stateName) {
        this.stateName = stateName;
    }

    // used for queries only as an alternative to specifying a specific state name
    private String stateNamePattern;
    public String getStateNamePattern() {
        return stateNamePattern;
    }
    public void setStateNamePattern(String stateNamePattern) {
        this.stateNamePattern = stateNamePattern;
    }

    @JsonIgnore
    @Override
    public boolean equals(Object obj) {
        boolean equal = true;

        if(equal == true) {
            if(home != ((StateIdentifier)obj).getHome()) {
                if(home != null && ((StateIdentifier)obj).getHome() != null) {
                    if(!home.equals(((StateIdentifier)obj).getHome())) {
                        equal = false;
                    }
                } else {
                    equal = false;
                }
            }
        }

        if(equal == true) {
            if(application != ((StateIdentifier)obj).getApplication()) {
                if(application != null && ((StateIdentifier)obj).getApplication() != null) {
                    if(!application.equals(((StateIdentifier)obj).getApplication())) {
                        equal = false;
                    }
                } else {
                    equal = false;
                }
            }
        }

        if(equal == true) {
            if(room != ((StateIdentifier)obj).getRoom()) {
                if(room != null && ((StateIdentifier)obj).getRoom() != null) {
                    if(!room.equals(((StateIdentifier)obj).getRoom())) {
                        equal = false;
                    }
                } else {
                    equal = false;
                }
            }
        }

        if(equal == true) {
            if(roomSet != ((StateIdentifier)obj).getRoomSet()) {
                if(roomSet != null && ((StateIdentifier)obj).getRoomSet() != null) {
                    if(!roomSet.equals(((StateIdentifier)obj).getRoomSet())) {
                        equal = false;
                    }
                } else {
                    equal = false;
                }
            }
        }

        if(equal == true) {
            if(outdoorArea != ((StateIdentifier)obj).getOutdoorArea()) {
                if(outdoorArea != null && ((StateIdentifier)obj).getOutdoorArea() != null) {
                    if(!outdoorArea.equals(((StateIdentifier)obj).getOutdoorArea())) {
                        equal = false;
                    }
                } else {
                    equal = false;
                }
            }
        }

        if(equal == true) {
            if(deviceController != ((StateIdentifier)obj).getDeviceController()) {
                if(deviceController != null && ((StateIdentifier)obj).getDeviceController() != null) {
                    if(!deviceController.equals(((StateIdentifier)obj).getDeviceController())) {
                        equal = false;
                    }
                } else {
                    equal = false;
                }
            }
        }

        if(equal == true) {
            if(device != ((StateIdentifier)obj).getDevice()) {
                if(device != null && ((StateIdentifier)obj).getDevice() != null) {
                    if(!device.equals(((StateIdentifier)obj).getDevice())) {
                        equal = false;
                    }
                } else {
                    equal = false;
                }
            }
        }

        if(equal == true) {
            if(user != ((StateIdentifier)obj).getUser()) {
                if(user != null && ((StateIdentifier)obj).getUser() != null) {
                    if(!user.equals(((StateIdentifier)obj).getUser())) {
                        equal = false;
                    }
                } else {
                    equal = false;
                }
            }
        }

        if(equal == true) {
            if(mobileDevice != ((StateIdentifier)obj).getMobileDevice()) {
                if(mobileDevice != null && ((StateIdentifier)obj).getMobileDevice() != null) {
                    if(!mobileDevice.equals(((StateIdentifier)obj).getMobileDevice())) {
                        equal = false;
                    }
                } else {
                    equal = false;
                }
            }
        }

        if(equal == true) {
            if(resource != ((StateIdentifier)obj).getResource()) {
                if(resource != null && ((StateIdentifier)obj).getResource() != null) {
                    if(!resource.equals(((StateIdentifier)obj).getResource())) {
                        equal = false;
                    }
                } else {
                    equal = false;
                }
            }
        }

        if(equal == true) {
            if(stateName != ((StateIdentifier)obj).getStateName()) {
                if(stateName != null && ((StateIdentifier)obj).getStateName() != null) {
                    if(!stateName.equals(((StateIdentifier)obj).getStateName())) {
                        equal = false;
                    }
                } else {
                    equal = false;
                }
            }
        }

        return equal;
    }

    @JsonIgnore
    @Override
    public int hashCode() {
        return ((home != null)?home.hashCode():0) +
                ((application != null)?application.hashCode():0) + 
                ((room != null)?room.hashCode():0) +
                ((roomSet != null)?roomSet.hashCode():0) +
                ((outdoorArea != null)?outdoorArea.hashCode():0) +
                ((deviceController != null)?deviceController.hashCode():0) +
                ((device != null)?device.hashCode():0) +
                ((resource != null)?resource.hashCode():0) +
                ((stateName != null)?stateName.hashCode():0) + 
                ((user != null)?user.hashCode():0) +
                ((mobileDevice != null)?mobileDevice.hashCode():0)
                ;
    }
}
