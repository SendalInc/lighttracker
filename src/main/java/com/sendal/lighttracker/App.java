package com.sendal.lighttracker;

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
import java.util.Map;
import java.io.IOException;

import org.bson.types.ObjectId;

import io.dropwizard.client.JerseyClientBuilder;
import javax.ws.rs.client.Client;

import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientRequestContext;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
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
import com.sendal.lighttracker.resources.LightTrackerDevelopmentPhaseClientAPIs;
import com.sendal.lighttracker.resources.LightTrackerDevelopmentUIServer;
import com.sendal.lighttracker.db.LightTrackerSubscriptionRecord;
import com.sendal.lighttracker.db.dao.LightTrackerSubscriptionRecordDAO;
import com.sendal.lighttracker.db.LightTrackerAnomaly;
import com.sendal.lighttracker.db.dao.LightTrackerAnomalyDAO;
import com.sendal.lighttracker.TimerEngine;

import com.sendal.externalapicommon.security.ClientFilterSigner;
import com.sendal.externalapicommon.security.ApiSecretAuthenticatorDC;
import com.sendal.externalapicommon.db.ApiSecret;
import com.sendal.externalapicommon.security.APISecurityAuthFilter;
import com.sendal.externalapicommon.security.IDPrincipal;
import com.sendal.externalapicommon.security.Signer;

import java.util.logging.Level;
import java.util.logging.Handler;
import java.util.logging.ConsoleHandler;
import org.glassfish.jersey.logging.LoggingFeature;

public class App extends Application<LightTrackerConfiguration>
{
    private final Logger logger = LoggerFactory.getLogger(App.class);

    private static ApiSecret apiSecret;

    // static for use in testing
    public static ApiSecret getApiSecret() {
        return apiSecret;
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
        String mongoPath = c.getMongo().getUri();

        MongoClient mongoClient = new MongoClient(new MongoClientURI(mongoPath));
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

                    ApiSecret apiSecret = App.getApiSecret();

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

        // extra API logging for demo purposes
        java.util.logging.Logger clogger = java.util.logging.Logger.getLogger("LoggingFeature");
        Handler handlerObj = new ConsoleHandler();
        handlerObj.setLevel(Level.ALL);
        clogger.addHandler(handlerObj);
        clogger.setLevel(Level.ALL);
        clogger.setUseParentHandlers(false);

        client.register(new LoggingFeature(
            clogger,
            Level.ALL,
            LoggingFeature.Verbosity.PAYLOAD_ANY,
            8192)); 

        e.jersey().register(new org.glassfish.jersey.filter.LoggingFilter(java.util.logging.Logger.getLogger("InboundRequestResponse"), true));


        apiSecret = new ApiSecret();
        apiSecret.setApiKey(c.getSendalSoftwareService().getApiKey());
        apiSecret.setSecretKey(c.getSendalSoftwareService().getSecretKey());
                
        LightTrackerSubscriptionRecordDAO lightTrackerSubscriptionRecordDao = new LightTrackerSubscriptionRecordDAO(
                database);
        LightTrackerAnomalyDAO lightTrackerAnomalyDao = new LightTrackerAnomalyDAO(database);

        APISecurityAuthFilter apiSecurityAuthFilter = new APISecurityAuthFilter.Builder<IDPrincipal>()
                                            .setAuthenticator(new ApiSecretAuthenticatorDC(
                                            c.getSendalSoftwareService().getScs3pssId(),
                                            c.getSendalSoftwareService().getApiKey(),
                                            c.getSendalSoftwareService().getSecretKey()))
                                            .setRealm("Sendal LightTracker PSS")
                                            .buildAuthFilter();

        e.jersey().register(new AuthDynamicFeature(apiSecurityAuthFilter));

        e.jersey().register(new AuthValueFactoryProvider.Binder<>(IDPrincipal.class));
        e.jersey().register(new JsonProcessingExceptionMapper(true));

        e.jersey().register(new HealthResource());
        e.jersey().register(new LightTrackerSubscription(client, lightTrackerSubscriptionRecordDao, lightTrackerAnomalyDao,  c.getSendalSoftwareService().getScsUrl(), c.getSendalSoftwareService().getScs3pssId(),  e.getValidator()));
        e.jersey().register(new LightTrackerStates(client, lightTrackerSubscriptionRecordDao, lightTrackerAnomalyDao, c.getSendalSoftwareService().getScsUrl(), c.getSendalSoftwareService().getScs3pssId(),  e.getValidator()));
        
        LightTrackerIntegrationService lightTrackerIntegrationService = new LightTrackerIntegrationService(client, lightTrackerSubscriptionRecordDao, lightTrackerAnomalyDao, c.getSendalSoftwareService().getScsUrl(), c.getSendalSoftwareService().getScs3pssId(), e.getValidator());
        e.jersey().register(lightTrackerIntegrationService);
        
        switch(c.getLightTrackerDeploymentPhase()) {
            case 1:
                logger.warn("Running in type 1 development environment");
                e.jersey().register(new LightTrackerDevelopmentPhaseClientAPIs(lightTrackerIntegrationService));
                e.jersey().register(new LightTrackerDevelopmentUIServer());
                break;

            case 2:
                logger.warn("Running in type 2 development environment");
                e.jersey().register(new LightTrackerDevelopmentUIServer());
                break;

            case 3:
                logger.warn("Running in type 3 production environment");
                break;
        }
            
        e.lifecycle().manage(new TimerEngine(client, lightTrackerSubscriptionRecordDao, lightTrackerAnomalyDao, c));
    }
    
    public static void main( String[] args ) throws Exception {
        new App().run(args);
    }
}
