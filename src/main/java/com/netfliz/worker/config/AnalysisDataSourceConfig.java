package com.netfliz.worker.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.netfliz.worker.repository.analysis",
        entityManagerFactoryRef = "analysisEntityManagerFactory",
        transactionManagerRef = "analysisTransactionManager"
)
public class AnalysisDataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.analysis")
    public DataSourceProperties analysisDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource analysisDataSource(
            @Qualifier("analysisDataSourceProperties") DataSourceProperties props
    ) {
        return props.initializeDataSourceBuilder().build();
    }

    @Primary
    @Bean(name = "analysisEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean analysisEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("analysisDataSource") DataSource dataSource) {

        return builder
                .dataSource(dataSource)
                .packages("com.netfliz.worker.entity.analysis")
                .persistenceUnit("analysis")
                .build();
    }

    @Primary
    @Bean(name = "analysisTransactionManager")
    public PlatformTransactionManager analysisTransactionManager(
            @Qualifier("analysisEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
