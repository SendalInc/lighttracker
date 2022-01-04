package com.sendal;

import org.junit.Rule;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.junit.runner.RunWith;

import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.*;

import com.sendal.common.state.StateReport;
import com.sendal.common.state.StateValue;
import com.sendal.common.state.StateRegistration;
import com.sendal.common.state.StateIdentifier;

import com.sendal.lighttracker.App;
import com.sendal.lighttracker.LightTrackerConfiguration;
import com.sendal.common.pss.SendalSoftwareServiceConfiguration;
import com.sendal.lighttracker.pojo.LightTrackerStateHistory;
import com.sendal.lighttracker.db.dao.LightTrackerAnomalyDAO;
import com.sendal.lighttracker.db.dao.LightTrackerSubscriptionRecordDAO;
import com.sendal.lighttracker.db.LightTrackerAnomaly;
import com.sendal.lighttracker.db.LightTrackerSubscriptionRecord;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBList;
import com.mongodb.DBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.Mongo;
import com.mongodb.WriteResult;
import com.mongodb.WriteConcern;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.ServerAddress;
import com.mongodb.MongoCredential;
import com.mongodb.MongoClientOptions;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.MongoClientSettings;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.glassfish.jersey.client.JerseyClientBuilder;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;

import com.sendal.externalapicommon.ExternalHomeConfiguration;
import com.sendal.externalapicommon.ExternalDeviceConfiguration;
import com.sendal.externalapicommon.ExternalRoomConfiguration;

import org.mockito.Mockito;
import static org.mockito.Mockito.*;

import static net.jadler.Jadler.*;
import net.jadler.JadlerMocker;
import static net.jadler.Jadler.initJadlerUsing;
import static net.jadler.Jadler.closeJadler;
import net.jadler.stubbing.server.jdk.JdkStubHttpServer;

import static org.hamcrest.Matchers.containsString;

import static org.assertj.core.api.Assertions.*;

import com.sendal.externalapicommon.security.TimeUtils;
import com.sendal.externalapicommon.security.HmacSignatureGenerator;
import com.sendal.externalapicommon.security.APISecurityConstants;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.io.IOException;
import java.util.Collections;



public class LightTrackerAnomalyTest {
    private DB database;
    private ObjectMapper mapper;
    private static final String scsEndpoint = "http://localhost:3000";

    private static final ObjectId homeId = new ObjectId("61c5dfeea758b354003494b2");
    private static final ObjectId lightingDeviceId = new ObjectId("61c5d8c073a5f7394079eb33");
    private static final String lightingDeviceName = "Light 1";

    private static final ObjectId lightingDevice2Id = new ObjectId("61cb2da68735edd67efcdba7");
    private static final String lightingDevice2Name = "Light 2";

    private LightTrackerAnomalyDAO lightTrackerAnomalyDao;
    private LightTrackerSubscriptionRecordDAO lightTrackerSubscriptionRecordDao;

    @Before
    public void before() {
        mapper = new ObjectMapper();

        initJadlerUsing(new JdkStubHttpServer(3000));

        Mongo mongo = new Mongo("127.0.0.1", 27017);

        // Drop a Single Database
        database = mongo.getDB("3psslighttrackertest");
        database.dropDatabase();


        // setup runtime database access
        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));

        MongoClient mongoClient = new MongoClient(new MongoClientURI("mongodb://127.0.0.1:27017/?&w=majority&wtimeoutMS=5000"));
        MongoDatabase database = mongoClient.getDatabase("3psslighttrackertest")
                .withCodecRegistry(pojoCodecRegistry);

        lightTrackerAnomalyDao = new LightTrackerAnomalyDAO(database);
        lightTrackerSubscriptionRecordDao = new LightTrackerSubscriptionRecordDAO(database);

        // insert a valid subscription record.
        LightTrackerSubscriptionRecord amsr = new LightTrackerSubscriptionRecord();
        amsr.setHomeId(homeId);
        amsr.setHomeConfig(lightHomeConfiguration());

        lightTrackerSubscriptionRecordDao.createHomeSubscription(amsr);
    }

    @After
    public void after() {
        closeJadler();
    } 

    private ExternalHomeConfiguration lightHomeConfiguration() {
        // we leave permissions empty for now as it's not used.
        ExternalHomeConfiguration homeConfig = new ExternalHomeConfiguration();
        homeConfig.timeZone = "America/New_York";
        homeConfig.postalCode = "02537";
        homeConfig.homeLongitude = "-70.4517";
        homeConfig.homeLatitude = "41.7418";
        homeConfig.deviceConfigurations = new HashMap<String, List<ExternalDeviceConfiguration>>();

        ExternalDeviceConfiguration lightConfig = new ExternalDeviceConfiguration();
        lightConfig.deviceId = lightingDeviceId.toString();
        lightConfig.deviceName = lightingDeviceName;
        lightConfig.resourcesConfig = Map.ofEntries(
                        entry("lighting",
                            Map.ofEntries(
                                entry("onOff", true),
                                entry("dimmable", true),
                                entry("color", false))));

        ExternalDeviceConfiguration light2Config = new ExternalDeviceConfiguration();
        light2Config.deviceId = lightingDevice2Id.toString();
        light2Config.deviceName = lightingDevice2Name;
        light2Config.resourcesConfig = Map.ofEntries(
                entry("lighting",
                        Map.ofEntries(
                                entry("onOff", true),
                                entry("dimmable", true),
                                entry("color", false))));

        homeConfig.deviceConfigurations.put("lighting", List.of(lightConfig, light2Config));

        return homeConfig;
}

    private StateReport createStateForDevice(ObjectId deviceId ,String stateName, String stateValue) {
        StateReport sr = new StateReport();
        StateIdentifier si = new StateIdentifier();
        si.setHome(homeId);
        si.setDevice(deviceId);
        si.setStateName(stateName);
        sr.setStateIdentifier(si);

        sr.setStateValue(stateValue);
        sr.setLastUpdate(new Date());

        return sr;
    }

    private LightTrackerConfiguration createAppConfiguration() {
        SendalSoftwareServiceConfiguration sssc = new SendalSoftwareServiceConfiguration();
        sssc.setScs3pssId("6061dfd0b224d6f41337e1ab");
        sssc.setApiKey("3ab6a152-0d28-4614-8280-33d4451b672d");
        sssc.setSecretKey("P2tswvsCzBif4OiEJlXeMJxr7mS94zAP");
        sssc.setScsUrl(scsEndpoint);

        LightTrackerConfiguration amc = new LightTrackerConfiguration();
        amc.setSendalSoftwareService(sssc);

        return amc;
    }
    


    @Test
    public void anomalyCreatedThenCancelledBeforeExpiration() {
        StateReport lightOn = createStateForDevice(lightingDeviceId, "isOn", "1");
        StateReport lightOff = createStateForDevice(lightingDeviceId, "isOn", "0");
        Client client = new JerseyClientBuilder().build();

        // send the device event
        resetJadler();
        onRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId + "/badgestate")
                .havingBody(containsString("\"appBadge\":\"green\""))
                .havingBody(containsString("\"appBadgeIcon\":\"none\""))
                .respond()
                .withStatus(200);

        onRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId.toString() + "/appstateupdate")
                .respond()
                .withStatus(200);

        LightTrackerAnomaly.lightStateUpdate(lightOn, client,
                lightTrackerAnomalyDao, scsEndpoint);

        // there should be a anomaly created
        LightTrackerAnomaly anomaly = lightTrackerAnomalyDao.getLightTrackerAnomaly(lightingDeviceId);
        assertThat(anomaly).isNotNull();
        assertThat(anomaly.getHomeId()).isEqualTo(homeId);
        assertThat(anomaly.getAnomalyState()).isEqualTo(LightTrackerAnomaly.STATE_INITIALANOMALYTEST);

        verifyThatRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId + "/badgestate")
                .havingBody(containsString("\"appBadge\":\"green\""))
                .havingBody(containsString("\"appBadgeIcon\":\"none\""))
                .receivedTimes(1);

        verifyThatRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId.toString() + "/appstateupdate")
                .receivedTimes(0);

        // progress from anomaly to ML evaluation state.
        resetJadler();
        onRequest()
            .havingMethodEqualTo("GET")
            .havingPathEqualTo("/api/v1/devices/" + lightingDeviceId + "/models/lighting/on")
            .respond()
            .withContentType("application/json")
            .withBody("[1.0,0.5,0.1,0]")
            .withStatus(200);

        anomaly.timerExpired(client, 
            createAppConfiguration(),
            lightTrackerSubscriptionRecordDao,
            lightTrackerAnomalyDao);

        verifyThatRequest()
            .havingMethodEqualTo("GET")
            .havingPathEqualTo("/api/v1/devices/" + lightingDeviceId + "/models/lighting/on")
            .receivedTimes(1);

        anomaly = lightTrackerAnomalyDao.getLightTrackerAnomaly(lightingDeviceId);
        assertThat(anomaly).isNotNull();
        assertThat(anomaly.getAnomalyState()).isEqualTo(LightTrackerAnomaly.STATE_LIGHTONNORMAL);

        // cancel the light event before it can expire.
        resetJadler();
        onRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId + "/badgestate")
                .havingBody(containsString("\"appBadge\":\"green\""))
                .havingBody(containsString("\"appBadgeIcon\":\"none\""))
                .respond()
                .withStatus(200);

        onRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId.toString() + "/appstateupdate")
                .respond()
                .withStatus(200);

        LightTrackerAnomaly.lightStateUpdate(lightOff, client,
                lightTrackerAnomalyDao, scsEndpoint);

        anomaly = lightTrackerAnomalyDao.getLightTrackerAnomaly(lightingDeviceId);
        assertThat(anomaly).isNull();

        verifyThatRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId + "/badgestate")
                .havingBody(containsString("\"appBadge\":\"green\""))
                .havingBody(containsString("\"appBadgeIcon\":\"none\""))
                .receivedTimes(1);

        verifyThatRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId.toString() + "/appstateupdate")
                .receivedTimes(1);
    }

    @Test
    public void anomalyCreatedAndExpired() {
        StateReport lightOn = createStateForDevice(lightingDeviceId, "isOn", "1");
        Client client = new JerseyClientBuilder().build();

        // send the device event
        resetJadler();
        onRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId + "/badgestate")
                .havingBody(containsString("\"appBadge\":\"green\""))
                .havingBody(containsString("\"appBadgeIcon\":\"none\""))
                .respond()
                .withStatus(200);

        onRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId.toString() + "/appstateupdate")
                .respond()
                .withStatus(200);

        LightTrackerAnomaly.lightStateUpdate(lightOn, client,
                lightTrackerAnomalyDao, scsEndpoint);

        // there should be a anomaly created
        LightTrackerAnomaly anomaly = lightTrackerAnomalyDao.getLightTrackerAnomaly(lightingDeviceId);
        assertThat(anomaly).isNotNull();
        assertThat(anomaly.getHomeId()).isEqualTo(homeId);
        assertThat(anomaly.getAnomalyState()).isEqualTo(LightTrackerAnomaly.STATE_INITIALANOMALYTEST);

        verifyThatRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId + "/badgestate")
                .havingBody(containsString("\"appBadge\":\"green\""))
                .havingBody(containsString("\"appBadgeIcon\":\"none\""))
                .receivedTimes(1);

        verifyThatRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId.toString() + "/appstateupdate")
                .receivedTimes(0);

        // progress from anomaly to ML evaluation state.
        resetJadler();
        onRequest()
                .havingMethodEqualTo("GET")
                .havingPathEqualTo(
                        "/api/v1/devices/" + lightingDeviceId + "/models/lighting/on")
                .respond()
                .withContentType("application/json")
                .withBody("[1.0,0.5,0.1,0]")
                .withStatus(200);

        anomaly.timerExpired(client,
                createAppConfiguration(),
                lightTrackerSubscriptionRecordDao,
                lightTrackerAnomalyDao);

        verifyThatRequest()
                .havingMethodEqualTo("GET")
                .havingPathEqualTo(
                        "/api/v1/devices/" + lightingDeviceId + "/models/lighting/on")
                .receivedTimes(1);

        anomaly = lightTrackerAnomalyDao.getLightTrackerAnomaly(lightingDeviceId);
        assertThat(anomaly).isNotNull();
        assertThat(anomaly.getAnomalyState()).isEqualTo(LightTrackerAnomaly.STATE_LIGHTONNORMAL);


        // progress - this should cause a pending anomaly
        resetJadler();
        onRequest()
                .havingMethodEqualTo("GET")
                .havingPathEqualTo(
                        "/api/v1/devices/" + lightingDeviceId + "/models/lighting/on")
                .respond()
                .withContentType("application/json")
                .withBody("[0,0,0,0]")
                .withStatus(200);

        anomaly.timerExpired(client,
                createAppConfiguration(),
                lightTrackerSubscriptionRecordDao,
                lightTrackerAnomalyDao);

        verifyThatRequest()
                .havingMethodEqualTo("GET")
                .havingPathEqualTo(
                        "/api/v1/devices/" + lightingDeviceId + "/models/lighting/on")
                .receivedTimes(1);

        anomaly = lightTrackerAnomalyDao.getLightTrackerAnomaly(lightingDeviceId);
        assertThat(anomaly).isNotNull();
        assertThat(anomaly.getAnomalyState()).isEqualTo(LightTrackerAnomaly.STATE_LIGHTPENDINGANOMALY);

        // progress again, this should generate a notification and a change of badge state
        resetJadler();
        onRequest()
            .havingMethodEqualTo("POST")
            .havingPathEqualTo("/api/v1/homes/" + homeId + "/badgestate")
            .havingBody(containsString("\"appBadge\":\"yellow\""))
            .havingBody(containsString("\"appBadgeIcon\":\"exclamation\""))
            .respond()
            .withStatus(200);

        onRequest()
            .havingMethodEqualTo("POST")
            .havingPathEqualTo("/api/v1/homes/" + homeId.toString() + "/appstateupdate")
            .respond()
            .withStatus(200);

        onRequest()
            .havingMethodEqualTo("POST")
            .havingPathEqualTo("/api/v1/homes/" + homeId + "/notifications/send")
            .respond()
            .withStatus(200);

        anomaly.timerExpired(client,
                createAppConfiguration(),
                lightTrackerSubscriptionRecordDao,
                lightTrackerAnomalyDao);

        anomaly = lightTrackerAnomalyDao.getLightTrackerAnomaly(lightingDeviceId);
        assertThat(anomaly).isNotNull();
        assertThat(anomaly.getAnomalyState()).isEqualTo(LightTrackerAnomaly.STATE_LIGHTONANOMALY);

        verifyThatRequest()
            .havingMethodEqualTo("POST")
            .havingPathEqualTo("/api/v1/homes/" + homeId + "/badgestate")
            .havingBody(containsString("\"appBadge\":\"yellow\""))
            .havingBody(containsString("\"appBadgeIcon\":\"exclamation\""))
            .receivedTimes(1);

        verifyThatRequest()
            .havingMethodEqualTo("POST")
            .havingPathEqualTo("/api/v1/homes/" + homeId.toString() + "/appstateupdate")
            .receivedTimes(1);

        verifyThatRequest()
            .havingMethodEqualTo("POST")
            .havingPathEqualTo("/api/v1/homes/" + homeId + "/notifications/send")
            .receivedTimes(1);

        // instruct the app to ignore the anomaly
        resetJadler();
        onRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId + "/badgestate")
                .havingBody(containsString("\"appBadge\":\"green\""))
                .havingBody(containsString("\"appBadgeIcon\":\"none\""))
                .respond()
                .withStatus(200);

        onRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId.toString() + "/appstateupdate")
                .respond()
                .withStatus(200);

        boolean ignoreResult = anomaly.setAnomalyIgnore(client, scsEndpoint, lightTrackerAnomalyDao);
        assertThat(ignoreResult).isTrue();
        assertThat(anomaly.getAnomalyState()).isEqualTo(LightTrackerAnomaly.STATE_LIGHTONANOMALYIGNORE);

        verifyThatRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId + "/badgestate")
                .havingBody(containsString("\"appBadge\":\"green\""))
                .havingBody(containsString("\"appBadgeIcon\":\"none\""))
                .receivedTimes(1);

        verifyThatRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId.toString() + "/appstateupdate")
                .receivedTimes(1);
    }


    @Test
    public void twoanomaliesCreatedIgnoredAndExpired() {
        StateReport light1On = createStateForDevice(lightingDeviceId, "isOn", "1");
        StateReport light2On = createStateForDevice(lightingDevice2Id, "isOn", "1");
        StateReport light2Off = createStateForDevice(lightingDevice2Id, "isOn", "0");
        Client client = new JerseyClientBuilder().build();

        // send the light event
        resetJadler();
        onRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId + "/badgestate")
                .havingBody(containsString("\"appBadge\":\"green\""))
                .havingBody(containsString("\"appBadgeIcon\":\"none\""))
                .respond()
                .withStatus(200);

        onRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId.toString() + "/appstateupdate")
                .respond()
                .withStatus(200);

        LightTrackerAnomaly.lightStateUpdate(light1On, client,
                lightTrackerAnomalyDao, scsEndpoint);

        // there should be a anomaly created
        LightTrackerAnomaly anomaly = lightTrackerAnomalyDao.getLightTrackerAnomaly(lightingDeviceId);
        assertThat(anomaly).isNotNull();
        assertThat(anomaly.getHomeId()).isEqualTo(homeId);
        assertThat(anomaly.getAnomalyState()).isEqualTo(LightTrackerAnomaly.STATE_INITIALANOMALYTEST);

        verifyThatRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId + "/badgestate")
                .havingBody(containsString("\"appBadge\":\"green\""))
                .havingBody(containsString("\"appBadgeIcon\":\"none\""))
                .receivedTimes(1);

        verifyThatRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId.toString() + "/appstateupdate")
                .receivedTimes(0);

        // progress from anomaly to ML evaluation state.
        resetJadler();
        onRequest()
                .havingMethodEqualTo("GET")
                .havingPathEqualTo(
                        "/api/v1/devices/" + lightingDeviceId + "/models/lighting/on")
                .respond()
                .withContentType("application/json")
                .withBody("[1.0,0.5,0.1,0]")
                .withStatus(200);

        anomaly.timerExpired(client,
                createAppConfiguration(),
                lightTrackerSubscriptionRecordDao,
                lightTrackerAnomalyDao);

        verifyThatRequest()
                .havingMethodEqualTo("GET")
                .havingPathEqualTo(
                        "/api/v1/devices/" + lightingDeviceId + "/models/lighting/on")
                .receivedTimes(1);

        anomaly = lightTrackerAnomalyDao.getLightTrackerAnomaly(lightingDeviceId);
        assertThat(anomaly).isNotNull();
        assertThat(anomaly.getAnomalyState()).isEqualTo(LightTrackerAnomaly.STATE_LIGHTONNORMAL);

        // progress - this should cause a pending anomaly
        resetJadler();
        onRequest()
                .havingMethodEqualTo("GET")
                .havingPathEqualTo(
                        "/api/v1/devices/" + lightingDeviceId + "/models/lighting/on")
                .respond()
                .withContentType("application/json")
                .withBody("[0,0,0,0]")
                .withStatus(200);

        anomaly.timerExpired(client,
                createAppConfiguration(),
                lightTrackerSubscriptionRecordDao,
                lightTrackerAnomalyDao);

        verifyThatRequest()
                .havingMethodEqualTo("GET")
                .havingPathEqualTo(
                        "/api/v1/devices/" + lightingDeviceId + "/models/lighting/on")
                .receivedTimes(1);

        anomaly = lightTrackerAnomalyDao.getLightTrackerAnomaly(lightingDeviceId);
        assertThat(anomaly).isNotNull();
        assertThat(anomaly.getAnomalyState()).isEqualTo(LightTrackerAnomaly.STATE_LIGHTPENDINGANOMALY);

        // progress again, this should generate a notification and a change of badge
        // state
        resetJadler();
        onRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId + "/badgestate")
                .havingBody(containsString("\"appBadge\":\"yellow\""))
                .havingBody(containsString("\"appBadgeIcon\":\"exclamation\""))
                .respond()
                .withStatus(200);

        onRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId.toString() + "/appstateupdate")
                .respond()
                .withStatus(200);

        onRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId + "/notifications/send")
                .respond()
                .withStatus(200);

        anomaly.timerExpired(client,
                createAppConfiguration(),
                lightTrackerSubscriptionRecordDao,
                lightTrackerAnomalyDao);

        anomaly = lightTrackerAnomalyDao.getLightTrackerAnomaly(lightingDeviceId);
        assertThat(anomaly).isNotNull();
        assertThat(anomaly.getAnomalyState()).isEqualTo(LightTrackerAnomaly.STATE_LIGHTONANOMALY);

        verifyThatRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId + "/badgestate")
                .havingBody(containsString("\"appBadge\":\"yellow\""))
                .havingBody(containsString("\"appBadgeIcon\":\"exclamation\""))
                .receivedTimes(1);

        verifyThatRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId.toString() + "/appstateupdate")
                .receivedTimes(1);

        verifyThatRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId + "/notifications/send")
                .receivedTimes(1);


        {
            // light2 anomaly
            // send the lighting event
            resetJadler();
            onRequest()
                    .havingMethodEqualTo("POST")
                    .havingPathEqualTo("/api/v1/homes/" + homeId + "/badgestate")
                    .respond()
                    .withStatus(200);

            onRequest()
                    .havingMethodEqualTo("POST")
                    .havingPathEqualTo("/api/v1/homes/" + homeId.toString() + "/appstateupdate")
                    .havingBody(containsString("\"appBadge\":\"yellow\""))
                    .havingBody(containsString("\"appBadgeIcon\":\"exclamation\""))
                    .respond()
                    .withStatus(200);

            LightTrackerAnomaly.lightStateUpdate(light2On, client,
                    lightTrackerAnomalyDao, scsEndpoint);

            // there should be a anomaly created
            LightTrackerAnomaly light2Anomaly = lightTrackerAnomalyDao.getLightTrackerAnomaly(lightingDevice2Id);
            assertThat(light2Anomaly).isNotNull();
            assertThat(light2Anomaly.getHomeId()).isEqualTo(homeId);
            assertThat(light2Anomaly.getAnomalyState()).isEqualTo(LightTrackerAnomaly.STATE_INITIALANOMALYTEST);

            verifyThatRequest()
                    .havingMethodEqualTo("POST")
                    .havingPathEqualTo("/api/v1/homes/" + homeId + "/badgestate")
                    .havingBody(containsString("\"appBadge\":\"yellow\""))
                    .havingBody(containsString("\"appBadgeIcon\":\"exclamation\""))
                    .receivedTimes(1);

            verifyThatRequest()
                    .havingMethodEqualTo("POST")
                    .havingPathEqualTo("/api/v1/homes/" + homeId.toString() + "/appstateupdate")
                    .receivedTimes(0);

            // progress from anomaly to ML evaluation state.
            resetJadler();
            onRequest()
                    .havingMethodEqualTo("GET")
                    .havingPathEqualTo(
                            "/api/v1/devices/" + lightingDevice2Id + "/models/lighting/on")
                    .respond()
                    .withContentType("application/json")
                    .withBody("[1.0,0.5,0.1,0]")
                    .withStatus(200);

            light2Anomaly.timerExpired(client,
                    createAppConfiguration(),
                    lightTrackerSubscriptionRecordDao,
                    lightTrackerAnomalyDao);

            verifyThatRequest()
                    .havingMethodEqualTo("GET")
                    .havingPathEqualTo(
                            "/api/v1/devices/" + lightingDevice2Id + "/models/lighting/on")
                    .receivedTimes(1);

            light2Anomaly = lightTrackerAnomalyDao.getLightTrackerAnomaly(lightingDevice2Id);
            assertThat(light2Anomaly).isNotNull();
            assertThat(light2Anomaly.getAnomalyState()).isEqualTo(LightTrackerAnomaly.STATE_LIGHTONNORMAL);

             // progress - this should cause a pending anomaly
             resetJadler();
             onRequest()
                     .havingMethodEqualTo("GET")
                     .havingPathEqualTo(
                             "/api/v1/devices/" + lightingDevice2Id + "/models/lighting/on")
                     .respond()
                     .withContentType("application/json")
                     .withBody("[0,0,0,0]")
                     .withStatus(200);

             light2Anomaly.timerExpired(client,
                     createAppConfiguration(),
                     lightTrackerSubscriptionRecordDao,
                     lightTrackerAnomalyDao);

             verifyThatRequest()
                     .havingMethodEqualTo("GET")
                     .havingPathEqualTo(
                             "/api/v1/devices/" + lightingDevice2Id + "/models/lighting/on")
                     .receivedTimes(1);

             light2Anomaly = lightTrackerAnomalyDao.getLightTrackerAnomaly(lightingDevice2Id);
             assertThat(light2Anomaly).isNotNull();
             assertThat(light2Anomaly.getAnomalyState()).isEqualTo(LightTrackerAnomaly.STATE_LIGHTPENDINGANOMALY);

            // progress again, this should generate a notification and a change of badge
            // state
            resetJadler();
            onRequest()
                    .havingMethodEqualTo("POST")
                    .havingPathEqualTo("/api/v1/homes/" + homeId + "/badgestate")
                    .havingBody(containsString("\"appBadge\":\"yellow\""))
                    .havingBody(containsString("\"appBadgeIcon\":\"exclamation\""))
                    .respond()
                    .withStatus(200);

            onRequest()
                    .havingMethodEqualTo("POST")
                    .havingPathEqualTo("/api/v1/homes/" + homeId.toString() + "/appstateupdate")
                    .respond()
                    .withStatus(200);

            onRequest()
                    .havingMethodEqualTo("POST")
                    .havingPathEqualTo("/api/v1/homes/" + homeId + "/notifications/send")
                    .respond()
                    .withStatus(200);

            light2Anomaly.timerExpired(client,
                    createAppConfiguration(),
                    lightTrackerSubscriptionRecordDao,
                    lightTrackerAnomalyDao);

            light2Anomaly = lightTrackerAnomalyDao.getLightTrackerAnomaly(lightingDevice2Id);
            assertThat(light2Anomaly).isNotNull();
            assertThat(light2Anomaly.getAnomalyState()).isEqualTo(LightTrackerAnomaly.STATE_LIGHTONANOMALY);

            verifyThatRequest()
                    .havingMethodEqualTo("POST")
                    .havingPathEqualTo("/api/v1/homes/" + homeId + "/badgestate")
                    .havingBody(containsString("\"appBadge\":\"yellow\""))
                    .havingBody(containsString("\"appBadgeIcon\":\"exclamation\""))
                    .receivedTimes(1);

            verifyThatRequest()
                    .havingMethodEqualTo("POST")
                    .havingPathEqualTo("/api/v1/homes/" + homeId.toString() + "/appstateupdate")
                    .receivedTimes(1);

            verifyThatRequest()
                    .havingMethodEqualTo("POST")
                    .havingPathEqualTo("/api/v1/homes/" + homeId + "/notifications/send")
                    .receivedTimes(1);
        }


        // instruct the app to ignore the anomaly
        resetJadler();
        onRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId + "/badgestate")
                .havingBody(containsString("\"appBadge\":\"green\""))
                .havingBody(containsString("\"appBadgeIcon\":\"none\""))
                .respond()
                .withStatus(200);

        onRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId.toString() + "/appstateupdate")
                .respond()
                .withStatus(200);

        boolean ignoreResult = anomaly.setAnomalyIgnore(client, scsEndpoint, lightTrackerAnomalyDao);
        assertThat(ignoreResult).isTrue();
        assertThat(anomaly.getAnomalyState()).isEqualTo(LightTrackerAnomaly.STATE_LIGHTONANOMALYIGNORE);

        verifyThatRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId + "/badgestate")
                .havingBody(containsString("\"appBadge\":\"green\""))
                .havingBody(containsString("\"appBadgeIcon\":\"none\""))
                .receivedTimes(0);

        verifyThatRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId.toString() + "/appstateupdate")
                .receivedTimes(1);

        {
            resetJadler();
            onRequest()
                    .havingMethodEqualTo("POST")
                    .havingPathEqualTo("/api/v1/homes/" + homeId + "/badgestate")
                    .havingBody(containsString("\"appBadge\":\"green\""))
                    .havingBody(containsString("\"appBadgeIcon\":\"none\""))
                    .respond()
                    .withStatus(200);

            onRequest()
                    .havingMethodEqualTo("POST")
                    .havingPathEqualTo("/api/v1/homes/" + homeId.toString() + "/appstateupdate")
                    .respond()
                    .withStatus(200);

            // light2 turns off, return to normal
            LightTrackerAnomaly.lightStateUpdate(light2Off, client,
                    lightTrackerAnomalyDao, scsEndpoint);

            anomaly = lightTrackerAnomalyDao.getLightTrackerAnomaly(lightingDevice2Id);
            assertThat(anomaly).isNull();

            verifyThatRequest()
                    .havingMethodEqualTo("POST")
                    .havingPathEqualTo("/api/v1/homes/" + homeId + "/badgestate")
                    .havingBody(containsString("\"appBadge\":\"green\""))
                    .havingBody(containsString("\"appBadgeIcon\":\"none\""))
                    .receivedTimes(1);

            verifyThatRequest()
                    .havingMethodEqualTo("POST")
                    .havingPathEqualTo("/api/v1/homes/" + homeId.toString() + "/appstateupdate")
                    .receivedTimes(1);
        }
    }
  }
