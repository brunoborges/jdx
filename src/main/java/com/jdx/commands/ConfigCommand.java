package com.jdx.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jdx.model.JdxConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

@Command(
    name = "config",
    description = "Get or set global configuration values"
)
public class ConfigCommand implements Callable<Integer> {
    
    @Parameters(index = "0", description = "Operation: get or set")
    private String operation;

    @Parameters(index = "1", description = "Configuration key")
    private String key;

    @Parameters(index = "2", arity = "0..1", description = "Configuration value (for set operation)")
    private String value;

    private static final String JDX_DIR = System.getProperty("user.home") + "/.jdx";
    private static final String CONFIG_FILE = JDX_DIR + "/config.yaml";

    @Override
    public Integer call() throws Exception {
        if (!"get".equals(operation) && !"set".equals(operation)) {
            System.err.println("Error: Operation must be 'get' or 'set'");
            return 1;
        }

        if ("get".equals(operation)) {
            return getConfig();
        } else {
            return setConfig();
        }
    }

    private int getConfig() throws Exception {
        JdxConfig config = loadConfig();
        
        // Simple key access
        String result = switch (key) {
            case "catalog.autorefresh_days" -> String.valueOf(config.catalog().autorefresh_days());
            case "defaults.runtime" -> config.defaults().runtime();
            case "safety.require_confirmation_on_persist" -> String.valueOf(config.safety().require_confirmation_on_persist());
            case "telemetry.enabled" -> String.valueOf(config.telemetry().enabled());
            default -> {
                System.err.println("Unknown configuration key: " + key);
                yield null;
            }
        };

        if (result != null) {
            System.out.println(result);
            return 0;
        }
        return 1;
    }

    private int setConfig() throws Exception {
        if (value == null) {
            System.err.println("Error: Value required for set operation");
            return 1;
        }

        JdxConfig config = loadConfig();
        
        // Update config based on key
        config = switch (key) {
            case "catalog.autorefresh_days" -> new JdxConfig(
                new JdxConfig.CatalogConfig(Integer.parseInt(value)),
                config.defaults(),
                config.safety(),
                config.telemetry()
            );
            case "defaults.runtime" -> new JdxConfig(
                config.catalog(),
                new JdxConfig.DefaultsConfig(value, config.defaults().vendor_preference()),
                config.safety(),
                config.telemetry()
            );
            case "safety.require_confirmation_on_persist" -> new JdxConfig(
                config.catalog(),
                config.defaults(),
                new JdxConfig.SafetyConfig(Boolean.parseBoolean(value)),
                config.telemetry()
            );
            case "telemetry.enabled" -> new JdxConfig(
                config.catalog(),
                config.defaults(),
                config.safety(),
                new JdxConfig.TelemetryConfig(Boolean.parseBoolean(value))
            );
            default -> {
                System.err.println("Unknown configuration key: " + key);
                yield null;
            }
        };

        if (config != null) {
            saveConfig(config);
            System.out.println("Configuration updated: " + key + " = " + value);
            return 0;
        }
        return 1;
    }

    private JdxConfig loadConfig() throws Exception {
        Path configPath = Paths.get(CONFIG_FILE);
        
        if (!Files.exists(configPath)) {
            // Return default config
            return new JdxConfig(
                new JdxConfig.CatalogConfig(7),
                new JdxConfig.DefaultsConfig("21", new String[]{"Microsoft", "Temurin", "any"}),
                new JdxConfig.SafetyConfig(true),
                new JdxConfig.TelemetryConfig(false)
            );
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(configPath.toFile(), JdxConfig.class);
    }

    private void saveConfig(JdxConfig config) throws Exception {
        Path jdxPath = Paths.get(JDX_DIR);
        if (!Files.exists(jdxPath)) {
            Files.createDirectories(jdxPath);
        }

        Path configPath = Paths.get(CONFIG_FILE);
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.writeValue(configPath.toFile(), config);
    }
}
