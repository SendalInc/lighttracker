package com.sendal.common.notification;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserNotification implements Serializable {

    //
    // copy references
    //
    protected Map<String, String> alertHeading;

    public Map<String, String> getAlertHeading() {
        return alertHeading;
    }

    public void setAlertHeading(Map<String, String> alertHeading) {
        this.alertHeading = alertHeading;
    }

    protected Map<String, String> alertContent;

    public Map<String, String> getAlertContent() {
        return alertContent;
    }

    public void setAlertContent(Map<String, String> alertContent) {
        this.alertContent = alertContent;
    }

    protected String alertDestination;

    public String getAlertDestination() {
        return alertDestination;
    }

    public void setAlertDestination(String alertDestination) {
        this.alertDestination = alertDestination;
    }

    protected String alertDestinationPath;

    public String getAlertDestinationPath() {
        return alertDestinationPath;
    }

    public void setAlertDestinationPath(String alertDestinationPath) {
        this.alertDestinationPath = alertDestinationPath;
    }
}
