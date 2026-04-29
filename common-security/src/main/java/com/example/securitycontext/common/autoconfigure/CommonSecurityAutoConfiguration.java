package com.example.securitycontext.common.autoconfigure;

import com.example.securitycontext.common.context.MdcContextFilter;
import com.example.securitycontext.common.jwt.JwtAuthenticationFilter;
import com.example.securitycontext.common.jwt.JwtProperties;
import com.example.securitycontext.common.jwt.JwtTokenProvider;
import com.example.securitycontext.common.passport.PassportAuthenticationFilter;
import com.example.securitycontext.common.passport.PassportIssuer;
import com.example.securitycontext.common.passport.PassportProperties;
import com.example.securitycontext.common.passport.PassportVerifier;
import com.example.securitycontext.common.relay.AuthRelayProperties;
import com.example.securitycontext.common.relay.FeignAuthRelayInterceptor;
import com.example.securitycontext.common.tracing.TracingSpanEnricher;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@AutoConfiguration
@EnableConfigurationProperties({
        JwtProperties.class,
        PassportProperties.class,
        AuthRelayProperties.class
})
public class CommonSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtTokenProvider jwtTokenProvider(JwtProperties props) {
        return new JwtTokenProvider(props);
    }

    @Bean
    @ConditionalOnMissingBean
    public PassportIssuer passportIssuer(PassportProperties props) {
        return new PassportIssuer(props);
    }

    @Bean
    @ConditionalOnMissingBean
    public PassportVerifier passportVerifier(PassportProperties props) {
        return new PassportVerifier(props);
    }

    @Bean
    @ConditionalOnMissingBean
    public TracingSpanEnricher tracingSpanEnricher(ObjectProvider<Tracer> tracerProvider) {
        return new TracingSpanEnricher(tracerProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.security", name = "mode", havingValue = "jwt", matchIfMissing = true)
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider provider,
                                                           TracingSpanEnricher enricher) {
        return new JwtAuthenticationFilter(provider, enricher);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.security", name = "mode", havingValue = "passport")
    public PassportAuthenticationFilter passportAuthenticationFilter(PassportVerifier verifier,
                                                                     PassportProperties props,
                                                                     TracingSpanEnricher enricher) {
        return new PassportAuthenticationFilter(verifier, props, enricher);
    }

    @Bean
    public MdcContextFilter mdcContextFilter() {
        return new MdcContextFilter();
    }

    @Bean
    public FilterRegistrationBean<MdcContextFilter> mdcContextFilterRegistration(MdcContextFilter filter) {
        FilterRegistrationBean<MdcContextFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setOrder(Ordered.LOWEST_PRECEDENCE - 100);
        reg.setEnabled(true);
        return reg;
    }

    @Bean
    public FeignAuthRelayInterceptor feignAuthRelayInterceptor(AuthRelayProperties relay,
                                                               PassportProperties passport) {
        return new FeignAuthRelayInterceptor(relay, passport);
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityFilterChain commonSecurityFilterChain(HttpSecurity http,
                                                         ObjectProvider<JwtAuthenticationFilter> jwt,
                                                         ObjectProvider<PassportAuthenticationFilter> passport)
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reg -> reg
                        .requestMatchers("/auth/**", "/actuator/**", "/error").permitAll()
                        .anyRequest().authenticated()
                );
        Filter inboundFilter = jwt.getIfAvailable();
        if (inboundFilter == null) inboundFilter = passport.getIfAvailable();
        if (inboundFilter != null) {
            http.addFilterBefore(inboundFilter, UsernamePasswordAuthenticationFilter.class);
        }
        return http.build();
    }
}
