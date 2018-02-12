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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.GetLastServiceOperationResponse;
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

    //was kibana requested by user?
    @JsonSerialize
    private boolean kibanaWanted = false;

    //has kibana been requested from ece?
    @JsonSerialize
    private boolean kibanaRequested = false;

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

    private void processParams(Map<String, Object> params) {
        if (params != null) {
            getClusterParams().putAll(EnumUtil.paramsToClusterConfigParams(params));
            getKibanaParams().putAll(EnumUtil.paramsToKibanaParams(params));
        }

        processClusterParams();
        processKibanaParams();

        if (getPlan_id().toLowerCase().contains(KibanaConfig.KIBANA)) {
            setKibanaWanted(true);
        }
    }

    public String getClusterId() {
        return getClusterParams().get(ClusterConfig.eceApiKeys.elasticsearch_cluster_id.name());
    }

    public String getClusterName() {
        return getClusterParams().get(ClusterConfig.eceApiKeys.cluster_name.name());
    }

    private void processClusterParams() {
        getClusterParams().putIfAbsent(ClusterConfig.eceApiKeys.cluster_name.name(), getService_instance_id());
        getClusterParams().putIfAbsent(ClusterConfig.eceApiKeys.zone_count.name(), ClusterConfig.DEFAULT_ZONE_COUNT);
        getClusterParams().putIfAbsent(ClusterConfig.eceApiKeys.elasticsearch_version.name(), ClusterConfig.DEFAULT_ELASTICSEARCH_VERSION);
        getClusterParams().putIfAbsent(ClusterConfig.eceApiKeys.memory_per_node.name(), ClusterConfig.DEFAULT_MEMORY_PER_NODE);
        getClusterParams().putIfAbsent(ClusterConfig.eceApiKeys.node_count_per_zone.name(), ClusterConfig.DEFAULT_NODE_COUNT_PER_ZONE);
        getClusterParams().putIfAbsent(ClusterConfig.eceApiKeys.topology_type.name(), ClusterConfig.DEFAULT_TOPOLOGY_TYPE);
    }

    String getCreateClusterBody() {
        JsonObject cluster = new JsonObject();
        JsonObject plan = new JsonObject();
        JsonObject elasticSearch = new JsonObject();
        JsonArray clusterTopology = new JsonArray();
        JsonObject topology = new JsonObject();

        cluster.addProperty(ClusterConfig.eceApiKeys.cluster_name.name(), getClusterParams().get(ClusterConfig.eceApiKeys.cluster_name.name()));
        elasticSearch.addProperty(ClusterConfig.eceApiKeys.version.name(), getClusterParams().get(ClusterConfig.eceApiKeys.elasticsearch_version.name()));
        plan.add(ClusterConfig.eceApiKeys.elasticsearch.name(), elasticSearch);
        plan.addProperty(ClusterConfig.eceApiKeys.zone_count.name(), Integer.valueOf(getClusterParams().get(ClusterConfig.eceApiKeys.zone_count.name())));

        topology.addProperty(ClusterConfig.eceApiKeys.topology_type.name(), getClusterParams().get(ClusterConfig.eceApiKeys.topology_type.name()));
        topology.addProperty(ClusterConfig.eceApiKeys.memory_per_node.name(), Integer.valueOf(getClusterParams().get(ClusterConfig.eceApiKeys.memory_per_node.name())));
        topology.addProperty(ClusterConfig.eceApiKeys.node_count_per_zone.name(), Integer.valueOf(getClusterParams().get(ClusterConfig.eceApiKeys.node_count_per_zone.name())));
        clusterTopology.add(topology);
        plan.add(ClusterConfig.eceApiKeys.cluster_topology.name(), clusterTopology);
        cluster.add(ClusterConfig.eceApiKeys.plan.name(), plan);

        return new GsonBuilder().create().toJson(cluster);
    }

    void processCreateClusterResponse(Object createClusterResponse, EceConfig eceConfig) {
        DocumentContext dc = JsonPath.parse(createClusterResponse);
        getClusterParams().put(ClusterConfig.eceApiKeys.elasticsearch_cluster_id.name(), dc.read("$." + ClusterConfig.eceApiKeys.elasticsearch_cluster_id.name()));

        getCredentials().put(ClusterConfig.credentialKeys.clusterId.name(), getClusterParams().get(ClusterConfig.eceApiKeys.elasticsearch_cluster_id.name()));
        getCredentials().put(ClusterConfig.credentialKeys.username.name(), dc.read("$." + ClusterConfig.eceApiKeys.credentials.name() + "." + ClusterConfig.eceApiKeys.username.name()));
        getCredentials().put(ClusterConfig.credentialKeys.password.name(), dc.read("$." + ClusterConfig.eceApiKeys.credentials.name() + "." + ClusterConfig.eceApiKeys.password.name()));

        getCredentials().put(ClusterConfig.credentialKeys.host.name(), eceConfig.getElasticsearchDomain());
        getCredentials().put(ClusterConfig.credentialKeys.port.name(), eceConfig.getElasticsearchPort());
        getCredentials().put(ClusterConfig.credentialKeys.uri.name(), "ece://" + getClusterParams().get(ClusterConfig.eceApiKeys.elasticsearch_cluster_id.name()) + "." + eceConfig.getElasticsearchDomain() + ":" + eceConfig.getElasticsearchPort());
        getCredentials().put(ClusterConfig.credentialKeys.eceEndpoint.name(), "https://" + getClusterParams().get(ClusterConfig.eceApiKeys.elasticsearch_cluster_id.name()) + "." + eceConfig.getElasticsearchDomain() + ":" + eceConfig.getElasticsearchPort());
    }

    private void processKibanaParams() {
        getKibanaParams().put(KibanaConfig.kibanaApiKeys.elasticsearch_cluster_id.name(), getClusterId());

        getKibanaParams().putIfAbsent(KibanaConfig.kibanaApiKeys.cluster_name.name(), getClusterName());
        getKibanaParams().putIfAbsent(KibanaConfig.kibanaApiKeys.zone_count.name(), KibanaConfig.DEFAULT_ZONE_COUNT);
        getKibanaParams().putIfAbsent(KibanaConfig.kibanaApiKeys.version.name(), KibanaConfig.DEFAULT_KIBANA_VERSION);
        getKibanaParams().putIfAbsent(KibanaConfig.kibanaApiKeys.memory_per_node.name(), KibanaConfig.DEFAULT_MEMORY_PER_NODE);
        getKibanaParams().putIfAbsent(KibanaConfig.kibanaApiKeys.node_count_per_zone.name(), KibanaConfig.DEFAULT_NODE_COUNT_PER_ZONE);
        getKibanaParams().putIfAbsent(KibanaConfig.kibanaApiKeys.cluster_topology.name(), KibanaConfig.DEFAULT_TOPOLOGY_TYPE);
    }

    String getCreateKibanaBody() {
        JsonObject cluster = new JsonObject();
        JsonObject plan = new JsonObject();
        JsonObject kibana = new JsonObject();
        JsonArray clusterTopology = new JsonArray();
        JsonObject topology = new JsonObject();

        cluster.addProperty(KibanaConfig.kibanaApiKeys.cluster_name.name(), getKibanaParams().get(KibanaConfig.kibanaApiKeys.cluster_name.name()));
        cluster.addProperty(KibanaConfig.kibanaApiKeys.elasticsearch_cluster_id.name(), getClusterParams().get(ClusterConfig.eceApiKeys.elasticsearch_cluster_id.name()));
        plan.addProperty(KibanaConfig.kibanaApiKeys.zone_count.name(), Integer.valueOf(getKibanaParams().get(KibanaConfig.kibanaApiKeys.zone_count.name())));

        topology.addProperty(KibanaConfig.kibanaApiKeys.memory_per_node.name(), Integer.valueOf(getKibanaParams().get(KibanaConfig.kibanaApiKeys.memory_per_node.name())));
        topology.addProperty(KibanaConfig.kibanaApiKeys.node_count_per_zone.name(), Integer.valueOf(getKibanaParams().get(KibanaConfig.kibanaApiKeys.node_count_per_zone.name())));
        clusterTopology.add(topology);
        plan.add(KibanaConfig.kibanaApiKeys.cluster_topology.name(), clusterTopology);
        kibana.addProperty(KibanaConfig.kibanaApiKeys.version.name(), KibanaConfig.DEFAULT_KIBANA_VERSION);
        plan.add(KibanaConfig.KIBANA, kibana);

        cluster.add(KibanaConfig.kibanaApiKeys.plan.name(), plan);

        return new GsonBuilder().create().toJson(cluster);
    }

    void processCreateKibanaResponse(Object createClusterResponse, EceConfig eceConfig) {
        String kibanaClusterId = JsonPath.parse(createClusterResponse).read("$." + KibanaConfig.kibanaApiKeys.kibana_cluster_id.name());
        getKibanaParams().put(KibanaConfig.kibanaApiKeys.kibana_cluster_id.name(), kibanaClusterId);
        getCredentials().put(ClusterConfig.credentialKeys.kibanaClusterId.name(), kibanaClusterId);
        getCredentials().put(ClusterConfig.credentialKeys.kibanaEndpoint.name(), "https://" + kibanaClusterId +
                "." + eceConfig.getElasticsearchDomain() + ":" + eceConfig.getElasticsearchPort());
        setKibanaRequested(true);
    }
}