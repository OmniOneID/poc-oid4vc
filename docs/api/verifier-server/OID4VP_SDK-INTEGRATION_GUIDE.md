# OID4VP SDK Integration Guide

## Table of Contents

1. [Overview](#1-overview)
2. [Dependency Configuration (build.gradle)](#2-dependency-configuration-buildgradle)
3. [Application Configuration (application.yml)](#3-application-configuration-applicationyml)
4. [Repository Implementation](#4-repository-implementation)
5. [Bean Configuration](#5-bean-configuration)
6. [External Integration Implementation](#6-external-integration-implementation)
7. [Controller Development](#7-controller-development)
8. [DB Schema Configuration (Liquibase)](#8-db-schema-configuration-liquibase)
9. [Appendix](#9-appendix)

---

## Change History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-01-30 | Initial release |

## 1. Overview

### 1.1 SDK Introduction

The OID4VP SDK is a Java library implementing the OpenID for Verifiable Presentations (OID4VP) 1.0 standard.
It enables Verifier server developers to easily implement VP (Verifiable Presentation) verification functionality.

### 1.2 Key Features

| Feature | Description |
|---------|-------------|
| **Verification Session Management** | Create verification requests, manage session states, handle expiration |
| **DCQL Support** | Define credential requests using Digital Credentials Query Language |
| **Multiple VP Formats** | Verify various credential formats including SD-JWT, OpenDID VC |
| **JAR (JWT-Secured Authorization Request)** | Generate signed Authorization Requests compliant with RFC 9101 |
| **VP Token Encrypted Storage** | VP Token encryption using AES-256-GCM |
| **Flexible Storage** | Support for In-Memory, File, and JPA storage options |

### 1.3 Prerequisites

| Item | Requirement |
|------|-------------|
| JDK | 21 or later |
| Spring Boot | 3.2.x or later |
| Build Tool | Gradle 8.x |
| Database | PostgreSQL recommended (when using JPA mode) |

### 1.4 Storage Options

> **Important**: The SDK supports 3 storage modes. Choose the mode that fits your requirements.

| Mode | Description | Use Case | Data Persistence |
|------|-------------|----------|------------------|
| **In-Memory (default)** | Ready to use without separate Repository implementation | Development/Test environments, PoC | Lost on server restart |
| **File** | Load configuration from JSON files | Small-scale operations, fixed configuration environments | Configuration only persisted (read-only) |
| **JPA** | Database-based storage | Production environments | Full persistence |

---

## 2. Dependency Configuration (build.gradle)

### 2.1 Basic Configuration

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.4'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com.example'
version = '1.0.0'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}
```

### 2.2 OID4VP SDK Dependency

Include the SDK as a subproject or add it as a JAR file.

**Option 1: Include as a subproject**

`settings.gradle`:
```groovy
rootProject.name = 'verifier-server'
include 'did-oid4vp-sdk-server'
```

`build.gradle`:
```groovy
dependencies {
    implementation project(':did-oid4vp-sdk-server')
}
```

**Option 2: Include as a JAR file**

```groovy
dependencies {
    implementation files('libs/did-oid4vp-sdk-server-3.0.0.jar')
    implementation fileTree(dir: "libs", includes: ["*.jar"])
}
```

### 2.3 Required and Recommended Libraries

```groovy
dependencies {
    // Spring Boot Core
    implementation 'org.springframework.boot:spring-boot-starter-web'
    
    // (Required when using JPA mode)
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    runtimeOnly 'org.postgresql:postgresql'
    implementation 'org.liquibase:liquibase-core'
    
    // Cryptography
    implementation 'org.bouncycastle:bcpkix-jdk18on:1.80'
    implementation 'com.nimbusds:nimbus-jose-jwt:9.37.4'
    
    // JSON Processing
    implementation 'com.google.code.gson:gson:2.10.1'
    
    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    
    // Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

### 2.4 Complete build.gradle Example

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.4'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com.example'
version = '1.0.0'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

bootJar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

dependencies {
    // OID4VP SDK
    implementation project(':oid4vp-sdk')
    
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    
    // Database (when using JPA mode)
    runtimeOnly 'org.postgresql:postgresql'
    implementation 'org.liquibase:liquibase-core'
    
    // Cryptography
    implementation 'org.bouncycastle:bcpkix-jdk18on:1.80'
    implementation 'com.nimbusds:nimbus-jose-jwt:9.37.4'
    
    // JSON
    implementation 'com.google.code.gson:gson:2.10.1'
    
    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    
    // Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

test {
    useJUnitPlatform()
}
```

---

## 3. Application Configuration (application.yml)

### 3.1 Configuration by Storage Mode

#### 3.1.1 In-Memory Mode (Default)

> **Ready to use without separate Repository implementation.** Suitable for development/test environments.

```yaml
server:
  port: 8081

spring:
  application:
    name: oid4vp-verifier
  jackson:
    default-property-inclusion: non_null
  # Exclude JPA/Database configuration
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
      - org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
      - org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
      - org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration

# In-Memory is automatically used when oid4vp.repository.type is not set
```

#### 3.1.2 File Mode

> **Loads configuration from JSON files.** Suitable for small-scale operations with fixed configuration.

```yaml
server:
  port: 8081

spring:
  application:
    name: oid4vp-verifier
  jackson:
    default-property-inclusion: non_null
  # Exclude JPA/Database configuration
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
      - org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
      - org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
      - org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration

# File-based Repository configuration
oid4vp:
  repository:
    type: file
    file:
      config-path: classpath:oid4vp/oid4vp-config.json
      scope-mapping-path: classpath:oid4vp/dcql-scope-mappings.json
```

**Configuration file location**: `src/main/resources/oid4vp/`

**oid4vp-config.json example**:
```json
{
  "baseUrl": "http://localhost:8081",
  "clientName": "OID4VP Verifier",
  "invocationScheme": "openid4vp://",
  "clientId": {
    "scheme": "decentralized_identifier",
    "value": "did:omn:verifier"
  },
  "session": {
    "sessionTtl": 300000
  },
  "endpoints": {
    "response": "/oid4vp/response",
    "request": "/oid4vp/request"
  },
  "clientMetadata": {
    "vpFormatsSupported": {
      "opendid_vc": {
        "proof_type_values": ["Secp256r1Signature2018"]
      },
      "dc+sd-jwt": {
        "sd-jwt_alg_values": ["ES256"],
        "kb-jwt_alg_values": ["ES256"]
      }
    }
  },
  "crypto": {
    "vpTokenEncryptionKey": "opAOdwHlm+TS4nFPAAVWKYxeRT0yrjfbScPf50tYp6I="
  }
}
```

**dcql-scope-mappings.json example**:
```json
[
  {
    "scope": "sdjwt_test",
    "description": "National ID credential query",
    "enabled": true,
    "dcqlQuery": "{\"credentials\":[{\"id\":\"national_id\",\"format\":\"dc+sd-jwt\",\"meta\":{\"vct_values\":[\"https://credentials.gov.kr/identity_credential\"]}}]}"
  },
  {
    "scope": "opendid_vc_test",
    "description": "Mobile Driver License credential query",
    "enabled": true,
    "dcqlQuery": "{\"credentials\":[{\"id\":\"student_id\",\"format\":\"opendid_vc\",\"meta\":{\"credential_schema_id_values\":[\"http://example.com/schema\"]}}]}"
  }
]
```

#### 3.1.3 JPA Mode

> **Database-based storage.** Recommended for production environments.

```yaml
server:
  port: 8081

spring:
  application:
    name: oid4vp-verifier
  jackson:
    default-property-inclusion: non_null
    
  # Database configuration
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/verifier
    username: ${DB_USERNAME:verifier}
    password: ${DB_PASSWORD:verifier123}
    
  jpa:
    open-in-view: true
    show-sql: false
    hibernate:
      ddl-auto: validate
      naming:
        physical-strategy: org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy
        
  liquibase:
    change-log: classpath:/db/changelog/master.xml
    enabled: true

# JPA-based Repository configuration
oid4vp:
  repository:
    type: jpa

logging:
  level:
    root: INFO
    com.example.oid4vp: DEBUG
    org.omnione.did.oid4vc: DEBUG
```

---

## 4. Repository Implementation

### 4.1 Implementation Requirements by Storage Mode

> **Important**: Repository implementation is NOT required when using In-Memory or File mode!

| Mode | OID4VPRepository | SessionRepository | DCQLScopeMappingRepository |
|------|------------------|-------------------|----------------------------|
| **In-Memory** | SDK built-in | SDK built-in | SDK built-in |
| **File** | Application implementation required | SDK built-in (In-Memory) | Application implementation required |
| **JPA** | Application implementation required | Application implementation required | Application implementation required |

### 4.2 SDK Built-in In-Memory Repository

The SDK provides the following In-Memory Repositories by default:

```java
// OID4VPRepositoryAutoConfiguration.java (SDK built-in)
@Configuration
public class OID4VPRepositoryAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(OID4VPRepository.class)
  public OID4VPRepository inMemoryOID4VPRepository() {
    log.info("No OID4VPRepository bean found. Using InMemoryOID4VPRepository as default.");
    return new InMemoryOID4VPRepository();
  }

  @Bean
  @ConditionalOnMissingBean(SessionRepository.class)
  public SessionRepository inMemorySessionRepository() {
    log.info("No SessionRepository bean found. Using InMemorySessionRepository as default.");
    return new InMemorySessionRepository();
  }

  @Bean
  @ConditionalOnMissingBean(DCQLScopeMappingRepository.class)
  public DCQLScopeMappingRepository inMemoryDCQLScopeMappingRepository() {
    log.info("No DCQLScopeMappingRepository bean found. Using InMemoryDCQLScopeMappingRepository as default.");
    return new InMemoryDCQLScopeMappingRepository();
  }
}
```

### 4.3 File Mode Repository Implementation (Application Server)

When using File mode, the following Repositories must be implemented in the application server:

#### 4.3.1 FileOID4VPRepository

```java
package com.example.oid4vp.repository.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.omnione.did.oid4vc.oid4vp.dto.OID4VPConfigDto;
import org.omnione.did.oid4vc.oid4vp.repository.OID4VPRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "oid4vp.repository.type", havingValue = "jpa", matchIfMissing = true)
public class FileOID4VPRepository implements OID4VPRepository {

    private static final String CONFIG_TYPE_OID4VP = "OID4VP";

    private final ObjectMapper objectMapper;

    @Value("${oid4vp.repository.file.config-path:classpath:oid4vp/oid4vp-config.json}")
    private Resource configFile;

    private final Map<Long, OID4VPConfigDto> configById = new ConcurrentHashMap<>();
    private final Map<String, OID4VPConfigDto> configByType = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @PostConstruct
    public void init() {
        loadFromFile();
    }

    private void loadFromFile() {
        if (configFile == null || !configFile.exists()) {
            log.warn("OID4VP config file not found: {}", configFile);
            return;
        }

        try {
            String configJson = new String(configFile.getInputStream().readAllBytes());
            
            OID4VPConfigDto dto = OID4VPConfigDto.builder()
                    .id(idGenerator.getAndIncrement())
                    .type(CONFIG_TYPE_OID4VP)
                    .config(configJson)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            configById.put(dto.getId(), dto);
            configByType.put(dto.getType(), dto);

            log.info("Loaded OID4VP config from file: {}", configFile.getFilename());
        } catch (IOException e) {
            log.error("Failed to load OID4VP config from file: {}", configFile, e);
            throw new RuntimeException("Failed to load OID4VP config from file", e);
        }
    }

    @Override
    public Optional<OID4VPConfigDto> findByType(String type) {
        return Optional.ofNullable(configByType.get(type));
    }

    @Override
    public Optional<OID4VPConfigDto> findById(Long id) {
        return Optional.ofNullable(configById.get(id));
    }

    @Override
    public List<OID4VPConfigDto> findAll() {
        return new ArrayList<>(configById.values());
    }

    @Override
    public OID4VPConfigDto save(OID4VPConfigDto dto) {
        log.warn("FileOID4VPRepository is read-only. Save operation updates in-memory only.");
        // Performs in-memory update only
        LocalDateTime now = LocalDateTime.now();
        if (dto.getId() == null) {
            dto.setId(idGenerator.getAndIncrement());
            dto.setCreatedAt(now);
        }
        dto.setUpdatedAt(now);
        configById.put(dto.getId(), dto);
        configByType.put(dto.getType(), dto);
        return dto;
    }

    @Override
    public void deleteById(Long id) {
        log.warn("FileOID4VPRepository is read-only. Delete operation updates in-memory only.");
        OID4VPConfigDto removed = configById.remove(id);
        if (removed != null) {
            configByType.remove(removed.getType());
        }
    }

    @Override
    public boolean existsByType(String type) {
        return configByType.containsKey(type);
    }

    @Override
    public long count() {
        return configById.size();
    }
}
```

#### 4.3.2 FileDCQLScopeMappingRepository

```java
package com.example.oid4vp.repository.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.omnione.did.oid4vc.oid4vp.dto.DCQLScopeMappingDto;
import org.omnione.did.oid4vc.oid4vp.repository.DCQLScopeMappingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "oid4vp.repository.type", havingValue = "file")
public class FileDCQLScopeMappingRepository implements DCQLScopeMappingRepository {

    private final ObjectMapper objectMapper;

    @Value("${oid4vp.repository.file.scope-mapping-path:classpath:oid4vp/dcql-scope-mappings.json}")
    private Resource scopeMappingFile;

    private final Map<Long, DCQLScopeMappingDto> mappingById = new ConcurrentHashMap<>();
    private final Map<String, DCQLScopeMappingDto> mappingByScope = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @PostConstruct
    public void init() {
        loadFromFile();
    }

    private void loadFromFile() {
        if (scopeMappingFile == null || !scopeMappingFile.exists()) {
            log.warn("DCQL scope mapping file not found: {}", scopeMappingFile);
            return;
        }

        try {
            List<DCQLScopeMappingDto> mappings = objectMapper.readValue(
                    scopeMappingFile.getInputStream(),
                    new TypeReference<List<DCQLScopeMappingDto>>() {}
            );

            LocalDateTime now = LocalDateTime.now();
            for (DCQLScopeMappingDto dto : mappings) {
                if (dto.getId() == null) dto.setId(idGenerator.getAndIncrement());
                if (dto.getCreatedAt() == null) dto.setCreatedAt(now);
                if (dto.getUpdatedAt() == null) dto.setUpdatedAt(now);
                if (dto.getEnabled() == null) dto.setEnabled(true);

                mappingById.put(dto.getId(), dto);
                mappingByScope.put(dto.getScope(), dto);
            }

            log.info("Loaded {} DCQL scope mappings from file", mappings.size());
        } catch (IOException e) {
            log.error("Failed to load DCQL scope mappings from file", e);
            throw new RuntimeException("Failed to load DCQL scope mappings from file", e);
        }
    }

    // ... remaining method implementations (same as In-Memory)
}
```

### 4.4 JPA Mode Repository Implementation (Application Server)

When using JPA mode, both Entity and Repository must be implemented.

#### 4.4.1 Entity Definitions

**VerificationSessionEntity**:
```java
package com.example.oid4vp.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "verification_session")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationSessionEntity {

    @Id
    @Column(name = "transaction_id", length = 64, nullable = false)
    private String transactionId;

    @Column(name = "state", length = 64, nullable = false)
    private String state;

    @Column(name = "nonce", length = 64, nullable = false)
    private String nonce;

    @Column(name = "dcql_query", columnDefinition = "TEXT")
    private String dcqlQuery;

    @Column(name = "response_mode", length = 32)
    private String responseMode;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Builder.Default
    @Column(name = "status", length = 32, nullable = false)
    private String status = "CREATED";

    @Column(name = "client_metadata", columnDefinition = "TEXT")
    private String clientMetadata;

    @Column(name = "request_uri_fetched_at")
    private Long requestUriFetchedAt;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "expires_at")
    private Long expiresAt;

    @Column(name = "vp_token", columnDefinition = "TEXT")
    private String vpToken;
}
```

**OID4VPEntity**:
```java
package com.example.oid4vp.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "oid4vp")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OID4VPEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "type", length = 50, nullable = false)
    private String type;

    @Column(name = "config", columnDefinition = "TEXT", nullable = false)
    private String config;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

**DCQLScopeMappingEntity**:
```java
package com.example.oid4vp.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "dcql_scope_mapping")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DCQLScopeMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "scope", length = 100, nullable = false, unique = true)
    private String scope;

    @Column(name = "dcql_query", columnDefinition = "TEXT", nullable = false)
    private String dcqlQuery;

    @Column(name = "description", length = 255)
    private String description;

    @Builder.Default
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

#### 4.4.2 Spring Data JPA Repository

```java
// VerificationSessionJpaRepository.java
public interface VerificationSessionJpaRepository 
        extends JpaRepository<VerificationSessionEntity, String> {
    Optional<VerificationSessionEntity> findByRequestId(String requestId);
    Optional<VerificationSessionEntity> findByState(String state);
}

// OID4VPJpaRepository.java
public interface OID4VPJpaRepository extends JpaRepository<OID4VPEntity, Long> {
    Optional<OID4VPEntity> findByType(String type);
    boolean existsByType(String type);
}

// DCQLScopeMappingJpaRepository.java
public interface DCQLScopeMappingJpaRepository 
        extends JpaRepository<DCQLScopeMappingEntity, Long> {
    Optional<DCQLScopeMappingEntity> findByScope(String scope);
    List<DCQLScopeMappingEntity> findAllByEnabledTrue();
    boolean existsByScope(String scope);
    void deleteByScope(String scope);
}
```

#### 4.4.3 SDK Repository Interface Implementation

```java
// JpaSessionRepository.java
@Slf4j
@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "oid4vp.repository.type", havingValue = "jpa", matchIfMissing = false)
public class JpaSessionRepository implements SessionRepository {

    private final VerificationSessionJpaRepository jpaRepository;

    @Override
    public void saveByState(String state, VerificationSession session) {
        VerificationSessionEntity entity = toEntity(session);
        jpaRepository.save(entity);
    }

    @Override
    public Optional<VerificationSession> findByState(String state) {
        return jpaRepository.findByState(state).map(this::toDto);
    }

    // ... remaining method implementations
    
    private VerificationSessionEntity toEntity(VerificationSession dto) { /* ... */ }
    private VerificationSession toDto(VerificationSessionEntity entity) { /* ... */ }
}
```

#### 4.4.4 JPA Configuration

In JPA mode, separate `@EnableJpaRepositories` and `@EntityScan` into a separate Configuration for conditional activation:

```java
package com.example.oid4vp.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ConditionalOnProperty(name = "oid4vp.repository.type", havingValue = "jpa")
@EnableJpaRepositories(basePackages = "com.example.oid4vp.repository.jpa")
@EntityScan(basePackages = "com.example.oid4vp.repository.entity")
public class JpaConfiguration {
    // Activated only in JPA mode
}
```

---

## 5. Bean Configuration

### 5.1 VPTokenVerifier Auto-Registration

The `did-oid4vc-formatter-sdk-server` (OID4VC Formatter SDK) provides `VPTokenVerifier` implementations as `@Component`.
**No separate Bean configuration is required.**

VPTokenVerifier implementations provided by the Formatter SDK:

| Implementation | Supported Formats | Description |
|----------------|-------------------|-------------|
| `SDJWTVPVerifier` | `dc+sd-jwt`, `vc+sd-jwt` | SD-JWT based Verifiable Credential |
| `OpenDIDVPVerifier` | `opendid_vc` | OpenDID specification Verifiable Credential |

### 5.2 VPTokenVerifier Implementations

VPTokenVerifier implementations provided by the Formatter SDK (`did-oid4vc-formatter-sdk-server`):

| Implementation | Supported Formats | Description |
|----------------|-------------------|-------------|
| `SDJWTVPVerifier` | `dc+sd-jwt`, `vc+sd-jwt` | SD-JWT based Verifiable Credential |
| `OpenDIDVPVerifier` | `opendid_vc` | OpenDID specification Verifiable Credential |

---

## 6. External Integration Implementation

### 6.1 CompactSigner Implementation

The `CompactSigner` interface must be implemented for Authorization Request signing.

```java
@FunctionalInterface
public interface CompactSigner {
    /**
     * Generates a signature for a hash value.
     * 
     * @param keyId Key ID to use for signing
     * @param hash Hash value to sign (SHA-256)
     * @return Signature byte array
     */
    byte[] sign(String keyId, byte[] hash) throws Exception;
}
```

**Open DID Wallet SDK Server Integration Example**:
```java
WalletManagerInterface walletManager = WalletManagerFactory
        .getWalletManager(WalletManagerFactory.WalletManagerType.FILE);
walletManager.connect("/path/to/wallet.wallet", "password".toCharArray());

CompactSigner signer = (keyId, hash) -> {
    return walletManager.generateCompactSignatureFromHash(keyId, hash);
};

String publicKeyMultibase = walletManager.getPublicKey("assert");
```

---

## 7. Controller Development

### 7.1 Endpoint Configuration

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/oid4vp/initiate` | POST | Start verification session |
| `/oid4vp/request/{request_id}` | GET | Retrieve Authorization Request (JAR) |
| `/oid4vp/response` | POST/GET | Receive and verify VP Token |
| `/wallet/public-keys.json` | GET | Retrieve preregistered client_id public keys — called during JAR-based Authorization Request with EUDI Wallet (temporary for testing) |

### 7.2 OID4VP Controller

```java
package com.example.oid4vp.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.omnione.did.oid4vc.oid4vp.dto.ServiceResult;
import org.omnione.did.oid4vc.oid4vp.exception.OID4VPException;
import org.omnione.did.oid4vc.oid4vp.service.AuthorizationService;
import org.omnione.did.oid4vc.oid4vp.service.InitiationService;
import org.omnione.did.oid4vc.oid4vp.service.OID4VPHelperService;
import org.omnione.did.oid4vc.oid4vp.util.jar.jws.CompactSigner;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@Controller
@RequestMapping("/oid4vp")
@RequiredArgsConstructor
public class OID4VPController {

    private final InitiationService initiationService;
    private final AuthorizationService authorizationService;
    private final OID4VPHelperService oid4VPHelperService;

    /**
     * Starts a verification session.
     */
    @PostMapping("/initiate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> initiateVerification(
            @RequestParam(required = false) String dcql_query,
            @RequestParam(required = false) String scope,
            @RequestParam(defaultValue = "direct_post") String response_mode,
            @RequestParam(required = false) String client_metadata,
            @RequestParam(defaultValue = "true") boolean use_request_uri) {
        
        try {
            ServiceResult<Map<String, Object>> result = initiationService.initiateVerification(
                    dcql_query, scope, response_mode, client_metadata, use_request_uri);
            return toMapResponse(result);
        } catch (OID4VPException e) {
            log.error("Initiation failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getErrorCode(),
                    "error_description", e.getErrorMsg()
            ));
        }
    }

    /**
     * Returns Authorization Request in JWS format.
     */
    @GetMapping("/request/{request_id}")
    public ResponseEntity<String> getAuthorizationRequest(
            @PathVariable String request_id,
            @RequestParam(required = false) String wallet_metadata) {
        
        try {
          // Construct verification method based on Verifier DID Document
          // In production environment, retrieve dynamically from DID Document
            String signKeyId = "assert";
            String verificationMethod = "did:omn:verifier" + "?versionId=" + "1" + "#" + signKeyId;
          // Example: "did:omn:verifier?versionId=1#assert"
          // Production implementation: verifierDidDoc.getId() + "?versionId=" + verifierDidDoc.getVersionId() + "#" + signKeyId
          
            // CompactSigner implementation (Wallet SDK integration)
            CompactSigner signer = (keyId, hash) -> {
                // Generate signature via Wallet SDK
                return signWithWallet(keyId, hash);
            };

            String publicKeyMultibase = getPublicKeyFromWallet(signKeyId);

            ServiceResult<String> result = authorizationService.getAuthorizationRequest(
                    request_id, signer, verificationMethod, publicKeyMultibase);
            return toStringResponse(result);
            
        } catch (Exception e) {
            log.error("Failed to get authorization request", e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    /**
     * Receives and verifies VP Token.
     */
    @RequestMapping(value = "/response", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResponseEntity<Map<String, Object>> receiveResponse(
            @RequestParam(required = false) String vp_token,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String error_description,
            HttpServletRequest request) {

        // VP Token parsing and verification logic
        // ... (refer to previous guide)
        
        return toMapResponse(result);
    }

    // Helper methods...
}
```

---

## 8. DB Schema Configuration (Liquibase)

> **Required only when using JPA mode.**

### 8.1 Changelog File Structure

```
src/main/resources/
└── db/
    └── changelog/
        ├── master.xml
        └── changeset/
            ├── 001-verification-session.xml
            ├── 002-oid4vp-config.xml
            └── 003-dcql-scope-mapping.xml
```

### 8.2 master.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

  <include file="changeset/001-verification-session.xml" relativeToChangelogFile="true"/>
  <include file="changeset/002-oid4vp-config.xml" relativeToChangelogFile="true"/>
  <include file="changeset/003-dcql-scope-mapping.xml" relativeToChangelogFile="true"/>

</databaseChangeLog>
```

(Changeset XML files are the same as in the previous guide)

---

## 9. Appendix

### 9.1 Sequence Diagram

```
┌────┐          ┌──────────┐          ┌────────┐
│ SP │          │ Verifier │          │ Wallet │
└─┬──┘          └────┬─────┘          └───┬────┘
  │                  │                    │
  │ POST /initiate   │                    │
  │ ────────────────>│                    │
  │                  │                    │
  │ {authorization_request_uri,           │
  │  transaction_id, state}               │
  │ <────────────────│                    │
  │                  │                    │
  │                  │ QR/DeepLink        │
  │                  │ ──────────────────>│
  │                  │                    │
  │                  │ GET /request/{id}  │
  │                  │ <──────────────────│
  │                  │                    │
  │                  │ JAR (signed JWT)   │
  │                  │ ──────────────────>│
  │                  │                    │
  │                  │ POST /response     │
  │                  │ (vp_token, state)  │
  │                  │ <──────────────────│
```

### 9.2 Session Status

| Status | Description |
|--------|-------------|
| `CREATED` | Session created |
| `REQUEST_FETCHED` | Authorization Request retrieved |
| `COMPLETED` | Verification completed successfully |
| `FAILED` | Verification failed |
| `EXPIRED` | Session expired |

### 9.3 VP Token Encryption Key Generation

```java
import org.omnione.did.oid4vc.oid4vp.util.crypto.VPTokenEncryptor;

public class KeyGenerator {
    public static void main(String[] args) {
        String key = VPTokenEncryptor.generateKey();
        System.out.println("Generated key: " + key);
    }
}
```

### 9.4 Storage Mode Selection Guide

| Use Case | Recommended Mode |
|----------|------------------|
| Local development / Unit testing | In-Memory |
| PoC / Demo | In-Memory or File |
| Small-scale operations with fixed configuration | File |
| Operations requiring dynamic configuration changes | JPA |
| High availability / Cluster environments | JPA |

### 9.5 Error Codes

For SDK error codes, please refer to the `OID4VPSDKError.md` (OID4VP SDK errors) and `FormatterSDKError.md` (Formatter SDK errors) documents.

---