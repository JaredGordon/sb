/*
 * Copyright (C) 2016-Present Pivotal Software, Inc. All rights reserved.
 * <p>
 * This program and the accompanying materials are made available under
 * the terms of the under the Apache License, Version 2.0 (the "License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.pivotal.ecosystem.ece;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.servicebroker.exception.*;
import org.springframework.cloud.servicebroker.model.*;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EceBroker implements ServiceInstanceService, ServiceInstanceBindingService {

    private EceClient eceClient;
    private ServiceInstanceRepository serviceInstanceRepository;
    private ServiceBindingRepository serviceBindingRepository;

    public EceBroker(EceClient eceClient, ServiceInstanceRepository serviceInstanceRepository, ServiceBindingRepository serviceBindingRepository) {
        super();
        this.eceClient = eceClient;
        this.serviceInstanceRepository = serviceInstanceRepository;
        this.serviceBindingRepository = serviceBindingRepository;
    }

    @Override
    public CreateServiceInstanceResponse createServiceInstance(CreateServiceInstanceRequest request) {
        if (!request.isAsyncAccepted()) {
            throw new ServiceBrokerAsyncRequiredException("broker only supports async requests.");
        }

        if (serviceInstanceRepository.findOne(request.getServiceInstanceId()) != null) {
            throw new ServiceInstanceExistsException(request.getServiceInstanceId(), request.getServiceDefinitionId());
        }

        ServiceInstance instance = new ServiceInstance(request);
        boolean exists;
        try {
            exists = eceClient.clusterExists(instance);
        } catch (Throwable t) {
            log.error("error checking cluster", t);
            throw new ServiceBrokerException("Error checking cluster.", t);
        }

        if (exists) {
            throw new ServiceInstanceExistsException(request.getServiceInstanceId(), request.getServiceDefinitionId());
        }

        try {
            log.info("creating service instance: " + request.getServiceInstanceId() + " service definition: " + request.getServiceDefinitionId());
            eceClient.createCluster(instance);

            GetLastServiceOperationResponse lo = new GetLastServiceOperationResponse();
            lo.withOperationState(OperationState.IN_PROGRESS);
            lo.withDescription("creating....");
            instance.setLastOperation(lo);
            saveInstance(instance);

            log.info("registered service instance: " + request.getServiceInstanceId());

            return new CreateServiceInstanceResponse().withAsync(true).withOperation(OperationState.IN_PROGRESS.getValue());
        } catch (Throwable t) {
            log.error("error creating cluster", t);
            throw new ServiceBrokerException("Error creating instance.", t);
        }
    }

    private void saveInstance(ServiceInstance instance) {
        log.info("saving service instance to repo: " + instance.getService_instance_id());
        serviceInstanceRepository.save(instance);
    }

    @Override
    public GetLastServiceOperationResponse getLastOperation(GetLastServiceOperationRequest getLastServiceOperationRequest) {
        log.info("getting last operation for service: " + getLastServiceOperationRequest.getServiceInstanceId());

        ServiceInstance instance;
        try {
            instance = serviceInstanceRepository.findOne(getLastServiceOperationRequest.getServiceInstanceId());
        } catch (Throwable t) {
            log.error("error retrieving instance.", t);
            return new GetLastServiceOperationResponse().withOperationState(OperationState.FAILED);
        }

        if (instance == null) {
            throw new ServiceInstanceDoesNotExistException(getLastServiceOperationRequest.getServiceInstanceId());
        }

        GetLastServiceOperationResponse lo = instance.getLastOperation();
        log.info("previous last operation was: " + lo.getState());

        if (!OperationState.IN_PROGRESS.equals(lo.getState())) {
            return lo;
        }

        try {
            if (lo.isDeleteOperation()) {
                if (!eceClient.isClusterStopped(instance)) {
                    log.info("cluster: " + getLastServiceOperationRequest.getServiceInstanceId() + " delete in progress, waiting for cluster to stop.");
                    return lo;
                }

                log.info("deleting cluster: " + getLastServiceOperationRequest.getServiceInstanceId());
                eceClient.deleteCluster(instance);
                instance.getLastOperation().withOperationState(OperationState.SUCCEEDED);
                instance.getLastOperation().withDescription("deleted.");
                saveInstance(instance);
                return instance.getLastOperation();
            }

            // If cluster not started yet, we're still in process.
            if (!eceClient.isClusterStarted(instance)) {
                log.info("cluster: " + getLastServiceOperationRequest.getServiceInstanceId() + " create in progress, waiting for cluster to start.");
                return lo;
            }

            // So, cluster is started. If we don't want kibana, we are done.
            if (!instance.isKibanaWanted()) {
                log.info("cluster: " + getLastServiceOperationRequest.getServiceInstanceId() + " create completed, cluster started.");
                instance.getLastOperation().withOperationState(OperationState.SUCCEEDED);
                instance.getLastOperation().withDescription("created.");
                saveInstance(instance);
                return instance.getLastOperation();
            }

            // We must want kibana too, but if it is not ready we are still in process
            log.info("checking to see if kibana in involved...");
            if (!eceClient.isKibanaEnabled(instance)) {
                log.info("cluster: " + getLastServiceOperationRequest.getServiceInstanceId() + " started, kibana pending.");
                return lo;
            }

            // Kibana is ready too, we are done.
            log.info("cluster: " + getLastServiceOperationRequest.getServiceInstanceId() + " create completed, cluster started, kibana started");
            instance.getLastOperation().withOperationState(OperationState.SUCCEEDED);
            instance.getLastOperation().withDescription("created");
            saveInstance(instance);
            return instance.getLastOperation();
        } catch (Throwable t) {
            log.error("error updating last operation.", t);
            instance.getLastOperation().withOperationState(OperationState.FAILED);
            saveInstance(instance);
            return instance.getLastOperation();
        }
    }

    @Override
    public DeleteServiceInstanceResponse deleteServiceInstance(DeleteServiceInstanceRequest request) {
        if (!request.isAsyncAccepted()) {
            throw new ServiceBrokerAsyncRequiredException("broker only supports async requests.");
        }

        ServiceInstance serviceInstance = serviceInstanceRepository.findOne(request.getServiceInstanceId());
        if (serviceInstance == null) {
            throw new ServiceInstanceDoesNotExistException(request.getServiceInstanceId());
        }

        log.info("deleting service instance from repo: " + request.getServiceInstanceId());

        try {
            eceClient.shutdownCluster(serviceInstance);

            GetLastServiceOperationResponse lo = new GetLastServiceOperationResponse();
            lo.withOperationState(OperationState.IN_PROGRESS);
            lo.withDescription("deleting....");
            lo.withDeleteOperation(true);
            serviceInstance.setLastOperation(lo);
            saveInstance(serviceInstance);

            return new DeleteServiceInstanceResponse().withAsync(true).withOperation(OperationState.IN_PROGRESS.getValue());

        } catch (FeignException e) {
            if(e.status() == 404) {
                //cluster does not exist, go ahead and delete the instance
                log.warn("cluster: " + serviceInstance.getService_instance_id() + " not found. Deleting instance.");
                GetLastServiceOperationResponse lo = new GetLastServiceOperationResponse();
                lo.withOperationState(OperationState.SUCCEEDED);
                lo.withDescription("cluster not found, instance deleted.");
                lo.withDeleteOperation(true);
                serviceInstance.setLastOperation(lo);
                saveInstance(serviceInstance);
                return new DeleteServiceInstanceResponse().withAsync(true).withOperation(OperationState.SUCCEEDED.getValue());
            }

            log.error("error deleting cluster", e);
            GetLastServiceOperationResponse lo = new GetLastServiceOperationResponse();
            lo.withOperationState(OperationState.FAILED);
            lo.withDescription("delete failed.");
            lo.withDeleteOperation(true);
            serviceInstance.setLastOperation(lo);
            saveInstance(serviceInstance);
            return new DeleteServiceInstanceResponse().withAsync(true).withOperation(OperationState.FAILED.getValue());
        }
    }

    @Override
    public UpdateServiceInstanceResponse updateServiceInstance(UpdateServiceInstanceRequest request) {
        throw new ServiceInstanceUpdateNotSupportedException("Broker does not support updating");
    }

    @Override
    public CreateServiceInstanceBindingResponse createServiceInstanceBinding(CreateServiceInstanceBindingRequest request) {
        ServiceInstance instance = serviceInstanceRepository.findOne(request.getServiceInstanceId());

        if (instance == null) {
            throw new ServiceInstanceDoesNotExistException(request.getServiceInstanceId());
        }

        ServiceBinding binding = serviceBindingRepository.findOne(request.getBindingId());
        if (binding != null) {
            throw new ServiceInstanceBindingExistsException(request.getServiceInstanceId(), request.getBindingId());
        }

        log.info("creating binding for service instance: " + request.getServiceInstanceId() + " service: " + request.getServiceInstanceId());
        binding = new ServiceBinding(request);

        try {
            eceClient.bindToCluster(instance, binding);
            binding.getCredentials().putAll(instance.getCredentials());

            log.info("saving binding: " + request.getBindingId());
            serviceBindingRepository.save(binding);

            return new CreateServiceInstanceAppBindingResponse().withCredentials(binding.getCredentials());

        } catch (Throwable t) {
            throw new ServiceBrokerException("error creating binding.", t);
        }
    }

    @Override
    public void deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest request) {
        ServiceBinding binding = serviceBindingRepository.findOne(request.getBindingId());
        if (binding == null) {
            throw new ServiceInstanceBindingDoesNotExistException(request.getBindingId());
        }

        ServiceInstance instance = serviceInstanceRepository.findOne(request.getServiceInstanceId());
        if (instance == null) {
            throw new ServiceInstanceDoesNotExistException(request.getServiceInstanceId());
        }

        try {
            log.info("deleting binding: " + request.getBindingId() + " for service instance: " + request.getServiceInstanceId());
            eceClient.unbindFromCluster(instance, binding);
            serviceBindingRepository.delete(binding);
        } catch (Throwable t) {
            throw new ServiceBrokerException("error deleting binding", t);
        }
    }
}