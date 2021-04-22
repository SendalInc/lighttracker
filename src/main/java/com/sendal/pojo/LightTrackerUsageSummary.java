package com.sendal.lighttracker;

import java.io.Serializable;

//
// Used to represent data returned to the IS about light usage summaries.
public class LightTrackerUsageSummary implements Serializable {

    public LightTrackerUsageSummary(String deviceId, String deviceName) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
    }
    
    private String deviceId;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    

    private String deviceName;

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }


    private long usageDaySeconds;

    public long getUsageDaySeconds() {
        return usageDaySeconds;
    }

    public void setUsageDaySeconds(long val) {
        this.usageDaySeconds = val;
    }

    private long nextHourOnProbability;// a integer percentage 0-100

    public void setNextHourOnProbability(long nextHourOnProbability) {
        this.nextHourOnProbability = nextHourOnProbability;
    }

    public long getNextHourOnProbability() {
        return nextHourOnProbability;
    }


    private long usageWeekSeconds;

    public long getUsageWeekSeconds() {
        return usageWeekSeconds;
    }

    public void setUsageWeekSeconds(long val) {
        this.usageWeekSeconds = val;
    }
    

    private long weekAverageSeconds;

    public long getWeekAverageSeconds() {
        return weekAverageSeconds;
    }

    public void setWeekAverageSeconds(long val) {
        this.weekAverageSeconds = val;
    }


    private long usageMonthSeconds;

    public long getUsageMonthSeconds() {
        return usageMonthSeconds;
    }

    public void setUsageMonthSeconds(long val) {
        this.usageMonthSeconds = val;
    }


    private long thirtyDayAverageSeconds;

    public long getThirtyDayAverageSeconds() {
        return thirtyDayAverageSeconds;
    }

    public void setThirtyDayAverageSeconds(long val) {
        this.thirtyDayAverageSeconds = val;
    }
}