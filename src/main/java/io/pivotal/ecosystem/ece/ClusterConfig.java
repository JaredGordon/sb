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
        clusterName,
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

    public static final String DEFAULT_ZONES_COUNT = "1";
    public static final String DEFAULT_MEMORY_PER_NODE = "1024";
    public static final String DEFAULT_NODE_COUNT_PER_ZONE = "3";
    public static final String DEFAULT_TOPOLOGY_TYPE = "default";
    public static final String DEFAULT_ELASTICSEARCH_VERSION = "5.4.1";

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
        initCreds(clusterId, parameters);
        initConfig(parameters);
    }

    ClusterConfig(@NonNull EceConfig eceConfig, @NonNull String clusterId, @NonNull Map<String, Object> parameters, boolean kibana) {
        this(eceConfig, clusterId, parameters);
        parameters.put(credentialKeys.enableKibana.name(), kibana);
    }

    private void initCreds(String clusterId, Map<String, Object> parameters) {
        EnumMap<credentialKeys, String> e = EnumUtil.paramsToCreds(parameters);
        e.put(credentialKeys.clusterId, clusterId);
        if (!e.containsKey(credentialKeys.clusterName)) {
            e.put(credentialKeys.clusterName, clusterId);
        }

        creds.putAll(e);
    }

    private void initConfig(Map<String, Object> parameters) {


        EnumMap<eceApiKeys, String> e = EnumUtil.paramsToConfig(parameters);
        e.put(eceApiKeys.zone_count, getValueOrLoadDefault(eceApiKeys.zone_count, parameters, DEFAULT_ZONES_COUNT));
        e.put(eceApiKeys.elasticsearch_version, getValueOrLoadDefault(eceApiKeys.elasticsearch_version, parameters, DEFAULT_ELASTICSEARCH_VERSION));
        e.put(eceApiKeys.memory_per_node, getValueOrLoadDefault(eceApiKeys.memory_per_node, parameters, DEFAULT_MEMORY_PER_NODE));
        e.put(eceApiKeys.node_count_per_zone, getValueOrLoadDefault(eceApiKeys.node_count_per_zone, parameters, DEFAULT_NODE_COUNT_PER_ZONE));
        e.put(eceApiKeys.topology_type, getValueOrLoadDefault(eceApiKeys.topology_type, parameters, DEFAULT_TOPOLOGY_TYPE));

        config.putAll(e);
    }

    public Map<String, Object> credsToParams() {
        Map<String, Object> m = new HashMap<>();
        for (credentialKeys key : credentialKeys.values()) {
            m.put(key.name(), creds.get(key));
        }

        return m;
    }

    //convenience
    String getClusterName() {
        return creds.get(credentialKeys.clusterName);
    }

    String getClusterId() {
        return creds.get(credentialKeys.clusterId);
    }

    private String getValueOrLoadDefault(eceApiKeys key, Map<String, Object> parameters, String defaultValue) {
        if (!parameters.containsKey(key.name())) {
            config.put(key, defaultValue);
        } else {
            config.put(key, parameters.get(key.name()).toString());
        }

        return config.get(key);
    }

    String getCreateClusterBody() {
        JsonObject cluster = new JsonObject();
        JsonObject plan = new JsonObject();
        JsonObject elasticSearch = new JsonObject();
        JsonArray clusterTopology = new JsonArray();
        JsonObject topology = new JsonObject();

        cluster.addProperty(eceApiKeys.cluster_name.name(), getClusterName());
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
        creds.put(credentialKeys.host, eceConfig.getElasticsearchDomain());
        creds.put(credentialKeys.port, eceConfig.getElasticsearchPort());
        creds.put(credentialKeys.uri, "ece://" + getClusterId() + "." + eceConfig.getElasticsearchDomain() + ":" + eceConfig.getElasticsearchPort());
        creds.put(credentialKeys.eceEndpoint, "https://" + getClusterId() + "." + eceConfig.getElasticsearchDomain() + ":" + eceConfig.getElasticsearchPort());
        creds.put(credentialKeys.kibanaEndpoint, "https://" + creds.get(credentialKeys.kibanaClusterId) + "." + eceConfig.getElasticsearchDomain() + ":" + eceConfig.getElasticsearchPort());

        return creds;
    }

    EnumMap<eceApiKeys, String> getConfig() {
        return config;
    }

    void extractCredentials(Object createClusterResponse) {
        DocumentContext dc = JsonPath.parse(createClusterResponse);
        creds.put(credentialKeys.clusterId, dc.read("$." + eceApiKeys.elasticsearch_cluster_id.name()));
        creds.put(credentialKeys.username, dc.read("$." + eceApiKeys.credentials.name() + "." + eceApiKeys.username.name()));
        creds.put(credentialKeys.password, dc.read("$." + eceApiKeys.credentials.name() + "." + eceApiKeys.password.name()));
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
}