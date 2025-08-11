package com.example.demo;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServicePort;
import io.kubernetes.client.openapi.models.V1ServiceSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.main.cloud-platform=kubernetes",
        "spring.cloud.kubernetes.client.namespace=default",
        "spring.cloud.kubernetes.loadbalancer.mode=service",
        "spring.cloud.loadbalancer.cache.ttl=3s",
        "debug=true"
})
class DemoApplicationTests {
    @Autowired
    LoadBalancerClientFactory clientFactory;

    @Autowired
    CoreV1Api coreV1Api;

    @Test
    void contextLoads() throws Exception {
        ReactiveLoadBalancer<ServiceInstance> loadBalancer = clientFactory.getInstance("mysvc");
        Response<ServiceInstance> response1 = Mono.from(loadBalancer.choose()).block();
        assertThat(response1.hasServer()).isFalse();

        createK8sSvc();
        Thread.sleep(Duration.ofSeconds(5)); //After the cache's ttl

        Response<ServiceInstance> response2 = Mono.from(loadBalancer.choose()).block();
        assertThat(response2.hasServer()).as("there shoud be one server").isTrue();
    }

    private void createK8sSvc() throws ApiException {
        V1Service body = new V1Service()
                .metadata(new V1ObjectMeta().namespace("default").name("mysvc"))
                .spec(new V1ServiceSpec().addPortsItem(new V1ServicePort().port(80)));
        coreV1Api.createNamespacedService("default", body, null, null, null, null);
    }

    @AfterEach
    void cleanup() throws ApiException {
        coreV1Api.deleteNamespacedService("mysvc", "default", null, null, null, null, null, null);
    }
}
