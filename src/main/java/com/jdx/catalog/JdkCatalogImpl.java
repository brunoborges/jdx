package com.jdx.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jdx.model.JdkInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of JDK catalog using JSON file storage.
 */
public class JdkCatalogImpl implements JdkCatalog {
    
    private static final String JDX_DIR = System.getProperty("user.home") + "/.jdx";
    private static final String CATALOG_FILE = JDX_DIR + "/catalog.json";
    
    private final Map<String, JdkInfo> catalog = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public JdkCatalogImpl() {
        ensureJdxDir();
        load();
    }

    private void ensureJdxDir() {
        Path jdxPath = Paths.get(JDX_DIR);
        if (!Files.exists(jdxPath)) {
            try {
                Files.createDirectories(jdxPath);
            } catch (IOException e) {
                System.err.println("Warning: Could not create .jdx directory: " + e.getMessage());
            }
        }
    }

    @Override
    public void add(JdkInfo jdk) {
        catalog.put(jdk.id(), jdk);
    }

    @Override
    public List<JdkInfo> getAll() {
        return new ArrayList<>(catalog.values());
    }

    @Override
    public Optional<JdkInfo> findById(String id) {
        return Optional.ofNullable(catalog.get(id));
    }

    @Override
    public List<JdkInfo> findByVersion(String version) {
        return catalog.values().stream()
            .filter(jdk -> matchesVersion(jdk.version(), version))
            .sorted(Comparator.comparing(JdkInfo::version).reversed())
            .toList();
    }

    private boolean matchesVersion(String jdkVersion, String requestedVersion) {
        // Simple version matching
        // Supports: "8", "1.8", "17", "17.0.11", "21", etc.
        String normalized = normalizeVersion(jdkVersion);
        String requested = normalizeVersion(requestedVersion);
        
        return normalized.startsWith(requested) || normalized.equals(requested);
    }

    private String normalizeVersion(String version) {
        // Remove quotes and normalize
        version = version.replaceAll("^\"|\"$", "");
        
        // Convert 1.8 to 8
        if (version.startsWith("1.8")) {
            version = "8" + version.substring(3);
        }
        
        return version;
    }

    @Override
    public void save() {
        try {
            Path catalogPath = Paths.get(CATALOG_FILE);
            CatalogData data = new CatalogData(new ArrayList<>(catalog.values()));
            mapper.writeValue(catalogPath.toFile(), data);
        } catch (IOException e) {
            System.err.println("Warning: Could not save catalog: " + e.getMessage());
        }
    }

    @Override
    public void load() {
        Path catalogPath = Paths.get(CATALOG_FILE);
        if (!Files.exists(catalogPath)) {
            return;
        }
        
        try {
            CatalogData data = mapper.readValue(catalogPath.toFile(), CatalogData.class);
            catalog.clear();
            if (data.jdks != null) {
                for (JdkInfo jdk : data.jdks) {
                    catalog.put(jdk.id(), jdk);
                }
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load catalog: " + e.getMessage());
        }
    }

    // Helper class for JSON serialization
    private static class CatalogData {
        public List<JdkInfo> jdks;

        public CatalogData() {}

        public CatalogData(List<JdkInfo> jdks) {
            this.jdks = jdks;
        }
    }
}
