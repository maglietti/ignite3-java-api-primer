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
import org.apache.ignite.catalog.annotations.ColumnRef;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Index;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Zone;

/**
 * Album entity representing music albums in the music store.
 * 
 * This demonstrates advanced Ignite 3 concepts:
 * - Composite Primary Keys: Multi-field primary key for distributed systems
 * - Data Colocation: Albums are placed on the same nodes as their Artists
 * - Foreign Key Indexes: Index on ArtistId for efficient joins
 * - Hierarchical Data: Second level in Artist → Album → Track hierarchy
 */
@Table(
    // Same zone as Artist for consistency
    zone = @Zone(value = "MusicStore", storageProfiles = "default"),
    
    // CRITICAL: Colocate albums with their artists for performance
    // This ensures Album and Artist data for the same ArtistId are on the same cluster node
    // This dramatically improves performance for queries like "get artist with albums"
    colocateBy = @ColumnRef("ArtistId"),
    
    // Create an index on ArtistId for efficient foreign key lookups
    // This supports fast queries like "find all albums by artist"
    indexes = @Index(value = "IFK_AlbumArtistId", columns = { @ColumnRef("ArtistId") })
)
public class Album {
    
    /**
     * Album identifier - part of composite primary key.
     * Combined with ArtistId to ensure uniqueness across the distributed cluster.
     */
    @Id
    private Integer AlbumId;
    
    /**
     * Artist identifier - part of composite primary key AND colocation key.
     * This serves multiple purposes:
     * 1. Part of the primary key for uniqueness
     * 2. Foreign key reference to Artist table
     * 3. Colocation key to ensure albums are stored with their artists
     */
    @Id
    private Integer ArtistId;
    
    /**
     * Album title with database column constraints.
     * Length is generous to accommodate long album titles.
     */
    @Column(value = "Title", length = 160, nullable = false)
    private String Title;
    
    /**
     * Default constructor required by Ignite 3 for deserialization.
     * Essential for all entity classes.
     */
    public Album() {
    }
    
    /**
     * Convenience constructor for creating Album instances.
     * 
     * @param albumId Unique album identifier
     * @param artistId Artist who created this album (also colocation key)
     * @param title Album title
     */
    public Album(Integer albumId, Integer artistId, String title) {
        this.AlbumId = albumId;
        this.ArtistId = artistId;
        this.Title = title;
    }
    
    public Integer getAlbumId() {
        return AlbumId;
    }
    
    public void setAlbumId(Integer albumId) {
        this.AlbumId = albumId;
    }
    
    public Integer getArtistId() {
        return ArtistId;
    }
    
    public void setArtistId(Integer artistId) {
        this.ArtistId = artistId;
    }
    
    public String getTitle() {
        return Title;
    }
    
    public void setTitle(String title) {
        this.Title = title;
    }
    
    @Override
    public String toString() {
        return "Album{" +
                "AlbumId=" + AlbumId +
                ", ArtistId=" + ArtistId +
                ", Title='" + Title + '\'' +
                '}';
    }
}