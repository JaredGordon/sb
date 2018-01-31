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

package io.pivotal.ecosystem.servicebroker;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.servicebroker.model.*;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class InstanceService implements ServiceInstanceService {

    public InstanceService(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    private CatalogService catalogService;

    @Override
    public CreateServiceInstanceResponse createServiceInstance(CreateServiceInstanceRequest request) {
        log.info("req params: " + request.getParameters());
        log.info("registered service instance: " + request.getServiceInstanceId());

        JsonObject cluster = new JsonObject();
        JsonObject plan = new JsonObject();
        JsonObject elasticSearch = new JsonObject();
        JsonArray clusterTopology = new JsonArray();
        JsonObject topology = new JsonObject();

        cluster.addProperty("hdfskjs", "dfghndlkfn");
        elasticSearch.addProperty("dkfjgndfk", "sdofinsdlf");
        plan.add("fieruhf", elasticSearch);
        plan.addProperty("weiuf", "woiefnwoe");

        topology.addProperty("weifn", "sdoivfsod");
        clusterTopology.add(topology);
        plan.add("iwveuw", clusterTopology);
        cluster.add("lsdfjsld", plan);

        String s = new GsonBuilder().create().toJson(cluster);

        String s1 = JsonPath.parse(s).read("$.hdfskjs");

        log.info(s);

        log.info(s1);

//        return new GsonBuilder().create().toJson(cluster);

        CreateServiceInstanceResponse resp = new CreateServiceInstanceResponse();
        return resp;
    }

    @Override
    public GetLastServiceOperationResponse getLastOperation(GetLastServiceOperationRequest getLastServiceOperationRequest) {
        GetLastServiceOperationResponse resp = new GetLastServiceOperationResponse();
        resp.withOperationState(OperationState.SUCCEEDED);
        return resp;
    }

    @Override
    public DeleteServiceInstanceResponse deleteServiceInstance(DeleteServiceInstanceRequest request) {
        return new DeleteServiceInstanceResponse();
    }

    @Override
    public UpdateServiceInstanceResponse updateServiceInstance(UpdateServiceInstanceRequest request) {
        return new UpdateServiceInstanceResponse();

    }
}