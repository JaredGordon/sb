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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.EnumMap;

import static io.pivotal.ecosystem.ece.ClusterConfig.credentialKeys;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfig.class)
public class ClusterConfigTest {

    @Autowired
    private EceConfig eceConfig;

    @Autowired
    private EnumUtil enumUtil;

    @Test
    public void testWithInstanceAndDefaults() throws Exception {
        ClusterConfig cc = new ClusterConfig(eceConfig, TestConfig.defaultsServiceInstance(TestConfig.SI_ID), enumUtil);
        assertNotNull(cc);
        cc.processCreateResponse(TestConfig.fromJson("createClusterResponse.json"));
        String id = cc.getCredentials().get(credentialKeys.clusterId);
        assertNotNull(id);
        assertFalse(TestConfig.SI_ID.equals(id));
        assertEquals(ClusterConfig.DEFAULT_MEMORY_PER_NODE, cc.getConfig().get(ClusterConfig.eceApiKeys.memory_per_node));
        assertEquals(ClusterConfig.DEFAULT_NODE_COUNT_PER_ZONE, cc.getConfig().get(ClusterConfig.eceApiKeys.node_count_per_zone));
        assertEquals(ClusterConfig.DEFAULT_TOPOLOGY_TYPE, cc.getConfig().get(ClusterConfig.eceApiKeys.topology_type));
        assertEquals(ClusterConfig.DEFAULT_ZONES_COUNT, cc.getConfig().get(ClusterConfig.eceApiKeys.zone_count));

        assertNotNull(cc.getCreateClusterBody());
    }

    @Test
    public void testCreateBody() throws IOException {
        String s1 = TestConfig.toJson(TestConfig.fromJson("createClusterRequestBody.json"));
        assertNotNull(s1);

        ClusterConfig cc1 = new ClusterConfig(eceConfig, TestConfig.customServiceInstance(TestConfig.SI_ID), enumUtil);
        String s2 = cc1.getCreateClusterBody();
        assertNotNull(s2);
        assertEquals(s1, s2);

        String s3 = TestConfig.toJson(TestConfig.fromJson("createClusterRequestBodyDefaults.json"));
        assertNotNull(s3);

        ClusterConfig cc2 = new ClusterConfig(eceConfig, TestConfig.defaultsServiceInstance(TestConfig.SI_ID), enumUtil);
        String s4 = cc2.getCreateClusterBody();
        assertNotNull(s4);
        assertEquals(s3, s4);
    }

    @Test
    public void testExtractCreds() throws IOException {
        Object o = TestConfig.fromJson("createClusterResponse.json");
        assertNotNull(o);

        ClusterConfig cc = new ClusterConfig(eceConfig, TestConfig.defaultsServiceInstance(TestConfig.SI_ID), enumUtil);
        cc.processCreateResponse(o);
        EnumMap<credentialKeys, String> m = cc.getCredentials();
        assertNotNull(m);
        assertEquals("d3228e4268d449e1be1a918e0eac49e3", m.get(credentialKeys.clusterId));
        assertEquals("aUser", m.get(credentialKeys.username));
        assertEquals("secret", m.get(credentialKeys.password));
    }
}
