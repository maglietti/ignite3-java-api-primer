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

package com.apache.ignite.examples.setup.model;

import org.apache.ignite.catalog.annotations.Column;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Zone;

/**
 * Genre entity representing music genre classifications (Rock, Jazz, Classical, etc.).
 * 
 * This demonstrates reference data patterns in distributed systems:
 * - Replicated Zone: Higher replication factor for frequently accessed lookup data
 * - Read-Heavy Optimization: 3 replicas across cluster for fast reads from any node
 * - Small Dataset: Lookup tables like genres are typically small and read frequently
 * - No Colocation: Reference data doesn't need colocation since it's replicated everywhere
 * 
 * Design Pattern: This represents the "Reference Data" pattern where small, 
 * frequently-read lookup tables are replicated across more nodes than transactional data.
 */
@Table(
    // Place in replicated zone for maximum read performance
    // MusicStoreReplicated has 3 replicas vs 2 for transactional data
    // This means genre data is available on more nodes for faster lookups
    zone = @Zone(value = "MusicStoreReplicated", storageProfiles = "default")
)
public class Genre {
    
    /**
     * Genre identifier - simple primary key for lookup table.
     * Integer IDs are efficient for joins and foreign key references.
     */
    @Id
    private Integer GenreId;
    
    /**
     * Genre name (Rock, Jazz, Classical, Metal, etc.).
     * Nullable=true follows original schema, though typically genres have names.
     */
    @Column(value = "Name", length = 120, nullable = true)
    private String Name;
    
    /**
     * Default constructor required by Ignite 3 for deserialization.
     */
    public Genre() {
    }
    
    /**
     * Constructor for creating Genre instances.
     * 
     * @param genreId Unique genre identifier
     * @param name Genre display name
     */
    public Genre(Integer genreId, String name) {
        this.GenreId = genreId;
        this.Name = name;
    }
    
    public Integer getGenreId() {
        return GenreId;
    }
    
    public void setGenreId(Integer genreId) {
        this.GenreId = genreId;
    }
    
    public String getName() {
        return Name;
    }
    
    public void setName(String name) {
        this.Name = name;
    }
    
    @Override
    public String toString() {
        return "Genre{" +
                "GenreId=" + GenreId +
                ", Name='" + Name + '\'' +
                '}';
    }
}