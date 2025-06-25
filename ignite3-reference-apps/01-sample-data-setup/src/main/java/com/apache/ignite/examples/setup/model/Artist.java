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
 * Artist entity representing music artists and bands in the music store.
 * 
 * This is the root entity in the music hierarchy: Artist → Album → Track
 * 
 * Key Ignite 3 concepts demonstrated:
 * - Schema-as-Code: Table definition through annotations
 * - Distribution Zones: Placement in "MusicStore" zone with 2 replicas
 * - Primary Key: Simple integer key for efficient lookups
 * - Column Mapping: Custom column definitions with constraints
 */
@Table(
    // Place this table in the "MusicStore" distribution zone
    // This zone has 2 replicas for high availability of business data
    zone = @Zone(value = "MusicStore", storageProfiles = "default")
)
public class Artist {
    
    /**
     * Primary key for the Artist table.
     * Ignite 3 uses this for data distribution across cluster nodes.
     */
    @Id
    private Integer ArtistId;
    
    /**
     * Artist or band name with explicit column mapping.
     * - length=120: Maximum string length for the database column
     * - nullable=false: This field is required (NOT NULL constraint)
     */
    @Column(value = "Name", length = 120, nullable = false)
    private String Name;
    
    /**
     * Default constructor required by Ignite 3 for object deserialization.
     * Always include a no-args constructor in your entity classes.
     */
    public Artist() {
    }
    
    /**
     * Convenience constructor for creating new Artist instances.
     * 
     * @param artistId Unique identifier for the artist
     * @param name Artist or band name
     */
    public Artist(Integer artistId, String name) {
        this.ArtistId = artistId;
        this.Name = name;
    }
    
    public Integer getArtistId() {
        return ArtistId;
    }
    
    public void setArtistId(Integer artistId) {
        this.ArtistId = artistId;
    }
    
    public String getName() {
        return Name;
    }
    
    public void setName(String name) {
        this.Name = name;
    }
    
    @Override
    public String toString() {
        return "Artist{" +
                "ArtistId=" + ArtistId +
                ", Name='" + Name + '\'' +
                '}';
    }
}