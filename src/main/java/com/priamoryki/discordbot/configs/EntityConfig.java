package com.priamoryki.discordbot.configs;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class EntityConfig {
    @Bean
    public EntityManager getEntityManager(EntityManagerFactory factory) {
        return factory.createEntityManager();
    }

    @Bean
    @DependsOn("fileLoader")
    public EntityManagerFactory getFactory() {
        return Persistence.createEntityManagerFactory("main");
    }
}
