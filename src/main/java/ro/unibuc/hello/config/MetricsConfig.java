package ro.unibuc.hello.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public Counter httpRequestsTotal(MeterRegistry registry) {
        return Counter.builder("http_requests_total")
                .description("Total number of HTTP requests")
                .register(registry);
    }

    @Bean
    public Counter successfulOperations(MeterRegistry registry) {
        return Counter.builder("successful_operations_total")
                .description("Total number of successful operations")
                .register(registry);
    }

    @Bean
    public Counter failedOperations(MeterRegistry registry) {
        return Counter.builder("failed_operations_total")
                .description("Total number of failed operations")
                .register(registry);
    }

    @Bean
    public Timer operationLatency(MeterRegistry registry) {
        return Timer.builder("operation_latency_seconds")
                .description("Operation latency in seconds")
                .register(registry);
    }

    @Bean
    public Counter activeUsers(MeterRegistry registry) {
        return Counter.builder("active_users_total")
                .description("Total number of active users")
                .register(registry);
    }
} 