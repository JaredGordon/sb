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

    public static final String KIBANA = "kibana";

    public static final String DEFAULT_ZONE_COUNT = "1";
    public static final String DEFAULT_MEMORY_PER_NODE = "1024";
    public static final String DEFAULT_KIBANA_VERSION = "5.3.0";
    public static final String DEFAULT_NODE_COUNT_PER_ZONE = "3";
    public static final String DEFAULT_TOPOLOGY_TYPE = "default";

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

    private final EnumUtil enumUtil = new EnumUtil();

    private EceConfig eceConfig;

    KibanaConfig(@NonNull ServiceInstance instance, EceConfig eceConfig) {
        super();
        this.eceConfig = eceConfig;
        initConfig(instance);
    }

    private void initConfig(ServiceInstance instance) {
        config.putAll(enumUtil.paramsToKibanaConfig(instance));

        ClusterConfig cc = new ClusterConfig(instance, eceConfig);
        config.put(kibanaApiKeys.elasticsearch_cluster_id, cc.getClusterId());

        loadValueOrDefault(kibanaApiKeys.cluster_name, cc.getClusterName());
        loadValueOrDefault(kibanaApiKeys.zone_count, DEFAULT_ZONE_COUNT);
        loadValueOrDefault(kibanaApiKeys.version, DEFAULT_KIBANA_VERSION);
        loadValueOrDefault(kibanaApiKeys.memory_per_node, DEFAULT_MEMORY_PER_NODE);
        loadValueOrDefault(kibanaApiKeys.node_count_per_zone, DEFAULT_NODE_COUNT_PER_ZONE);
        loadValueOrDefault(kibanaApiKeys.cluster_topology, DEFAULT_TOPOLOGY_TYPE);
    }

    public String getClusterName() {
        return config.get(kibanaApiKeys.cluster_name);
    }

    String getCreateClusterBody() {
        JsonObject cluster = new JsonObject();
        JsonObject plan = new JsonObject();
        JsonObject kibana = new JsonObject();
        JsonArray clusterTopology = new JsonArray();
        JsonObject topology = new JsonObject();

        cluster.addProperty(kibanaApiKeys.cluster_name.name(), config.get(kibanaApiKeys.cluster_name));
        cluster.addProperty(kibanaApiKeys.elasticsearch_cluster_id.name(), config.get(kibanaApiKeys.elasticsearch_cluster_id));
        plan.addProperty(kibanaApiKeys.zone_count.name(), Integer.valueOf(config.get(kibanaApiKeys.zone_count)));

        topology.addProperty(kibanaApiKeys.memory_per_node.name(), Integer.valueOf(config.get(kibanaApiKeys.memory_per_node)));
        topology.addProperty(kibanaApiKeys.node_count_per_zone.name(), Integer.valueOf(config.get(kibanaApiKeys.node_count_per_zone)));
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

    @SuppressWarnings("unchecked")
    static boolean wasKibanaRequested(ServiceInstance instance) {
        if (!instance.getParameters().containsKey(KIBANA)) {
            return false;
        }

        return ((Map<String, Object>) instance.getParameters().get(KIBANA)).containsKey(ClusterConfig.credentialKeys.kibanaClusterId.name());
    }

    static boolean includesKibana(ServiceInstance instance) {
        return instance.getParameters().containsKey(KIBANA);
    }

    private void loadValueOrDefault(kibanaApiKeys key, String defaultValue) {
        if (!config.containsKey(key)) {
            config.put(key, defaultValue);
        }
    }

    public void configToParams(ServiceInstance instance) {
        enumUtil.enumsToParams(config, KibanaConfig.KIBANA, instance);
    }
}