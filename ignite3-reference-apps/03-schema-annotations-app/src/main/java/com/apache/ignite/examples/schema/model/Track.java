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

import java.math.BigDecimal;
import org.apache.ignite.catalog.annotations.Column;
import org.apache.ignite.catalog.annotations.ColumnRef;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Index;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Zone;

/**
 * Track entity demonstrating complex colocation and multi-field annotations.
 * 
 * Shows advanced distributed data modeling including multi-level colocation,
 * composite primary keys, foreign key relationships, and complex field types.
 * Third level in music catalog hierarchy for performance optimization.
 */
@Table(
    zone = @Zone(value = "MusicStore", storageProfiles = "default"),
    colocateBy = @ColumnRef("AlbumId"),
    indexes = {
        @Index(value = "IFK_TrackAlbumId", columns = { @ColumnRef("AlbumId") }),
        @Index(value = "IFK_TrackGenreId", columns = { @ColumnRef("GenreId") }),
        @Index(value = "IFK_TrackMediaTypeId", columns = { @ColumnRef("MediaTypeId") })
    }
)
public class Track {
    
    @Id
    private Integer TrackId;
    
    @Id
    private Integer AlbumId;
    
    @Column(value = "Name", length = 200, nullable = false)
    private String Name;
    
    @Column(value = "MediaTypeId", nullable = false)
    private Integer MediaTypeId;
    
    @Column(value = "GenreId", nullable = true)
    private Integer GenreId;
    
    @Column(value = "Composer", length = 220, nullable = true)
    private String Composer;
    
    @Column(value = "Milliseconds", nullable = false)
    private Integer Milliseconds;
    
    @Column(value = "Bytes", nullable = true)
    private Integer Bytes;
    
    @Column(value = "UnitPrice", precision = 10, scale = 2, nullable = false)
    private BigDecimal UnitPrice;
    
    public Track() {
    }
    
    public Track(Integer trackId, Integer albumId, String name, Integer mediaTypeId, 
                 Integer genreId, String composer, Integer milliseconds, Integer bytes, BigDecimal unitPrice) {
        this.TrackId = trackId;
        this.AlbumId = albumId;
        this.Name = name;
        this.MediaTypeId = mediaTypeId;
        this.GenreId = genreId;
        this.Composer = composer;
        this.Milliseconds = milliseconds;
        this.Bytes = bytes;
        this.UnitPrice = unitPrice;
    }
    
    public Integer getTrackId() {
        return TrackId;
    }
    
    public void setTrackId(Integer trackId) {
        this.TrackId = trackId;
    }
    
    public Integer getAlbumId() {
        return AlbumId;
    }
    
    public void setAlbumId(Integer albumId) {
        this.AlbumId = albumId;
    }
    
    public String getName() {
        return Name;
    }
    
    public void setName(String name) {
        this.Name = name;
    }
    
    public Integer getMediaTypeId() {
        return MediaTypeId;
    }
    
    public void setMediaTypeId(Integer mediaTypeId) {
        this.MediaTypeId = mediaTypeId;
    }
    
    public Integer getGenreId() {
        return GenreId;
    }
    
    public void setGenreId(Integer genreId) {
        this.GenreId = genreId;
    }
    
    public String getComposer() {
        return Composer;
    }
    
    public void setComposer(String composer) {
        this.Composer = composer;
    }
    
    public Integer getMilliseconds() {
        return Milliseconds;
    }
    
    public void setMilliseconds(Integer milliseconds) {
        this.Milliseconds = milliseconds;
    }
    
    public Integer getBytes() {
        return Bytes;
    }
    
    public void setBytes(Integer bytes) {
        this.Bytes = bytes;
    }
    
    public BigDecimal getUnitPrice() {
        return UnitPrice;
    }
    
    public void setUnitPrice(BigDecimal unitPrice) {
        this.UnitPrice = unitPrice;
    }
    
    @Override
    public String toString() {
        return "Track{" +
                "TrackId=" + TrackId +
                ", AlbumId=" + AlbumId +
                ", Name='" + Name + '\'' +
                ", MediaTypeId=" + MediaTypeId +
                ", GenreId=" + GenreId +
                ", Composer='" + Composer + '\'' +
                ", Milliseconds=" + Milliseconds +
                ", Bytes=" + Bytes +
                ", UnitPrice=" + UnitPrice +
                '}';
    }
}