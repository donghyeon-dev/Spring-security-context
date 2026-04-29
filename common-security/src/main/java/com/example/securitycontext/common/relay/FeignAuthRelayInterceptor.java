package com.example.securitycontext.common.relay;

import com.example.securitycontext.common.passport.PassportProperties;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class FeignAuthRelayInterceptor implements RequestInterceptor {

    private final AuthRelayProperties relayProperties;
    private final PassportProperties passportProperties;

    public FeignAuthRelayInterceptor(AuthRelayProperties relayProperties,
                                     PassportProperties passportProperties) {
        this.relayProperties = relayProperties;
        this.passportProperties = passportProperties;
    }

    @Override
    public void apply(RequestTemplate template) {
        HttpServletRequest current = currentRequest();
        if (current == null) return;

        if ("passport".equalsIgnoreCase(relayProperties.getMode())) {
            String passport = current.getHeader(passportProperties.getHeaderName());
            if (passport != null) {
                template.header(passportProperties.getHeaderName(), passport);
            }
        } else {
            String authorization = current.getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null) {
                template.header(HttpHeaders.AUTHORIZATION, authorization);
            }
        }
        String requestId = current.getHeader("X-Request-Id");
        if (requestId != null) {
            template.header("X-Request-Id", requestId);
        }
    }

    private HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            return sra.getRequest();
        }
        return null;
    }
}
