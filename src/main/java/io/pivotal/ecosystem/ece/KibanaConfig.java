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

abstract class KibanaConfig {

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
}