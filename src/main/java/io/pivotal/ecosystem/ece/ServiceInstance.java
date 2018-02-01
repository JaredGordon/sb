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

//    @JsonSerialize
//    @JsonProperty("id")
//    @Id
//    private String id;

    @JsonSerialize
//    @JsonProperty("service_instance_id")
    @Id
    private String service_instance_id;

    @JsonSerialize
//    @JsonProperty("organization_guid")
    private String organization_guid;

    @JsonSerialize
//    @JsonProperty("plan_id")
    private String plan_id;

    @JsonSerialize
//    @JsonProperty("service_id")
//    @Id
    private String service_id;

    @JsonSerialize
//    @JsonProperty("space_guid")
    private String space_guid;

    @JsonSerialize
//    @JsonProperty("parameters")
    private final Map<String, Object> parameters = new HashMap<>();

    @JsonSerialize
//    @JsonProperty("lastOperation")
    private GetLastServiceOperationResponse lastOperation;

    @JsonSerialize
//    @JsonProperty("accepts_incomplete")
    private boolean accepts_incomplete = false;

//    @JsonSerialize
//    @JsonProperty("deleted")
//    private boolean deleted = false;

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

//        log.info("processing parameters...");
        if (request.getParameters() != null) {
            getParameters().putAll(request.getParameters());
        }
//        log.info("processed parameters: ", request.getParameters());
    }

    public ServiceInstance(UpdateServiceInstanceRequest request) {
        this();
        setService_instance_id(request.getServiceInstanceId());
        setPlan_id(request.getPlanId());
        setService_id(request.getServiceDefinitionId());
        if (request.getParameters() != null) {
            getParameters().putAll(request.getParameters());
        }
    }

//    public void addParameter(@NonNull String key, @NonNull Object value) {
//        this.parameters.put(key, value);
//    }

//    public Object getParameter(@NonNull String key) {
//        return this.parameters.get(key);
//    }

//    public GetLastServiceOperationResponse getLastOperation() {
//        return this.lastOperation;
//    }

//    public void setLastOperation(GetLastServiceOperationResponse lastOperation) {
//        this.lastOperation = lastOperation;
//    }

//    public CreateServiceInstanceResponse getCreateResponse() {
//        CreateServiceInstanceResponse resp = new CreateServiceInstanceResponse();
//        resp.withAsync(true);
//        return resp;
//    }
//
//    public DeleteServiceInstanceResponse getDeleteResponse() {
//        DeleteServiceInstanceResponse resp = new DeleteServiceInstanceResponse();
//        resp.withAsync(true);
//        return resp;
//    }
//
//    public UpdateServiceInstanceResponse getUpdateResponse() {
//        UpdateServiceInstanceResponse resp = new UpdateServiceInstanceResponse();
//        resp.withAsync(true);
//        return resp;
//    }

    private boolean isInState(@NonNull OperationState state) {
//        lastOperationSanity();

        return getLastOperation().getState().equals(state);
    }

//    private boolean isOperation(@NonNull String operation) {
//        lastOperationSanity();
//
//        return getLastOperation().getOperation().equals(operation);
//    }

    public boolean isInProgress() {
        return isInState(OperationState.IN_PROGRESS);
    }

    public boolean isFailed() {
        return isInState(OperationState.FAILED);
    }

    public boolean isSuccessful() {
        return isInState(OperationState.SUCCEEDED);
    }

//    public boolean isCreate() {
//        return isOperation(LastOperation.CREATE);
//    }
//
//    public boolean isDelete() {
//        return isOperation(LastOperation.DELETE);
//    }

//    private void lastOperationSanity() {
//        if (getLastOperation() == null || getLastOperation().getState() == null || getLastOperation().getOperation() == null) {
//            throw new ServiceBrokerException("instance has no last operation.");
//        }
//    }
}