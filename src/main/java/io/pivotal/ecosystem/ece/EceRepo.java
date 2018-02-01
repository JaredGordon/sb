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

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;

@Repository
@FeignClient(url = "${ECE_URL}", name = "ece")
interface EceRepo {

    @GetMapping(value = "/api/v1/clusters/elasticsearch")
    Object getClustersInfo();

    @GetMapping(value = "/api/v1/clusters/elasticsearch/{clusterId}")
    Object getClusterInfo(@PathVariable(value = "clusterId") String clusterId);

    @PostMapping(value = "/api/v1/clusters/elasticsearch", consumes = "application/json")
    Object createCluster(@RequestBody String body);

    @DeleteMapping(value = "/api/v1/clusters/elasticsearch/{clusterId}")
    Object deleteCluster(@PathVariable("clusterId") String clusterId);

    @PostMapping(value = "/api/v1/clusters/elasticsearch/{clusterId}/_shutdown")
    Object shutdownCluster(@PathVariable("clusterId") String clusterId);

    @PostMapping(value = "/api/v1/clusters/kibana", consumes = "application/json")
    Object createKibana(@RequestBody String body);
}