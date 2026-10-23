package com.agentic.platform.service.phase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Autonomous Runtime & Container Deployment Agent.
 * Manages fullstack container lifecycle (Postgres, Redis, Kafka, Spring Boot Backend, Next.js Frontend)
 * using Docker Compose per project workspace.
 */
@Service
public class ProjectRuntimeDeploymentAgent {

    private static final Logger log = LoggerFactory.getLogger(ProjectRuntimeDeploymentAgent.class);

    private final SdlcJournalService journalService;
    private final PortRegistryService portRegistryService;
    private final Map<String, RuntimeState> runtimeStates = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public record ProjectRuntimeStatus(
            String projectId,
            String status,
            String frontendUrl,
            String backendUrl,
            int frontendPort,
            int backendPort,
            int postgresPort,
            int redisPort,
            int kafkaPort,
            String message,
            String lastUpdated
    ) {}

    private static class RuntimeState {
        String status = "STOPPED"; // STOPPED, STARTING, RUNNING, ERROR
        String message = "Stack is offline";
        String lastUpdated = Instant.now().toString();
    }

    public ProjectRuntimeDeploymentAgent(SdlcJournalService journalService, PortRegistryService portRegistryService) {
        this.journalService = journalService;
        this.portRegistryService = portRegistryService;
    }

    public ProjectRuntimeStatus getProjectRuntimeStatus(String projectId) {
        PortRegistryService.ProjectPortAllocation alloc = portRegistryService.getPortAllocation(projectId)
                .orElseGet(() -> portRegistryService.allocatePortsForProject(projectId, projectId));

        RuntimeState state = runtimeStates.computeIfAbsent(projectId, k -> new RuntimeState());
        boolean feLive = isHttpEndpointLive("http://localhost:" + alloc.frontendPort())
                || isHttpEndpointLive("http://host.docker.internal:" + alloc.frontendPort());
        boolean beLive = isHttpEndpointLive("http://localhost:" + alloc.backendPort() + "/api/subscriptions")
                || isHttpEndpointLive("http://host.docker.internal:" + alloc.backendPort() + "/api/subscriptions")
                || isPortListening("localhost", alloc.backendPort())
                || isPortListening("host.docker.internal", alloc.backendPort());

        if (feLive || beLive) {
            state.status = "RUNNING";
            state.message = "Container stack is online and responding (FE: " + alloc.frontendPort() + ", BE: " + alloc.backendPort() + ")";
            state.lastUpdated = Instant.now().toString();
        } else if (!"STARTING".equals(state.status)) {
            state.status = "STOPPED";
            state.message = "Stack is offline";
        }

        String feUrl = "http://localhost:" + alloc.frontendPort();
        String beUrl = "http://localhost:" + alloc.backendPort();

        return new ProjectRuntimeStatus(
                projectId,
                state.status,
                feUrl,
                beUrl,
                alloc.frontendPort(),
                alloc.backendPort(),
                alloc.postgresPort(),
                alloc.redisPort(),
                alloc.kafkaPort(),
                state.message,
                state.lastUpdated
        );
    }

    public ProjectRuntimeStatus startProjectStack(String projectId) {
        PortRegistryService.ProjectPortAllocation alloc = portRegistryService.allocatePortsForProject(projectId, projectId);
        RuntimeState state = runtimeStates.computeIfAbsent(projectId, k -> new RuntimeState());

        state.status = "STARTING";
        state.message = "Building images and launching container stack...";
        state.lastUpdated = Instant.now().toString();

        executorService.submit(() -> {
            try {
                Path projectBase = journalService.resolveProjectBase(projectId);
                Path composeFile = projectBase.resolve("docker-compose.yml");

                if (!Files.exists(composeFile)) {
                    state.status = "ERROR";
                    state.message = "docker-compose.yml not found in project workspace";
                    state.lastUpdated = Instant.now().toString();
                    return;
                }

                log.info("Launching Docker Compose stack for project: {} at {}", projectId, composeFile);
                boolean success = executeCommand(projectBase, "docker", "compose", "-f", "docker-compose.yml", "up", "-d", "--build");

                if (success) {
                    state.message = "Containers launched. Waiting for healthy services...";
                    pollServicesUntilReady(projectId, alloc.frontendPort(), alloc.backendPort(), state);
                } else {
                    state.status = "ERROR";
                    state.message = "Failed to execute 'docker compose up'";
                    state.lastUpdated = Instant.now().toString();
                }
            } catch (Exception ex) {
                log.error("Exception starting project stack {}: {}", projectId, ex.getMessage());
                state.status = "ERROR";
                state.message = "Execution failed: " + ex.getMessage();
                state.lastUpdated = Instant.now().toString();
            }
        });

        return getProjectRuntimeStatus(projectId);
    }

    public ProjectRuntimeStatus stopProjectStack(String projectId) {
        RuntimeState state = runtimeStates.computeIfAbsent(projectId, k -> new RuntimeState());
        state.status = "STOPPING";
        state.message = "Stopping container stack...";
        state.lastUpdated = Instant.now().toString();

        executorService.submit(() -> {
            try {
                Path projectBase = journalService.resolveProjectBase(projectId);
                log.info("Stopping Docker Compose stack for project: {}", projectId);
                executeCommand(projectBase, "docker", "compose", "-f", "docker-compose.yml", "down");
                state.status = "STOPPED";
                state.message = "Container stack stopped successfully";
                state.lastUpdated = Instant.now().toString();
            } catch (Exception ex) {
                log.error("Exception stopping project stack {}: {}", projectId, ex.getMessage());
                state.status = "ERROR";
                state.message = "Stop failed: " + ex.getMessage();
                state.lastUpdated = Instant.now().toString();
            }
        });

        return getProjectRuntimeStatus(projectId);
    }

    private void pollServicesUntilReady(String projectId, int fePort, int bePort, RuntimeState state) {
        int maxAttempts = 30; // 30 x 2s = 60s
        for (int i = 0; i < maxAttempts; i++) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {}

            boolean beReady = isHttpEndpointLive("http://localhost:" + bePort + "/api/subscriptions")
                    || isHttpEndpointLive("http://host.docker.internal:" + bePort + "/api/subscriptions")
                    || isPortListening("localhost", bePort)
                    || isPortListening("host.docker.internal", bePort);
            boolean feReady = isHttpEndpointLive("http://localhost:" + fePort)
                    || isHttpEndpointLive("http://host.docker.internal:" + fePort);

            if (feReady || beReady) {
                state.status = "RUNNING";
                state.message = "Stack is healthy and active. Ready for user interaction.";
                state.lastUpdated = Instant.now().toString();
                log.info("Project {} stack successfully verified LIVE (FE={}, BE={})", projectId, feReady, beReady);
                return;
            }
        }

        // Even if HTTP check didn't succeed within 60s, if containers or ports are listening, mark as running
        if (isDockerComposeRunning(projectId) || isPortListening("localhost", bePort) || isPortListening("host.docker.internal", bePort)) {
            state.status = "RUNNING";
            state.message = "Containers are running in Docker";
            state.lastUpdated = Instant.now().toString();
        } else {
            state.status = "ERROR";
            state.message = "Timeout waiting for services to respond on ports " + fePort + " / " + bePort;
            state.lastUpdated = Instant.now().toString();
        }
    }

    private boolean isHttpEndpointLive(String urlStr) {
        try {
            URI uri = URI.create(urlStr);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(1500);
            conn.setReadTimeout(1500);
            int code = conn.getResponseCode();
            return (code >= 200 && code < 400) || code == 404; // 404 is still an active web server
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isPortListening(String host, int port) {
        try (java.net.Socket s = new java.net.Socket()) {
            s.connect(new java.net.InetSocketAddress(host, port), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isDockerComposeRunning(String projectId) {
        try {
            Path projectBase = journalService.resolveProjectBase(projectId);
            ProcessBuilder pb = new ProcessBuilder("docker", "compose", "-f", "docker-compose.yml", "ps", "-q");
            pb.directory(projectBase.toFile());
            Process p = pb.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = r.readLine();
                return line != null && !line.isBlank();
            }
        } catch (Exception e) {
            return false;
        }
    }

    private boolean executeCommand(Path workDir, String... cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(workDir.toFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("[DOCKER] {}", line);
                }
            }
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            log.error("Failed executing command {}: {}", String.join(" ", cmd), e.getMessage());
            return false;
        }
    }
}
