/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.apache.ignite.examples.setup.util;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apache.ignite.examples.setup.config.IgniteConfiguration;

/**
 * Connection utilities for Apache Ignite 3 cluster operations.
 * 
 * Provides basic connection management and table access operations
 * for the music store sample data setup.
 */
public class ConnectionUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectionUtils.class);
    
    /**
     * Creates a connection to the Ignite cluster using default address.
     * 
     * @return IgniteClient instance connected to 127.0.0.1:10800
     */
    public static IgniteClient connectToCluster() {
        return IgniteConfiguration.createClient();
    }
    
    /**
     * Creates a connection to the Ignite cluster using specified address.
     * 
     * @param address Cluster address in format "host:port"
     * @return IgniteClient instance connected to the specified address
     */
    public static IgniteClient connectToCluster(String address) {
        logger.info("    >>> Creating Ignite client connection to: {}", address);
        
        IgniteClient client = IgniteConfiguration.createClient(address);
        
        logger.info("    <<< Successfully connected to Ignite cluster at: {}", address);
        logger.info("");
        
        return client;
    }
    
    /**
     * Checks if a table exists in the cluster.
     * 
     * @param client IgniteClient instance
     * @param tableName Name of the table to check
     * @return true if table exists, false otherwise
     */
    public static boolean tableExists(IgniteClient client, String tableName) {
        try {
            Table table = client.tables().table(tableName);
            return table != null;
        } catch (Exception e) {
            logger.debug("Table '{}' does not exist: {}", tableName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets the row count for a specific table.
     * 
     * @param client IgniteClient instance
     * @param tableName Name of the table
     * @return Number of rows in the table, -1 if error
     */
    public static long getTableRowCount(IgniteClient client, String tableName) {
        try {
            var resultSet = client.sql().execute(null, 
                "SELECT COUNT(*) as row_count FROM " + tableName);
            
            if (resultSet.hasNext()) {
                return resultSet.next().longValue("row_count");
            }
            return 0;
        } catch (Exception e) {
            logger.error("Error getting row count for table '{}': {}", tableName, e.getMessage());
            return -1;
        }
    }
}