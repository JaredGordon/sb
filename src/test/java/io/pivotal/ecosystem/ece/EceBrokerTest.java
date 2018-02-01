/*
 * Copyright (C) 2017-Present Pivotal Software, Inc. All rights reserved.
 * <p>
 * This program and the accompanying materials are made available under
 * the terms of the under the Apache License, Version 2.0 (the "License”);
 * you may not use this file except in compliance with the License.
 * <p>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * <p>
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.pivotal.ecosystem.ece;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.GetLastServiceOperationRequest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;


/**
 * test will create and delete a cluster on an ece. @Ignore tests unless you are doing integration testing and have a test
 * ece available. You will need to edit the application.properties file in src/test/resources to add your ece environment data
 * for this test to work.
 */

@RunWith(SpringRunner.class)
@SpringBootTest
@Ignore
public class EceBrokerTest {

    private static final String ID = "deleteme";

    @Autowired
    private EceBroker eceBroker;

    @Test
    public void testNoKibanaCluster() throws InterruptedException {
        testLifeCycle("oneNodeCluster");
    }

    @Test
    public void testKibanaCluster() throws InterruptedException {
        testLifeCycle("oneNodeClusterWithKibana");
    }

    private void testLifeCycle(String planId) throws InterruptedException {
        CreateServiceInstanceRequest creq = new CreateServiceInstanceRequest().withAsyncAccepted(true).withServiceInstanceId(TestConfig.SI_ID);
        assertNotNull(eceBroker.createServiceInstance(creq));

        GetLastServiceOperationRequest lreq = new GetLastServiceOperationRequest(TestConfig.SI_ID);
        assertNotNull(eceBroker.getLastOperation(lreq));

        DeleteServiceInstanceRequest dreq = new DeleteServiceInstanceRequest(TestConfig.SI_ID, TestConfig.SD_ID, TestConfig.PLAN_ID, null, true);
        assertNotNull(eceBroker.deleteServiceInstance(dreq));

        assertNotNull(eceBroker.getLastOperation(lreq));
    }
}