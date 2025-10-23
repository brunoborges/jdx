package com.jdx.catalog;

import com.jdx.model.JdkInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JdkCatalogImplTest {

    @TempDir
    Path tempDir;
    
    private JdkCatalogImpl catalog;
    private String originalUserHome;

    @BeforeEach
    void setUp() throws IOException {
        // Set up temporary .jdx directory for each test
        originalUserHome = System.getProperty("user.home");
        
        // Create a unique subdirectory for each test
        Path testDir = tempDir.resolve("test-" + System.nanoTime());
        Files.createDirectories(testDir);
        System.setProperty("user.home", testDir.toString());
        
        catalog = new JdkCatalogImpl();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        // Restore original user.home
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
        }
    }

    @Test
    void testAddJdk() {
        JdkInfo jdk = createTestJdk("temurin-21", "21.0.1");
        
        catalog.add(jdk);
        
        Optional<JdkInfo> found = catalog.findById("temurin-21");
        assertThat(found).isPresent();
        assertThat(found.get()).isEqualTo(jdk);
    }

    @Test
    void testGetAll() {
        JdkInfo jdk1 = createTestJdk("temurin-21", "21.0.1");
        JdkInfo jdk2 = createTestJdk("temurin-17", "17.0.9");
        
        catalog.add(jdk1);
        catalog.add(jdk2);
        
        List<JdkInfo> all = catalog.getAll();
        assertThat(all).hasSize(2);
        assertThat(all).containsExactlyInAnyOrder(jdk1, jdk2);
    }

    @Test
    void testFindById() {
        JdkInfo jdk = createTestJdk("oracle-17", "17.0.9");
        catalog.add(jdk);
        
        Optional<JdkInfo> found = catalog.findById("oracle-17");
        assertThat(found).isPresent();
        assertThat(found.get().version()).isEqualTo("17.0.9");
        
        Optional<JdkInfo> notFound = catalog.findById("nonexistent");
        assertThat(notFound).isEmpty();
    }

    @Test
    void testFindByVersion() {
        JdkInfo jdk21 = createTestJdk("temurin-21", "21.0.1");
        JdkInfo jdk17 = createTestJdk("temurin-17", "17.0.9");
        JdkInfo jdk17_2 = createTestJdk("oracle-17", "17.0.11");
        
        catalog.add(jdk21);
        catalog.add(jdk17);
        catalog.add(jdk17_2);
        
        List<JdkInfo> found21 = catalog.findByVersion("21");
        assertThat(found21).hasSize(1);
        assertThat(found21.get(0).id()).isEqualTo("temurin-21");
        
        List<JdkInfo> found17 = catalog.findByVersion("17");
        assertThat(found17).hasSize(2);
    }

    @Test
    void testFindByVersionExact() {
        JdkInfo jdk = createTestJdk("temurin-17", "17.0.9");
        catalog.add(jdk);
        
        List<JdkInfo> found = catalog.findByVersion("17.0.9");
        assertThat(found).hasSize(1);
        assertThat(found.get(0).version()).isEqualTo("17.0.9");
    }

    @Test
    void testFindByVersionWithJava8() {
        JdkInfo jdk8 = createTestJdk("oracle-8", "1.8.0_392");
        catalog.add(jdk8);
        
        List<JdkInfo> found = catalog.findByVersion("8");
        assertThat(found).hasSize(1);
        assertThat(found.get(0).id()).isEqualTo("oracle-8");
    }

    @Test
    void testSaveAndLoad() throws IOException {
        JdkInfo jdk1 = createTestJdk("temurin-21", "21.0.1");
        JdkInfo jdk2 = createTestJdk("temurin-17", "17.0.9");
        
        catalog.add(jdk1);
        catalog.add(jdk2);
        catalog.save();
        
        // Create new catalog instance to test loading
        JdkCatalogImpl newCatalog = new JdkCatalogImpl();
        List<JdkInfo> all = newCatalog.getAll();
        
        assertThat(all).hasSize(2);
        assertThat(all).containsExactlyInAnyOrder(jdk1, jdk2);
    }

    @Test
    void testLoadNonexistentCatalog() {
        // Should not throw exception
        catalog.load();
        assertThat(catalog.getAll()).isEmpty();
    }

    @Test
    void testDuplicateJdk() {
        JdkInfo jdk1 = createTestJdk("temurin-21", "21.0.1");
        JdkInfo jdk2 = createTestJdk("temurin-21", "21.0.2"); // Same ID, different version
        
        catalog.add(jdk1);
        catalog.add(jdk2); // Should overwrite
        
        Optional<JdkInfo> found = catalog.findById("temurin-21");
        assertThat(found).isPresent();
        assertThat(found.get().version()).isEqualTo("21.0.2");
    }

    @Test
    void testCatalogFileFormat() throws IOException {
        // Ensure catalog directory exists
        Path jdxDir = tempDir.resolve(".jdx");
        Files.createDirectories(jdxDir);
        
        JdkInfo jdk = createTestJdk("temurin-21", "21.0.1");
        catalog.add(jdk);
        catalog.save();
        
        Path catalogFile = tempDir.resolve(".jdx/catalog.json");
        assertThat(catalogFile).exists();
        
        String content = Files.readString(catalogFile);
        assertThat(content).contains("\"id\" : \"temurin-21\"");
        assertThat(content).contains("\"version\" : \"21.0.1\"");
        assertThat(content).contains("\"jdks\"");
    }

    @Test
    void testEmptyCatalog() {
        assertThat(catalog.getAll()).isEmpty();
        assertThat(catalog.findById("anything")).isEmpty();
        assertThat(catalog.findByVersion("21")).isEmpty();
    }

    @Test
    void testVersionMatchingWithPatch() {
        JdkInfo jdk = createTestJdk("temurin-17", "17.0.9");
        catalog.add(jdk);
        
        // Should match major version
        assertThat(catalog.findByVersion("17")).hasSize(1);
        
        // Should match major.minor version
        assertThat(catalog.findByVersion("17.0")).hasSize(1);
        
        // Should match full version
        assertThat(catalog.findByVersion("17.0.9")).hasSize(1);
        
        // Should not match different version
        assertThat(catalog.findByVersion("18")).isEmpty();
    }

    private JdkInfo createTestJdk(String id, String version) {
        return new JdkInfo(
            id,
            version,
            "Test Vendor",
            "x86_64",
            "/test/path/" + id,
            Set.of("jlink"),
            true
        );
    }
}
