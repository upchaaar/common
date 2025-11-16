package tech.mayanktiwari.upchaar.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import tech.mayanktiwari.upchaar.common.idempotency.IdempotencyFilter;
import tech.mayanktiwari.upchaar.common.idempotency.IdempotencyProperties;
import tech.mayanktiwari.upchaar.common.idempotency.IdempotencyStore;
import tech.mayanktiwari.upchaar.common.outbox.OutboxProperties;
import tech.mayanktiwari.upchaar.common.util.CorrelationIdFilter;

@Slf4j
@AutoConfiguration
@EnableConfigurationProperties({ IdempotencyProperties.class, OutboxProperties.class })
public class CommonAutoConfiguration {

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {
        log.info("Registering CorrelationIdFilter");
        FilterRegistrationBean<CorrelationIdFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new CorrelationIdFilter());
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }

    @Bean
    @ConditionalOnProperty(prefix = "upchaar.idempotency",
                           name = "enabled",
                           havingValue = "true",
                           matchIfMissing = true)
    public FilterRegistrationBean<IdempotencyFilter> idempotencyFilter(
            IdempotencyStore store, IdempotencyProperties properties
    ) {
        log.info("Registering IdempotencyFilter");
        FilterRegistrationBean<IdempotencyFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new IdempotencyFilter(store, properties));
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(2);
        return registrationBean;
    }
}