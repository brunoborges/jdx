package com.jdx.catalog;

import com.jdx.model.JdkInfo;

import java.util.List;
import java.util.Optional;

/**
 * Interface for managing the catalog of discovered JDKs.
 */
public interface JdkCatalog {
    
    /**
     * Add a JDK to the catalog.
     */
    void add(JdkInfo jdkInfo);
    
    /**
     * Get all JDKs in the catalog.
     */
    List<JdkInfo> getAll();
    
    /**
     * Find a JDK by its ID.
     */
    Optional<JdkInfo> findById(String id);
    
    /**
     * Find JDKs matching a version specification.
     */
    List<JdkInfo> findByVersion(String versionSpec);
    
    /**
     * Save the catalog to disk.
     */
    void save();
    
    /**
     * Load the catalog from disk.
     */
    void load();
}
