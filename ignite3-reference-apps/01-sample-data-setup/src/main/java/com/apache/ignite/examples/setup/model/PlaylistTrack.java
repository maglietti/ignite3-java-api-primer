package com.apache.ignite.examples.setup.model;

import org.apache.ignite.catalog.annotations.ColumnRef;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Index;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Zone;

@Table(
    zone = @Zone(value = "MusicStore", storageProfiles = "default"),
    colocateBy = @ColumnRef("PlaylistId"),
    indexes = {
        @Index(value = "IFK_PlaylistTrackPlaylistId", columns = { @ColumnRef("PlaylistId") }),
        @Index(value = "IFK_PlaylistTrackTrackId", columns = { @ColumnRef("TrackId") })
    }
)
public class PlaylistTrack {
    
    @Id
    private Integer PlaylistId;
    
    @Id
    private Integer TrackId;
    
    public PlaylistTrack() {
    }
    
    public PlaylistTrack(Integer playlistId, Integer trackId) {
        this.PlaylistId = playlistId;
        this.TrackId = trackId;
    }
    
    public Integer getPlaylistId() {
        return PlaylistId;
    }
    
    public void setPlaylistId(Integer playlistId) {
        this.PlaylistId = playlistId;
    }
    
    public Integer getTrackId() {
        return TrackId;
    }
    
    public void setTrackId(Integer trackId) {
        this.TrackId = trackId;
    }
    
    @Override
    public String toString() {
        return "PlaylistTrack{" +
                "PlaylistId=" + PlaylistId +
                ", TrackId=" + TrackId +
                '}';
    }
}