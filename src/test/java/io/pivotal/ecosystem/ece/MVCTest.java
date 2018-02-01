/*
 * Copyright (C) 2016-Present Pivotal Software, Inc. All rights reserved.
 * <p>
 * This program and the accompanying materials are made available under
 * the terms of the under the Apache License, Version 2.0 (the "License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.pivotal.ecosystem.ece;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.servicebroker.controller.ServiceInstanceController;
import org.springframework.cloud.servicebroker.model.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.OperationState;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MVCTest {

    private static final String ID = "deleteme";

    private MockMvc mockMvc;

    @Autowired
    @InjectMocks
    private EceBroker eceBroker;

    @Autowired
    private EceRepo eceRepo;

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private ServiceInstanceRepository serviceInstanceRepository;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ServiceInstanceController(catalogService, eceBroker))
                .build();
    }

    @Test
    public void testAsyncLastOperation() throws Exception {
        ServiceInstance si = TestConfig.defaultsServiceInstance(ID);
        si.setLastOperation(new GetLastServiceOperationResponse().withOperationState(OperationState.IN_PROGRESS).withDescription("creating."));
        when(serviceInstanceRepository.findOne(ID)).thenReturn(si);
        when(eceRepo.getClusterInfo(any(String.class))).thenReturn(TestConfig.fromJson("clusterInfo.json"));
        this.mockMvc.perform(get("/v2/service_instances/" + ID + "/last_operation?service_id=" + TestConfig.SD_ID + "&plan_id=" + TestConfig.PLAN_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
//                .andDo(print());
    }

    public static String toJson(Object object) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(object);
    }
}
