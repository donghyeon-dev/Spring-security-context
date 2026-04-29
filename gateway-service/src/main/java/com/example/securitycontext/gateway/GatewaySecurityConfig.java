package com.example.securitycontext.gateway;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GatewaySecurityProperties.class)
public class GatewaySecurityConfig {

    @Bean
    public AuthenticationGlobalFilter authenticationGlobalFilter(GatewaySecurityProperties props) {
        return new AuthenticationGlobalFilter(props);
    }
}
