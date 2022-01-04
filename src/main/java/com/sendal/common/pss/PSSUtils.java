package com.sendal.common.pss;

import org.slf4j.Logger;

import java.util.Map;
import java.util.HashMap;
import org.bson.types.ObjectId;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response;

public class PSSUtils {

    public static final String APPBADGE_NONE = "none";
    public static final String APPBADGE_GREEN = "green";
    public static final String APPBADGE_YELLOW = "yellow";
    public static final String APPBADGE_RED = "red";

    public static final String APPBADGEICON_NONE = "none";
    public static final String APPBADGEICON_CHECK = "check";
    public static final String APPBADGEICON_QUESTION = "question";
    public static final String APPBADGEICON_EXCLAMATION = "exclamation";

    public static void updateBadgeForHome(Client client, String scsEndpoint, Logger logger, ObjectId homeId, String appBadge, String appBadgeIcon) {
        Invocation invocation;
        Response badgeResponse = null;

        Map<String, String> badgeInfo = Map.of(
        "appBadge", appBadge,
        "appBadgeIcon", appBadgeIcon);

        synchronized (client) {
            invocation = client.target(scsEndpoint).path("/api/v1/homes/" + homeId.toString() + "/badgestate")
                    .request()
                    .buildPost(Entity.json(badgeInfo));
        }

        try {
            badgeResponse = invocation.invoke(Response.class);

            if (badgeResponse.getStatus() != Status.OK.getStatusCode()) {
                // badge update failed
                String responseBody = badgeResponse.readEntity(String.class);

                logger.error(
                        "Failed to update badge -  " + badgeResponse.getStatus() + " - " + responseBody);
            }
        } catch (Exception exception) {
            // Output expected ConnectException.
            logger.error("Exception when updating badge - " + exception);
        } finally {
            if (badgeResponse != null) {
                badgeResponse.close();
            }
        }
    }

    public static void updateAppState(Client client, String scsEndpoint, Logger logger, ObjectId homeId,
           String appStateSignature) {
        Invocation invocation;
        Response appStateResponse = null;

        Map<String, String> appState = Map.of("appStateSignature", appStateSignature);

        synchronized (client) {
            invocation = client.target(scsEndpoint).path("/api/v1/homes/" + homeId.toString() + "/appstateupdate")
                    .request()
                    .buildPost(Entity.json(appState));
        }

        try {
            appStateResponse = invocation.invoke(Response.class);

            if (appStateResponse.getStatus() != Status.OK.getStatusCode()) {
                // badge update failed
                String responseBody = appStateResponse.readEntity(String.class);

                logger.error(
                        "Failed to update appState -  " + appStateResponse.getStatus() + " - " + responseBody);
            }
        } catch (Exception exception) {
            // Output expected ConnectException.
            logger.error("Exception when updating appState - " + exception);
        } finally {
            if (appStateResponse != null) {
                appStateResponse.close();
            }
        }
    }
}
