package com.bookshop.order;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient catalogRestClient(LoadBalancerClient loadBalancerClient) {
        return RestClient.builder()
                .requestInterceptor(new ServiceDiscoveryInterceptor(loadBalancerClient))
                .baseUrl("http://catalog-service")
                .build();
    }

    static class ServiceDiscoveryInterceptor implements ClientHttpRequestInterceptor {
        private final LoadBalancerClient loadBalancerClient;

        ServiceDiscoveryInterceptor(LoadBalancerClient loadBalancerClient) {
            this.loadBalancerClient = loadBalancerClient;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                            ClientHttpRequestExecution execution) throws
                IOException {
            URI originalUri = request.getURI();
            String serviceName = originalUri.getHost();
            ServiceInstance instance = loadBalancerClient.choose(serviceName);
            if (instance != null) {
                URI newUri = URI.create(
                        originalUri.getScheme() + "://" +
                                instance.getHost() + ":" + instance.getPort() +
                                originalUri.getPath() +
                                (originalUri.getQuery() != null ? "?" + originalUri.getQuery() : "")
                );
                HttpRequest newRequest = new HttpRequest() {
                    @Override
                    public HttpMethod getMethod() {
                        return request.getMethod();
                    }

                    @Override
                    public URI getURI() {
                        return newUri;
                    }

                    @Override
                    public HttpHeaders getHeaders() {
                        return request.getHeaders();
                    }

                    @Override
                    public Map<String, Object> getAttributes() {
                        return request.getAttributes();
                    }
                };
                return execution.execute(newRequest, body);
            }
            return execution.execute(request, body);
        }
    }
}
