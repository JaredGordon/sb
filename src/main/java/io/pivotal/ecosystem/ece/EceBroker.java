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

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.servicebroker.exception.*;
import org.springframework.cloud.servicebroker.model.*;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.stereotype.Service;

import java.util.Map;

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

        if (eceClient.clusterExists(instance)) {
            throw new ServiceInstanceExistsException(request.getServiceInstanceId(), request.getServiceDefinitionId());
        }

        log.info("creating service instance: " + request.getServiceInstanceId() + " service definition: " + request.getServiceDefinitionId());
        try {
            eceClient.createCluster(instance, isKibana(instance));

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

    private boolean isKibana(ServiceInstance instance) {
        return instance.getPlan_id().toLowerCase().contains(ClusterConfig.credentialKeys.enableKibana.name());
    }

    private ServiceInstance saveInstance(ServiceInstance instance) {
        log.info("saving service instance to repo: " + instance.getService_instance_id());
        serviceInstanceRepository.save(instance);
        return instance;
    }

    @Override
    public GetLastServiceOperationResponse getLastOperation(GetLastServiceOperationRequest getLastServiceOperationRequest) {
        //todo implement this once we have a service to test against. should check on status, update instance, and return new last op.
        GetLastServiceOperationResponse resp = new GetLastServiceOperationResponse();
        resp.withOperationState(OperationState.SUCCEEDED);
        return resp;
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
            serviceInstance.setLastOperation(lo);
            saveInstance(serviceInstance);

            return new DeleteServiceInstanceResponse().withAsync(true).withOperation(OperationState.IN_PROGRESS.getValue());

        } catch (Throwable t) {
            log.error("error deleting cluster", t);
            throw new ServiceBrokerException("error deleting cluster", t);
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

            Map<String, Object> creds = eceClient.getClusterCredentials(instance);
            binding.getCredentials().putAll(creds);

            log.info("saving binding: " + request.getBindingId());
            serviceBindingRepository.save(binding);

            return new CreateServiceInstanceAppBindingResponse().withCredentials(creds);

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