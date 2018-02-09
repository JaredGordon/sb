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
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.GetLastServiceOperationResponse;
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

    private final EnumUtil enumUtil = new EnumUtil();
    private final ClusterConfig clusterConfig = new ClusterConfig();
    private final KibanaConfig kibanaConfig = new KibanaConfig();

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
    private final Map<String, String> clusterParams = new HashMap<>();

    @JsonSerialize
    private final Map<String, String> credentials = new HashMap<>();

    @JsonSerialize
    private final Map<String, String> kibanaParams = new HashMap<>();

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

        processParams(request.getParameters());
    }

    public ServiceInstance(UpdateServiceInstanceRequest request) {
        this();
        setService_instance_id(request.getServiceInstanceId());
        setPlan_id(request.getPlanId());
        setService_id(request.getServiceDefinitionId());

        processParams(request.getParameters());
    }

    private void processParams(Map<String, Object> params) {
        if (params == null) {
            return;
        }

        getClusterParams().putAll(enumUtil.paramsToClusterConfigParams(params));
        clusterConfig.processParams(this);

        getKibanaParams().putAll(enumUtil.paramsToKibanaParams(params));
        kibanaConfig.processParams(this);
    }

    public Object getCreateClusterBody() {
        return clusterConfig.getCreateClusterBody(this);
    }

    public Object getCreateKibanaBody() {
        return kibanaConfig.getCreateClusterBody(this);
    }

    public void processCreateResponse(Object response, EceConfig eceConfig) {
        clusterConfig.processCreateResponse(response, this, eceConfig);
    }

    public void processCreateKibanaResponse(Object response, EceConfig eceConfig) {
        kibanaConfig.processCreateResponse(response, this, eceConfig);
    }

    public String getClusterId() {
        return getClusterParams().get(ClusterConfig.eceApiKeys.elasticsearch_cluster_id.name());
    }

    public String getClusterName() {
        return getClusterParams().get(ClusterConfig.eceApiKeys.cluster_name.name());
    }

    public boolean isKibanaWanted() {
        return getKibanaParams().containsKey(KibanaConfig.kibanaApiKeys.kibana_cluster_id.name());
    }

    public boolean isKibanaRequested() {
        return isKibanaWanted() && getKibanaParams().get(KibanaConfig.kibanaApiKeys.kibana_cluster_id.name()).equals(KibanaConfig.REQUESTED);
    }
}