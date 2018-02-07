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

import feign.Body;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.springframework.stereotype.Repository;

@Repository
interface EceRepo {

    @RequestLine("GET /clusters/elasticsearch")
    Object getClustersInfo();

    @RequestLine("GET /clusters/elasticsearch/{clusterId}")
    Object getClusterInfo(@Param("clusterId") String clusterId);

    @RequestLine("POST /clusters/elasticsearch")
    @Headers("Content-Type: application/json")
    @Body("{body}")
    Object createCluster(@Param("body") Object body);

    @RequestLine("DELETE /clusters/elasticsearch/{clusterId}")
    Object deleteCluster(@Param("clusterId") String clusterId);

    @RequestLine("POST /clusters/elasticsearch/{clusterId}/_shutdown")
    Object shutdownCluster(@Param("clusterId") String clusterId);

    @RequestLine("POST /clusters/kibana")
    @Headers("Content-Type: application/json")
    @Body("{body}")
    Object createKibana(@Param("body") Object body);
}