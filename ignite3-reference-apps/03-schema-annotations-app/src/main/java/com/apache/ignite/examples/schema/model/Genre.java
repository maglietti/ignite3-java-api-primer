package com.apache.ignite.examples.schema.model;

import org.apache.ignite.catalog.annotations.Column;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Zone;

/**
 * Genre entity demonstrating reference data patterns with replication zones.
 * 
 * Shows distribution zone configuration for read-heavy reference data,
 * higher replication factors for availability, and lookup table optimization
 * patterns in distributed systems.
 */
@Table(zone = @Zone(value = "MusicStoreReplicated", storageProfiles = "default"))
public class Genre {
    
    @Id
    private Integer GenreId;
    
    @Column(value = "Name", length = 120, nullable = true)
    private String Name;
    
    public Genre() {
    }
    
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