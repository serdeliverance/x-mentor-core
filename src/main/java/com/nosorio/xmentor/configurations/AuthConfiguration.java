package com.nosorio.xmentor.configurations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.net.URI;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "keycloak")
@Data
public class AuthConfiguration {

    private final RestTemplate restTemplate;
    private final WebClient webClient;

    private String discoveryEndpoint;
    private String adminUrl;
    private String realmUrl;
    private String adminUsername;
    private String adminPassword;
    private ClientID clientId;
    private Secret clientSecret;
    private String realm;

    private URI logoutUrl;
    private URI tokenUrl;
    private URI usersUrl;
    private String publicKey;

    @PostConstruct
    public void init() {
        log.info("Getting metadata from Keycloak");
        Mono<String> metadataResponse = webClient.get().uri(this.discoveryEndpoint).retrieve().bodyToMono(String.class);
        metadataResponse.subscribe(data -> {
            try {
                OIDCProviderMetadata metadata = OIDCProviderMetadata.parse(data);
                this.logoutUrl = metadata.getEndSessionEndpointURI();
                this.tokenUrl = metadata.getTokenEndpointURI();
                this.usersUrl = URI.create(adminUrl + "/users");
            } catch (ParseException e) {
                throw new RuntimeException("Unable to parse provider metadata: " + this.discoveryEndpoint, e);
            }
        });
        log.info("Getting public key");
        Mono<String> publicKeyResponse = webClient.get().uri(this.realmUrl).retrieve().bodyToMono(String.class);
        publicKeyResponse.subscribe(publicKey -> {
            try {
                final ObjectNode node = new ObjectMapper().readValue(publicKey, ObjectNode.class);
                this.publicKey = node.get("public_key").asText();
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Unable to parse provider public key: " + this.realmUrl, e);
            }

        });
    }

}