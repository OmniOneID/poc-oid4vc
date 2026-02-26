# OID4VCI SDK Integration Guide

This document describes how to build a Verifiable Credential (VC) issuance system based on OID4VC by integrating `issuer-sdk` and `authorization-sdk` into an application server (Issuer Server).

---

## 0. Adding SDK Dependencies (Build Configuration)

Configure `build.gradle` to add all libraries, including `issuer-sdk` and `authorization-sdk`, as JAR files.

### 0.1 Placing JAR Files
Place the prepared SDK and related library JAR files in the `libs` directory of the project root.
*   `libs/issuer-sdk-x.x.x.jar`
*   `libs/authorization-sdk-x.x.x.jar`
*   `libs/did-wallet-sdk-x.x.x.jar` (and other dependencies)

### 0.2 Dependency Settings (`build.gradle`)
Include all JAR files in the `libs` directory as dependencies.

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
    
    // ... other required dependencies
}
```

---

## 1. Applying Property Files (Configuration)

Manage SDK and infrastructure settings using Spring Boot's configuration file separation feature.

### 1.1 `application.yml` (Main Configuration)
Import property files for each module in the main configuration file.

```yaml
spring:
  config:
    import:
      - "classpath:application-issuer-sdk.yml"       # Issuer SDK settings
      - "classpath:application-authorization-server.yml" # Authorization Server SDK settings
      - "classpath:application-database.yml"         # DB settings
```

### 1.2 `application-issuer-sdk.yml`
Define metadata paths, external service URLs, and security settings required for Issuer SDK operation.

```yaml
issuer:
  base-url: http://10.48.17.124:8080
  data-dir: /path/to/data # Directory containing metadata and certificates
  metadata-file-path: metadata/issuer_meta_univ_local.json # Path to metadata file (relative to data-dir)
  root-ca-file-name: rootCA.crt # (Optional) Root CA for x5c proof verification
  sdk:
    credential-configurations:
      StudentID:
        format: open-did-vc
        identifiers:
          - TEC
          - UCR

# Authorization Server and Resource Server security settings
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
Define settings related to the Authorization Server. (OAuth2 Client and integration settings)

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
Configure database connection information.

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/issuer_db
    username: user
    password: password
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate # 'validate' is recommended when using Liquibase
```

---

## 2. Applying Issuer Metadata

Define and apply Issuer Metadata, which is important in the OID4VC protocol.

### 2.1 Writing the Metadata File (`issuer_meta_univ_local.json`)
Located in the `metadata/` directory, it defines each **Endpoint URL** and **supported Credential settings**.

```json
{
  "credential_issuer": "http://10.48.17.124:8080",
  "credential_offer_endpoint": "http://10.48.17.124:8080/credential-offer",
  "credential_endpoint": "http://10.48.17.124:8080/credential",
  "nonce_endpoint": "http://10.48.17.124:8080/nonce",
  ...
}
```
*   **Important:** The Endpoint URLs defined here are used when the SDK dynamically maps them to controllers.

---

## 3. Configuration Development

### 3.1 Security Filter Chain (`IssuerSecurityConfig`)
Configure Access Token validation logic through OAuth2 Resource Server settings.

```java
@Configuration
@EnableWebSecurity
public class IssuerSecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/credential-offer/**", "/.well-known/**").permitAll() // Public Endpoints
                .anyRequest().authenticated() // Others require a token
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
```

### 3.2 Request Mapping (Dynamic Mapping)
Read the URLs defined in the metadata and dynamically connect them to controller methods.

```java
@Configuration
@RequiredArgsConstructor
public class DynamicEndpointConfig {
    
    private final CredentialService credentialService;
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    // ... Inject Controllers

    @EventListener(ApplicationReadyEvent.class)
    public void registerDynamicEndpoints() {
        IssuerMetadataResponse metadata = credentialService.getIssuerMetadata();
        
        // Read metadata URLs and map them to controller methods
        registerEndpoint(metadata.getCredentialOfferEndpoint(), RequestMethod.GET, "getCredentialOffer", String.class);
        registerEndpoint(metadata.getCredentialEndpoint(), RequestMethod.POST, "issueCredential", CredentialRequest.class, Jwt.class);
        registerEndpoint(metadata.getNotificationEndpoint(), RequestMethod.POST, "handleNotification", NotificationRequest.class);
        // ... Register other endpoints
    }
}
```

---

## 4. Controller Development

Implement controllers that call the SDK's `CredentialService` to handle actual business logic. For dynamic mapping, annotations like `@PostMapping` should be removed from the method level or written to be compatible with dynamic configuration.

```java
@RestController
@RequiredArgsConstructor
public class CredentialIssuanceController {

    private final CredentialService credentialService;

    // Target for dynamic mapping (no URL annotation)
    public ResponseEntity<Object> issueCredential(@RequestBody CredentialRequest request, 
                                                  @AuthenticationPrincipal Jwt accessToken) {
        Object response = credentialService.issueCredential(request, accessToken);
        return ResponseEntity.ok(response);
    }

    // When using PathVariable
    public CredentialOfferResponse getCredentialOffer(@PathVariable("request_id") String requestId) {
        return credentialService.processCredentialOffer(requestId, ...);
    }

    // Notification handling
    public ResponseEntity<Void> handleNotification(@RequestBody NotificationRequest request) {
        credentialService.handleNotification(request);
        return ResponseEntity.ok().build();
    }
}
```

---

## 5. DB Environment Setup and Implementation Development

Develop database schema management and JPA implementations.

### 5.1 Applying Liquibase
Use Liquibase to manage DB schema change history. Use `src/main/resources/db/changelog/master.xml` as the entry point to manage changeSets.

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

**set.0/project-init_oid4vc.xml:** (Example of table creation)
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
            <!-- ... other columns omitted -->
        </createTable>
    </changeSet>
</databaseChangeLog>
```

### 5.2 Developing DB Implementations (Entity, Repository, Adapter)
The SDK only defines the interface (`Store`), and the application server must implement the actual data storage. Here is an example of a `CredentialOffer` related implementation.

**1. Entity Class (Oid4vcOfferEntity.java):**
Map to the DB table using JPA annotations.
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
Extend Spring Data JPA to provide CRUD functionality.
```java
public interface Oid4vcOfferRepository extends JpaRepository<Oid4vcOfferEntity, Long> {
    Optional<Oid4vcOfferEntity> findByOfferId(String offerId);
}
```

**3. JPA Component / Adapter (JpaCredentialOfferStore.java):**
Implement the SDK's `CredentialOfferStore` interface to handle the data storage/retrieval logic requested by the SDK through the Repository.
```java
@Component
@RequiredArgsConstructor
public class JpaCredentialOfferStore implements CredentialOfferStore {
    
    private final Oid4vcOfferRepository repository;

    @Override
    public void save(String id, CredentialOffer offer) {
        Oid4vcOfferEntity entity = new Oid4vcOfferEntity();
        entity.setOfferId(id);
        // ... DTO -> Entity conversion logic
        repository.save(entity);
    }
    
    // ... Implement other methods
}
```

---

## 6. Provider Development

Implement Providers that provide user information and encryption keys required for issuance.

### 6.1 UserDataProvider
Returns claim information to be included in the VC based on the user ID (Subject). You can configure it to prioritize dynamic data by integrating with `UserClaimsStore`.

```java
@Service
@RequiredArgsConstructor
public class MockUserDataProvider implements UserDataProvider {
    private final UserClaimsStore userClaimsStore;

    @Override
    public Map<String, Object> getUserClaims(String userId, String credentialType) {
        // 1. Check dynamic claim store (Higher priority)
        Map<String, Object> dynamicClaims = userClaimsStore.getClaims(userId, credentialType);
        if (dynamicClaims != null && !dynamicClaims.isEmpty()) {
            return dynamicClaims;
        }

        // 2. Return default values (Fallback)
        return Map.of("given_name", "Gildong", "family_name", "Hong");
    }
}
```

### 6.2 KeyDataProvider
Returns key information to be used for VC signing.
```java
@Service
public class MockKeyDataProvider implements KeyDataProvider {
    @Override
    public Map<String, Object> getKeyInfo(String userId, String credentialType) {
        // KMS integration or Keystore lookup
        return Map.of("privateKey", ..., "keyId", ...);
    }
}
```

---

## 7. Sample Page Application (Overriding)

The issuance screen (providing QR codes, etc.) can use the default page provided by the SDK or be overridden by customizing it as needed.

### 7.1 View Controller Implementation (`IssuanceGatewayController`)
Generate QR code data using the SDK's `IssuanceGatewayService` and return the view.

```java
@Slf4j
@Controller
@RequiredArgsConstructor
public class IssuanceGatewayController {

    private final IssuanceGatewayService issuanceGatewayService;

    // 1. Enter Issuance Page (Returns View)
    @GetMapping("/oid4vci/test")
    public String issue(Model model) {
        // Custom view name: "issue" (src/main/resources/templates/issue.html)
        // If you want the SDK default view, the file should not exist at that path or follow SDK settings
        return "issue"; 
    }

    // 2. QR Data Generation API
    @PostMapping("/qr-data/generate-qr")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> generateQrData(@RequestBody Map<String, String> data) {
        String userId = data.get("userId");
        String grantType = data.get("grantType");

        try {
            // Generate Credential Offer URI
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

### 7.2 View Template (`issue.html`)
Creating an `issue.html` file in the application server's `src/main/resources/templates/` path will render this file instead of the SDK's default page.

```html
<!DOCTYPE html>
<html>
<head>
    <title>Custom Issuance Page</title>
</head>
<body>
    <h1>Credential Issuance</h1>
    <!-- Call /qr-data/generate-qr with JavaScript to generate QR -->
</body>
</html>
```
