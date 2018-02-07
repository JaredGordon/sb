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
        kibanaEndpoint,
        kibanaClusterId
    }

    private final EnumMap<credentialKeys, String> creds = new EnumMap<>(credentialKeys.class);
    private final EnumMap<eceApiKeys, String> config = new EnumMap<>(eceApiKeys.class);

    public static final String ELASTIC_SEARCH = "elasticsearch";
    public static final String CREDENTIALS = "credentials";

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
    private EnumUtil enumUtil;

    ClusterConfig(@NonNull EceConfig eceConfig, @NonNull ServiceInstance instance, @NonNull EnumUtil enumUtil) {
        super();
        this.eceConfig = eceConfig;
        this.enumUtil = enumUtil;
        initConfig(instance);
    }

    private void initConfig(ServiceInstance instance) {
        config.putAll(enumUtil.paramsToConfig(instance));

        loadValueOrDefault(eceApiKeys.cluster_name, instance.getService_instance_id());
        loadValueOrDefault(eceApiKeys.zone_count, DEFAULT_ZONES_COUNT);
        loadValueOrDefault(eceApiKeys.elasticsearch_version, DEFAULT_ELASTICSEARCH_VERSION);
        loadValueOrDefault(eceApiKeys.memory_per_node, DEFAULT_MEMORY_PER_NODE);
        loadValueOrDefault(eceApiKeys.node_count_per_zone, DEFAULT_NODE_COUNT_PER_ZONE);
        loadValueOrDefault(eceApiKeys.topology_type, DEFAULT_TOPOLOGY_TYPE);
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

    void processCreateResponse(Object createClusterResponse) {
        DocumentContext dc = JsonPath.parse(createClusterResponse);
        config.put(eceApiKeys.elasticsearch_cluster_id, dc.read("$." + eceApiKeys.elasticsearch_cluster_id.name()));

        creds.put(credentialKeys.clusterId, config.get(eceApiKeys.elasticsearch_cluster_id));
        creds.put(credentialKeys.username, dc.read("$." + eceApiKeys.credentials.name() + "." + eceApiKeys.username.name()));
        creds.put(credentialKeys.password, dc.read("$." + eceApiKeys.credentials.name() + "." + eceApiKeys.password.name()));

        creds.put(credentialKeys.host, eceConfig.getElasticsearchDomain());
        creds.put(credentialKeys.port, eceConfig.getElasticsearchPort());
        creds.put(credentialKeys.uri, "ece://" + config.get(eceApiKeys.elasticsearch_cluster_id) + "." + eceConfig.getElasticsearchDomain() + ":" + eceConfig.getElasticsearchPort());
        creds.put(credentialKeys.eceEndpoint, "https://" + config.get(eceApiKeys.elasticsearch_cluster_id) + "." + eceConfig.getElasticsearchDomain() + ":" + eceConfig.getElasticsearchPort());
        creds.put(credentialKeys.kibanaEndpoint, "https://" + creds.get(credentialKeys.kibanaClusterId) + "." + eceConfig.getElasticsearchDomain() + ":" + eceConfig.getElasticsearchPort());
    }

    static String getClusterId(ServiceInstance instance) {
        return instance.getParameters().containsKey(credentialKeys.clusterId.name()) ? instance.getParameters().get(credentialKeys.clusterId.name()).toString() : null;
    }

    private void loadValueOrDefault(eceApiKeys key, String defaultValue) {
        if (!config.containsKey(key)) {
            config.put(key, defaultValue);
        }
    }
}