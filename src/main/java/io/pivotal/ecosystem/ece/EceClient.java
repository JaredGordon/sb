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
import java.util.Map;

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

    void createCluster(ServiceInstance instance, boolean kibana) {
        ClusterConfig cc = new ClusterConfig(eceConfig, instance.getService_instance_id(), instance.getParameters(), kibana);
        log.info("creating cluster: " + cc.getClusterName());

        Object resp = eceRepo.createCluster(cc.getCreateClusterBody());
        cc.extractCredentials(resp);
        instance.getParameters().putAll(cc.credsToParams());
    }

    void shutdownCluster(ServiceInstance instance) {
        ClusterConfig cc = new ClusterConfig(eceConfig, instance.getService_instance_id(), instance.getParameters());
        log.info("stopping cluster: " + cc.getClusterName());

        eceRepo.shutdownCluster(cc.getClusterId());
    }

    void deleteCluster(ServiceInstance instance) {
        ClusterConfig cc = new ClusterConfig(eceConfig, instance.getService_instance_id(), instance.getParameters());
        log.info("deleting cluster: " + cc.getClusterName());

        eceRepo.deleteCluster(cc.getClusterId());
    }

    Map<String, Object> getClusterCredentials(ServiceInstance instance) {
        return new ClusterConfig(eceConfig, instance.getService_instance_id(), instance.getParameters()).credsToParams();
    }

    void bindToCluster(ServiceInstance instance, ServiceBinding binding) {
        //no-op for now
        ClusterConfig cc = new ClusterConfig(eceConfig, instance.getService_instance_id(), instance.getParameters());
        log.info("binding app: " + binding.getApp_guid() + " to cluster: " + cc.getClusterName());
    }

    void unbindFromCluster(ServiceInstance instance, ServiceBinding binding) {
        //no-op for now
        ClusterConfig cc = new ClusterConfig(eceConfig, instance.getService_instance_id(), instance.getParameters());
        log.info("unbinding app: " + binding.getApp_guid() + " from cluster: " + cc.getClusterName());
    }

    private void createKibana(ServiceInstance instance) {
        KibanaConfig kc = new KibanaConfig(new ClusterConfig(eceConfig, instance.getService_instance_id(), instance.getParameters()));
        log.info("creating kibana cluster: " + kc.getClusterName());

        Object resp = eceRepo.createKibana(kc.getCreateClusterBody());
        instance.getParameters().putAll(kc.extractCredentials(resp));
    }

    private String getClusterStatus(Object clusterInfo) {
        return JsonPath.parse(clusterInfo).read("$.status");
    }

    boolean clusterExists(ServiceInstance instance) {
        List<String> l = JsonPath.parse(eceRepo.getClustersInfo()).read("$..cluster_name");
        return l.contains(new ClusterConfig(eceConfig, instance.getService_instance_id(), instance.getParameters()).getClusterName());
    }

    boolean isClusterStarted(ServiceInstance instance) {
        return isClusterInState(instance, ClusterConfig.clusterState.started);
    }

    boolean isClusterStopped(ServiceInstance instance) {
        return isClusterInState(instance, ClusterConfig.clusterState.stopped);
    }

    private boolean isClusterInState(ServiceInstance instance, ClusterConfig.clusterState state) {
        ClusterConfig cc = new ClusterConfig(eceConfig, instance.getService_instance_id(), instance.getParameters());
        String status = getClusterStatus(eceRepo.getClusterInfo(cc.getClusterId()));
        return state.name().equalsIgnoreCase(status);
    }

    boolean isKibanaEnabled(ServiceInstance instance) {
        //do we even want kibana?
        if (!ClusterConfig.includesKibana(instance.getParameters())) {
            return false;
        }

        //if we have not yet started the enable process, do so now
        if (!KibanaConfig.wasKibanaRequested(instance.getParameters())) {
            createKibana(instance);
        }

        //if we've requested kibana, check to see if it's enabled yet
        return getKibanaEnabled(instance);
    }

    private boolean getKibanaEnabled(ServiceInstance instance) {
        List<Boolean> l = JsonPath.parse(eceRepo.getClusterInfo(ClusterConfig.getClusterId(instance.getParameters()))).read("$..associated_kibana_clusters[*].enabled");
        for (Boolean b : l) {
            if (!b) {
                return false;
            }
        }
        return true;
    }
}
