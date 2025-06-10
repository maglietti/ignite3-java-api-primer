package com.example.model;

import org.apache.ignite.catalog.annotations.*;

import java.math.BigDecimal;

/**
 * Represents a track (song) in the Chinook database.
 * This class maps to the Track table which contains information about music tracks.
 */
@Table(
        zone = @Zone(value = "Chinook", storageProfiles = "default"),
        colocateBy = @ColumnRef("AlbumId"),
        indexes = {
            @Index(value = "IFK_TrackAlbumId", columns = { @ColumnRef("AlbumId") }),
            @Index(value = "IFK_TrackGenreId", columns = { @ColumnRef("GenreId") }),
            @Index(value = "IFK_TrackMediaTypeId", columns = { @ColumnRef("MediaTypeId") })
        }
)
public class Track {
    // Primary key field
    @Id
    @Column(value = "TrackId", nullable = false)
    private Integer TrackId;

    @Column(value = "Name", nullable = false)
    private String Name;

    // Foreign keys
    @Id
    @Column(value = "AlbumId", nullable = true)
    private Integer AlbumId;

    @Column(value = "MediaTypeId", nullable = false)
    private Integer MediaTypeId;

    @Column(value = "GenreId", nullable = true)
    private Integer GenreId;

    @Column(value = "Composer", nullable = true)
    private String Composer;

    @Column(value = "Milliseconds", nullable = false)
    private Integer Milliseconds;

    @Column(value = "Bytes", nullable = true)
    private Integer Bytes;

    @Column(value = "UnitPrice", nullable = false)
    private BigDecimal UnitPrice;

    /**
     * Default constructor required for serialization
     */
    public Track() { }

    /**
     * Constructs a Track with specified details
     *
     * @param trackId The unique identifier for the track
     * @param name The name of the track
     * @param albumId The ID of the album this track belongs to
     * @param mediaTypeId The ID of the media type (e.g., MPEG, AAC)
     * @param genreId The ID of the music genre
     * @param composer The composer of the track
     * @param milliseconds The length of the track in milliseconds
     * @param bytes The size of the track in bytes
     * @param unitPrice The price per unit
     */
    public Track(Integer trackId, String name, Integer albumId, Integer mediaTypeId,
                 Integer genreId, String composer, Integer milliseconds,
                 Integer bytes, BigDecimal unitPrice) {
        this.TrackId = trackId;
        this.Name = name;
        this.AlbumId = albumId;
        this.MediaTypeId = mediaTypeId;
        this.GenreId = genreId;
        this.Composer = composer;
        this.Milliseconds = milliseconds;
        this.Bytes = bytes;
        this.UnitPrice = unitPrice;
    }

    // Getters and setters

    public Integer getTrackId() { return TrackId; }
    public void setTrackId(Integer trackId) { this.TrackId = trackId; }

    public String getName() { return Name; }
    public void setName(String name) { this.Name = name; }

    public Integer getAlbumId() { return AlbumId; }
    public void setAlbumId(Integer albumId) { this.AlbumId = albumId; }

    public Integer getMediaTypeId() { return MediaTypeId; }
    public void setMediaTypeId(Integer mediaTypeId) { this.MediaTypeId = mediaTypeId; }

    public Integer getGenreId() { return GenreId; }
    public void setGenreId(Integer genreId) { this.GenreId = genreId; }

    public String getComposer() { return Composer; }
    public void setComposer(String composer) { this.Composer = composer; }

    public Integer getMilliseconds() { return Milliseconds; }
    public void setMilliseconds(Integer milliseconds) { this.Milliseconds = milliseconds; }

    public Integer getBytes() { return Bytes; }
    public void setBytes(Integer bytes) { this.Bytes = bytes; }

    public BigDecimal getUnitPrice() { return UnitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.UnitPrice = unitPrice; }

    @Override
    public String toString() {
        return "Track{" +
                "TrackId=" + TrackId +
                ", Name='" + Name + '\'' +
                ", AlbumId=" + AlbumId +
                ", MediaTypeId=" + MediaTypeId +
                ", GenreId=" + GenreId +
                ", Composer='" + Composer + '\'' +
                ", Milliseconds=" + Milliseconds +
                ", Bytes=" + Bytes +
                ", UnitPrice=" + UnitPrice +
                '}';
    }
}