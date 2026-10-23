package com.agentic.platform.service.phase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service that tracks and manages dynamic port allocations across all generated projects
 * to prevent port collisions between backend, frontend, postgres, redis, and kafka containers.
 */
@Service
public class PortRegistryService {

    private static final Logger log = LoggerFactory.getLogger(PortRegistryService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, ProjectPortAllocation> cache = new ConcurrentHashMap<>();

    public record ProjectPortAllocation(
            String projectId,
            String projectName,
            int frontendPort,
            int backendPort,
            int postgresPort,
            int redisPort,
            int kafkaPort,
            String status,
            String updatedAt
    ) {}

    public Path resolveRegistryPath() {
        if (Files.exists(Paths.get("workspace"))) {
            return Paths.get("workspace", "PORT_REGISTRY.json");
        }
        if (Files.exists(Paths.get("..", "workspace"))) {
            return Paths.get("..", "workspace", "PORT_REGISTRY.json");
        }
        return Paths.get("workspace", "PORT_REGISTRY.json");
    }

    public synchronized ProjectPortAllocation allocatePortsForProject(String projectId, String projectName) {
        loadRegistry();
        if (cache.containsKey(projectId)) {
            return cache.get(projectId);
        }

        Set<Integer> usedPorts = new HashSet<>();
        // Add platform reserved ports
        usedPorts.addAll(List.of(3000, 8080, 5432, 6379));

        for (ProjectPortAllocation alloc : cache.values()) {
            usedPorts.add(alloc.frontendPort());
            usedPorts.add(alloc.backendPort());
            usedPorts.add(alloc.postgresPort());
            usedPorts.add(alloc.redisPort());
            usedPorts.add(alloc.kafkaPort());
        }

        int frontendPort = findFreePort(3200, 3300, usedPorts);
        usedPorts.add(frontendPort);

        int backendPort = findFreePort(8100, 8200, usedPorts);
        usedPorts.add(backendPort);

        int postgresPort = findFreePort(5433, 5499, usedPorts);
        usedPorts.add(postgresPort);

        int redisPort = findFreePort(6380, 6450, usedPorts);
        usedPorts.add(redisPort);

        int kafkaPort = findFreePort(9092, 9150, usedPorts);
        usedPorts.add(kafkaPort);

        ProjectPortAllocation newAlloc = new ProjectPortAllocation(
                projectId,
                projectName != null ? projectName : projectId,
                frontendPort,
                backendPort,
                postgresPort,
                redisPort,
                kafkaPort,
                "CONFIGURED",
                Instant.now().toString()
        );

        cache.put(projectId, newAlloc);
        saveRegistry();
        log.info("Allocated ports for project {}: FE={}, BE={}, PG={}, REDIS={}, KAFKA={}",
                projectId, frontendPort, backendPort, postgresPort, redisPort, kafkaPort);

        return newAlloc;
    }

    public Optional<ProjectPortAllocation> getPortAllocation(String projectId) {
        loadRegistry();
        return Optional.ofNullable(cache.get(projectId));
    }

    public Map<String, ProjectPortAllocation> getAllAllocations() {
        loadRegistry();
        return Collections.unmodifiableMap(cache);
    }

    private int findFreePort(int start, int end, Set<Integer> usedPorts) {
        for (int p = start; p <= end; p++) {
            if (!usedPorts.contains(p) && isPortAvailable(p)) {
                return p;
            }
        }
        return start; // Fallback
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private synchronized void loadRegistry() {
        try {
            Path path = resolveRegistryPath();
            if (!Files.exists(path)) {
                return;
            }
            JsonNode root = objectMapper.readTree(path.toFile());
            JsonNode allocs = root.path("allocations");
            if (allocs.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = allocs.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    String pId = entry.getKey();
                    JsonNode node = entry.getValue();
                    ProjectPortAllocation alloc = new ProjectPortAllocation(
                            pId,
                            node.path("projectName").asText(pId),
                            node.path("frontendPort").asInt(3200),
                            node.path("backendPort").asInt(8180),
                            node.path("postgresPort").asInt(5433),
                            node.path("redisPort").asInt(6380),
                            node.path("kafkaPort").asInt(9092),
                            node.path("status").asText("CONFIGURED"),
                            node.path("updatedAt").asText(Instant.now().toString())
                    );
                    cache.put(pId, alloc);
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to load PORT_REGISTRY.json: {}", ex.getMessage());
        }
    }

    private synchronized void saveRegistry() {
        try {
            Path path = resolveRegistryPath();
            Files.createDirectories(path.getParent());

            ObjectNode root = objectMapper.createObjectNode();
            ObjectNode platform = root.putObject("platform");
            platform.put("dashboard_ui", 3000);
            platform.put("orchestrator_service", 8080);
            platform.put("postgres", 5432);
            platform.put("redis", 6379);

            ObjectNode allocNode = root.putObject("allocations");
            for (ProjectPortAllocation alloc : cache.values()) {
                ObjectNode item = allocNode.putObject(alloc.projectId());
                item.put("projectName", alloc.projectName());
                item.put("frontendPort", alloc.frontendPort());
                item.put("backendPort", alloc.backendPort());
                item.put("postgresPort", alloc.postgresPort());
                item.put("redisPort", alloc.redisPort());
                item.put("kafkaPort", alloc.kafkaPort());
                item.put("status", alloc.status());
                item.put("updatedAt", alloc.updatedAt());
            }

            ObjectNode ranges = root.putObject("ranges");
            ranges.putObject("frontend").put("start", 3200).put("end", 3300);
            ranges.putObject("backend").put("start", 8100).put("end", 8200);
            ranges.putObject("postgres").put("start", 5433).put("end", 5499);
            ranges.putObject("redis").put("start", 6380).put("end", 6450);
            ranges.putObject("kafka").put("start", 9092).put("end", 9150);

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), root);
        } catch (Exception ex) {
            log.warn("Failed to save PORT_REGISTRY.json: {}", ex.getMessage());
        }
    }
}
