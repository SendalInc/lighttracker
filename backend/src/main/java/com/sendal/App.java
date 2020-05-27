package com.sendal.lighttracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;

import java.util.List;
import java.util.Collections;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.io.File;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.type.TypeReference;

import io.dropwizard.client.JerseyClientBuilder;
import javax.ws.rs.client.Client;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientRequestContext;

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

import com.sendal.lighttracker.resources.LightTrackerSubscription;
import com.sendal.lighttracker.resources.LightTrackerStates;
import com.sendal.lighttracker.resources.HealthResource;
import com.sendal.lighttracker.resources.LightTrackerIntegrationService;
import com.sendal.lighttracker.db.LightTrackerSubscriptionRecord;
import com.sendal.lighttracker.db.dao.LightTrackerSubscriptionRecordDAO;
import com.sendal.lighttracker.db.LightTrackerStateHistory;
import com.sendal.lighttracker.db.LightTrackerAnomoly;
import com.sendal.lighttracker.db.dao.LightTrackerAnomolyDAO;
import com.sendal.lighttracker.TimerEngine;

import com.sendal.externalapicommon.security.ClientFilterSigner;
import com.sendal.externalapicommon.security.ApiSecretAuthenticator;
import com.sendal.externalapicommon.db.dao.ApiSecretsDAO;
import com.sendal.externalapicommon.db.ApiSecret;
import com.sendal.externalapicommon.security.APISecurityAuthFilter;
import com.sendal.externalapicommon.security.IDPrincipal;
import com.sendal.externalapicommon.security.TimeUtils;
import com.sendal.externalapicommon.security.Signer;
import com.sendal.externalapicommon.security.HmacSignatureGenerator;
import com.sendal.externalapicommon.security.APISecurityConstants;


public class App extends Application<LightTrackerConfiguration>
{
    private static ApiSecretsDAO apiSecretsDAO;
    private final Logger logger = LoggerFactory.getLogger(App.class);

    // static for use in testing
    public static ApiSecretsDAO getApiSecretsDAO() {
        return apiSecretsDAO;
    }
        
    @Override
    public void initialize(Bootstrap<LightTrackerConfiguration> bootstrap) {
    }

    @Override
    public void run(LightTrackerConfiguration c, Environment e) throws Exception {
        logger.info("Running LightTracker app");

        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));

        // standard datastore
        MongoClient mongoClient = new MongoClient(new MongoClientURI(c.getMongo().getUri()));
        MongoDatabase database = mongoClient.getDatabase(c.getMongo().getDatabase())
                .withCodecRegistry(pojoCodecRegistry);

        ClientRequestFilter hmacRequestFilter = new ClientRequestFilter() {
            public void filter(ClientRequestContext ctx) throws IOException, IllegalStateException {
                // map the request to a secret.
                final Map<String, List<Object>> headers = ctx.getHeaders();

                // the Sendal3PSSId header is a way the client invocation can tell us what the server ID is
                // in this method - which let's us look up keys.  It's not needed by the remote side, and could
                // be removed, but we leave it in the outgoing request for now.
                if (headers.get("Sendal3PSSId") != null) {
                    ObjectId thirdPSSObjectId = new ObjectId((String) (headers.get("Sendal3PSSId").get(0)));

                    ApiSecret apiSecret = App.getApiSecretsDAO()
                            .getSecretForConsumer(thirdPSSObjectId);

                    ClientFilterSigner.signForContext(ctx, apiSecret);

                } else {
                    //throw new IllegalStateException("3PSS missing for signature");
                    // let this go since we may have non-auth usages in LT (e.g. ML runner)
                }
            }
        };

        final Client client = new JerseyClientBuilder(e).using(c.getJerseyClientConfiguration())
                                                              .build("LightTracker");
        
        client.register(hmacRequestFilter);

                
        apiSecretsDAO =  new ApiSecretsDAO(database);
        LightTrackerSubscriptionRecordDAO lightTrackerSubscriptionRecordDao = new LightTrackerSubscriptionRecordDAO(
                database);
        LightTrackerAnomolyDAO lightTrackerAnomolyDao = new LightTrackerAnomolyDAO(database);


        // re-use the signing infrastructure from Sendal.  This is a bit of a cheat in that we are looking into the same infrastructure in deployments.
        e.jersey().register(new AuthDynamicFeature(new APISecurityAuthFilter.Builder<IDPrincipal>()
                                                                    .setAuthenticator(new ApiSecretAuthenticator(apiSecretsDAO, null))
                                                                    .setRealm("Sendal 3rd Party Software Services")
                                                                    .buildAuthFilter()));

        e.jersey().register(new AuthValueFactoryProvider.Binder<>(IDPrincipal.class));
        e.jersey().register(new JsonProcessingExceptionMapper(true));

        e.jersey().register(new HealthResource());
        e.jersey().register(new LightTrackerSubscription(client, lightTrackerSubscriptionRecordDao, c.getSendalSoftwareService().getScsUrl(), c.getSendalSoftwareService().getScs3pssId(),  e.getValidator()));
        e.jersey().register(new LightTrackerStates(client, lightTrackerSubscriptionRecordDao, 
                lightTrackerAnomolyDao, c.getSendalSoftwareService().getScsUrl(), c.getSendalSoftwareService().getScs3pssId(),  e.getValidator()));
        e.jersey().register(new LightTrackerIntegrationService(client, lightTrackerSubscriptionRecordDao, c.getSendalSoftwareService().getScsUrl(), c.getSendalSoftwareService().getScs3pssId(), e.getValidator()));

        e.lifecycle().manage(new TimerEngine(client, lightTrackerSubscriptionRecordDao, lightTrackerAnomolyDao, c));
    }
    
    public static void main( String[] args ) throws Exception {
        new App().run(args);
    }
}
