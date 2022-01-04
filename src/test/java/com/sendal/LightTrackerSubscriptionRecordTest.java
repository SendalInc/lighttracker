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

import static org.hamcrest.Matchers.containsString;

import com.sendal.common.state.StateReport;
import com.sendal.common.state.StateValue;
import com.sendal.common.state.StateRegistration;
import com.sendal.common.state.StateIdentifier;


import com.sendal.externalapicommon.ExternalHomeConfiguration;
import com.sendal.externalapicommon.ExternalDeviceConfiguration;
import com.sendal.externalapicommon.ExternalRoomConfiguration;

import com.sendal.common.coredb.DBPermissions;

import com.mongodb.*;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.*;
import org.bson.Document;
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

import org.mockito.Mockito;
import static org.mockito.Mockito.*;

import static net.jadler.Jadler.*;
import net.jadler.JadlerMocker;
import static net.jadler.Jadler.initJadlerUsing;
import static net.jadler.Jadler.closeJadler;
import net.jadler.stubbing.server.jdk.JdkStubHttpServer;

import static org.assertj.core.api.Assertions.*;

import com.sendal.externalapicommon.security.TimeUtils;
import com.sendal.externalapicommon.security.HmacSignatureGenerator;
import com.sendal.externalapicommon.security.APISecurityConstants;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.io.IOException;
import java.util.Collections;

import com.sendal.lighttracker.*;
import com.sendal.lighttracker.db.*;
import com.sendal.lighttracker.db.dao.*;

public class LightTrackerSubscriptionRecordTest {
    
    private DB database;
    private ObjectMapper mapper;
    private LightTrackerSubscriptionRecordDAO subscriptionRecordDao;
    private LightTrackerAnomalyDAO lightTrackerAnomalyDao;

    private String scs3pssId = "";
    private String scsEndpoint = "http://localhost:3000";

    static private final String testAppID = "61d4c1a7f896ac4805789d8a";
    static private final String testAppSecretKey = "BcQJ9Z7WwOlyxyQYXX6RVRMYZ244QERi";
    static private final String testAppApiKey = " c5645d61-389a-486e-a5a8-6a46a39ff877";

    private final static ObjectId homeId = new ObjectId("60c103f488c376aaaab365fa");
    private static final ObjectId lightingDeviceId = new ObjectId("61c5d8c073a5f7394079eb33");
    private static final String lightingDeviceName = "Test Light";
    private static final ObjectId lightingDeviceIdNotInConfig = new ObjectId("61c603819917a9eea46ac455");

    private final ClientRequestFilter hmacRequestFilter = new ClientRequestFilter() {
        public void filter(ClientRequestContext ctx) throws IOException {

            String timestamp = TimeUtils.getCurrentTimestamp();

            byte[] content = null;

            if (ctx.hasEntity()) {
                content = ctx.getEntity().toString().getBytes();
            }

            String signature = HmacSignatureGenerator.generate(testAppSecretKey, ctx.getMethod(),
                    timestamp, ctx.getUri().getPath(), content);

            // add the headers
            final Map<String, List<Object>> headers = ctx.getHeaders();
            headers.put(APISecurityConstants.DEFAULT_SIGNATURE_HTTP_HEADER, Collections.singletonList(signature));
            headers.put(APISecurityConstants.DEFAULT_TIMESTAMP_HTTP_HEADER, Collections.singletonList(timestamp));
            headers.put(APISecurityConstants.DEFAULT_VERSION_HTTP_HEADER, Collections.singletonList("3"));
            headers.put(APISecurityConstants.DEFAULT_API_KEYNAME, Collections.singletonList(testAppApiKey));
        }
    };

    @Rule
    public final DropwizardAppRule<LightTrackerConfiguration> RULE = new DropwizardAppRule<LightTrackerConfiguration>(
            App.class, "config-test.yml");

    @Before
    public void before() {
        initJadlerUsing(new JdkStubHttpServer(3000));

        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));

        MongoClientOptions options = MongoClientOptions.builder().writeConcern(WriteConcern.JOURNALED).build();
        MongoClient mongoClient = new MongoClient(
                        Arrays.asList(new ServerAddress("127.0.0.1", 27017)), options);
        MongoDatabase datastore = mongoClient.getDatabase("3psslighttrackertest").withCodecRegistry(pojoCodecRegistry);

        datastore.drop();

        subscriptionRecordDao = new LightTrackerSubscriptionRecordDAO(datastore);
        lightTrackerAnomalyDao = new LightTrackerAnomalyDAO(datastore);

        mapper = new ObjectMapper();
    }

    @After
    public void after() {
        closeJadler();
    }

     private ExternalHomeConfiguration standardTestConfiguration() {
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
                    entry("color", false)
                )
            )
        );

        homeConfig.deviceConfigurations.put("lighting", List.of(lightConfig));

        return homeConfig;    
    }

    private ExternalHomeConfiguration emptyTestConfiguration() {
        // we leave permissions empty for now as it's not used.
        ExternalHomeConfiguration homeConfig = new ExternalHomeConfiguration();
        homeConfig.timeZone = "America/New_York";
        homeConfig.postalCode = "02537";
        homeConfig.homeLongitude = "-70.4517";
        homeConfig.homeLatitude = "41.7418";
        homeConfig.deviceConfigurations = new HashMap<String, List<ExternalDeviceConfiguration>>();

        return homeConfig;
     }

    @Test
    public void subscriptionTest() throws Exception {
        DBPermissions permissions = new DBPermissions();

        Client client = new JerseyClientBuilder().build();
        client.register(hmacRequestFilter);
  
        onRequest()
            .havingMethodEqualTo("GET")
            .havingPathEqualTo("/api/v1/homes/"+homeId.toString()+"/configuration")
            .respond()
            // NOTE: post body is not tested here.
            .withContentType("application/json")
            .withBody(mapper.writeValueAsString(standardTestConfiguration()))
            .withStatus(200);

        onRequest()
            .havingMethodEqualTo("POST")
            .havingPathEqualTo("/api/v1/states/subscriptions/subscribe")
            .respond()
            // NOTE: post body is not tested here.
            .withStatus(200);
            
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

        Response response = client.target(String.format("http://localhost:" + RULE.getLocalPort() + "/api/v1/homes/"+ homeId.toString() + "/subscription"))
            .request()
            .post(Entity.json(mapper.writeValueAsString(permissions)));

        assertThat(response.getStatus()).isEqualTo(200);

        verifyThatRequest()
            .havingMethodEqualTo("GET")
            .havingPathEqualTo("/api/v1/homes/"+homeId.toString()+"/configuration")
            .receivedTimes(1);

        verifyThatRequest()
            .havingMethodEqualTo("POST")
            .havingPathEqualTo("/api/v1/states/subscriptions/subscribe")
            .receivedTimes(1);

        verifyThatRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId + "/badgestate")
                .havingBody(containsString("\"appBadge\":\"green\""))
                .havingBody(containsString("\"appBadgeIcon\":\"none\""))
                .receivedTimes(1);

        verifyThatRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId + "/appstateupdate")
                .receivedTimes(1);

        // verify the database.
        LightTrackerSubscriptionRecord subscriptionRecord = subscriptionRecordDao.getHomeSubscription(homeId);
        assertThat(subscriptionRecord).isNotNull();
        assertThat(subscriptionRecord.getPermissions()).isNotNull();
        assertThat(subscriptionRecord.getHomeConfig()).isNotNull();

        response.close();
    }

    @Test
    public void unsubscriptionTest() throws Exception {
        // run the subscription test
        subscriptionTest();

        // use the API to delete the subscription
        Client client = new JerseyClientBuilder().build();
        client.register(hmacRequestFilter);
  
        resetJadler();

        Response response = client.target(String.format("http://localhost:" + RULE.getLocalPort() + "/api/v1/homes/"+ homeId.toString() + "/subscription"))
            .request()
            .delete();

        assertThat(response.getStatus()).isEqualTo(200);

        LightTrackerSubscriptionRecord subscriptionRecord = subscriptionRecordDao.getHomeSubscription(homeId);
        assertThat(subscriptionRecord).isNull();
    }

    @Test 
    public void configurationUpdatedToEmpty() throws Exception {
        // run the subscription test
        subscriptionTest();

        // insert an anomaly
        LightTrackerAnomaly ama = new LightTrackerAnomaly();
        ama.setDeviceId(lightingDeviceIdNotInConfig);
        ama.setHomeId(homeId);
        ama.setAnomalyState(LightTrackerAnomaly.STATE_LIGHTONNORMAL);

        lightTrackerAnomalyDao.createLightTrackerAnomaly(ama);

        // use the API to delete the subscription
        Client client = new JerseyClientBuilder().build();
        client.register(hmacRequestFilter);
  
        resetJadler();

        onRequest()
            .havingMethodEqualTo("GET")
            .havingPathEqualTo("/api/v1/homes/"+homeId.toString()+"/configuration")
            .respond()
            // NOTE: post body is not tested here.
            .withContentType("application/json")
            .withBody(mapper.writeValueAsString(emptyTestConfiguration()))
            .withStatus(200);

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

         Response response = client.target(String.format("http://localhost:" + RULE.getLocalPort() + "/api/v1/homes/"+ homeId.toString() + "/configuration/updated"))
            .request()
            .post(Entity.json("{}"));

        assertThat(response.getStatus()).isEqualTo(200);

        verifyThatRequest()
            .havingMethodEqualTo("GET")
            .havingPathEqualTo("/api/v1/homes/"+homeId.toString()+"/configuration")
            .receivedTimes(1);

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

        // verify the database.
        LightTrackerSubscriptionRecord subscriptionRecord = subscriptionRecordDao.getHomeSubscription(homeId);
        assertThat(subscriptionRecord).isNotNull();
        assertThat(subscriptionRecord.getPermissions()).isNotNull();
        assertThat(subscriptionRecord.getHomeConfig()).isNotNull();

        assertThat(subscriptionRecord.getHomeConfig().deviceConfigurations.keySet().size()).isEqualTo(0);

        List<LightTrackerAnomaly> anomalies = lightTrackerAnomalyDao.getHouseAllActiveLights(homeId);
        assertThat(anomalies.size()).isEqualTo(0);

        response.close();
    }

    @Test 
    public void configurationUpdatedUnchanged() throws Exception {
        // run the subscription test
        subscriptionTest();

        // insert an anomaly
        LightTrackerAnomaly ama = new LightTrackerAnomaly();
        ama.setDeviceId(lightingDeviceId);
        ama.setHomeId(homeId);
        ama.setAnomalyState(LightTrackerAnomaly.STATE_LIGHTONNORMAL);

        lightTrackerAnomalyDao.createLightTrackerAnomaly(ama);
        
        // use the API to update the subscription
        Client client = new JerseyClientBuilder().build();
        client.register(hmacRequestFilter);
  
        resetJadler();

        onRequest()
            .havingMethodEqualTo("GET")
            .havingPathEqualTo("/api/v1/homes/"+homeId.toString()+"/configuration")
            .respond()
            // NOTE: post body is not tested here.
            .withContentType("application/json")
            .withBody(mapper.writeValueAsString(standardTestConfiguration()))
            .withStatus(200);

        onRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/states/subscriptions/subscribe")
                .respond()
                // NOTE: post body is not tested here.
                .withStatus(200);

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

         Response response = client.target(String.format("http://localhost:" + RULE.getLocalPort() + "/api/v1/homes/"+ homeId.toString() + "/configuration/updated"))
            .request()
            .post(Entity.json("{}"));

        assertThat(response.getStatus()).isEqualTo(200);

        verifyThatRequest()
            .havingMethodEqualTo("GET")
            .havingPathEqualTo("/api/v1/homes/"+homeId.toString()+"/configuration")
            .receivedTimes(1);

        verifyThatRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/states/subscriptions/subscribe")
                .receivedTimes(1);

        verifyThatRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId + "/badgestate")
                .havingBody(containsString("\"appBadge\":\"green\""))
                .havingBody(containsString("\"appBadgeIcon\":\"none\""))
                .receivedTimes(1);

        verifyThatRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/api/v1/homes/" + homeId + "/appstateupdate")
                .receivedTimes(1);

        // verify the database.
        LightTrackerSubscriptionRecord subscriptionRecord = subscriptionRecordDao.getHomeSubscription(homeId);
        assertThat(subscriptionRecord).isNotNull();
        assertThat(subscriptionRecord.getPermissions()).isNotNull();
        assertThat(subscriptionRecord.getHomeConfig()).isNotNull();

        assertThat(subscriptionRecord.getHomeConfig().deviceConfigurations.keySet().size()).isEqualTo(1);

        List<LightTrackerAnomaly> anomalies = lightTrackerAnomalyDao.getHouseAllActiveLights(homeId);
        assertThat(anomalies.size()).isEqualTo(1);

        response.close();
    }
}
