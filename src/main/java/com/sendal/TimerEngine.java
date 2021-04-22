package com.sendal.lighttracker;

import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Calendar;

import javax.ws.rs.client.Client;

import com.sendal.lighttracker.db.LightTrackerAnomoly;
import com.sendal.lighttracker.db.dao.LightTrackerAnomolyDAO;
import com.sendal.lighttracker.db.dao.LightTrackerSubscriptionRecordDAO;
import com.sendal.lighttracker.LightTrackerConfiguration;

public class TimerEngine implements Managed {
    private static final double TIMERENGINE_POLL_INTERVAL = 5.0;
    private static final double TIMERENGINE_EXPIRED_LOCK_MAX_AGE = 10.0;
    private static final double TIMERENGINE_MAX_EVALUATION_TIME_MS = 3.0 * 1000.0;

    private final Logger logger = LoggerFactory.getLogger(TimerEngine.class);

    private final Client client;
    private final LightTrackerAnomolyDAO lightTrackerAnomoloyDao;
    private final LightTrackerSubscriptionRecordDAO lightTrackerSubscriptionRecordDao;
    private final LightTrackerConfiguration configuration;

    private Timer activeTimer = null;

    public TimerEngine(Client client, LightTrackerSubscriptionRecordDAO lightTrackerSubscriptionRecordDao, LightTrackerAnomolyDAO lightTrackerAnomoloyDao, 
            LightTrackerConfiguration configuration) {
        this.client = client;
        this.lightTrackerAnomoloyDao = lightTrackerAnomoloyDao;
        this.configuration = configuration;
        this.lightTrackerSubscriptionRecordDao = lightTrackerSubscriptionRecordDao;
    }

    public void processFireTimer() {
        long startTimeStamp = System.currentTimeMillis();
        LightTrackerAnomoly ltAnomoly = null;
        try {
            do {
                // logger.debug("Timer running...");
                ltAnomoly = lightTrackerAnomoloyDao.getAnExpiredRecord(System.currentTimeMillis() / 1000,
                        TIMERENGINE_EXPIRED_LOCK_MAX_AGE);

                if (ltAnomoly != null) {
                    ltAnomoly.timerExpired(client, configuration, 
                            lightTrackerSubscriptionRecordDao, lightTrackerAnomoloyDao);
                }
            } while (ltAnomoly != null
                    && System.currentTimeMillis() - startTimeStamp < TIMERENGINE_MAX_EVALUATION_TIME_MS);

            if (ltAnomoly != null) {
                logger.warn("Timer engine could not process all expired entries - overload");
            }

        } catch (Exception e) {
            logger.error("Exception in timer task: " + e);
        }
    }

    private final TimerTask timerTask = new TimerTask() {
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

        timer.scheduleAtFixedRate(timerTask, cal.getTime(), ((long) TIMERENGINE_POLL_INTERVAL) * 1000L);
    }

    @Override
    public void stop() throws Exception {
        logger.info("Timer Engine Stopped");
        if (activeTimer != null) {
            activeTimer.cancel();
            activeTimer = null;
        }
    }
}
