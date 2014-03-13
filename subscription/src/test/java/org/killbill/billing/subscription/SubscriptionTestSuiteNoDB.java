/*
 * Copyright 2010-2013 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.subscription;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.inject.Inject;

import org.killbill.billing.GuicyKillbillTestSuiteNoDB;
import org.killbill.billing.TestKillbillConfigSource;
import org.killbill.billing.account.api.AccountData;
import org.killbill.billing.api.TestApiListener;
import org.killbill.billing.catalog.api.Catalog;
import org.killbill.billing.catalog.api.CatalogService;
import org.killbill.billing.subscription.api.SubscriptionBaseInternalApi;
import org.killbill.billing.subscription.api.SubscriptionBaseService;
import org.killbill.billing.subscription.api.migration.SubscriptionBaseMigrationApi;
import org.killbill.billing.subscription.api.timeline.SubscriptionBaseTimelineApi;
import org.killbill.billing.subscription.api.transfer.SubscriptionBaseTransferApi;
import org.killbill.billing.subscription.api.user.SubscriptionBaseBundle;
import org.killbill.billing.subscription.api.user.TestSubscriptionHelper;
import org.killbill.billing.subscription.engine.dao.MockSubscriptionDaoMemory;
import org.killbill.billing.subscription.engine.dao.SubscriptionDao;
import org.killbill.billing.subscription.glue.TestDefaultSubscriptionModuleNoDB;
import org.killbill.billing.util.KillbillConfigSource;
import org.killbill.billing.util.config.SubscriptionConfig;
import org.killbill.billing.util.svcsapi.bus.BusService;
import org.killbill.clock.ClockMock;
import org.mockito.Mockito;
import org.skife.jdbi.v2.IDBI;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;

public class SubscriptionTestSuiteNoDB extends GuicyKillbillTestSuiteNoDB {

    protected static final Logger log = LoggerFactory.getLogger(SubscriptionTestSuiteNoDB.class);

    @Inject
    protected SubscriptionBaseService subscriptionBaseService;
    @Inject
    protected SubscriptionBaseInternalApi subscriptionInternalApi;
    @Inject
    protected SubscriptionBaseTransferApi transferApi;

    @Inject
    protected SubscriptionBaseMigrationApi migrationApi;
    @Inject
    protected SubscriptionBaseTimelineApi repairApi;

    @Inject
    protected CatalogService catalogService;
    @Inject
    protected SubscriptionConfig config;
    @Inject
    protected SubscriptionDao dao;
    @Inject
    protected ClockMock clock;
    @Inject
    protected BusService busService;

    @Inject
    protected IDBI idbi;

    @Inject
    protected TestSubscriptionHelper testUtil;
    @Inject
    protected TestApiListener testListener;

    @Inject
    protected SubscriptionTestInitializer subscriptionTestInitializer;

    protected Catalog catalog;
    protected AccountData accountData;
    protected SubscriptionBaseBundle bundle;

    @Override
    protected KillbillConfigSource getConfigSource() throws IOException, URISyntaxException {
        return new TestKillbillConfigSource("/subscription.properties");
    }

    @BeforeClass(groups = "fast")
    public void beforeClass() throws Exception {
        final Injector g = Guice.createInjector(Stage.PRODUCTION, new TestDefaultSubscriptionModuleNoDB(configSource));
        g.injectMembers(this);

        // For TestApiListener#isCompleted
        Mockito.doReturn(0L).when(idbi).withHandle(Mockito.<HandleCallback<Long>>any());
    }

    @BeforeMethod(groups = "fast")
    public void beforeMethod() throws Exception {

        // CLEANUP ALL DB TABLES OR IN MEMORY STRUCTURES
        ((MockSubscriptionDaoMemory) dao).reset();

        subscriptionTestInitializer.startTestFamework(testListener, clock, busService, subscriptionBaseService);

        this.catalog = subscriptionTestInitializer.initCatalog(catalogService);
        this.accountData = subscriptionTestInitializer.initAccountData();
        this.bundle = subscriptionTestInitializer.initBundle(subscriptionInternalApi, internalCallContext);
    }

    @AfterMethod(groups = "fast")
    public void afterMethod() throws Exception {
        subscriptionTestInitializer.stopTestFramework(testListener, busService, subscriptionBaseService);
    }

    protected void assertListenerStatus() {
        testListener.assertListenerStatus();
    }
}
