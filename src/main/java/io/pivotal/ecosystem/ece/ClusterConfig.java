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
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.NonNull;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class ClusterConfig {

    public enum clusterState {
        started,
        stopped
    }

    public enum credentialKeys {
        clusterId,
        username,
        password,
        host,
        port,
        uri,
        eceEndpoint,
        enableKibana,
        kibanaEndpoint,
        kibanaClusterId
    }

    private final EnumMap<credentialKeys, String> creds = new EnumMap<>(credentialKeys.class);
    private final EnumMap<eceApiKeys, String> config = new EnumMap<>(eceApiKeys.class);

    public static final String ELASTIC_SEARCH = "elasticsearch";

    static final String DEFAULT_ZONES_COUNT = "1";
    static final String DEFAULT_MEMORY_PER_NODE = "1024";
    static final String DEFAULT_NODE_COUNT_PER_ZONE = "3";
    static final String DEFAULT_TOPOLOGY_TYPE = "default";
    private static final String DEFAULT_ELASTICSEARCH_VERSION = "5.4.1";

    public enum eceApiKeys {
        cluster_name,
        elasticsearch_cluster_id,
        elasticsearch,
        elasticsearch_version,
        zone_count,
        topology_type,
        memory_per_node,
        node_count_per_zone,
        cluster_topology,
        plan,
        version,
        credentials,
        username,
        password
    }

    private EceConfig eceConfig;

    ClusterConfig(@NonNull EceConfig eceConfig, @NonNull String clusterId, @NonNull Map<String, Object> parameters) {
        super();
        this.eceConfig = eceConfig;
        initConfig(clusterId, parameters);
    }

    ClusterConfig(@NonNull EceConfig eceConfig, @NonNull String clusterId, @NonNull Map<String, Object> parameters, boolean kibana) {
        this(eceConfig, clusterId, parameters);
        parameters.put(credentialKeys.enableKibana.name(), kibana);
    }

    private void initConfig(String clusterId, Map<String, Object> parameters) {

        config.put(eceApiKeys.elasticsearch_cluster_id, clusterId);

        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) parameters.get(ELASTIC_SEARCH);

        loadValueOrDefault(eceApiKeys.zone_count, m, DEFAULT_ZONES_COUNT);
        loadValueOrDefault(eceApiKeys.elasticsearch_version, m, DEFAULT_ELASTICSEARCH_VERSION);
        loadValueOrDefault(eceApiKeys.memory_per_node, m, DEFAULT_MEMORY_PER_NODE);
        loadValueOrDefault(eceApiKeys.node_count_per_zone, m, DEFAULT_NODE_COUNT_PER_ZONE);
        loadValueOrDefault(eceApiKeys.topology_type, m, DEFAULT_TOPOLOGY_TYPE);
        loadValueOrDefault(eceApiKeys.cluster_name, m, clusterId);
    }

    public Map<String, Object> credsToParams() {
        Map<String, Object> m = new HashMap<>();
        for (credentialKeys key : credentialKeys.values()) {
            m.put(key.name(), creds.get(key));
        }

        return m;
    }

    String getClusterName() {
        return config.get(eceApiKeys.cluster_name);
    }

    String getClusterId() {
        return config.get(eceApiKeys.elasticsearch_cluster_id);
    }

    String getCreateClusterBody() {
        JsonObject cluster = new JsonObject();
        JsonObject plan = new JsonObject();
        JsonObject elasticSearch = new JsonObject();
        JsonArray clusterTopology = new JsonArray();
        JsonObject topology = new JsonObject();

        cluster.addProperty(eceApiKeys.cluster_name.name(), config.get(eceApiKeys.cluster_name));
        elasticSearch.addProperty(eceApiKeys.version.name(), config.get(eceApiKeys.elasticsearch_version));
        plan.add(eceApiKeys.elasticsearch.name(), elasticSearch);
        plan.addProperty(eceApiKeys.zone_count.name(), Integer.valueOf(config.get(eceApiKeys.zone_count)));

        topology.addProperty(eceApiKeys.topology_type.name(), config.get(eceApiKeys.topology_type));
        topology.addProperty(eceApiKeys.memory_per_node.name(), Integer.valueOf(config.get(eceApiKeys.memory_per_node)));
        topology.addProperty(eceApiKeys.node_count_per_zone.name(), Integer.valueOf(config.get(eceApiKeys.node_count_per_zone)));
        clusterTopology.add(topology);
        plan.add(eceApiKeys.cluster_topology.name(), clusterTopology);
        cluster.add(eceApiKeys.plan.name(), plan);

        return new GsonBuilder().create().toJson(cluster);
    }

    EnumMap<credentialKeys, String> getCredentials() {
        return creds;
    }

    EnumMap<eceApiKeys, String> getConfig() {
        return config;
    }

    void loadCredentials(Object createClusterResponse) {
        DocumentContext dc = JsonPath.parse(createClusterResponse);
        creds.put(credentialKeys.clusterId, dc.read("$." + eceApiKeys.elasticsearch_cluster_id.name()));
        creds.put(credentialKeys.username, dc.read("$." + eceApiKeys.credentials.name() + "." + eceApiKeys.username.name()));
        creds.put(credentialKeys.password, dc.read("$." + eceApiKeys.credentials.name() + "." + eceApiKeys.password.name()));

        creds.put(credentialKeys.host, eceConfig.getElasticsearchDomain());
        creds.put(credentialKeys.port, eceConfig.getElasticsearchPort());
        creds.put(credentialKeys.uri, "ece://" + config.get(eceApiKeys.elasticsearch_cluster_id) + "." + eceConfig.getElasticsearchDomain() + ":" + eceConfig.getElasticsearchPort());
        creds.put(credentialKeys.eceEndpoint, "https://" + config.get(eceApiKeys.elasticsearch_cluster_id) + "." + eceConfig.getElasticsearchDomain() + ":" + eceConfig.getElasticsearchPort());
        creds.put(credentialKeys.kibanaEndpoint, "https://" + creds.get(credentialKeys.kibanaClusterId) + "." + eceConfig.getElasticsearchDomain() + ":" + eceConfig.getElasticsearchPort());
    }

    static String getClusterId(Map<String, Object> parameters) {
        return parameters.containsKey(credentialKeys.clusterId.name()) ? parameters.get(credentialKeys.clusterId.name()).toString() : null;
    }

    static boolean includesKibana(Map<String, Object> parameters) {
        Object o = parameters.get(credentialKeys.enableKibana.name());
        if (o == null) {
            return false;
        }
        return Boolean.valueOf(o.toString());
    }

    private void loadValueOrDefault(eceApiKeys key, Map<String, Object> parameters, String defaultValue) {
        if (parameters == null || !parameters.containsKey(key.name())) {
            config.put(key, defaultValue);
        } else {
            config.put(key, parameters.get(key.name()).toString());
        }
    }
}