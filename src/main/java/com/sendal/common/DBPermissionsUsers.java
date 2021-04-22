package com.sendal.common.coredb;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;

// CLOVER:OFF
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DBPermissionsUsers implements Serializable {

    private boolean sendNotifications;
    public boolean getSendNotifications() {
        return sendNotifications;
    }

    public void setSendNotifications(boolean sendNotifications) {
        this.sendNotifications = sendNotifications;
    }
}
// CLOVER:ON