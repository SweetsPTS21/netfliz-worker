package com.netfliz.worker.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "kafka.concurrency")
public class KafkaConcurrencyProperties {
    private int videoView;
    private int userActivity;
    private int recommendation;
    private int notification;
    private int videoProgress;
    private int payment;
    private int analytics;
}
