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

package com.apache.ignite.examples.schema.model;

import org.apache.ignite.catalog.annotations.Column;
import org.apache.ignite.catalog.annotations.ColumnRef;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Index;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Zone;

/**
 * Album entity demonstrating composite primary keys and colocation patterns.
 * 
 * Shows advanced annotation concepts including multi-field primary keys,
 * data colocation for performance optimization, and foreign key indexes.
 * Second level in music catalog hierarchy for distributed data modeling.
 */
@Table(
    zone = @Zone(value = "MusicStore", storageProfiles = "default"),
    colocateBy = @ColumnRef("ArtistId"),
    indexes = @Index(value = "IFK_AlbumArtistId", columns = { @ColumnRef("ArtistId") })
)
public class Album {
    
    @Id
    private Integer AlbumId;
    
    @Id
    private Integer ArtistId;
    
    @Column(value = "Title", length = 160, nullable = false)
    private String Title;
    
    public Album() {
    }
    
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