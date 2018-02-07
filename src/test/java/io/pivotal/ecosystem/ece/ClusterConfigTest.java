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
import java.util.Map;

import static io.pivotal.ecosystem.ece.ClusterConfig.credentialKeys;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfig.class)
public class ClusterConfigTest {

    @Autowired
    private EceConfig eceConfig;

    @Test
    public void testWithInstanceAndDefaults() throws Exception {
        ServiceInstance instance = TestConfig.defaultsServiceInstance(TestConfig.SI_ID);
        ClusterConfig cc = new ClusterConfig(instance, eceConfig);
        cc.processCreateResponse(TestConfig.fromJson("createClusterResponse.json"));
        cc.configToParams(instance);

        @SuppressWarnings("unchecked")
        Map<String, Object> creds = (Map<String, Object>) instance.getParameters().get(ClusterConfig.CREDENTIALS);

        assertEquals("d3228e4268d449e1be1a918e0eac49e3", creds.get(credentialKeys.clusterId.name()));
        assertEquals("aUser", creds.get(credentialKeys.username.name()));
        assertEquals("secret", creds.get(credentialKeys.password.name()));

        assertNotNull(cc.getCreateClusterBody());
    }

    @Test
    public void testCreateBody() throws IOException {
        String s1 = TestConfig.toJson(TestConfig.fromJson("createClusterRequestBody.json"));
        assertNotNull(s1);

        ClusterConfig cc1 = new ClusterConfig(TestConfig.customServiceInstance(TestConfig.SI_ID), eceConfig);
        String s2 = cc1.getCreateClusterBody();
        assertNotNull(s2);
        assertEquals(s1, s2);

        String s3 = TestConfig.toJson(TestConfig.fromJson("createClusterRequestBodyDefaults.json"));
        assertNotNull(s3);

        ClusterConfig cc2 = new ClusterConfig(TestConfig.defaultsServiceInstance(TestConfig.SI_ID), eceConfig);
        String s4 = cc2.getCreateClusterBody();
        assertNotNull(s4);
        assertEquals(s3, s4);
    }
}
