-- ============================================================================
-- GridGain 9 / Apache Ignite 3: Zone Replication Verification Script
-- ============================================================================
--
-- PURPOSE
-- Diagnose row count discrepancies between Control Center and SQL queries.
-- When Control Center shows 2-4x higher row counts than SELECT COUNT(*),
-- the cause is partition statistics being summed across all replicas.
--
-- HOW TO USE
-- Run this script in the SQL CLI or any JDBC client. Each section builds
-- on the previous one, guiding you from cluster health through to the
-- root cause of row count discrepancies.
--
-- ============================================================================


-- ============================================================================
-- SECTION 1: CLUSTER AND REPLICATION HEALTH
-- ============================================================================
-- This section answers three questions:
--   1. How many nodes are in the cluster?
--   2. What replication settings are configured for each zone?
--   3. Is the cluster healthy enough to support the configured replication?
--
-- A cluster needs at least N nodes to support REPLICAS=N. If MIN_REPLICAS
-- is less than ZONE_REPLICAS, some partitions are under-replicated.
--
-- STATUS column values:
--   HEALTHY - All partitions have the expected number of replicas
--   UNDER-REPLICATED - Some partitions have fewer replicas than configured
--   UNEVEN - Inconsistent replica counts across partitions
--
-- When STATUS is not HEALTHY, partitions may be in one of these states:
--   INITIALIZING - Partition starting up
--   INSTALLING_SNAPSHOT - Receiving data from leader
--   CATCHING_UP - Replicating log entries, not yet current
--   UNAVAILABLE - Not started or stopping
--   BROKEN - State machine error (requires attention)
-- ============================================================================

SELECT '1. CLUSTER AND REPLICATION HEALTH' AS _;

SELECT
    (SELECT COUNT(DISTINCT NODE_NAME) FROM SYSTEM.LOCAL_ZONE_PARTITION_STATES) AS CLUSTER_NODES,
    z.ZONE_NAME,
    z.ZONE_REPLICAS,
    z.ZONE_PARTITIONS,
    MIN(replica_stats.REPLICA_COUNT) AS MIN_REPLICAS,
    MAX(replica_stats.REPLICA_COUNT) AS MAX_REPLICAS,
    CASE
        WHEN MIN(replica_stats.REPLICA_COUNT) < z.ZONE_REPLICAS THEN 'UNDER-REPLICATED'
        WHEN MIN(replica_stats.REPLICA_COUNT) = MAX(replica_stats.REPLICA_COUNT) THEN 'HEALTHY'
        ELSE 'UNEVEN'
    END AS STATUS
FROM SYSTEM.ZONES z
JOIN (
    SELECT ZONE_NAME, PARTITION_ID, COUNT(*) AS REPLICA_COUNT
    FROM SYSTEM.LOCAL_ZONE_PARTITION_STATES
    GROUP BY ZONE_NAME, PARTITION_ID
) replica_stats ON z.ZONE_NAME = replica_stats.ZONE_NAME
GROUP BY z.ZONE_NAME, z.ZONE_REPLICAS, z.ZONE_PARTITIONS
ORDER BY z.ZONE_NAME;


-- ============================================================================
-- SECTION 2: TABLES AND ZONES
-- ============================================================================
-- Tables are assigned to distribution zones. All tables in the same zone
-- share the same replication and partitioning configuration.
--
-- Partition statistics (Section 3) are aggregated at zone level, not per
-- table. To verify per-table row counts, use the template query below.
-- ============================================================================

SELECT '2. TABLES AND ZONES' AS _;

SELECT
    ZONE_NAME,
    TABLE_NAME
FROM SYSTEM.TABLES
WHERE ZONE_NAME IS NOT NULL
ORDER BY ZONE_NAME, TABLE_NAME;


-- ============================================================================
-- SECTION 2b: PER-TABLE ROW COUNTS (customize for your schema)
-- ============================================================================
-- Partition statistics cannot be broken down by table. To verify actual
-- row counts, run COUNT(*) on each table. Modify the query below for your
-- schema, then compare the sum per zone against ESTIMATED_UNIQUE_ROWS in
-- Section 3.
--
-- Example for Music Store schema (uncomment to run):
-- ============================================================================

SELECT '2b. PER-TABLE ROW COUNTS: Customize query below for your schema' AS _;

SELECT 'MUSICSTORE' AS ZONE_NAME, 'Album' AS TABLE_NAME, COUNT(*) AS ROW_COUNT FROM Album
UNION ALL SELECT 'MUSICSTORE', 'Artist', COUNT(*) FROM Artist
UNION ALL SELECT 'MUSICSTORE', 'Customer', COUNT(*) FROM Customer
UNION ALL SELECT 'MUSICSTORE', 'Employee', COUNT(*) FROM Employee
UNION ALL SELECT 'MUSICSTORE', 'Invoice', COUNT(*) FROM Invoice
UNION ALL SELECT 'MUSICSTORE', 'InvoiceLine', COUNT(*) FROM InvoiceLine
UNION ALL SELECT 'MUSICSTORE', 'Playlist', COUNT(*) FROM Playlist
UNION ALL SELECT 'MUSICSTORE', 'PlaylistTrack', COUNT(*) FROM PlaylistTrack
UNION ALL SELECT 'MUSICSTORE', 'Track', COUNT(*) FROM Track
UNION ALL SELECT 'MUSICSTOREREPLICATED', 'Genre', COUNT(*) FROM Genre
UNION ALL SELECT 'MUSICSTOREREPLICATED', 'MediaType', COUNT(*) FROM MediaType
ORDER BY ZONE_NAME, TABLE_NAME;


-- ============================================================================
-- SECTION 3: THE ROW COUNT DISCREPANCY PROVEN
-- ============================================================================
-- This query directly compares actual row counts against partition statistics
-- to prove the replication multiplier formula:
--
--   partition_statistics = actual_rows x replication_factor
--
-- If Control Center displays raw partition statistics without dividing by
-- the replication factor, it will show 2-3x the actual row count.
--
-- COLUMNS EXPLAINED:
--   ACTUAL_ROWS: Real row count from COUNT(*) summed by zone
--   REPLICATION_FACTOR: Configured replicas for the zone
--   EXPECTED_REPLICATED: ACTUAL_ROWS x REPLICATION_FACTOR (calculated)
--   ACTUAL_REPLICATED: What partition statistics report
--
-- EXPECTED_REPLICATED should equal ACTUAL_REPLICATED, proving the formula.
--
-- NOTE: Modify the actual_counts CTE below to match your schema.
-- ============================================================================

SELECT '3. ROW COUNT DISCREPANCY: Direct comparison proves the formula' AS _;

WITH actual_counts AS (
    SELECT 'MUSICSTORE' AS ZONE_NAME, COUNT(*) AS ROW_COUNT FROM Album
    UNION ALL SELECT 'MUSICSTORE', COUNT(*) FROM Artist
    UNION ALL SELECT 'MUSICSTORE', COUNT(*) FROM Customer
    UNION ALL SELECT 'MUSICSTORE', COUNT(*) FROM Employee
    UNION ALL SELECT 'MUSICSTORE', COUNT(*) FROM Invoice
    UNION ALL SELECT 'MUSICSTORE', COUNT(*) FROM InvoiceLine
    UNION ALL SELECT 'MUSICSTORE', COUNT(*) FROM Playlist
    UNION ALL SELECT 'MUSICSTORE', COUNT(*) FROM PlaylistTrack
    UNION ALL SELECT 'MUSICSTORE', COUNT(*) FROM Track
    UNION ALL SELECT 'MUSICSTOREREPLICATED', COUNT(*) FROM Genre
    UNION ALL SELECT 'MUSICSTOREREPLICATED', COUNT(*) FROM MediaType
),
actual_by_zone AS (
    SELECT ZONE_NAME, SUM(ROW_COUNT) AS ACTUAL_ROWS
    FROM actual_counts
    GROUP BY ZONE_NAME
),
partition_totals AS (
    SELECT ZONE_NAME, SUM(ESTIMATED_ROWS) AS TOTAL_REPLICATED_ROWS
    FROM SYSTEM.LOCAL_ZONE_PARTITION_STATES
    GROUP BY ZONE_NAME
)
SELECT
    z.ZONE_NAME,
    a.ACTUAL_ROWS,
    z.ZONE_REPLICAS AS REPLICATION_FACTOR,
    a.ACTUAL_ROWS * z.ZONE_REPLICAS AS EXPECTED_REPLICATED,
    pt.TOTAL_REPLICATED_ROWS AS ACTUAL_REPLICATED
FROM SYSTEM.ZONES z
JOIN actual_by_zone a ON z.ZONE_NAME = a.ZONE_NAME
JOIN partition_totals pt ON z.ZONE_NAME = pt.ZONE_NAME
ORDER BY z.ZONE_NAME;


-- ============================================================================
-- SUMMARY
-- ============================================================================
-- If Control Center shows row counts 2-4x higher than SQL queries:
--
-- 1. Check REPLICATION_FACTOR in Section 3
-- 2. Compare SUM_PARTITION_ESTIMATES vs ESTIMATED_UNIQUE_ROWS
-- 3. The ratio should equal your replication factor
--
-- This is expected behavior in the partition statistics. Control Center
-- should divide by replication factor when displaying row counts, or
-- clearly label the metric as "replicated storage" rather than "row count."
-- ============================================================================

SELECT 'SUMMARY: Row count discrepancy = actual_rows x replication_factor' AS _;
