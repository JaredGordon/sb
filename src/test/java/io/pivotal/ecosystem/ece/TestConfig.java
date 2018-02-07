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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Configuration
class TestConfig {

    static final String SI_ID = "deleteme";
    static final String SD_ID = "ece";
    static final String PLAN_ID = "oneNodeCluster";

    private static final String ORG_GUID = "anOrgGuid";
    private static final String SPACE_GUID = "aSpaceGuid";

    static final String CLUSTER_ID = "d3228e4268d449e1be1a918e0eac49e3";
    static final String CLUSTER_NAME = "my-cluster";

    @MockBean
    public ServiceInstanceRepository serviceInstanceRepository;

    @MockBean
    public ServiceBindingRepository serviceBindingRepository;

    @MockBean
    EceRepo eceRepo;

    @Bean
    public EnumUtil enumUtil() {
        return new EnumUtil();
    }

    @Bean
    public CatalogService catalogService() {
        return new CatalogService();
    }

    private static CreateServiceInstanceRequest createServiceInstanceRequestDefaults(String id) {
        CreateServiceInstanceRequest req = new CreateServiceInstanceRequest(SD_ID, PLAN_ID, ORG_GUID, SPACE_GUID, getDefaultsParameters());
        req.withServiceInstanceId(id);
        req.withAsyncAccepted(true);
        return req;
    }

    private static CreateServiceInstanceRequest createServiceInstanceRequestCustom(String id) {
        CreateServiceInstanceRequest req = new CreateServiceInstanceRequest(SD_ID, PLAN_ID, ORG_GUID, SPACE_GUID, getCustomParameters());
        req.withServiceInstanceId(id);
        req.withAsyncAccepted(true);
        return req;
    }

    static ServiceInstance defaultsServiceInstance(String id) {
        return new ServiceInstance(createServiceInstanceRequestDefaults(id));
    }

    static ServiceInstance customServiceInstance(String id) {
        return new ServiceInstance(createServiceInstanceRequestCustom(id));
    }

    static Map<String, Object> getDefaultsParameters() {
        return new HashMap<>();
    }

    static String toJson(Object o) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writer().writeValueAsString(o);
    }

    static File getFile(String name) {
        ClassLoader classLoader = TestConfig.class.getClassLoader();
        return new File(classLoader.getResource(name).getFile());
    }

    @SuppressWarnings("unchecked")
    static Map<Object, Object> fromJson(String fileName) throws IOException {
        return (Map<Object, Object>) new ObjectMapper().readValue(getFile(fileName), HashMap.class);
    }

    @Bean
    public EceConfig eceConfig() {
        return new EceConfig("domain", "1234");
    }

    static Map<String, Object> defaultsServiceInstance() {
        return new HashMap<>();
    }

    private static Map<String, Object> getCustomParameters() {
        Map<String, Object> m = new HashMap<>();
        m.put(ClusterConfig.eceApiKeys.elasticsearch_version.name(), "1.2.3");
        m.put(ClusterConfig.eceApiKeys.cluster_name.name(), "my-cluster");
        Map<String, Object> ec = new HashMap<>();
        ec.put(ClusterConfig.ELASTIC_SEARCH, m);
        return ec;
    }
}