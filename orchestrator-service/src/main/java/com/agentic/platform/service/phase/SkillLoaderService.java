package com.agentic.platform.service.phase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service responsible for discovering and dynamically loading markdown skill specifications
 * (from .agents/skills or .agent/skills) into agent prompt contexts.
 */
@Service
public class SkillLoaderService {

    private static final Logger log = LoggerFactory.getLogger(SkillLoaderService.class);
    private static final Pattern YAML_FRONTMATTER = Pattern.compile("^---\\s*\\r?\\n.*?\\r?\\n---\\s*\\r?\\n", Pattern.DOTALL);

    private final Map<String, String> skillCache = new ConcurrentHashMap<>();
    private Path resolvedSkillsRoot;

    public SkillLoaderService() {
        this.resolvedSkillsRoot = findSkillsDirectory();
        if (resolvedSkillsRoot != null) {
            log.info("SkillLoaderService initialized. Discovered skills root at: {}", resolvedSkillsRoot.toAbsolutePath());
        } else {
            log.warn("SkillLoaderService: No .agents/skills or .agent/skills directory found in candidate paths.");
        }
    }

    public synchronized Path getSkillsRoot() {
        if (resolvedSkillsRoot == null || !Files.exists(resolvedSkillsRoot)) {
            resolvedSkillsRoot = findSkillsDirectory();
        }
        return resolvedSkillsRoot;
    }

    private Path findSkillsDirectory() {
        List<Path> candidates = List.of(
                Paths.get(".agents", "skills"),
                Paths.get(".agent", "skills"),
                Paths.get("..", ".agents", "skills"),
                Paths.get("..", ".agent", "skills"),
                Paths.get("/app", ".agents", "skills"),
                Paths.get("/app", ".agent", "skills"),
                Paths.get("c:", "project", "satset-magic", ".agents", "skills"),
                Paths.get("c:", "project", "satset-magic", ".agent", "skills")
        );

        for (Path candidate : candidates) {
            try {
                if (Files.exists(candidate) && Files.isDirectory(candidate)) {
                    return candidate.normalize();
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public Optional<String> loadSkillContent(String skillName) {
        if (skillName == null || skillName.isBlank()) {
            return Optional.empty();
        }

        String cleanName = skillName.trim();
        if (skillCache.containsKey(cleanName)) {
            return Optional.of(skillCache.get(cleanName));
        }

        Path root = getSkillsRoot();
        if (root == null) {
            return Optional.empty();
        }

        Path skillFile = root.resolve(cleanName).resolve("SKILL.md");
        if (Files.exists(skillFile) && Files.isRegularFile(skillFile)) {
            try {
                String raw = Files.readString(skillFile);
                // Strip frontmatter if present for cleaner prompt context
                Matcher matcher = YAML_FRONTMATTER.matcher(raw);
                String cleanContent = matcher.replaceFirst("").trim();
                skillCache.put(cleanName, cleanContent);
                log.info("Loaded skill '{}' from {}", cleanName, skillFile);
                return Optional.of(cleanContent);
            } catch (IOException e) {
                log.warn("Failed to read skill file {}: {}", skillFile, e.getMessage());
            }
        } else {
            log.warn("Skill '{}' not found at {}", cleanName, skillFile);
        }

        return Optional.empty();
    }

    public String loadSkillsAsPromptContext(String installedSkillsCsv) {
        if (installedSkillsCsv == null || installedSkillsCsv.isBlank()) {
            return "";
        }

        List<String> skillNames = Arrays.stream(installedSkillsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        int loadedCount = 0;

        for (String skillName : skillNames) {
            Optional<String> content = loadSkillContent(skillName);
            if (content.isPresent()) {
                if (loadedCount == 0) {
                    sb.append("\n================================================================================\n");
                    sb.append("🛠️ ACTIVE SPECIALIZED SKILL GUIDELINES & DIRECTIVES (Loaded dynamically from .agents/skills):\n");
                    sb.append("You MUST strictly enforce and follow the standards and workflow instructions defined below:\n");
                }
                sb.append("\n--- [MOUNTED SKILL: ").append(skillName).append("] ---\n");
                sb.append(content.get()).append("\n");
                loadedCount++;
            }
        }

        if (loadedCount > 0) {
            sb.append("================================================================================\n\n");
        }

        return sb.toString();
    }

    public List<String> listAvailableSkills() {
        Path root = getSkillsRoot();
        if (root == null) {
            return List.of();
        }
        try (var stream = Files.list(root)) {
            return stream.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Failed to list skills in {}: {}", root, e.getMessage());
            return List.of();
        }
    }
}
