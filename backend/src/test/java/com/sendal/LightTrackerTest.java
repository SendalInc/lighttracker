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
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.TimeZone;

import com.sendal.common.StateReport;
import com.sendal.common.StateUpdater;
import com.sendal.common.StateValue;
import com.sendal.common.StateRegistration;
import com.sendal.common.coredb.DBDevice;
import com.sendal.common.StateIdentifier;

import com.sendal.lighttracker.App;
import com.sendal.lighttracker.LightTrackerConfiguration;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBList;
import com.mongodb.DBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.Mongo;
import com.mongodb.WriteResult;
import com.mongodb.WriteConcern;

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

import com.sendal.externalapicommon.db.AccessPermission;

import com.sendal.externalapicommon.security.TimeUtils;
import com.sendal.externalapicommon.security.HmacSignatureGenerator;
import com.sendal.externalapicommon.security.APISecurityConstants;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.io.IOException;
import java.util.Collections;

import com.sendal.lighttracker.db.LightTrackerStateHistory;


public class LightTrackerTest {
    
  }
