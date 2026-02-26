package com.example.did.oid4vc.verifier.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ConditionalOnProperty(name = "oid4vp.repository.type", havingValue = "jpa")
@EntityScan(basePackages = {"com.example.did.oid4vc.verifier.repository.entity"})
@EnableJpaRepositories(basePackages = {"com.example.did.oid4vc.verifier.repository"})
public class JpaConfiguration {
}
