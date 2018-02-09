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
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfig.class)
@Slf4j
public class KibanaConfigTest {

    @Autowired
    private EceConfig eceConfig;

    @Test
    public void testWithInstanceAndDefaults() {
        ServiceInstance instance = TestConfig.defaultsServiceInstance(TestConfig.SI_ID);

        assertEquals(instance.getClusterParams().get(ClusterConfig.eceApiKeys.memory_per_node.name()), ClusterConfig.DEFAULT_MEMORY_PER_NODE);
        assertEquals(instance.getClusterParams().get(ClusterConfig.eceApiKeys.elasticsearch_cluster_id.name()), TestConfig.CLUSTER_ID);
        assertEquals(instance.getClusterParams().get(ClusterConfig.eceApiKeys.node_count_per_zone.name()), ClusterConfig.DEFAULT_NODE_COUNT_PER_ZONE);
        assertEquals(instance.getClusterParams().get(ClusterConfig.eceApiKeys.zone_count.name()), ClusterConfig.DEFAULT_ZONE_COUNT);

        assertEquals(instance.getKibanaParams().get(KibanaConfig.kibanaApiKeys.memory_per_node.name()), KibanaConfig.DEFAULT_MEMORY_PER_NODE);
        assertEquals(instance.getKibanaParams().get(KibanaConfig.kibanaApiKeys.elasticsearch_cluster_id.name()), TestConfig.CLUSTER_ID);
        assertEquals(instance.getKibanaParams().get(KibanaConfig.kibanaApiKeys.node_count_per_zone.name()), KibanaConfig.DEFAULT_NODE_COUNT_PER_ZONE);
        assertEquals(instance.getKibanaParams().get(KibanaConfig.kibanaApiKeys.zone_count.name()), KibanaConfig.DEFAULT_ZONE_COUNT);

        assertNotNull(instance.getCreateClusterBody());
    }

    @Test
    public void testCreateBody() throws IOException {
        String s1 = TestConfig.toJson(TestConfig.fromJson("createKibanaRequestBody.json"));
        assertNotNull(s1);

        ServiceInstance instance = TestConfig.defaultsServiceInstance(TestConfig.SI_ID);

        String s2 = instance.getCreateKibanaBody().toString();
        assertNotNull(s2);
        assertEquals(s1, s2);
    }

    @Test
    public void testExtractCreds() throws IOException {
        Object o = TestConfig.fromJson("createKibanaResponse.json");
        assertNotNull(o);

        ServiceInstance instance = TestConfig.defaultsServiceInstance(TestConfig.SI_ID);
        instance.processCreateKibanaResponse(o, eceConfig);

        Map<String, String> m2 = instance.getCredentials();
        assertNotNull(m2);
        assertEquals("aKibanaClusterId", m2.get(ClusterConfig.credentialKeys.kibanaClusterId.name()));
    }
}
