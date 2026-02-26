# OID4VP SDK 연동 가이드

## 목차

1. [개요](#1-개요)
2. [의존성 설정 (build.gradle)](#2-의존성-설정-buildgradle)
3. [애플리케이션 설정 (application.yml)](#3-애플리케이션-설정-applicationyml)
4. [Repository 구현](#4-repository-구현)
5. [Bean 설정](#5-bean-설정)
6. [외부 연동 구현](#6-외부-연동-구현)
7. [Controller 개발](#7-controller-개발)
8. [DB 스키마 설정 (Liquibase)](#8-db-스키마-설정-liquibase)
9. [부록](#9-부록)

---

## 변경 이력

| 버전 | 날짜         | 변경 내용 |
|------|------------|----------|
| 1.0.0 | 2025-01-30 | 최초 작성 |

## 1. 개요

### 1.1 SDK 소개

OID4VP SDK는 OpenID for Verifiable Presentations (OID4VP) 1.0 표준을 구현한 Java 라이브러리입니다.
Verifier 서버 개발자가 VP(Verifiable Presentation) 검증 기능을 쉽게 구현할 수 있도록 지원합니다.

### 1.2 주요 기능

| 기능 | 설명 |
|------|------|
| **검증 세션 관리** | 검증 요청 생성, 세션 상태 관리, 만료 처리 |
| **DCQL 지원** | Digital Credentials Query Language를 통한 크리덴셜 요청 정의 |
| **다양한 VP 포맷** | SD-JWT, OpenDID VC 등 다양한 크리덴셜 포맷 검증 |
| **JAR(JWT-Secured Authorization Request)** | RFC 9101 준수 서명된 Authorization Request 생성 |
| **VP Token 암호화 저장** | AES-256-GCM을 사용한 VP Token 암호화 |
| **유연한 저장소** | In-Memory, File, JPA 등 다양한 저장소 지원 |

### 1.3 전제 조건

| 항목 | 요구 사항 |
|------|----------|
| JDK | 21 이상 |
| Spring Boot | 3.2.x 이상 |
| Build Tool | Gradle 8.x |
| Database | PostgreSQL 권장 (JPA 모드 사용 시) |

### 1.4 저장소 옵션

> **중요**: SDK는 3가지 저장소 모드를 지원합니다. 요구사항에 맞는 모드를 선택하세요.

| 모드 | 설명 | 적용 대상 | 데이터 영속성 |
|------|------|----------|--------------|
| **In-Memory (기본)** | 별도 Repository 구현 없이 즉시 사용 가능 | 개발/테스트 환경, PoC | 서버 재시작 시 소멸 |
| **File** | JSON 파일 기반 설정 로드 | 소규모 운영, 설정 고정 환경 | 설정만 영속 (읽기 전용) |
| **JPA** | 데이터베이스 기반 저장 | 운영 환경 | 완전한 영속성 |

---

## 2. 의존성 설정 (build.gradle)

### 2.1 기본 설정

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

### 2.2 OID4VP SDK 의존성

SDK를 서브 프로젝트로 포함하거나 JAR 파일로 추가합니다.

**방법 1: 서브 프로젝트로 포함**

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

**방법 2: JAR 파일로 포함**

```groovy
dependencies {
    implementation files('libs/did-oid4vp-sdk-server-3.0.0.jar')
    implementation fileTree(dir: "libs", includes: ["*.jar"])
}
```

### 2.3 필수 및 권장 라이브러리

```groovy
dependencies {
    // Spring Boot Core
    implementation 'org.springframework.boot:spring-boot-starter-web'
    
    // (JPA 모드 사용 시 필수)
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

### 2.4 전체 build.gradle 예시

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
    
    // Database (JPA 모드 사용 시)
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

## 3. 애플리케이션 설정 (application.yml)

### 3.1 저장소 모드별 설정

#### 3.1.1 In-Memory 모드 (기본값)

> **별도 Repository 구현 없이 즉시 사용 가능합니다.** 개발/테스트 환경에 적합합니다.

```yaml
server:
  port: 8081

spring:
  application:
    name: oid4vp-verifier
  jackson:
    default-property-inclusion: non_null
  # JPA/Database 설정 제외
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
      - org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
      - org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
      - org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration

# oid4vp.repository.type 미설정 시 In-Memory 자동 사용
```

#### 3.1.2 File 모드

> **JSON 파일에서 설정을 로드합니다.** 설정이 고정된 소규모 운영 환경에 적합합니다.

```yaml
server:
  port: 8081

spring:
  application:
    name: oid4vp-verifier
  jackson:
    default-property-inclusion: non_null
  # JPA/Database 설정 제외
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
      - org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
      - org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
      - org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration

# File 기반 Repository 설정
oid4vp:
  repository:
    type: file
    file:
      config-path: classpath:oid4vp/oid4vp-config.json
      scope-mapping-path: classpath:oid4vp/dcql-scope-mappings.json
```

**설정 파일 위치**: `src/main/resources/oid4vp/`

**oid4vp-config.json 예시**:
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

**dcql-scope-mappings.json 예시**:
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

#### 3.1.3 JPA 모드

> **데이터베이스 기반 저장소입니다.** 운영 환경에 권장됩니다.

```yaml
server:
  port: 8081

spring:
  application:
    name: oid4vp-verifier
  jackson:
    default-property-inclusion: non_null
    
  # Database 설정
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

# JPA 기반 Repository 설정
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

## 4. Repository 구현

### 4.1 저장소 모드별 구현 요구사항

> **중요**: In-Memory 또는 File 모드 사용 시 Repository 구현이 필요하지 않습니다!

| 모드      | OID4VPRepository | SessionRepository | DCQLScopeMappingRepository |
|---------|------------------|-------------------|----------------------------|
| **In-Memory** | SDK 내장 | SDK 내장 | SDK 내장 |
| **File** | 응용 구현 필요 | SDK 내장 (In-Memory) | 응용 구현 필요 |
| **JPA** | 응용 구현 필요 | 응용 구현 필요 | 응용 구현 필요 |

### 4.2 SDK 내장 In-Memory Repository

SDK는 다음 In-Memory Repository를 기본 제공합니다:

```java
// OID4VPRepositoryAutoConfiguration.java (SDK 내장)
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

### 4.3 File 모드 Repository 구현 (응용 서버)

File 모드 사용 시, 다음 Repository를 응용 서버에서 구현해야 합니다:

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
        // In-memory 업데이트만 수행
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

    // ... 나머지 메서드 구현 (In-Memory와 동일)
}
```

### 4.4 JPA 모드 Repository 구현 (응용 서버)

JPA 모드 사용 시, Entity와 Repository를 모두 구현해야 합니다.

#### 4.4.1 Entity 정의

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

#### 4.4.3 SDK Repository 인터페이스 구현

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

    // ... 나머지 메서드 구현
    
    private VerificationSessionEntity toEntity(VerificationSession dto) { /* ... */ }
    private VerificationSession toDto(VerificationSessionEntity entity) { /* ... */ }
}
```

#### 4.4.4 JPA Configuration

JPA 모드에서 `@EnableJpaRepositories`와 `@EntityScan`을 별도 Configuration으로 분리하여 조건부 활성화합니다:

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
    // JPA 모드일 때만 활성화
}
```

---

## 5. Bean 설정

### 5.1 VPTokenVerifier 자동 등록

`did-oid4vc-formatter-sdk-server` (OID4VC Formatter SDK)에서 `VPTokenVerifier` 구현체를 `@Component`로 자동 등록합니다.
**별도의 Bean 설정이 필요하지 않습니다.**

Formatter SDK에서 기본 제공하는 VPTokenVerifier 구현체:

| 구현체 | 지원 포맷 | 설명 |
|--------|----------|------|
| `SDJWTVPVerifier` | `dc+sd-jwt`, `vc+sd-jwt` | SD-JWT 기반 Verifiable Credential |
| `OpenDIDVPVerifier` | `opendid_vc` | OpenDID 규격 Verifiable Credential |

### 5.2 VPTokenVerifier 구현체

Formatter SDK (`did-oid4vc-formatter-sdk-server`)에서 제공하는 VPTokenVerifier 구현체:

| 구현체 | 지원 포맷 | 설명 |
|--------|----------|------|
| `SDJWTVPVerifier` | `dc+sd-jwt`, `vc+sd-jwt` | SD-JWT 기반 Verifiable Credential |
| `OpenDIDVPVerifier` | `opendid_vc` | OpenDID 규격 Verifiable Credential |

---

## 6. 외부 연동 구현

### 6.1 CompactSigner 구현

Authorization Request 서명을 위해 `CompactSigner` 인터페이스를 구현해야 합니다.

```java
@FunctionalInterface
public interface CompactSigner {
    /**
     * Hash 값에 대한 서명을 생성합니다.
     * 
     * @param keyId 서명에 사용할 키 ID
     * @param hash 서명할 해시 값 (SHA-256)
     * @return 서명 바이트 배열
     */
    byte[] sign(String keyId, byte[] hash) throws Exception;
}
```

**Open DID Wallet SDK Server 연동 예시**:
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

## 7. Controller 개발

### 7.1 엔드포인트 구성

| 엔드포인트 | 메서드 | 설명 |
|-----------|--------|------|
| `/oid4vp/initiate` | POST | 검증 세션 시작 |
| `/oid4vp/request/{request_id}` | GET | Authorization Request 조회 (JAR) |
| `/oid4vp/response` | POST/GET | VP Token 수신 및 검증 |
| `/wallet/public-keys.json` | GET | preregistered client_id 공개키 조회 — EUDI Wallet 연동 시 JAR 방식의 Authorization Request에서 호출 (임시 테스트용) |

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
     * 검증 세션을 시작합니다.
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
     * Authorization Request를 JWS 형식으로 반환합니다.
     */
    @GetMapping("/request/{request_id}")
    public ResponseEntity<String> getAuthorizationRequest(
            @PathVariable String request_id,
            @RequestParam(required = false) String wallet_metadata) {
        
        try {
          // Verifier DID Document 기반 verification method 구성
          // 실제 운영 환경에서는 DID Document에서 동적으로 조회
            String signKeyId = "assert";
            String verificationMethod = "did:omn:verifier" + "?versionId=" + "1" + "#" + signKeyId;
          // 예시: "did:omn:verifier?versionId=1#assert"
          // 실제 구현: verifierDidDoc.getId() + "?versionId=" + verifierDidDoc.getVersionId() + "#" + signKeyId

          // CompactSigner 구현 (Wallet SDK 연동)
            CompactSigner signer = (keyId, hash) -> {
                // Wallet SDK를 통한 서명 생성
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
     * VP Token을 수신하고 검증합니다.
     */
    @RequestMapping(value = "/response", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResponseEntity<Map<String, Object>> receiveResponse(
            @RequestParam(required = false) String vp_token,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String error_description,
            HttpServletRequest request) {

        // VP Token 파싱 및 검증 로직
        // ... (이전 가이드 참조)
        
        return toMapResponse(result);
    }

    // Helper methods...
}
```

---

## 8. DB 스키마 설정 (Liquibase)

> **JPA 모드 사용 시에만 필요합니다.**

### 8.1 Changelog 파일 구조

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

(Changeset XML 파일은 이전 가이드와 동일)

---

## 9. 부록

### 9.1 시퀀스 다이어그램

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

### 9.2 세션 상태 (Status)

| 상태 | 설명 |
|------|------|
| `CREATED` | 세션 생성됨 |
| `REQUEST_FETCHED` | Authorization Request 조회됨 |
| `COMPLETED` | 검증 성공 완료 |
| `FAILED` | 검증 실패 |
| `EXPIRED` | 세션 만료 |

### 9.3 VP Token 암호화 키 생성

```java
import org.omnione.did.oid4vc.oid4vp.util.crypto.VPTokenEncryptor;

public class KeyGenerator {
    public static void main(String[] args) {
        String key = VPTokenEncryptor.generateKey();
        System.out.println("Generated key: " + key);
    }
}
```

### 9.4 저장소 모드 선택 가이드

| 사용 사례 | 권장 모드 |
|----------|----------|
| 로컬 개발 / 단위 테스트 | In-Memory |
| PoC / 데모 | In-Memory 또는 File |
| 설정이 고정된 소규모 운영 | File |
| 동적 설정 변경이 필요한 운영 | JPA |
| 고가용성 / 클러스터 환경 | JPA |

### 9.5 에러 코드

SDK 에러 코드는 `OID4VPSDKError.md` (OID4VP SDK 에러), `FormatterSDKError.md` (Formatter SDK 에러) 문서를 참조하세요.

---