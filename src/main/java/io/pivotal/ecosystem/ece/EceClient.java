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

import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
class EceClient {

    private EceRepo eceRepo;
    private EceConfig eceConfig;

    public EceClient(EceConfig eceConfig, EceRepo eceRepo) {
        super();
        this.eceConfig = eceConfig;
        this.eceRepo = eceRepo;
    }

    void createCluster(ServiceInstance instance) {
        log.info("creating cluster: " + instance.getService_instance_id());

        Object resp = eceRepo.createCluster(instance.getCreateClusterBody());
        instance.processCreateResponse(resp, eceConfig);
    }

    void shutdownCluster(ServiceInstance instance) {
        log.info("stopping cluster: " + instance.getClusterId());
        eceRepo.shutdownCluster(instance.getClusterId());
    }

    void deleteCluster(ServiceInstance instance) {
        log.info("deleting cluster: " + instance.getClusterId());
        eceRepo.deleteCluster(instance.getClusterId());
    }

    void bindToCluster(ServiceInstance instance, ServiceBinding binding) {
        //no-op for now
        log.info("binding app: " + binding.getApp_guid() + " to cluster: " + instance.getClusterId());
    }

    void unbindFromCluster(ServiceInstance instance, ServiceBinding binding) {
        //no-op for now
        log.info("unbinding app: " + binding.getApp_guid() + " from cluster: " + instance.getClusterId());
    }

    private void createKibana(ServiceInstance instance) {
        log.info("creating kibana cluster for instance: " + instance.getService_instance_id());

        Object resp = eceRepo.createKibana(instance.getCreateKibanaBody());
        instance.processCreateKibanaResponse(resp, eceConfig);
    }

    private String getClusterStatus(Object clusterInfo) {
        return JsonPath.parse(clusterInfo).read("$.status");
    }

    boolean clusterExists(ServiceInstance instance) {
        List<String> l = JsonPath.parse(eceRepo.getClustersInfo()).read("$..cluster_name");
        return l.contains(instance.getClusterName());
    }

    boolean isClusterStarted(ServiceInstance instance) {
        return isClusterInState(instance, ClusterConfig.clusterState.started);
    }

    boolean isClusterStopped(ServiceInstance instance) {
        return isClusterInState(instance, ClusterConfig.clusterState.stopped);
    }

    private boolean isClusterInState(ServiceInstance instance, ClusterConfig.clusterState state) {
        log.info("checking status on clusterId: " + instance.getClusterId());
        String status = getClusterStatus(eceRepo.getClusterInfo(instance.getClusterId()));
        return state.name().equalsIgnoreCase(status);
    }

    boolean isKibanaEnabled(ServiceInstance instance) {
        //do we even want kibana?
        if (!instance.isKibanaWanted()) {
            return false;
        }

        //if we have not yet started the enable process, do so now
        if (! instance.isKibanaRequested()) {
            createKibana(instance);
        }

        //if we've requested kibana, check to see if it's enabled yet
        return getKibanaEnabled(instance);
    }

    private boolean getKibanaEnabled(ServiceInstance instance) {
        List<Boolean> l = JsonPath.parse(eceRepo.getClusterInfo(instance.getClusterId())).read("$..associated_kibana_clusters[*].enabled");
        for (Boolean b : l) {
            if (!b) {
                return false;
            }
        }
        return true;
    }
}
