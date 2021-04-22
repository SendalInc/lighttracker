package com.sendal.lighttracker.db.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bson.types.ObjectId;

import com.sendal.lighttracker.db.LightTrackerAnomoly;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

public class LightTrackerAnomolyDAO {

    private final Logger logger = LoggerFactory.getLogger(LightTrackerAnomolyDAO.class);

    private MongoCollection<LightTrackerAnomoly> collection;

    public LightTrackerAnomolyDAO(MongoDatabase database) {
        collection = database.getCollection("LightTrackerAnomolies", LightTrackerAnomoly.class);
    }

    public LightTrackerAnomoly getLightTrackerAnomoly(ObjectId id) {
        LightTrackerAnomoly value = collection.find(eq("_id", id)).first();

        return value;
    }

    public ObjectId createLightTrackerAnomoly(LightTrackerAnomoly lightTrackerAnomoly) {
        collection.insertOne(lightTrackerAnomoly);

        return lightTrackerAnomoly.getDeviceId();
    }

    public LightTrackerAnomoly updateLightTrackerAnomoly(ObjectId deviceId, double previousOlock, double olock, String nextAction, Long timeOfNextAction) {
        LightTrackerAnomoly lightTrackerAnomoly = null;

        // the extra effort is done here to ensure we don't update a record at the same time the light is turning off
        // and trying to delete the record.
        try {
            lightTrackerAnomoly = collection.findOneAndUpdate(and(
                eq("_id", deviceId), 
                eq("olock", previousOlock)
            ),
            combine(
                set("olock", olock),
                set("nextAction", nextAction),
                set("timeOfNextAction", timeOfNextAction)
            )
            );
        } catch (Exception e) {
            logger.error("Exception in getAnExpiredRecord: " + e);
        }

        return lightTrackerAnomoly;
    }


    public boolean deleteLightTrackerAnomoly(ObjectId id) {
        DeleteResult result = collection.deleteOne(eq("_id", id));
        return (result.getDeletedCount() != 0);
    }

    public LightTrackerAnomoly getAnExpiredRecord(long timeStamp, double lockMaxAge) {
        LightTrackerAnomoly lightTrackerAnomoly = null;

        try {
            lightTrackerAnomoly = collection.findOneAndUpdate(and(
                lte("timeOfNextAction", timeStamp),
                or(
                    exists("olock", false), 
                    lte("olock", timeStamp - lockMaxAge)
                )
                ),
                set("olock", timeStamp)
            );

            if(lightTrackerAnomoly != null) {
                lightTrackerAnomoly.olock = timeStamp;
            }
        } catch (Exception e) {
            logger.error("Exception in getAnExpiredRecord: " + e);
        }

        return lightTrackerAnomoly;
    }
}
