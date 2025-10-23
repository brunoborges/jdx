package com.jdx.commands;

import com.jdx.catalog.JdkCatalog;
import com.jdx.catalog.JdkCatalogImpl;
import com.jdx.discovery.JdkDiscovery;
import com.jdx.discovery.JdkDiscoveryImpl;
import com.jdx.model.JdkInfo;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;

/**
 * Command to scan the system for installed JDKs and save them to the catalog.
 */
@Command(name = "scan", description = "Scan for installed JDKs and update catalog")
public class ScanCommand implements Runnable {
    
    @Option(names = {"--deep"}, description = "Perform deep scan, searching beyond standard JDK installation locations")
    private boolean deep;
    
    private final JdkDiscovery discovery;
    private final JdkCatalog catalog;
    
    public ScanCommand() {
        this.discovery = new JdkDiscoveryImpl();
        this.catalog = new JdkCatalogImpl();
    }
    
    @Override
    public void run() {
        System.out.println("Scanning for JDK installations" + (deep ? " (deep scan)..." : "..."));
        
        List<JdkInfo> jdks = deep ? discovery.deepScan() : discovery.scan();
        
        if (jdks.isEmpty()) {
            System.out.println("No JDKs found.");
            return;
        }
        
        System.out.println("\nFound " + jdks.size() + " JDK(s):");
        for (JdkInfo jdk : jdks) {
            System.out.println("  - " + jdk.id() + ": " + jdk.version() + " (" + jdk.vendor() + ") at " + jdk.path());
        }
        
        // Save to catalog
        for (JdkInfo jdk : jdks) {
            catalog.add(jdk);
        }
        catalog.save();
        System.out.println("\nCatalog updated successfully.");
    }
}
