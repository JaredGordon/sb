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

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static io.pivotal.ecosystem.ece.TestConfig.CLUSTER_ID;
import static io.pivotal.ecosystem.ece.TestConfig.CLUSTER_NAME;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class EceClientTest {

    @Autowired
    private EceRepo eceRepo;

    @Autowired
    private EceClient eceClient;

    @Test
    public void testGetClusterStatus() throws Exception {
        when(eceRepo.getClusterInfo(any(String.class))).thenReturn(TestConfig.fromJson("clusterInfo.json"));
        ServiceInstance instance = TestConfig.defaultsServiceInstance(CLUSTER_NAME);
        instance.getParameters().put(ClusterConfig.credentialKeys.clusterId.name(), CLUSTER_ID);

        assertTrue(eceClient.isClusterStarted(instance));
        assertFalse(eceClient.isClusterStopped(instance));
    }

    @Test
    public void testGetKibanaEnabled() throws IOException {
        when(eceRepo.getClusterInfo(any(String.class))).thenReturn(TestConfig.fromJson("clusterInfo.json"));
        ServiceInstance instance = TestConfig.defaultsServiceInstance(CLUSTER_NAME);
        instance.getParameters().put(ClusterConfig.credentialKeys.clusterId.name(), CLUSTER_ID);
        instance.getParameters().put(ClusterConfig.credentialKeys.enableKibana.name(), true);
        instance.getParameters().put("kibanaClusterId", "12345");

        assertTrue(eceClient.isKibanaEnabled(instance));
    }

    @Test
    public void testClusterExists() throws IOException {
        when(eceRepo.getClustersInfo()).thenReturn(TestConfig.fromJson("clustersInfo.json"));
        ServiceInstance instance = TestConfig.defaultsServiceInstance("foo");
        assertFalse(eceClient.clusterExists(instance));

        instance = TestConfig.defaultsServiceInstance("admin-console-elasticsearch");
        assertTrue(eceClient.clusterExists(instance));
    }
}