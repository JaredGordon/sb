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

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.JsonPath;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
class KibanaConfig {

    public static final String KIBANA = "kibana";

    public static final String DEFAULT_ZONE_COUNT = "1";
    public static final String DEFAULT_MEMORY_PER_NODE = "1024";
    public static final String DEFAULT_KIBANA_VERSION = "5.3.0";
    public static final String DEFAULT_NODE_COUNT_PER_ZONE = "3";
    public static final String DEFAULT_TOPOLOGY_TYPE = "default";

    public enum kibanaApiKeys {
        cluster_name,
        kibana_cluster_id,
        elasticsearch_cluster_id,
        memory_per_node,
        node_count_per_zone,
        cluster_topology,
        version,
        zone_count,
        plan
    }

    public void processParams(ServiceInstance instance) {
        instance.getKibanaParams().put(kibanaApiKeys.elasticsearch_cluster_id.name(), instance.getClusterId());

        loadValueOrDefault(instance, kibanaApiKeys.cluster_name, instance.getClusterName());
        loadValueOrDefault(instance, kibanaApiKeys.zone_count, DEFAULT_ZONE_COUNT);
        loadValueOrDefault(instance, kibanaApiKeys.version, DEFAULT_KIBANA_VERSION);
        loadValueOrDefault(instance, kibanaApiKeys.memory_per_node, DEFAULT_MEMORY_PER_NODE);
        loadValueOrDefault(instance, kibanaApiKeys.node_count_per_zone, DEFAULT_NODE_COUNT_PER_ZONE);
        loadValueOrDefault(instance, kibanaApiKeys.cluster_topology, DEFAULT_TOPOLOGY_TYPE);
    }

    String getCreateClusterBody(ServiceInstance instance) {
        JsonObject cluster = new JsonObject();
        JsonObject plan = new JsonObject();
        JsonObject kibana = new JsonObject();
        JsonArray clusterTopology = new JsonArray();
        JsonObject topology = new JsonObject();

        cluster.addProperty(kibanaApiKeys.cluster_name.name(), instance.getKibanaParams().get(kibanaApiKeys.cluster_name.name()));
        cluster.addProperty(kibanaApiKeys.elasticsearch_cluster_id.name(), instance.getClusterParams().get(ClusterConfig.eceApiKeys.elasticsearch_cluster_id.name()));
        plan.addProperty(kibanaApiKeys.zone_count.name(), Integer.valueOf(instance.getKibanaParams().get(kibanaApiKeys.zone_count.name())));

        topology.addProperty(kibanaApiKeys.memory_per_node.name(), Integer.valueOf(instance.getKibanaParams().get(kibanaApiKeys.memory_per_node.name())));
        topology.addProperty(kibanaApiKeys.node_count_per_zone.name(), Integer.valueOf(instance.getKibanaParams().get(kibanaApiKeys.node_count_per_zone.name())));
        clusterTopology.add(topology);
        plan.add(kibanaApiKeys.cluster_topology.name(), clusterTopology);
        kibana.addProperty(kibanaApiKeys.version.name(), DEFAULT_KIBANA_VERSION);
        plan.add(KIBANA, kibana);

        cluster.add(kibanaApiKeys.plan.name(), plan);

        return new GsonBuilder().create().toJson(cluster);
    }

    void processCreateResponse(Object createClusterResponse, ServiceInstance instance, EceConfig eceConfig) {
        String kibanaClusterId = JsonPath.parse(createClusterResponse).read("$." + kibanaApiKeys.kibana_cluster_id.name());
        instance.getKibanaParams().put(kibanaApiKeys.kibana_cluster_id.name(), kibanaClusterId);
        instance.getCredentials().put(ClusterConfig.credentialKeys.kibanaClusterId.name(), kibanaClusterId);
        instance.getCredentials().put(ClusterConfig.credentialKeys.kibanaEndpoint.name(), "https://" + kibanaClusterId +
                "." + eceConfig.getElasticsearchDomain() + ":" + eceConfig.getElasticsearchPort());
        instance.setKibanaRequested(true);
    }

    private void loadValueOrDefault(ServiceInstance instance, kibanaApiKeys key, String defaultValue) {
        if (!instance.getKibanaParams().containsKey(key.name())) {
            instance.getKibanaParams().put(key.name(), defaultValue);
        }
    }
}