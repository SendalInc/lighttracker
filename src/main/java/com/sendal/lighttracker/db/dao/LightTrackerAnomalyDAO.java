package com.sendal.lighttracker.db.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import java.util.List;
import java.util.ArrayList;

import com.sendal.lighttracker.db.LightTrackerAnomaly;

import com.sendal.common.olock.OLockFailureException;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

public class LightTrackerAnomalyDAO {

    private final Logger logger = LoggerFactory.getLogger(LightTrackerAnomalyDAO.class);

    private static final double EXPIRED_LOCK_MAX_AGE = 10.0 * 1000;

    private MongoCollection<LightTrackerAnomaly> collection;

    public LightTrackerAnomalyDAO(MongoDatabase database) {
        collection = database.getCollection("LightTrackerAnomalies", LightTrackerAnomaly.class);
    }

    public LightTrackerAnomaly getLightTrackerAnomaly(ObjectId id) {
        LightTrackerAnomaly value = collection.find(eq("_id", id)).first();

        return value;
    }

    public List<LightTrackerAnomaly> getHouseAllActiveLights(ObjectId homeId) {
        List<LightTrackerAnomaly> anomalies = (List<LightTrackerAnomaly>) collection.find(
                eq("homeId", homeId))
                .into(new ArrayList<LightTrackerAnomaly>());

        return anomalies;
    }

    public List<LightTrackerAnomaly>getHouseActiveAnomalies(ObjectId homeId) {
        List<LightTrackerAnomaly> anomalies = (List<LightTrackerAnomaly>) collection.find(
                    and(
                        eq("homeId", homeId),
                        eq("anomalyState", LightTrackerAnomaly.STATE_LIGHTONANOMALY)
                    )
                ).into(new ArrayList<LightTrackerAnomaly>());

        return anomalies;
    }

    public LightTrackerAnomaly getLightTrackerAnomalyWithLock(ObjectId id)
            throws OLockFailureException {
        // gets can block for a time in order to get the concurrency lock.
        LightTrackerAnomaly value = null;

        Long timestamp = System.currentTimeMillis();

        // see if the record exists at all
        if (collection.find(eq("_id", id)).first() != null) {
            while (value == null && (System.currentTimeMillis()) < (timestamp + 1.5 * EXPIRED_LOCK_MAX_AGE)) {
                try {
                    value = collection.findOneAndUpdate(and(
                            eq("_id", id),
                            or(
                                    exists("olock", false),
                                    lte("olock", timestamp - EXPIRED_LOCK_MAX_AGE))),
                            set("olock", timestamp));

                    if (value != null) {
                        value.olock = timestamp;
                        break; // from while loop
                    }
                } catch (Exception e) {
                    logger.error("Exception getting IAQRoomState: " + e);
                }

                // we get here because of some issue above
                try {
                    Thread.sleep(200); // 0.2 second wait

                } catch (java.lang.InterruptedException e) {

                }
            }

            if (value == null) {
                logger.error("Failed to lock LightTracker anomaly " + id);
                throw new OLockFailureException("Failed to lock LightTrackerAnomaly " + id);
            }
        }

        return value;
    }

    public ObjectId createLightTrackerAnomaly(LightTrackerAnomaly lightTrackerAnomaly) {
        collection.insertOne(lightTrackerAnomaly);

        return lightTrackerAnomaly.getDeviceId();
    }

    /*
    public LightTrackerAnomaly updateLightTrackerAnomaly(LightTrackerAnomaly lightTrackerAnomaly) {
        collection.replaceOne(eq("_id", lightTrackerAnomaly.getDeviceId()), lightTrackerAnomaly);
        return lightTrackerAnomaly;
    }
*/
    public LightTrackerAnomaly updateLightTrackerAnomaly(ObjectId deviceId, double previousOlock, double olock, String anomalyState, Long timeOfNextAction) {
        LightTrackerAnomaly lightTrackerAnomaly = null;

        // the extra effort is done here to ensure we don't update a record at the same time the light is turning off
        // and trying to delete the record.
        try {
            lightTrackerAnomaly = collection.findOneAndUpdate(and(
                eq("_id", deviceId), 
                eq("olock", previousOlock)
            ),
            combine(
                set("olock", olock),
                set("anomalyState", anomalyState),
                set("timeOfNextAction", timeOfNextAction)
            )
            );
        } catch (Exception e) {
            logger.error("Exception in getAnExpiredRecord: " + e);
        }

        return lightTrackerAnomaly;
    }


    public boolean deleteLightTrackerAnomaly(ObjectId id) {
        DeleteResult result = collection.deleteOne(eq("_id", id));
        return (result.getDeletedCount() != 0);
    }

    public boolean deleteLightTrackerAnomoliesForHome(ObjectId homeId) {
        DeleteResult result = collection.deleteMany(eq("homeId", homeId));
        return (result.getDeletedCount() != 0);
    }

    public LightTrackerAnomaly getAnExpiredRecord(long timeStamp, double lockMaxAge) {
        LightTrackerAnomaly lightTrackerAnomaly = null;

        try {
            lightTrackerAnomaly = collection.findOneAndUpdate(and(
                lte("timeOfNextAction", timeStamp),
                ne("timeOfNextAction", 0),
                or(
                    exists("olock", false), 
                    lte("olock", timeStamp - lockMaxAge)
                )
                ),
                set("olock", timeStamp)
            );

            if(lightTrackerAnomaly != null) {
                lightTrackerAnomaly.olock = timeStamp;
            }
        } catch (Exception e) {
            logger.error("Exception in getAnExpiredRecord: " + e);
        }

        return lightTrackerAnomaly;
    }
}
