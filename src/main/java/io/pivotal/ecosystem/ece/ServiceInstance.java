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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.OperationState;
import org.springframework.cloud.servicebroker.model.UpdateServiceInstanceRequest;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@RedisHash("instances")
@Slf4j
public class ServiceInstance implements Serializable {

    public static final String DELETE_REQUEST_ID = "DELETE_REQUEST_ID";

    public static final long serialVersionUID = 1L;

    @JsonSerialize
    @Id
    private String service_instance_id;

    @JsonSerialize
    private String organization_guid;

    @JsonSerialize
    private String plan_id;

    @JsonSerialize
    private String service_id;

    @JsonSerialize
    private String space_guid;

    @JsonSerialize
    private final Map<String, Object> parameters = new HashMap<>();

    @JsonSerialize
    private GetLastServiceOperationResponse lastOperation;

    @JsonSerialize
    private boolean accepts_incomplete = false;

    public ServiceInstance() {
        super();
    }

    //TODO deal with stuff in response bodies
    public ServiceInstance(CreateServiceInstanceRequest request) {
        this();
        setService_instance_id(request.getServiceInstanceId());
        setOrganization_guid(request.getOrganizationGuid());
        setPlan_id(request.getPlanId());
        setService_id(request.getServiceDefinitionId());
        setSpace_guid(request.getSpaceGuid());
        processParameters(request.getParameters());
    }

    public ServiceInstance(UpdateServiceInstanceRequest request) {
        this();
        setService_instance_id(request.getServiceInstanceId());
        setPlan_id(request.getPlanId());
        setService_id(request.getServiceDefinitionId());
        processParameters(request.getParameters());
    }

    private void processParameters(Map<String, Object> m) {
        log.debug("processing parameters...");
        if (m != null) {
            this.parameters.putAll(m);
        }
        log.debug("processed parameters: ", m);
    }

    private boolean isInState(@NonNull OperationState state) {
        return getLastOperation().getState().equals(state);
    }

    public boolean isInProgress() {
        return isInState(OperationState.IN_PROGRESS);
    }

    public boolean isFailed() {
        return isInState(OperationState.FAILED);
    }

    public boolean isSuccessful() {
        return isInState(OperationState.SUCCEEDED);
    }

}