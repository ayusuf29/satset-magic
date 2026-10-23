package com.agentic.platform.service;

import com.agentic.platform.service.phase.ProjectScaffoldingService;
import com.agentic.platform.service.phase.SdlcJournalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectScaffoldingServiceTest {

    static class StubJournalService extends SdlcJournalService {
        private String c4 = "";
        private String sql = "";
        private String summary = "";
        private Path customBase = null;

        public void setC4(String c4) { this.c4 = c4; }
        public void setSql(String sql) { this.sql = sql; }
        public void setSummary(String summary) { this.summary = summary; }
        public void setCustomBase(Path base) { this.customBase = base; }

        @Override
        public Path resolveProjectBase(String projectId) {
            if (customBase != null) return customBase;
            return super.resolveProjectBase(projectId);
        }

        @Override
        public String readArtifactContent(String projectId, String relativePath) {
            if ("architecture/C4_MODEL.md".equals(relativePath)) return c4;
            if ("backlog/DATABASE_SCHEMA.sql".equals(relativePath)) return sql;
            if ("src/IMPLEMENTATION_SUMMARY.md".equals(relativePath)) return summary;
            return "";
        }
    }

    @Test
    @DisplayName("Should dynamically extract domain from C4 Model without hardcoded domain checks")
    void shouldExtractDomainFromC4Model() {
        StubJournalService journal = new StubJournalService();
        journal.setC4("""
                # C4 Architecture Model
                ## Bounded Contexts
                - **Aggregate Root**: `HospitalBed`
                - **Aggregate Root**: `PatientAdmission`
                """);

        ProjectScaffoldingService scaffoldingService = new ProjectScaffoldingService(journal);
        ProjectScaffoldingService.DomainProfile profile = scaffoldingService.resolveDomainProfile("PRJ-HOSP-1", "Hospital Ward System", "Manage beds and admissions");

        assertThat(profile.primaryEntityName()).isEqualTo("HospitalBed");
        assertThat(profile.secondaryEntityName()).isEqualTo("PatientAdmission");
        assertThat(profile.serviceName()).isEqualTo("HospitalBedService");
        assertThat(profile.controllerName()).isEqualTo("HospitalBedController");
        assertThat(profile.slug()).isEqualTo("hospital-ward-system");
    }

    @Test
    @DisplayName("Should dynamically extract domain from SQL DDL schema when C4 model is not present")
    void shouldExtractDomainFromSqlSchema() {
        StubJournalService journal = new StubJournalService();
        journal.setSql("""
                CREATE TABLE poultry_batches (
                    id UUID PRIMARY KEY,
                    batch_code VARCHAR(50)
                );
                CREATE TABLE feeding_logs (
                    id UUID PRIMARY KEY,
                    amount_kg DECIMAL
                );
                """);

        ProjectScaffoldingService scaffoldingService = new ProjectScaffoldingService(journal);
        ProjectScaffoldingService.DomainProfile profile = scaffoldingService.resolveDomainProfile("PRJ-FARM-2", "Smart Poultry Farm", "IoT feed tracker");

        assertThat(profile.primaryEntityName()).isEqualTo("PoultryBatch");
        assertThat(profile.secondaryEntityName()).isEqualTo("FeedingLog");
        assertThat(profile.serviceName()).isEqualTo("PoultryBatchService");
        assertThat(profile.controllerName()).isEqualTo("PoultryBatchController");
    }

    @Test
    @DisplayName("Should materialize concrete TDD test classes from TDD_PLAN_AND_SPECS.md onto disk")
    void shouldMaterializeTddTestBlocks(@TempDir Path tempDir) throws IOException {
        Path testsDir = tempDir.resolve("tests");
        Files.createDirectories(testsDir);

        String tddMarkdown = """
                # TDD Plan
                Here are the test specs:

                ```java
                package com.agentic.hospital.domain;

                import org.junit.jupiter.api.Test;
                import static org.assertj.core.api.Assertions.assertThat;

                public class HospitalBedDomainTest {
                    @Test
                    void shouldAdmitPatientSuccessfully() {
                        assertThat(true).isTrue();
                    }
                }
                ```
                """;
        Files.writeString(testsDir.resolve("TDD_PLAN_AND_SPECS.md"), tddMarkdown);

        StubJournalService journal = new StubJournalService();
        ProjectScaffoldingService scaffoldingService = new ProjectScaffoldingService(journal);

        int count = scaffoldingService.materializeTddTestBlocks(tempDir);
        assertThat(count).isEqualTo(1);

        Path backendTest = tempDir.resolve("src/backend/src/test/java/com/agentic/hospital/domain/HospitalBedDomainTest.java");
        Path testsBackend = tempDir.resolve("tests/backend/HospitalBedDomainTest.java");

        assertThat(backendTest).exists();
        assertThat(testsBackend).exists();
        assertThat(Files.readString(backendTest)).contains("class HospitalBedDomainTest");
    }

    @Test
    @DisplayName("Should materialize Java domain code and TSX page from IMPLEMENTATION_SUMMARY.md")
    void shouldMaterializeCodeBlocksFromSummary(@TempDir Path tempDir) throws IOException {
        Path srcDir = tempDir.resolve("src");
        Files.createDirectories(srcDir);

        String summaryMarkdown = """
                # Implementation Summary

                ```java
                package com.agentic.hospital.domain;

                public record HospitalBed(String id, String wardNumber, String status) {
                    public static HospitalBed create(String id, String wardNumber) {
                        return new HospitalBed(id, wardNumber, "AVAILABLE");
                    }
                }
                ```

                ```java
                package com.agentic.hospital.port;

                public interface HospitalBedRepositoryPort {
                    HospitalBed save(HospitalBed bed);
                }
                ```

                ```tsx
                import React from 'react';

                export default function WardDashboard() {
                  return <div>Ward Dashboard</div>;
                }
                ```
                """;
        Files.writeString(srcDir.resolve("IMPLEMENTATION_SUMMARY.md"), summaryMarkdown);

        StubJournalService journal = new StubJournalService();
        ProjectScaffoldingService scaffoldingService = new ProjectScaffoldingService(journal);

        scaffoldingService.extractAndMaterializeCodeBlocks(tempDir);

        Path entityFile = tempDir.resolve("src/backend/src/main/java/com/agentic/hospital/domain/HospitalBed.java");
        Path portFile = tempDir.resolve("src/backend/src/main/java/com/agentic/hospital/port/HospitalBedRepositoryPort.java");
        Path pageFile = tempDir.resolve("src/frontend/app/page.tsx");

        assertThat(entityFile).exists();
        assertThat(portFile).exists();
        assertThat(pageFile).exists();

        assertThat(Files.readString(entityFile)).contains("public record HospitalBed");
        assertThat(Files.readString(portFile)).contains("public interface HospitalBedRepositoryPort");
        assertThat(Files.readString(pageFile)).contains("export default function WardDashboard");
    }

    @Test
    @DisplayName("Should scaffold build manifests and entrypoint cleanly without hardcoded dummy code")
    void shouldScaffoldBuildManifestsAndFallback(@TempDir Path tempDir) {
        StubJournalService journal = new StubJournalService();
        journal.setCustomBase(tempDir);
        journal.setC4("""
                - **Aggregate Root**: `MedicalRecord`
                """);

        ProjectScaffoldingService scaffoldingService = new ProjectScaffoldingService(journal);

        scaffoldingService.scaffoldTddTestSuites("PRJ-MED-1", "Medical Record Tracker", "Desc");
        scaffoldingService.scaffoldSourceCode("PRJ-MED-1", "Medical Record Tracker", "Desc");

        // Verify manifests
        assertThat(tempDir.resolve("src/backend/pom.xml")).exists();
        assertThat(tempDir.resolve("src/backend/Dockerfile")).exists();
        assertThat(tempDir.resolve("docker-compose.yml")).exists();
        assertThat(tempDir.resolve("src/frontend/package.json")).exists();
        assertThat(tempDir.resolve("src/frontend/Dockerfile")).exists();

        // Verify main class entrypoint
        Path mainClass = tempDir.resolve("src/backend/src/main/java/com/agentic/prjmed1/MedicalRecordTrackerApplication.java");
        assertThat(mainClass).exists();

        // Verify fallback test baseline created since no TDD_PLAN_AND_SPECS.md was provided
        Path fallbackTest = tempDir.resolve("src/backend/src/test/java/com/agentic/prjmed1/domain/MedicalRecordBaselineTest.java");
        assertThat(fallbackTest).exists();

        // Verify fallback dynamic frontend dashboard created since no TSX in summary
        Path fallbackPage = tempDir.resolve("src/frontend/app/page.tsx");
        assertThat(fallbackPage).exists();
        try {
            assertThat(Files.readString(fallbackPage)).contains("MedicalRecordTrackerDashboard");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

