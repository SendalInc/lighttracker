package com.sendal.lighttracker;

import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;
import java.util.Calendar;

import org.bson.types.ObjectId;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ProcessingException;

import com.sendal.lighttracker.db.LightTrackerAnomaly;
import com.sendal.lighttracker.db.dao.LightTrackerAnomalyDAO;
import com.sendal.lighttracker.db.dao.LightTrackerSubscriptionRecordDAO;
import com.sendal.lighttracker.LightTrackerConfiguration;

public class TimerEngine implements Managed {
    private static final double TIMERENGINE_POLL_INTERVAL = 5.0 * 1000;
    private static final double TIMERENGINE_EXPIRED_LOCK_MAX_AGE = 10.0 * 1000;
    private static final double TIMERENGINE_MAX_EVALUATION_TIME_MS = 3.0 * 1000.0;

    private final Logger logger = LoggerFactory.getLogger(TimerEngine.class);

    private final Client client;
    private final LightTrackerAnomalyDAO lightTrackerAnomoloyDao;
    private final LightTrackerSubscriptionRecordDAO lightTrackerSubscriptionRecordDao;
    private final LightTrackerConfiguration configuration;

    public TimerEngine(Client client, LightTrackerSubscriptionRecordDAO lightTrackerSubscriptionRecordDao, LightTrackerAnomalyDAO lightTrackerAnomoloyDao, 
            LightTrackerConfiguration configuration) {
        this.client = client;
        this.lightTrackerAnomoloyDao = lightTrackerAnomoloyDao;
        this.configuration = configuration;
        this.lightTrackerSubscriptionRecordDao = lightTrackerSubscriptionRecordDao;
    }

    public void processFireTimer() {
        long startTimeStamp = System.currentTimeMillis();
        LightTrackerAnomaly ltAnomaly = null;
        try {
            do {
                // logger.debug("Timer running...");
                ltAnomaly = lightTrackerAnomoloyDao.getAnExpiredRecord(System.currentTimeMillis(),
                        TIMERENGINE_EXPIRED_LOCK_MAX_AGE);

                if (ltAnomaly != null) {
                    ltAnomaly.timerExpired(client, configuration, 
                            lightTrackerSubscriptionRecordDao, lightTrackerAnomoloyDao);
                }
            } while (ltAnomaly != null
                    && System.currentTimeMillis() - startTimeStamp < TIMERENGINE_MAX_EVALUATION_TIME_MS);

            if (ltAnomaly != null) {
                logger.warn("Timer engine could not process all expired entries - overload");
            }

        } catch (Exception e) {
            logger.error("Exception in timer task: " + e);
        }
    }

    private TimerTask timerTask = new TimerTask() {
        public void run() {
            processFireTimer();
        }
    };

    @Override
    public void start() throws Exception {
        logger.info("Timer Engine Started");
        Timer timer = new Timer("Timer Engine");

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        timer.scheduleAtFixedRate(timerTask, cal.getTime(), ((long) TIMERENGINE_POLL_INTERVAL));
    }

    @Override
    public void stop() throws Exception {
        logger.info("Timer Engine Stopped");
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }
}
