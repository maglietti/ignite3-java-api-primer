package com.apache.ignite.examples.schema.model;

import org.apache.ignite.catalog.annotations.Column;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Zone;

/**
 * Artist entity demonstrating basic Ignite 3 annotation patterns.
 * 
 * Shows fundamental schema-as-code concepts including table mapping,
 * distribution zone assignment, and simple primary key patterns.
 * Root entity in music catalog hierarchy for colocation demonstrations.
 */
@Table(zone = @Zone(value = "MusicStore", storageProfiles = "default"))
public class Artist {
    
    @Id
    private Integer ArtistId;
    
    @Column(value = "Name", length = 120, nullable = false)
    private String Name;
    
    public Artist() {
    }
    
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