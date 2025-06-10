package com.example.model;

import org.apache.ignite.catalog.annotations.*;

/**
 * Represents a playlist track in the Chinook database.
 * This class maps to the PlaylistTrack table which contains the many-to-many relationship
 * between playlists and tracks.
 */
@Table(
        zone = @Zone(value = "Chinook", storageProfiles = "default"),
        colocateBy = @ColumnRef("PlaylistId"),
        indexes = {
            @Index(value = "IFK_PlaylistTrackPlaylistId", columns = { @ColumnRef("PlaylistId") }),
            @Index(value = "IFK_PlaylistTrackTrackId", columns = { @ColumnRef("TrackId") })
        }
)
public class PlaylistTrack {
    // Composite primary key fields
    @Id
    @Column(value = "PlaylistId", nullable = false)
    private Integer PlaylistId;

    @Id
    @Column(value = "TrackId", nullable = false)
    private Integer TrackId;

    /**
     * Default constructor required for serialization
     */
    public PlaylistTrack() { }

    /**
     * Constructs a PlaylistTrack with specified details
     *
     * @param playlistId The ID of the playlist
     * @param trackId The ID of the track
     */
    public PlaylistTrack(Integer playlistId, Integer trackId) {
        this.PlaylistId = playlistId;
        this.TrackId = trackId;
    }

    // Getters and setters

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