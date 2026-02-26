# OID4VCI SDK 적용 가이드

본 문서는 응용 서버(Issuer Server)에 `issuer-sdk` 및 `authorization-sdk`를 통합하여 OID4VC 기반의 자격증명(VC) 발급 시스템을 구축하는 방법을 설명합니다.

---

## 0. SDK 의존성 추가 (Build Configuration)

프로젝트에 `issuer-sdk` 및 `authorization-sdk`를 포함한 모든 라이브러리를 JAR 파일 형태로 추가하도록 `build.gradle`을 설정합니다.

### 0.1 JAR 파일 배치
준비된 SDK 및 관련 라이브러리 JAR 파일들을 프로젝트 루트의 `libs` 디렉토리에 배치합니다.
*   `libs/issuer-sdk-x.x.x.jar`
*   `libs/authorization-sdk-x.x.x.jar`
*   `libs/did-wallet-sdk-x.x.x.jar` (기타 의존 라이브러리 등)

### 0.2 의존성 설정 (`build.gradle`)
`libs` 디렉토리에 있는 모든 JAR 파일들을 의존성에 포함시킵니다.

```groovy
dependencies {
    // Local JARs (SDKs and other libraries)
    implementation fileTree(dir: 'libs', include: ['*.jar'])

    // Spring Boot Starters
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
    
    // DB & Utilities
    runtimeOnly 'org.postgresql:postgresql'
    implementation 'org.liquibase:liquibase-core'
    implementation 'com.google.code.gson:gson'
    
    // ... 기타 필요한 의존성
}
```

---

## 1. 프로퍼티 파일 적용 (Configuration)

Spring Boot의 설정 파일 분리 기능을 사용하여 SDK 및 인프라 설정을 관리합니다.

### 1.1 `application.yml` (메인 설정)
메인 설정 파일에서 각 모듈별 프로퍼티 파일을 import 합니다.

```yaml
spring:
  config:
    import:
      - "classpath:application-issuer-sdk.yml"       # Issuer SDK 설정
      - "classpath:application-authorization-server.yml" # Authorization Server SDK 설정
      - "classpath:application-database.yml"         # DB 설정
```

### 1.2 `application-issuer-sdk.yml`
Issuer SDK 동작에 필요한 메타데이터 경로, 외부 서비스 URL, 보안 설정을 정의합니다.

```yaml
issuer:
  base-url: http://10.48.17.124:8080
  data-dir: /path/to/data # 메타데이터 및 인증서가 포함된 디렉토리
  metadata-file-path: metadata/issuer_meta_univ_local.json # 메타데이터 파일 경로 (data-dir 상대 경로)
  root-ca-file-name: rootCA.crt # (선택) x5c proof 검증을 위한 Root CA
  sdk:
    credential-configurations:
      StudentID:
        format: open-did-vc
        identifiers:
          - TEC
          - UCR

# 인가 서버 및 리소스 서버 보안 설정
clients:
  auth-server:
    url: http://localhost:8080

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: ${clients.auth-server.url}/oauth2/jwks
```

### 1.3 `application-authorization-server.yml`
Authorization Server 관련 설정을 정의합니다. (OAuth2 Client 및 연동 설정)

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: "YOUR-ACTUAL-CLIENT-ID"
            client-secret: "YOUR-ACTUAL-SECRET"
            scope:
              - openid
              - profile
              - email

oid4vc:
  auth:
    issuer-url: http://10.48.17.124:8080
    clients:
      redirect-url: http://10.48.17.124:8080/auth/callback
      issuer-server:
        url: http://10.48.17.124:8080
```

### 1.4 `application-database.yml`
데이터베이스 연결 정보를 설정합니다.

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/issuer_db
    username: user
    password: password
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate # Liquibase 사용 시 validate 권장
```

---

## 2. Issuer Metadata 적용

OID4VC 프로토콜에서 중요한 Issuer Metadata를 정의하고 적용합니다.

### 2.1 메타데이터 파일 작성 (`issuer_meta_univ_local.json`)
`metadata/` 디렉토리에 위치하며, 각 **Endpoint URL**과 **지원하는 Credential 설정**을 정의합니다.

```json
{
  "credential_issuer": "http://10.48.17.124:8080",
  "credential_offer_endpoint": "http://10.48.17.124:8080/credential-offer",
  "credential_endpoint": "http://10.48.17.124:8080/credential",
  "nonce_endpoint": "http://10.48.17.124:8080/nonce",
  ...
}
```
*   **중요:** 여기에 정의된 Endpoint URL은 SDK가 동적으로 컨트롤러에 매핑할 때 사용됩니다.

---

## 3. Config 개발

### 3.1 Security Filter Chain (`IssuerSecurityConfig`)
OAuth2 Resource Server 설정을 통해 Access Token 검증 로직을 구성합니다.

```java
@Configuration
@EnableWebSecurity
public class IssuerSecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/credential-offer/**", "/.well-known/**").permitAll() // 공개 Endpoint
                .anyRequest().authenticated() // 그 외는 토큰 필요
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
```

### 3.2 Request Mapping (동적 매핑)
메타데이터에 정의된 URL을 읽어 컨트롤러 메서드와 동적으로 연결합니다.

```java
@Configuration
@RequiredArgsConstructor
public class DynamicEndpointConfig {
    
    private final CredentialService credentialService;
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    // ... Controller 주입

    @EventListener(ApplicationReadyEvent.class)
    public void registerDynamicEndpoints() {
        IssuerMetadataResponse metadata = credentialService.getIssuerMetadata();
        
        // 메타데이터 URL을 읽어 컨트롤러 메서드에 매핑
        registerEndpoint(metadata.getCredentialOfferEndpoint(), RequestMethod.GET, "getCredentialOffer", String.class);
        registerEndpoint(metadata.getCredentialEndpoint(), RequestMethod.POST, "issueCredential", CredentialRequest.class, Jwt.class);
        registerEndpoint(metadata.getNotificationEndpoint(), RequestMethod.POST, "handleNotification", NotificationRequest.class);
        // ... 기타 Endpoint 등록
    }
}
```

---

## 4. Controller 개발

SDK의 `CredentialService`를 호출하여 실제 비즈니스 로직을 처리하는 컨트롤러를 구현합니다. 동적 매핑을 위해 `@PostMapping` 등의 어노테이션은 메서드 레벨에서 제거하거나, 동적 설정과 호환되도록 작성합니다.

```java
@RestController
@RequiredArgsConstructor
public class CredentialIssuanceController {

    private final CredentialService credentialService;

    // 동적 매핑 대상 (URL 어노테이션 없음)
    public ResponseEntity<Object> issueCredential(@RequestBody CredentialRequest request, 
                                                  @AuthenticationPrincipal Jwt accessToken) {
        Object response = credentialService.issueCredential(request, accessToken);
        return ResponseEntity.ok(response);
    }

    // PathVariable을 사용하는 경우
    public CredentialOfferResponse getCredentialOffer(@PathVariable("request_id") String requestId) {
        return credentialService.processCredentialOffer(requestId, ...);
    }

    // 알림 처리
    public ResponseEntity<Void> handleNotification(@RequestBody NotificationRequest request) {
        credentialService.handleNotification(request);
        return ResponseEntity.ok().build();
    }
}
```

---

## 5. DB 환경 구성 및 구현체 개발

데이터베이스 스키마 관리와 JPA 구현체를 개발합니다.

### 5.1 Liquibase 적용
DB 스키마 변경 이력을 관리하기 위해 Liquibase를 사용합니다.
`src/main/resources/db/changelog/master.xml` 파일을 진입점으로 하여 changeSet을 관리합니다.

**master.xml:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.6.xsd">

    <include file="changeset/set_master.xml" relativeToChangelogFile="true"/>
</databaseChangeLog>
```

**changeset/set_master.xml:**
```xml
<databaseChangeLog ...>
    <include file="set.0/project-init_oid4vc.xml" relativeToChangelogFile="true"/>
</databaseChangeLog>
```

**set.0/project-init_oid4vc.xml:** (테이블 생성 예시)
```xml
<databaseChangeLog ...>
    <changeSet id="create-t_oid4vc_offer" author="oid4vc-team">
        <createTable tableName="t_oid4vc_offer">
            <column name="id" type="BIGINT" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="offer_id" type="VARCHAR(64)">
                <constraints nullable="false" unique="true"/>
            </column>
            <column name="pre_auth_code" type="VARCHAR(128)"/>
            <column name="status" type="VARCHAR(20)">
                <constraints nullable="false"/>
            </column>
            <column name="expires_at" type="TIMESTAMP"/>
            <!-- ... 기타 컬럼 생략 -->
        </createTable>
    </changeSet>
</databaseChangeLog>
```

### 5.2 DB 구현체 개발 (Entity, Repository, Adapter)
SDK는 인터페이스(`Store`)만 정의하고, 실제 데이터 저장은 응용 서버가 구현해야 합니다. 여기서는 `CredentialOffer` 관련 구현 예시를 보여줍니다.

**1. Entity Class (Oid4vcOfferEntity.java):**
JPA Annotation을 사용하여 DB 테이블과 매핑합니다.
```java
@Getter
@Setter
@Entity
@Table(name = "t_oid4vc_offer") 
public class Oid4vcOfferEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offer_id", unique = true, nullable = false, length = 64)
    private String offerId;

    @Column(name = "pre_auth_code", length = 128)
    private String preAuthCode;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
```

**2. Repository Interface (Oid4vcOfferRepository.java):**
Spring Data JPA를 상속받아 CRUD 기능을 제공합니다.
```java
public interface Oid4vcOfferRepository extends JpaRepository<Oid4vcOfferEntity, Long> {
    Optional<Oid4vcOfferEntity> findByOfferId(String offerId);
}
```

**3. JPA Component / Adapter (JpaCredentialOfferStore.java):**
SDK의 `CredentialOfferStore` 인터페이스를 구현하여, SDK가 요청하는 데이터 저장/조회 로직을 Repository를 통해 처리합니다.
```java
@Component
@RequiredArgsConstructor
public class JpaCredentialOfferStore implements CredentialOfferStore {
    
    private final Oid4vcOfferRepository repository;

    @Override
    public void save(String id, CredentialOffer offer) {
        Oid4vcOfferEntity entity = new Oid4vcOfferEntity();
        entity.setOfferId(id);
        // ... DTO -> Entity 변환 로직
        repository.save(entity);
    }
    
    // ... 기타 메서드 구현
}
```

---

## 6. Provider 개발

발급에 필요한 사용자 정보와 암호화 키를 제공하는 Provider를 구현합니다.

### 6.1 UserDataProvider
사용자 ID(Subject)를 기반으로 VC에 들어갈 클레임 정보를 반환합니다. `UserClaimsStore`를 연동하여 동적 데이터를 우선 반환하도록 구성할 수 있습니다.

```java
@Service
@RequiredArgsConstructor
public class MockUserDataProvider implements UserDataProvider {
    private final UserClaimsStore userClaimsStore;

    @Override
    public Map<String, Object> getUserClaims(String userId, String credentialType) {
        // 1. 동적 Claim 저장소 확인 (우선순위 높음)
        Map<String, Object> dynamicClaims = userClaimsStore.getClaims(userId, credentialType);
        if (dynamicClaims != null && !dynamicClaims.isEmpty()) {
            return dynamicClaims;
        }

        // 2. 기본값 반환 (Fallback)
        return Map.of("given_name", "Gildong", "family_name", "Hong");
    }
}
```

### 6.2 KeyDataProvider
VC 서명에 사용할 키 정보를 반환합니다.
```java
@Service
public class MockKeyDataProvider implements KeyDataProvider {
    @Override
    public Map<String, Object> getKeyInfo(String userId, String credentialType) {
        // KMS 연동 또는 Keystore 조회
        return Map.of("privateKey", ..., "keyId", ...);
    }
}
```

---

## 7. 샘플 페이지 적용 (Overriding)

발급 화면(QR 코드 제공 등)은 SDK에서 제공하는 기본 페이지를 그대로 사용하거나, 필요에 따라 커스터마이징하여 오버라이딩할 수 있습니다.

### 7.1 View Controller 구현 (`IssuanceGatewayController`)
SDK의 `IssuanceGatewayService`를 사용하여 QR 코드 데이터를 생성하고 뷰를 반환합니다.

```java
@Slf4j
@Controller
@RequiredArgsConstructor
public class IssuanceGatewayController {

    private final IssuanceGatewayService issuanceGatewayService;

    // 1. 발급 페이지 진입 (View 반환)
    @GetMapping("/oid4vci/test")
    public String issue(Model model) {
        // 커스텀 뷰 이름: "issue" (src/main/resources/templates/issue.html)
        // SDK 기본 뷰를 원하면 해당 경로에 파일이 없어야 하거나 SDK 설정 따름
        return "issue"; 
    }

    // 2. QR 데이터 생성 API
    @PostMapping("/qr-data/generate-qr")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> generateQrData(@RequestBody Map<String, String> data) {
        String userId = data.get("userId");
        String grantType = data.get("grantType");

        try {
            // Credential Offer URI 생성
            Map<String, Object> response = issuanceGatewayService.generateCredentialOfferUri(userId, grantType);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating QR data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal Server Error", "message", e.getMessage()));
        }
    }
}
```

### 7.2 View 템플릿 (`issue.html`)
응용 서버의 `src/main/resources/templates/` 경로에 `issue.html` 파일을 생성하면, SDK의 기본 페이지 대신 이 파일이 렌더링됩니다.

```html
<!DOCTYPE html>
<html>
<head>
    <title>Custom Issuance Page</title>
</head>
<body>
    <h1>Credential Issuance</h1>
    <!-- JavaScript로 /qr-data/generate-qr 호출하여 QR 생성 -->
</body>
</html>
```
