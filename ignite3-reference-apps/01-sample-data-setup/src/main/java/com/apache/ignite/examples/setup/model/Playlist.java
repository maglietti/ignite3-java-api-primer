package com.apache.ignite.examples.setup.model;

import org.apache.ignite.catalog.annotations.Column;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Zone;

@Table(
    zone = @Zone(value = "MusicStore", storageProfiles = "default")
)
public class Playlist {
    
    @Id
    private Integer PlaylistId;
    
    @Column(value = "Name", length = 120, nullable = true)
    private String Name;
    
    public Playlist() {
    }
    
    public Playlist(Integer playlistId, String name) {
        this.PlaylistId = playlistId;
        this.Name = name;
    }
    
    public Integer getPlaylistId() {
        return PlaylistId;
    }
    
    public void setPlaylistId(Integer playlistId) {
        this.PlaylistId = playlistId;
    }
    
    public String getName() {
        return Name;
    }
    
    public void setName(String name) {
        this.Name = name;
    }
    
    @Override
    public String toString() {
        return "Playlist{" +
                "PlaylistId=" + PlaylistId +
                ", Name='" + Name + '\'' +
                '}';
    }
}