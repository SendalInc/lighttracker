package com.sendal.lighttracker.db;

import java.io.Serializable;
import org.bson.types.ObjectId;
import java.util.*;
import com.sendal.externalapicommon.ExternalHomeConfiguration;

import org.bson.codecs.pojo.annotations.*;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import com.sendal.common.coredb.DBPermissions;


public class LightTrackerSubscriptionRecord implements Serializable {

    @BsonId
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId homeId;

    public ObjectId getHomeId() {
        return homeId;
    }
    public void setHomeId(ObjectId id) {
        homeId = id;
    }

    DBPermissions permissions;

    public DBPermissions getPermissions() {
        return permissions;
    }

    public void setPermissions(DBPermissions permissions) {
        this.permissions = permissions;
    }

    ExternalHomeConfiguration homeConfig;

    public ExternalHomeConfiguration getHomeConfig() {
        return homeConfig;
    }

    public void setHomeConfig(ExternalHomeConfiguration homeConfig) {
        this.homeConfig = homeConfig;
    }
}
