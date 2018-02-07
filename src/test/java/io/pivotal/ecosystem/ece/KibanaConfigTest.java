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

    @Autowired
    private EnumUtil enumUtil;

    @Test
    public void testWithInstanceAndDefaults() {
        ServiceInstance instance = TestConfig.defaultsServiceInstance(TestConfig.SI_ID);
        ClusterConfig cc = new ClusterConfig(eceConfig, instance, enumUtil);
        KibanaConfig kc = new KibanaConfig(cc, instance, enumUtil);

        assertEquals(cc.getConfig().get(ClusterConfig.eceApiKeys.memory_per_node), kc.getConfig().get(KibanaConfig.kibanaApiKeys.memory_per_node));
        assertEquals(cc.getConfig().get(ClusterConfig.eceApiKeys.elasticsearch_cluster_id), kc.getConfig().get(KibanaConfig.kibanaApiKeys.elasticsearch_cluster_id));
        assertEquals(cc.getConfig().get(ClusterConfig.eceApiKeys.node_count_per_zone), kc.getConfig().get(KibanaConfig.kibanaApiKeys.node_count_per_zone));
        assertEquals(cc.getConfig().get(ClusterConfig.eceApiKeys.zone_count), kc.getConfig().get(KibanaConfig.kibanaApiKeys.zone_count));

        assertNotNull(cc.getCreateClusterBody());
    }

    @Test
    public void testCreateBody() throws IOException {
        String s1 = TestConfig.toJson(TestConfig.fromJson("createKibanaRequestBody.json"));
        assertNotNull(s1);

        ServiceInstance instance = TestConfig.defaultsServiceInstance(TestConfig.SI_ID);

        ClusterConfig cc = new ClusterConfig(eceConfig, instance, enumUtil);
        cc.getConfig().put(ClusterConfig.eceApiKeys.elasticsearch_cluster_id, TestConfig.CLUSTER_ID);
        KibanaConfig kc = new KibanaConfig(cc, instance, enumUtil);
        String s2 = kc.getCreateClusterBody();
        assertNotNull(s2);
        assertEquals(s1, s2);
    }

    @Test
    public void testExtractCreds() throws IOException {
        Object o = TestConfig.fromJson("createKibanaResponse.json");
        assertNotNull(o);

        ServiceInstance instance = TestConfig.defaultsServiceInstance(TestConfig.SI_ID);

        ClusterConfig cc = new ClusterConfig(eceConfig, instance, enumUtil);
        KibanaConfig kc = new KibanaConfig(cc, instance, enumUtil);

        Map<String, Object> m2 = kc.extractCredentials(o);
        assertNotNull(m2);
        assertEquals("aKibanaClusterId", m2.get(ClusterConfig.credentialKeys.kibanaClusterId.name()));
    }
}
