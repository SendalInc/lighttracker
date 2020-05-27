package com.sendal.lighttracker.db.dao;

import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import java.util.List;
import java.util.ArrayList;

import com.sendal.lighttracker.db.LightTrackerSubscriptionRecord;

import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

public class LightTrackerSubscriptionRecordDAO {

    private MongoCollection<LightTrackerSubscriptionRecord> collection;

    public LightTrackerSubscriptionRecordDAO(MongoDatabase database) {
        collection = database.getCollection("LightTrackerSubscriptions", LightTrackerSubscriptionRecord.class);
    }

    public LightTrackerSubscriptionRecord getHomeSubscription(ObjectId id) {
        LightTrackerSubscriptionRecord value = collection.find(eq("_id", id)).first();

        return value;
    }

    public ObjectId createHomeSubscription(LightTrackerSubscriptionRecord subscription) {
        collection.insertOne(subscription);
        return subscription.getHomeId();
    }

    public LightTrackerSubscriptionRecord updateHomeSubscription(LightTrackerSubscriptionRecord subscription) {
        collection.replaceOne(eq("_id", subscription.getHomeId()), subscription);
        return subscription;
    }

    public boolean deleteSubscription(ObjectId id) {
        DeleteResult result = collection.deleteOne(eq("_id", id));
        return (result.getDeletedCount() != 0);
    }

}
