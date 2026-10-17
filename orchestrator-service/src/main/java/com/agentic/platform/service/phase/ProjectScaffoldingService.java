package com.agentic.platform.service.phase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Service responsible for scaffolding clean architecture source code (Spring Boot 3 backend,
 * Next.js 14 frontend, container manifests, and TDD test suites) dynamically tailored to each project domain.
 * Eliminates all static/hardcoded legacy templates.
 */
@Service
public class ProjectScaffoldingService {

    private static final Logger log = LoggerFactory.getLogger(ProjectScaffoldingService.class);
    private final SdlcJournalService journalService;

    public ProjectScaffoldingService(SdlcJournalService journalService) {
        this.journalService = journalService;
    }

    public record DomainProfile(
            String slug,
            String packageName,
            String packageDir,
            String applicationClassName,
            String primaryEntityName,
            String primaryEntityLower,
            String secondaryEntityName,
            String serviceName,
            String controllerName,
            String useCasePortName,
            String repoPortName,
            String appTitle
    ) {}

    public DomainProfile resolveDomainProfile(String projectId, String taskName, String description) {
        String combined = ((taskName != null ? taskName : "") + " " + (description != null ? description : "")).toLowerCase();

        String slug;
        String primaryEntity;
        String secondaryEntity;
        String servicePrefix;

        if (combined.contains("saas") || combined.contains("subscri") || combined.contains("billing")) {
            slug = "saas-billing";
            primaryEntity = "Subscription";
            secondaryEntity = "UsageRecord";
            servicePrefix = "SubscriptionBilling";
        } else if (combined.contains("melsp") || combined.contains("forest") || combined.contains("restoration")) {
            slug = "melsp-environment";
            primaryEntity = "Project";
            secondaryEntity = "Indicator";
            servicePrefix = "ProjectManagement";
        } else if (combined.contains("order") || combined.contains("checkout") || combined.contains("ecommerce")) {
            slug = "order-commerce";
            primaryEntity = "Order";
            secondaryEntity = "OrderItem";
            servicePrefix = "OrderProcessing";
        } else if (combined.contains("auth") || combined.contains("identity") || combined.contains("user")) {
            slug = "identity-auth";
            primaryEntity = "Account";
            secondaryEntity = "SessionToken";
            servicePrefix = "AccountIdentity";
        } else {
            String cleanTask = (taskName != null ? taskName : projectId)
                    .replaceAll("[^a-zA-Z0-9]", " ")
                    .trim();
            String[] parts = cleanTask.split("\\s+");
            String mainWord = parts.length > 0 && !parts[0].isBlank() ? parts[0] : "Domain";
            mainWord = Character.toUpperCase(mainWord.charAt(0)) + mainWord.substring(1).toLowerCase();

            slug = mainWord.toLowerCase() + "-platform";
            primaryEntity = mainWord;
            secondaryEntity = mainWord + "Audit";
            servicePrefix = mainWord + "Management";
        }

        String packageName = "com.agentic." + slug.replace("-", ".");
        String packageDir = packageName.replace(".", "/");
        String applicationClassName = Character.toUpperCase(slug.charAt(0))
                + slug.substring(1).replace("-", "") + "Application";

        return new DomainProfile(
                slug,
                packageName,
                packageDir,
                applicationClassName,
                primaryEntity,
                primaryEntity.toLowerCase(),
                secondaryEntity,
                servicePrefix + "Service",
                primaryEntity + "Controller",
                "Manage" + primaryEntity + "UseCase",
                primaryEntity + "RepositoryPort",
                taskName != null ? taskName : projectId
        );
    }

    public void scaffoldTddTestSuites(String projectId, String taskName, String description) {
        try {
            Path projectBase = journalService.resolveProjectBase(projectId);
            Path testsDir = projectBase.resolve("tests");
            Path backendTests = testsDir.resolve("backend");
            Path frontendTests = testsDir.resolve("frontend");
            Files.createDirectories(backendTests);
            Files.createDirectories(frontendTests);

            DomainProfile profile = resolveDomainProfile(projectId, taskName, description);

            Path serviceTest = backendTests.resolve(profile.serviceName() + "Test.java");
            Files.writeString(serviceTest, String.format("""
package %s.service;

import %s.application.service.%s;
import %s.domain.model.%s;
import %s.domain.port.out.%s;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("%s TDD Specification")
class %sTest {

    @Mock
    private %s repositoryPort;

    private %s service;

    @BeforeEach
    void setUp() {
        service = new %s(repositoryPort);
    }

    @Test
    @DisplayName("Should successfully create %s when valid specifications provided")
    void shouldCreate%sSuccessfully() {
        when(repositoryPort.findById("test-id-101")).thenReturn(Optional.empty());
        when(repositoryPort.save(any(%s.class))).thenAnswer(inv -> inv.getArgument(0));

        %s created = service.create("test-id-101", "Primary %s Record", "ACTIVE");

        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo("test-id-101");
        assertThat(created.getStatus()).isEqualTo("ACTIVE");
        verify(repositoryPort, times(1)).save(any(%s.class));
    }

    @Test
    @DisplayName("Should reject creation when duplicate ID exists")
    void shouldFailWhenDuplicateExists() {
        %s existing = new %s("test-id-101", "Existing", "ACTIVE", Instant.now());
        when(repositoryPort.findById("test-id-101")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.create("test-id-101", "Duplicate Entry", "ACTIVE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }
}
""",
                    profile.packageName(),
                    profile.packageName(), profile.serviceName(),
                    profile.packageName(), profile.primaryEntityName(),
                    profile.packageName(), profile.repoPortName(),
                    profile.serviceName(),
                    profile.serviceName(),
                    profile.repoPortName(),
                    profile.serviceName(),
                    profile.serviceName(),
                    profile.primaryEntityName(),
                    profile.primaryEntityName(),
                    profile.primaryEntityName(),
                    profile.primaryEntityName(),
                    profile.primaryEntityName(),
                    profile.primaryEntityName(),
                    profile.primaryEntityName(),
                    profile.primaryEntityName()
            ));

            // Domain Aggregate Unit Test (Invariant boundaries)
            Path domainTest = backendTests.resolve(profile.primaryEntityName() + "DomainTest.java");
            Files.writeString(domainTest, String.format("""
package %s.domain;

import %s.domain.model.%s;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("%s Domain Aggregate Invariant Tests")
class %sDomainTest {

    @Test
    @DisplayName("Should initialize domain entity with valid attributes")
    void shouldInitializeWithValidAttributes() {
        Instant now = Instant.now();
        %s entity = new %s("id-001", "Primary Domain", "ACTIVE", now);

        assertThat(entity.getId()).isEqualTo("id-001");
        assertThat(entity.getName()).isEqualTo("Primary Domain");
        assertThat(entity.getStatus()).isEqualTo("ACTIVE");
        assertThat(entity.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should support status mutation through setter")
    void shouldMutateStatus() {
        %s entity = new %s("id-002", "Updatable", "PENDING", Instant.now());
        entity.setStatus("ACTIVE");
        assertThat(entity.getStatus()).isEqualTo("ACTIVE");
    }
}
""", profile.packageName(), profile.packageName(), profile.primaryEntityName(),
                    profile.primaryEntityName(), profile.primaryEntityName(),
                    profile.primaryEntityName(), profile.primaryEntityName(),
                    profile.primaryEntityName(), profile.primaryEntityName()));

            // Tenant Context Thread-Local Security Test
            Path tenantTest = backendTests.resolve("TenantContextHolderTest.java");
            Files.writeString(tenantTest, String.format("""
package %s.domain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TenantContextHolder Thread-Local Isolation Tests")
class TenantContextHolderTest {

    public static class TenantContextHolder {
        private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();
        public static void setTenantId(String tenantId) { CURRENT_TENANT.set(tenantId); }
        public static Optional<String> getTenantId() { return Optional.ofNullable(CURRENT_TENANT.get()); }
        public static void clear() { CURRENT_TENANT.remove(); }
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Should isolate and clear tenant context across invocations")
    void shouldIsolateTenantContext() {
        TenantContextHolder.setTenantId("tenant-alpha");
        assertThat(TenantContextHolder.getTenantId()).contains("tenant-alpha");

        TenantContextHolder.clear();
        assertThat(TenantContextHolder.getTenantId()).isEmpty();
    }
}
""", profile.packageName()));

            Path testTsConfig = testsDir.resolve("tsconfig.json");
            if (!Files.exists(testTsConfig)) {
                Files.writeString(testTsConfig, """
{
  "compilerOptions": {
    "target": "ES2022",
    "lib": ["dom", "dom.iterable", "esnext"],
    "allowJs": true,
    "skipLibCheck": true,
    "strict": false,
    "noEmit": true,
    "esModuleInterop": true,
    "module": "esnext",
    "moduleResolution": "bundler",
    "resolveJsonModule": true,
    "isolatedModules": true,
    "jsx": "react-jsx"
  },
  "include": ["**/*.ts", "**/*.tsx"],
  "exclude": ["node_modules"]
}
""");
            }

            Path testPkgJson = testsDir.resolve("package.json");
            Files.writeString(testPkgJson, String.format("""
{
  "name": "%s-tests",
  "version": "1.0.0",
  "private": true
}
""", profile.slug()));

            log.info("Successfully scaffolded domain-tailored TDD test suites for project: {}", projectId);
        } catch (Exception ex) {
            log.error("Failed to scaffold TDD test suites for project {}: {}", projectId, ex.getMessage());
        }
    }

    public void scaffoldSourceCode(String projectId, String taskName, String description) {
        try {
            Path projectBase = journalService.resolveProjectBase(projectId);
            Path srcDir = projectBase.resolve("src");
            Path backendDir = srcDir.resolve("backend");
            Path frontendDir = srcDir.resolve("frontend");

            DomainProfile profile = resolveDomainProfile(projectId, taskName, description);

            Path backendSrc = backendDir.resolve("src/main/java/" + profile.packageDir());
            Path modelDir = backendSrc.resolve("domain/model");
            Path portInDir = backendSrc.resolve("domain/port/in");
            Path portOutDir = backendSrc.resolve("domain/port/out");
            Path appServiceDir = backendSrc.resolve("application/service");
            Path webAdapterDir = backendSrc.resolve("adapter/in/web");
            Path persistenceDir = backendSrc.resolve("adapter/out/persistence");
            Path resDir = backendDir.resolve("src/main/resources");

            Files.createDirectories(modelDir);
            Files.createDirectories(portInDir);
            Files.createDirectories(portOutDir);
            Files.createDirectories(appServiceDir);
            Files.createDirectories(webAdapterDir);
            Files.createDirectories(persistenceDir);
            Files.createDirectories(resDir);

            // 1. Maven pom.xml
            Files.writeString(backendDir.resolve("pom.xml"), String.format("""
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.4</version>
        <relativePath/>
    </parent>
    <groupId>%s</groupId>
    <artifactId>%s-backend</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <name>%s-backend</name>
    <description>%s Backend Service</description>
    <properties>
        <java.version>21</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
""", profile.packageName(), profile.slug(), profile.slug(), profile.appTitle()));

            // 2. Spring Boot Main Application
            Files.writeString(backendSrc.resolve(profile.applicationClassName() + ".java"), String.format("""
package %s;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class %s {
    public static void main(String[] args) {
        SpringApplication.run(%s.class, args);
    }
}
""", profile.packageName(), profile.applicationClassName(), profile.applicationClassName()));

            // 3. Domain Model
            Files.writeString(modelDir.resolve(profile.primaryEntityName() + ".java"), String.format("""
package %s.domain.model;

import java.time.Instant;

public class %s {
    private String id;
    private String name;
    private String status;
    private Instant createdAt;

    public %s() {}

    public %s(String id, String name, String status, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
""", profile.packageName(), profile.primaryEntityName(), profile.primaryEntityName(), profile.primaryEntityName()));

            // 4. Inbound Port (UseCase)
            Files.writeString(portInDir.resolve(profile.useCasePortName() + ".java"), String.format("""
package %s.domain.port.in;

import %s.domain.model.%s;
import java.util.Optional;

public interface %s {
    %s create(String id, String name, String status);
    Optional<%s> findById(String id);
}
""", profile.packageName(), profile.packageName(), profile.primaryEntityName(), profile.useCasePortName(),
                    profile.primaryEntityName(), profile.primaryEntityName()));

            // 5. Outbound Port (Repository)
            Files.writeString(portOutDir.resolve(profile.repoPortName() + ".java"), String.format("""
package %s.domain.port.out;

import %s.domain.model.%s;
import java.util.Optional;

public interface %s {
    %s save(%s entity);
    Optional<%s> findById(String id);
}
""", profile.packageName(), profile.packageName(), profile.primaryEntityName(), profile.repoPortName(),
                    profile.primaryEntityName(), profile.primaryEntityName(), profile.primaryEntityName()));

            // 6. Application Service
            Files.writeString(appServiceDir.resolve(profile.serviceName() + ".java"), String.format("""
package %s.application.service;

import %s.domain.model.%s;
import %s.domain.port.in.%s;
import %s.domain.port.out.%s;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class %s implements %s {

    private final %s repository;

    public %s(%s repository) {
        this.repository = repository;
    }

    @Override
    public %s create(String id, String name, String status) {
        if (repository.findById(id).isPresent()) {
            throw new IllegalArgumentException("Entity with ID " + id + " already exists");
        }
        %s entity = new %s(id, name, status, Instant.now());
        return repository.save(entity);
    }

    @Override
    public Optional<%s> findById(String id) {
        return repository.findById(id);
    }
}
""", profile.packageName(), profile.packageName(), profile.primaryEntityName(),
                    profile.packageName(), profile.useCasePortName(),
                    profile.packageName(), profile.repoPortName(),
                    profile.serviceName(), profile.useCasePortName(),
                    profile.repoPortName(),
                    profile.serviceName(), profile.repoPortName(),
                    profile.primaryEntityName(),
                    profile.primaryEntityName(), profile.primaryEntityName(),
                    profile.primaryEntityName()));

            // 7. REST Controller Web Adapter
            Files.writeString(webAdapterDir.resolve(profile.controllerName() + ".java"), String.format("""
package %s.adapter.in.web;

import %s.domain.model.%s;
import %s.domain.port.in.%s;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/%s")
public class %s {

    private final %s useCase;

    public %s(%s useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/{id}")
    public ResponseEntity<%s> getById(@PathVariable String id) {
        return useCase.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<%s> create(@RequestParam String id, @RequestParam String name, @RequestParam(defaultValue = "ACTIVE") String status) {
        return ResponseEntity.ok(useCase.create(id, name, status));
    }
}
""", profile.packageName(), profile.packageName(), profile.primaryEntityName(),
                    profile.packageName(), profile.useCasePortName(),
                    profile.primaryEntityLower(), profile.controllerName(),
                    profile.useCasePortName(),
                    profile.controllerName(), profile.useCasePortName(),
                    profile.primaryEntityName(),
                    profile.primaryEntityName()));

            // 7.5. Tenant Context Holder in Domain
            Files.writeString(modelDir.resolve("TenantContextHolder.java"), String.format("""
package %s.domain.model;

import java.util.Optional;

public class TenantContextHolder {
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    public static void setTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static Optional<String> getTenantId() {
        return Optional.ofNullable(CURRENT_TENANT.get());
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
""", profile.packageName()));

            // 7.6. Maven Unit Tests (src/test/java)
            Path backendTestSrc = backendDir.resolve("src/test/java/" + profile.packageDir());
            Path testAppServiceDir = backendTestSrc.resolve("application/service");
            Path testModelDir = backendTestSrc.resolve("domain/model");
            Files.createDirectories(testAppServiceDir);
            Files.createDirectories(testModelDir);

            Files.writeString(testAppServiceDir.resolve(profile.serviceName() + "Test.java"), String.format("""
package %s.application.service;

import %s.domain.model.%s;
import %s.domain.port.out.%s;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("%s Application Service Unit Tests")
class %sTest {

    @Mock
    private %s repository;

    private %s service;

    @BeforeEach
    void setUp() {
        service = new %s(repository);
    }

    @Test
    @DisplayName("Should successfully create %s when valid specifications provided")
    void shouldCreateSuccessfully() {
        when(repository.findById("test-id-1")).thenReturn(Optional.empty());
        when(repository.save(any(%s.class))).thenAnswer(inv -> inv.getArgument(0));

        %s created = service.create("test-id-1", "Active Domain", "ACTIVE");

        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo("test-id-1");
        verify(repository, times(1)).save(any(%s.class));
    }

    @Test
    @DisplayName("Should reject creation when duplicate ID exists")
    void shouldFailWhenDuplicateExists() {
        %s existing = new %s("test-id-1", "Existing", "ACTIVE", Instant.now());
        when(repository.findById("test-id-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.create("test-id-1", "Duplicate Entry", "ACTIVE"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
""", profile.packageName(), profile.packageName(), profile.primaryEntityName(),
                    profile.packageName(), profile.repoPortName(),
                    profile.serviceName(), profile.serviceName(),
                    profile.repoPortName(), profile.serviceName(),
                    profile.serviceName(), profile.primaryEntityName(),
                    profile.primaryEntityName(), profile.primaryEntityName(),
                    profile.primaryEntityName(), profile.primaryEntityName(),
                    profile.primaryEntityName()));

            Files.writeString(testModelDir.resolve(profile.primaryEntityName() + "DomainTest.java"), String.format("""
package %s.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("%s Aggregate Invariant Tests")
class %sDomainTest {

    @Test
    @DisplayName("Should initialize domain aggregate with valid invariants")
    void shouldInitializeWithValidInvariants() {
        Instant now = Instant.now();
        %s entity = new %s("dom-101", "Domain Agg", "ACTIVE", now);

        assertThat(entity.getId()).isEqualTo("dom-101");
        assertThat(entity.getName()).isEqualTo("Domain Agg");
        assertThat(entity.getStatus()).isEqualTo("ACTIVE");
    }
}
""", profile.packageName(), profile.primaryEntityName(), profile.primaryEntityName(),
                    profile.primaryEntityName(), profile.primaryEntityName()));

            // 8. Backend Dockerfile
            Files.writeString(backendDir.resolve("Dockerfile"), """
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
""");

            // 9. Frontend Next.js Setup
            Path feApp = frontendDir.resolve("app");
            Files.createDirectories(feApp);

            Files.writeString(frontendDir.resolve("package.json"), String.format("""
{
  "name": "%s-frontend",
  "version": "1.0.0",
  "private": true,
  "scripts": {
    "dev": "next dev",
    "build": "next build",
    "start": "next start"
  },
  "dependencies": {
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "next": "14.2.5",
    "lucide-react": "^0.446.0"
  }
}
""", profile.slug()));

            Files.writeString(feApp.resolve("page.tsx"), String.format("""
import React from 'react';

export default function %sDashboard() {
  return (
    <main className="min-h-screen bg-slate-950 text-white p-8">
      <header className="mb-8 border-b border-slate-800 pb-4">
        <h1 className="text-2xl font-bold tracking-tight text-white">%s</h1>
        <p className="text-sm text-slate-400">Autonomous Domain Console &bull; Project: %s</p>
      </header>
      <section className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800">
          <h2 className="text-sm font-semibold text-slate-300">Domain Entity</h2>
          <p className="text-2xl font-bold mt-2 text-blue-400">%s</p>
          <p className="text-xs text-slate-500 mt-1">Clean Architecture Hexagonal Core</p>
        </div>
        <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800">
          <h2 className="text-sm font-semibold text-slate-300">Operational Status</h2>
          <p className="text-2xl font-bold mt-2 text-emerald-400">HEALTHY / ONLINE</p>
          <p className="text-xs text-slate-500 mt-1">Spring Boot 3.3 REST Adapter Active</p>
        </div>
        <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800">
          <h2 className="text-sm font-semibold text-slate-300">Persistence Store</h2>
          <p className="text-2xl font-bold mt-2 text-indigo-400">PostgreSQL 16</p>
          <p className="text-xs text-slate-500 mt-1">ACID Isolation & Optimistic Locking</p>
        </div>
      </section>
    </main>
  );
}
""", profile.primaryEntityName(), profile.appTitle(), projectId, profile.primaryEntityName()));

            // 10. Frontend Dockerfile
            Files.writeString(frontendDir.resolve("Dockerfile"), """
FROM node:20-alpine AS builder
WORKDIR /app
COPY package.json ./
RUN npm install
COPY . .
RUN npm run build

FROM node:20-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
COPY --from=builder /app/.next ./.next
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/package.json ./package.json
EXPOSE 3000
CMD ["npm", "start"]
""");

            // 11. Project Root docker-compose.yml
            Files.writeString(projectBase.resolve("docker-compose.yml"), String.format("""
services:
  postgres:
    image: postgres:16-alpine
    container_name: %s-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: %s_db
      POSTGRES_USER: %s_user
      POSTGRES_PASSWORD: %s_secure_password_2026
    ports:
      - "5433:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./backlog/DATABASE_SCHEMA.sql:/docker-entrypoint-initdb.d/init.sql:ro
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U %s_user -d %s_db"]
      interval: 10s
      timeout: 5s
      retries: 5

  backend:
    build:
      context: ./src/backend
      dockerfile: Dockerfile
    container_name: %s-backend
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: docker
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: %s_db
      DB_USER: %s_user
      DB_PASS: %s_secure_password_2026
    ports:
      - "8081:8081"

  frontend:
    build:
      context: ./src/frontend
      dockerfile: Dockerfile
    container_name: %s-frontend
    restart: unless-stopped
    depends_on:
      - backend
    environment:
      NEXT_PUBLIC_API_URL: http://localhost:8081
    ports:
      - "3000:3000"

volumes:
  postgres_data:
""",
                    profile.slug(),
                    profile.slug().replace("-", "_"),
                    profile.slug().replace("-", "_"),
                    profile.slug().replace("-", "_"),
                    profile.slug().replace("-", "_"),
                    profile.slug().replace("-", "_"),
                    profile.slug(),
                    profile.slug().replace("-", "_"),
                    profile.slug().replace("-", "_"),
                    profile.slug().replace("-", "_"),
                    profile.slug()
            ));

            log.info("Successfully scaffolded concrete source code and manifests for project: {}", projectId);
        } catch (Exception ex) {
            log.error("Failed to scaffold source code for project {}: {}", projectId, ex.getMessage());
        }
    }
}
