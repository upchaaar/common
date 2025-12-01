# Upchaar Common Libraries
> Shared utilities and foundational components for Upchaar Hospital Management System microservices.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9+-blue.svg)](https://maven.apache.org/)

---

## 📋 Table of Contents
- [Overview](#-overview)
- [Features](#-features)
- [Installation](#-installation)
- [Architecture](#-architecture)
- [Components](#-components)
  - [API Response Handling](#api-response-handling)
  - [Security & Context](#security--context)
  - [Idempotency Pattern](#idempotency-pattern)
  - [Transactional Outbox Pattern](#transactional-outbox-pattern)
  - [Event Handling](#event-handling)
  - [PII/PHI Protection](#piiphi-protection)
  - [Utilities](#utilities)
- [Configuration](#-configuration)
- [Usage Examples](#-usage-examples)
- [Contributing](#-contributing)
- [License](#-license)
- [Support](#-support)
- [Version History](#-version-history)

---

## 🎯 Overview
`common-libs` is a shared library module providing cross-cutting concerns, utilities, and design pattern implementations for all Upchaar microservices. It ensures consistency, reduces code duplication, and enforces enterprise patterns across the distributed system.

Design Principles:
- DRY (Don't Repeat Yourself)
- Single Responsibility
- Dependency Inversion
- Auto-configuration ready
- Testable and extensible

For quick local setup, Git hooks, and how to start, see docs/GETTING_STARTED.md.

---

## ✨ Features
| Feature | Description |
|---------|-------------|
| Standardized Responses | RFC 7807 compliant error handling, unified success responses |
| Multi-tenancy Support | Thread-safe tenant context propagation |
| Idempotency | Prevent duplicate operations with configurable key-based checks |
| Transactional Outbox | Reliable event publishing with at-least-once delivery guarantee |
| Event-Driven Utilities | Event envelope, serialization, and versioning support |
| PII/PHI Masking | HIPAA/GDPR compliant data masking utilities |
| Distributed Tracing | Correlation ID propagation across services |
| Security Context | Thread-local user and role management |
| Auto-Configuration | Zero-config integration with Spring Boot services |

---

## 📦 Installation
Target runtime: Java 21 and Maven 3.9+.

Maven dependency (align coordinates with your actual pom):
```xml
<dependency>
    <groupId>tech.mayanktiwari.upchaar</groupId>
    <artifactId>common-libs</artifactId>
    <version>1.0.0</version>
</dependency>
```

Build from source:
```bash
git clone https://github.com/mayanktiwari/upchaar-common-libs.git
cd upchaar-common-libs
mvn -v   # verify Java 21 + Maven 3.9+
mvn clean install
```

---

## 🏗️ Architecture
```
common-libs/
├── api/                    # Response wrappers, error handling
├── security/               # Tenant/user context, role constants
├── idempotency/            # Idempotency filter, store interface
├── outbox/                 # Outbox entity, publisher interface
├── event/                  # Event envelope, serializer
├── pii/                    # PII masking utilities
├── util/                   # Correlation ID, general helpers
└── config/                 # Auto-configuration classes
```

Design Pattern: Hexagonal Architecture (Ports & Adapters)
- Ports: Interfaces (e.g., IdempotencyStore, OutboxPublisher)
- Adapters: Implementations in service repos (e.g., JPA adapters)

See docs/ARCHITECTURE.md for more details.

---

## 🧩 Components

### API Response Handling
ApiResponse<T>
- Fields:
    - success (boolean)
    - data (T)
    - message (String)
    - correlationId (String)
- Example:
```java
ApiResponse<PatientDto> response = ApiResponse.success(patientDto);
ApiResponse<List<Doctor>> response = ApiResponse.success(doctors, "Retrieved 10 doctors");
```

ApiError (RFC 7807)
- Fields: type, title, status, detail, instance, timestamp, correlationId, extensions
- Example:
```java
ApiError error = ApiError.of(400, "Validation Failed", "Email is required");
```

ProblemDetailMapper
- Methods:
```java
ProblemDetail pd = ProblemDetailMapper.notFound("Patient not found", "/api/v1/patients/123", correlationId);
ProblemDetail pd = ProblemDetailMapper.validationError(fieldErrors, "/api/v1/patients", correlationId);
ProblemDetail pd = ProblemDetailMapper.accessDenied("Insufficient permissions", "/api/v1/doctors", correlationId);
ProblemDetail pd = ProblemDetailMapper.conflict("Email already exists", "/api/v1/patients", correlationId);
ProblemDetail pd = ProblemDetailMapper.internalError("Database connection failed", "/api/v1/patients", correlationId);
```

---

### Security & Context
TenantContext (Thread-local)
- Methods:
```java
TenantContext.setTenantId("hospital-abc-123");
String tenantId = TenantContext.getTenantId();
boolean isSet = TenantContext.isSet();
TenantContext.requireTenant();
TenantContext.clear();
```
- Best practice:
```java
try {
    TenantContext.setTenantId(tenantId);
    // business logic
} finally {
    TenantContext.clear();
}
```

SecurityContext (Thread-local)
- Methods:
```java
SecurityContext.setUserContext("user-123", "DOCTOR");
String userId = SecurityContext.getUserId();
String role = SecurityContext.getRole();
boolean isSet = SecurityContext.isSet();
SecurityContext.requireUser();
SecurityContext.clear();
```

RoleConstants
- Constants:
```java
RoleConstants.HOSPITAL_ADMIN
RoleConstants.DOCTOR
RoleConstants.PATIENT
RoleConstants.ROLE_HOSPITAL_ADMIN
RoleConstants.ROLE_DOCTOR
RoleConstants.ROLE_PATIENT
```
- Usage:
```java
@PreAuthorize("hasRole(T(tech.mayanktiwari.upchaar.common.security.RoleConstants).HOSPITAL_ADMIN)")
public void createDoctor() { }
```

---

### Idempotency Pattern
IdempotencyProperties (prefix: upchaar.idempotency)
- enabled (boolean, default true)
- ttl (Duration, default PT24H)
- header-name (String, default Idempotency-Key)
- store-full-response (boolean, default false)
- max-response-size (int, default 5120)

IdempotencyFilter
- Flow:
    1. Extract Idempotency-Key
    2. Compute request hash (method + URI + tenant + user + body)
    3. Check IdempotencyStore
    4. If same hash → return cached response
    5. If different hash → 409 Conflict
    6. If not found → save placeholder and proceed

IdempotencyStore (implement in service repos)
- Methods:
```java
void save(IdempotencyRecord record);
Optional<IdempotencyRecord> findByKey(String tenantId, String idempotencyKey);
boolean exists(String tenantId, String idempotencyKey);
void deleteExpired();
```
- Example (service repo):
```java
@Repository
public interface IdempotencyJpaRepository extends JpaRepository<IdempotencyEntity, String> {
    Optional<IdempotencyEntity> findByTenantIdAndIdempotencyKey(String tenantId, String key);
}

@Service
public class IdempotencyStoreImpl implements IdempotencyStore {
    @Autowired
    private IdempotencyJpaRepository repo;
    // implement methods
}
```

---

### Transactional Outbox Pattern
OutboxProperties (prefix: upchaar.outbox)
- enabled (boolean, default true)
- poll-interval (Duration, default PT2S)
- batch-size (int, default 100)
- max-retry-attempts (int, default 5)
- initial-retry-delay (Duration, default PT1S)
- max-retry-delay (Duration, default PT60S)
- retention-period (Duration, default P7D)
- auto-cleanup (boolean, default true)
- cleanup-schedule (String, default 0 0 2 * * ?)

OutboxEntity (JPA)
- Fields: id, aggregateType, aggregateId, eventType, eventVersion, tenantId, payload, correlationId, causationId, userId, createdAt, publishedAt, status, retryCount, lastError, nextRetryAt
- Status:
```java
public enum OutboxStatus { PENDING, PUBLISHED, FAILED, PROCESSING }
```

OutboxPublisher (implement in service repos)
- Methods:
```java
void save(EventEnvelope<?> envelope, String aggregateType, String aggregateId);
int publishPending();
boolean publish(OutboxEntity outbox);
void markAsPublished(String outboxId);
void markAsFailed(String outboxId, String errorMessage);
void scheduleRetry(String outboxId, String errorMessage);
int deletePublished(int olderThanDays);
long getPendingCount();
List<OutboxEntity> getFailedEvents(int limit);
```
- Implementation pattern:
```java
@Service
@RequiredArgsConstructor
public class OutboxPublisherImpl implements OutboxPublisher {
    private final OutboxRepository repo;
    private final SnsClient snsClient;

    @Transactional
    public void save(EventEnvelope<?> envelope, String aggregateType, String aggregateId) {
        OutboxEntity entity = OutboxEntity.builder()
            .aggregateType(aggregateType)
            .aggregateId(aggregateId)
            .eventType(envelope.getEventType())
            // map remaining fields
            .build();
        repo.save(entity);
    }

    @Scheduled(fixedDelayString = "${upchaar.outbox.poll-interval}")
    public int publishPending() {
        List<OutboxEntity> pending = repo.findPendingEvents(batchSize);
        int published = 0;
        for (OutboxEntity outbox : pending) {
            if (publish(outbox)) {
                markAsPublished(outbox.getId());
                published++;
            }
        }
        return published;
    }
}
```

---

### Event Handling
EventEnvelope<T>
- Fields: eventId, eventType, version, tenantId, correlationId, causationId, timestamp, userId, payload
- Factory:
```java
EventEnvelope<PatientRegistered> envelope = EventEnvelope.of(
    "patient.registered",
    "v1",
    "hospital-123",
    new PatientRegistered(patientId, firstName, lastName)
);
```

EventSerializer
- Methods:
```java
String json = EventSerializer.serialize(envelope);
EventEnvelope<PatientRegistered> envelope = EventSerializer.deserialize(json, PatientRegistered.class);
String json = EventSerializer.toJson(anyObject);
MyClass obj = EventSerializer.fromJson(json, MyClass.class);
```

---

### PII/PHI Protection
PiiMasker
- Methods:
    - maskEmail(email) → j***@e***.com
    - maskPhone(phone) → ****7890
    - maskName(name) → J*** D***
    - maskNationalId(id) → ****6789
    - maskAddress(addr) → ***
    - maskGeneric(value) → s***3
- Example:
```java
@Data
public class PatientResponseDto {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;

    @JsonProperty("email")
    public String getMaskedEmail() { return PiiMasker.maskEmail(this.email); }

    @JsonProperty("phone")
    public String getMaskedPhone() { return PiiMasker.maskPhone(this.phone); }
}
```
- Logging:
```java
log.info("Patient registered: id={}, email={}",
    patientId,
    PiiMasker.maskEmail(patient.getEmail())
);
```

---

### Utilities
CorrelationIdFilter
- Behavior:
    1. Extract X-Correlation-Id header
    2. If absent, generate UUID
    3. Put into MDC as correlationId
    4. Add to response header
    5. Clear MDC afterwards
- Logback config:
```xml
<pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} [%X{correlationId}] - %msg%n</pattern>
```
- Usage:
```java
String correlationId = MDC.get("correlationId");
```

---

## ⚙️ Configuration
application.yml
```yaml
# Idempotency Configuration
upchaar:
  idempotency:
    enabled: true
    ttl: PT24H
    header-name: Idempotency-Key
    store-full-response: false
    max-response-size: 5120

  # Outbox Configuration
  outbox:
    enabled: true
    poll-interval: PT2S
    batch-size: 100
    max-retry-attempts: 5
    initial-retry-delay: PT1S
    max-retry-delay: PT60S
    retention-period: P7D
    auto-cleanup: true
    cleanup-schedule: "0 0 2 * * ?"
```

Environment overrides
- Dev:
```yaml
upchaar:
  idempotency:
    ttl: PT1H
  outbox:
    poll-interval: PT5S
```

- Prod:
```yaml
upchaar:
  outbox:
    poll-interval: PT1S
    batch-size: 500
```

---

## 📚 Usage Examples

Example 1: REST Controller with Idempotency
```java
@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {
    @PostMapping
    public ResponseEntity<ApiResponse<PatientDto>> createPatient(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreatePatientRequest request) {
        TenantContext.setTenantId(tenantId);
        try {
            PatientDto patient = patientService.create(request);
            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(patient, "Patient created successfully"));
        } finally {
            TenantContext.clear();
        }
    }
}
```

Example 2: Publishing Domain Events
```java
@Service
@RequiredArgsConstructor
public class PatientService {
    private final PatientRepository patientRepo;
    private final OutboxPublisher outboxPublisher;

    @Transactional
    public Patient register(CreatePatientRequest request) {
        Patient patient = patientRepo.save(mapToEntity(request));

        PatientRegistered payload = new PatientRegistered(
            patient.getId(),
            patient.getFirstName(),
            patient.getLastName(),
            patient.getMrn()
        );

        EventEnvelope<PatientRegistered> envelope = EventEnvelope.of(
            "patient.registered",
            "v1",
            TenantContext.getTenantId(),
            payload
        );
        envelope.setCorrelationId(MDC.get("correlationId"));
        envelope.setUserId(SecurityContext.getUserId());

        outboxPublisher.save(envelope, "Patient", patient.getId());
        return patient;
    }
}
```

Example 3: Exception Handling
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
            EntityNotFoundException ex,
            HttpServletRequest request) {
        String correlationId = MDC.get("correlationId");
        ProblemDetail pd = ProblemDetailMapper.notFound(
            ex.getMessage(),
            request.getRequestURI(),
            correlationId
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );
        String correlationId = MDC.get("correlationId");
        ProblemDetail pd = ProblemDetailMapper.validationError(
            errors,
            request.getRequestURI(),
            correlationId
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }
}
```

Example 4: Consuming Events
```java
@Service
@Slf4j
public class PatientEventConsumer {
    @SqsListener(value = "${aws.sqs.queue.patient-events}")
    public void handlePatientRegistered(String message) {
        try {
            EventEnvelope<PatientRegistered> envelope =
                EventSerializer.deserialize(message, PatientRegistered.class);

            TenantContext.setTenantId(envelope.getTenantId());
            MDC.put("correlationId", envelope.getCorrelationId());

            log.info("Processing patient.registered event: patientId={}",
                envelope.getPayload().getPatientId());

            // Business logic
        } catch (Exception e) {
            log.error("Failed to process event", e);
            throw e; // DLQ after retries
        } finally {
            TenantContext.clear();
            MDC.clear();
        }
    }
}
```

---

## 🤝 Contributing
We welcome contributions! Please read CONTRIBUTING.md for:
- Development workflow
- Commit message conventions
- Pre-push build checks
- How to run scripts/setup-hooks.sh for Git hooks
- Coding standards and testing

---

## 📄 License
Proprietary License  
© 2024 Mayank Tiwari - Upchaar Hospital Management System  
This software is confidential and proprietary. Unauthorized copying, modification, distribution, or use is strictly prohibited.  
For licensing inquiries: [devmayanktiwari@gmail.com](mailto:devmayanktiwari@gmail.com)

---

## 📞 Support
- Issues: https://github.com/mayanktiwari/upchaar-common-libs/issues
- Email: [devmayanktiwari@gmail.com](mailto:devmayanktiwari@gmail.com)

---

## 🔖 Version History
| Version | Date       | Changes |
|---------|------------|---------|
| 1.0.0 | 2025-11-17 | Initial release with core components |

Built with ❤️ 