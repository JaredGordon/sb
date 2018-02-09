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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;


/**
 * test will create and delete serviceInstances on a local redis installation. @Ignore tests unless you are doing unit
 * testing and have a redis environment handy.You will need to edit the application.properties file in
 * src/test/resources to add your ece environment data for this test to work.
 */

@RunWith(SpringRunner.class)
@SpringBootTest
@Ignore
public class RedisTest {

    @Autowired
    private ServiceInstanceRepository serviceInstanceRepository;

    @Before
    public void setUp() {
        if ((serviceInstanceRepository.findOne(TestConfig.SI_ID)) != null) {
            serviceInstanceRepository.delete(TestConfig.SI_ID);
        }
    }

    @Test
    public void testCreateFindAndDelete() {
        ServiceInstance instance = serviceInstanceRepository.findOne(TestConfig.SI_ID);
        assertNull(instance);

        instance = TestConfig.customServiceInstance(TestConfig.SI_ID);
        assertNotNull(instance);

        instance = serviceInstanceRepository.save(instance);
        assertNotNull(instance);
        assertEquals(TestConfig.SI_ID, instance.getService_instance_id());
        assertNotNull(instance.getClusterParams());
        assertTrue(instance.getClusterParams().size() > 0);

        instance = serviceInstanceRepository.findOne(TestConfig.SI_ID);
        assertNotNull(instance);
        assertNotNull(instance.getClusterParams());
        assertTrue(instance.getClusterParams().size() > 0);

        serviceInstanceRepository.delete(TestConfig.SI_ID);
    }
}