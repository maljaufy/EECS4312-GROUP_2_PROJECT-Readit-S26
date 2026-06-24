package com.redditclone.shared.port;

import java.util.List;
import java.util.Map;

public interface DeltaLakePort {

    /*
    Delta Lake port interface: Interface for Delta Lake operation
    */
    // Bronze Layer - Raw events
    void writeBronze(String tableName, Map<String, Object> data);
    void writeBronzeBatch(String tableName, List<Map<String, Object>> data);

    // Silver Layer - Cleaned/Validated data
    void writeSilver(String tableName, Map<String, Object> data);
    void writeSilverBatch(String tableName, List<Map<String, Object>> data);

    // Gold Layer - Aggregated facts
    void writeGold(String tableName, Map<String, Object> data);
    void writeGoldBatch(String tableName, List<Map<String, Object>> data);

    // Read from Gold tables (for analytics dashboards)
    List<Map<String, Object>> readGold(String query);

    // Utility operations
    void createTable(String tableName, String schemaJson);
    boolean tableExists(String tableName);
    void optimize(String tableName);
    void vacuum(String tableName);
}
