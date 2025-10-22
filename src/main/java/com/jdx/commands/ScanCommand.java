package com.jdx.commands;

import com.jdx.catalog.JdkCatalogImpl;
import com.jdx.discovery.JdkDiscoveryImpl;
import com.jdx.model.JdkInfo;
import picocli.CommandLine.Command;

import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "scan",
    description = "Discover and catalog all JDKs on this machine"
)
public class ScanCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        System.out.println("Scanning for JDKs...");

        JdkDiscoveryImpl discovery = new JdkDiscoveryImpl();
        List<JdkInfo> jdks = discovery.scan();

        System.out.println("Found " + jdks.size() + " JDK(s)");

        JdkCatalogImpl catalog = new JdkCatalogImpl();
        for (JdkInfo jdk : jdks) {
            catalog.add(jdk);
            System.out.println("  - " + jdk.version() + " (" + jdk.vendor() + ") at " + jdk.path());
        }

        catalog.save();

        System.out.println("Scan complete. Found " + jdks.size() + " JDK(s).");
        System.out.println("Run 'jdx list' to see details.");

        return 0;
    }
}
