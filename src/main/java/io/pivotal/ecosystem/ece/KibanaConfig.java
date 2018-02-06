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
import lombok.NonNull;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

class KibanaConfig {

    private static final String KIBANA = "kibana";

    private static final String DEFAULT_ZONES_COUNT = "1";
    private static final String DEFAULT_MEMORY_PER_NODE = "1024";
    private static final String DEFAULT_KIBANA_VERSION = "5.3.0";
    private static final String DEFAULT_NODE_COUNT_PER_ZONE = "3";
    private static final String DEFAULT_TOPOLOGY_TYPE = "default";

    private final EnumMap<kibanaApiKeys, String> config = new EnumMap<>(KibanaConfig.kibanaApiKeys.class);

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

    private ClusterConfig clusterConfig;

    KibanaConfig(@NonNull ClusterConfig clusterConfig, @NonNull Map<String, Object> parameters) {
        super();
        this.clusterConfig = clusterConfig;
        initConfig(parameters);
    }

    private void initConfig(Map<String, Object> parameters) {
        config.put(kibanaApiKeys.elasticsearch_cluster_id, clusterConfig.getClusterId());

        if (!parameters.containsKey(KIBANA)) {
            config.put(kibanaApiKeys.cluster_name, clusterConfig.getConfig().get(ClusterConfig.eceApiKeys.cluster_name));
            config.put(kibanaApiKeys.zone_count, DEFAULT_ZONES_COUNT);
            config.put(kibanaApiKeys.version, DEFAULT_KIBANA_VERSION);
            config.put(kibanaApiKeys.memory_per_node, DEFAULT_MEMORY_PER_NODE);
            config.put(kibanaApiKeys.node_count_per_zone, DEFAULT_NODE_COUNT_PER_ZONE);
            config.put(kibanaApiKeys.cluster_topology, DEFAULT_TOPOLOGY_TYPE);
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) parameters.get(KIBANA);

        loadValue(kibanaApiKeys.cluster_name, m, clusterConfig.getConfig().get(ClusterConfig.eceApiKeys.cluster_name));
        loadValue(kibanaApiKeys.zone_count, m, DEFAULT_ZONES_COUNT);
        loadValue(kibanaApiKeys.version, m, DEFAULT_KIBANA_VERSION);
        loadValue(kibanaApiKeys.memory_per_node, m, DEFAULT_MEMORY_PER_NODE);
        loadValue(kibanaApiKeys.node_count_per_zone, m, DEFAULT_NODE_COUNT_PER_ZONE);
        loadValue(kibanaApiKeys.cluster_topology, m, DEFAULT_TOPOLOGY_TYPE);
    }

    public EnumMap<kibanaApiKeys, String> getConfig() {
        return config;
    }

    String getCreateClusterBody() {
        JsonObject cluster = new JsonObject();
        JsonObject plan = new JsonObject();
        JsonObject kibana = new JsonObject();
        JsonArray clusterTopology = new JsonArray();
        JsonObject topology = new JsonObject();

        cluster.addProperty(kibanaApiKeys.cluster_name.name(), config.get(kibanaApiKeys.cluster_name));
        cluster.addProperty(kibanaApiKeys.elasticsearch_cluster_id.name(), config.get(kibanaApiKeys.elasticsearch_cluster_id));
        plan.addProperty(kibanaApiKeys.zone_count.name(), Integer.valueOf(clusterConfig.getConfig().get(ClusterConfig.eceApiKeys.zone_count)));

        topology.addProperty(kibanaApiKeys.memory_per_node.name(), Integer.valueOf(clusterConfig.getConfig().get(ClusterConfig.eceApiKeys.memory_per_node)));
        topology.addProperty(kibanaApiKeys.node_count_per_zone.name(), Integer.valueOf(clusterConfig.getConfig().get(ClusterConfig.eceApiKeys.node_count_per_zone)));
        clusterTopology.add(topology);
        plan.add(kibanaApiKeys.cluster_topology.name(), clusterTopology);
        kibana.addProperty(kibanaApiKeys.version.name(), DEFAULT_KIBANA_VERSION);
        plan.add(KIBANA, kibana);

        cluster.add(kibanaApiKeys.plan.name(), plan);

        return new GsonBuilder().create().toJson(cluster);
    }

    Map<String, Object> extractCredentials(Object createClusterResponse) {
        Map<String, Object> m = new HashMap<>();
        m.put(ClusterConfig.credentialKeys.kibanaClusterId.name(), JsonPath.parse(createClusterResponse).read("$." + kibanaApiKeys.kibana_cluster_id.name()));

        return m;
    }

    static boolean wasKibanaRequested(Map<String, Object> parameters) {
        return parameters.containsKey(ClusterConfig.credentialKeys.kibanaClusterId.name());
    }

    private String getValueOrDefault(kibanaApiKeys key, Map<String, Object> parameters, String defaultValue) {
        if (!parameters.containsKey(key.name())) {
            return defaultValue;
        }
        return parameters.get(key.name()).toString();
    }

    private void loadValue(kibanaApiKeys key, Map<String, Object> map, String defaultValue) {
        config.put(key, getValueOrDefault(key, map, defaultValue));
    }
}