package com.example.securitycontext.common.relay;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class AuthRelayProperties {

    /**
     * jwt | passport. Selects which header to relay on outbound calls and which inbound filter is active.
     */
    private String mode = "jwt";

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
