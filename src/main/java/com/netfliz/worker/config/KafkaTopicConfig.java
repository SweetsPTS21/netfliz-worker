package com.netfliz.worker.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.topics.video-view}")
    private String videoViewTopic;

    @Value("${kafka.topics.user-activity}")
    private String userActivityTopic;

    @Value("${kafka.topics.recommendation}")
    private String recommendationTopic;

    @Value("${kafka.topics.notification}")
    private String notificationTopic;

    @Value("${kafka.topics.video-progress}")
    private String videoProgressTopic;

    @Value("${kafka.topics.payment}")
    private String paymentTopic;

    @Value("${kafka.topics.analytics}")
    private String analyticsTopic;

    @Value("${kafka.default.partitions}")
    private int partitions;

    @Value("${kafka.default.replication-factor}")
    private short replicationFactor;

    @Value("${kafka.default.min-insync-replicas}")
    private String minInSyncReplicas;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic videoViewTopic() {
        return TopicBuilder.name(videoViewTopic)
                .partitions(partitions)
                .replicas(replicationFactor)
                .compact() // Log compaction cho dữ liệu trạng thái
                .build();
    }

    @Bean
    public NewTopic userActivityTopic() {
        return TopicBuilder.name(userActivityTopic)
                .partitions(partitions)
                .replicas(replicationFactor)
                .config("retention.ms", "604800000") // 7 ngày
                .build();
    }

    @Bean
    public NewTopic recommendationTopic() {
        return TopicBuilder.name(recommendationTopic)
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }

    @Bean
    public NewTopic notificationTopic() {
        return TopicBuilder.name(notificationTopic)
                .partitions(partitions)
                .replicas(replicationFactor)
                .config("retention.ms", "86400000") // 1 ngày
                .build();
    }

    @Bean
    public NewTopic videoProgressTopic() {
        return TopicBuilder.name(videoProgressTopic)
                .partitions(partitions)
                .replicas(replicationFactor)
                .compact()
                .build();
    }

    @Bean
    public NewTopic paymentTopic() {
        return TopicBuilder.name(paymentTopic)
                .partitions(partitions)
                .replicas(replicationFactor)
                .config("retention.ms", "2592000000") // 30 ngày
                .config("min.insync.replicas", minInSyncReplicas) // Đảm bảo an toàn cho payment
                .build();
    }

    @Bean
    public NewTopic analyticsTopic() {
        return TopicBuilder.name(analyticsTopic)
                .partitions(partitions * 2) // Nhiều partition hơn cho analytics
                .replicas(replicationFactor)
                .build();
    }
}
