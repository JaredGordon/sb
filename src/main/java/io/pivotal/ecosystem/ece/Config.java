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

import feign.Feign;
import feign.auth.BasicAuthRequestInterceptor;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.config.java.AbstractCloudConfig;
import org.springframework.cloud.service.PooledServiceConnectorConfig;
import org.springframework.cloud.servicebroker.model.BrokerApiVersion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@EnableRedisRepositories
@Slf4j
@Profile("cloud")
public class Config extends AbstractCloudConfig {

    @Bean
    public BrokerApiVersion brokerApiVersion() {
        return new BrokerApiVersion();
    }

    @Bean
    public RedisConnectionFactory redisFactory() {
        PooledServiceConnectorConfig.PoolConfig poolConfig = new PooledServiceConnectorConfig.PoolConfig(5, 30, 3000);
        PooledServiceConnectorConfig redisConfig = new PooledServiceConnectorConfig(poolConfig);
        return connectionFactory().redisConnectionFactory(redisConfig);
    }

    @Bean
    public BasicAuthRequestInterceptor basicAuthRequestInterceptor() {
        return new BasicAuthRequestInterceptor(uid, pw);
    }

    @Bean
    public EceRepo eceRepo() {
        return Feign
                .builder().requestInterceptor(basicAuthRequestInterceptor())
                .encoder(new GsonEncoder())
                .decoder(new GsonDecoder())
                .target(EceRepo.class, "https://" + eceHost + ":" + ecePort + "/api/v1/");
    }

    @Bean
    public EceConfig eceConfig() {
        return new EceConfig(elasticsearchDomain, elasticsearchPort);
    }

    @Value("${ECE_HOST}")
    private String eceHost;

    @Value("${ECE_PORT}")
    private String ecePort;

    @Value("${ECE_ADMIN_UID}")
    private String uid;

    @Value("${ECE_ADMIN_PW}")
    private String pw;

    @Value("${ELASTICSEARCH_DOMAIN}")
    private String elasticsearchDomain;

    @Value("${ELASTICSEARCH_PORT}")
    private String elasticsearchPort;
}