package com.jdx.commands;

import com.jdx.catalog.JdkCatalogImpl;
import com.jdx.model.JdkInfo;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "list",
    description = "List all discovered JDKs"
)
public class ListCommand implements Callable<Integer> {
    
    @Option(names = {"--json"}, description = "Output in JSON format")
    private boolean json;

    @Override
    public Integer call() throws Exception {
        JdkCatalogImpl catalog = new JdkCatalogImpl();
        List<JdkInfo> jdks = catalog.getAll();

        if (jdks.isEmpty()) {
            System.out.println("No JDKs found. Run 'jdx scan' to discover JDKs.");
            return 0;
        }

        if (json) {
            // Simple JSON output
            System.out.println("[");
            for (int i = 0; i < jdks.size(); i++) {
                JdkInfo jdk = jdks.get(i);
                System.out.printf("  {\"id\": \"%s\", \"version\": \"%s\", \"vendor\": \"%s\", \"arch\": \"%s\", \"path\": \"%s\", \"valid\": %b}%s%n",
                    jdk.id(), jdk.version(), jdk.vendor(), jdk.arch(), jdk.path(), jdk.valid(),
                    i < jdks.size() - 1 ? "," : "");
            }
            System.out.println("]");
        } else {
            // Table format
            System.out.printf("%-30s %-15s %-20s %-80s%n",
                "ID", "VERSION", "VENDOR", "PATH");
            System.out.println("-".repeat(150));
            
            for (JdkInfo jdk : jdks) {
                System.out.printf("%-30s %-15s %-20s %-80s%n",
                    jdk.id(),
                    jdk.version(),
                    truncate(jdk.vendor(), 20),
                    truncate(jdk.path(), 80));
            }
            
            System.out.println("\nTotal: " + jdks.size() + " JDK(s)");
        }

        return 0;
    }

    private String truncate(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }
}
