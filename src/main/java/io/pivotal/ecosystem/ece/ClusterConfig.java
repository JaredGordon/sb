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
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
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

    public static final String ELASTIC_SEARCH = "elasticsearch";
    public static final String CREDENTIALS = "credentials";

    static final String DEFAULT_ZONE_COUNT = "1";
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

    public void processParams(ServiceInstance instance) {
        loadValueOrDefault(instance, eceApiKeys.cluster_name, instance.getService_instance_id());
        loadValueOrDefault(instance, eceApiKeys.zone_count, DEFAULT_ZONE_COUNT);
        loadValueOrDefault(instance, eceApiKeys.elasticsearch_version, DEFAULT_ELASTICSEARCH_VERSION);
        loadValueOrDefault(instance, eceApiKeys.memory_per_node, DEFAULT_MEMORY_PER_NODE);
        loadValueOrDefault(instance, eceApiKeys.node_count_per_zone, DEFAULT_NODE_COUNT_PER_ZONE);
        loadValueOrDefault(instance, eceApiKeys.topology_type, DEFAULT_TOPOLOGY_TYPE);
    }

    String getCreateClusterBody(ServiceInstance instance) {
        JsonObject cluster = new JsonObject();
        JsonObject plan = new JsonObject();
        JsonObject elasticSearch = new JsonObject();
        JsonArray clusterTopology = new JsonArray();
        JsonObject topology = new JsonObject();

        cluster.addProperty(eceApiKeys.cluster_name.name(), instance.getClusterParams().get(eceApiKeys.cluster_name.name()));
        elasticSearch.addProperty(eceApiKeys.version.name(), instance.getClusterParams().get(eceApiKeys.elasticsearch_version.name()));
        plan.add(eceApiKeys.elasticsearch.name(), elasticSearch);
        plan.addProperty(eceApiKeys.zone_count.name(), Integer.valueOf(instance.getClusterParams().get(eceApiKeys.zone_count.name())));

        topology.addProperty(eceApiKeys.topology_type.name(), instance.getClusterParams().get(eceApiKeys.topology_type.name()));
        topology.addProperty(eceApiKeys.memory_per_node.name(), Integer.valueOf(instance.getClusterParams().get(eceApiKeys.memory_per_node.name())));
        topology.addProperty(eceApiKeys.node_count_per_zone.name(), Integer.valueOf(instance.getClusterParams().get(eceApiKeys.node_count_per_zone.name())));
        clusterTopology.add(topology);
        plan.add(eceApiKeys.cluster_topology.name(), clusterTopology);
        cluster.add(eceApiKeys.plan.name(), plan);

        return new GsonBuilder().create().toJson(cluster);
    }

    void processCreateResponse(Object createClusterResponse, ServiceInstance instance, EceConfig eceConfig) {
        DocumentContext dc = JsonPath.parse(createClusterResponse);
        instance.getClusterParams().put(eceApiKeys.elasticsearch_cluster_id.name(), dc.read("$." + eceApiKeys.elasticsearch_cluster_id.name()));

        instance.getCredentials().put(credentialKeys.clusterId.name(), instance.getClusterParams().get(eceApiKeys.elasticsearch_cluster_id.name()));
        instance.getCredentials().put(credentialKeys.username.name(), dc.read("$." + eceApiKeys.credentials.name() + "." + eceApiKeys.username.name()));
        instance.getCredentials().put(credentialKeys.password.name(), dc.read("$." + eceApiKeys.credentials.name() + "." + eceApiKeys.password.name()));

        instance.getCredentials().put(credentialKeys.host.name(), eceConfig.getElasticsearchDomain());
        instance.getCredentials().put(credentialKeys.port.name(), eceConfig.getElasticsearchPort());
        instance.getCredentials().put(credentialKeys.uri.name(), "ece://" + instance.getClusterParams().get(eceApiKeys.elasticsearch_cluster_id.name()) + "." + eceConfig.getElasticsearchDomain() + ":" + eceConfig.getElasticsearchPort());
        instance.getCredentials().put(credentialKeys.eceEndpoint.name(), "https://" + instance.getClusterParams().get(eceApiKeys.elasticsearch_cluster_id.name()) + "." + eceConfig.getElasticsearchDomain() + ":" + eceConfig.getElasticsearchPort());
    }

    private void loadValueOrDefault(ServiceInstance instance, eceApiKeys key, String defaultValue) {
        if (!instance.getClusterParams().containsKey(key.name())) {
            instance.getClusterParams().put(key.name(), defaultValue);
        }
    }
}