package com.example.model;

import org.apache.ignite.catalog.annotations.*;

/**
 * Represents an album in the Chinook database.
 * This class maps to the Album table which contains information about music albums.
 */
@Table(
        zone = @Zone(value = "Chinook", storageProfiles = "default"),
        colocateBy = @ColumnRef("ArtistId"),
        indexes = {
            @Index(value = "IFK_AlbumArtistId", columns = { @ColumnRef("ArtistId") })
        }
)
public class Album {
    // Primary key field
    @Id
    @Column(value = "AlbumId", nullable = false)
    private Integer AlbumId;

    @Column(value = "Title", nullable = false)
    private String Title;

    // Foreign key to Artist table
    @Id
    @Column(value = "ArtistId", nullable = false)
    private Integer ArtistId;

    /**
     * Default constructor required for serialization
     */
    public Album() { }

    /**
     * Constructs an Album with specified details
     *
     * @param albumId The unique identifier for the album
     * @param title The title of the album
     * @param artistId The ID of the artist who created the album
     */
    public Album(Integer albumId, String title, Integer artistId) {
        this.AlbumId = albumId;
        this.Title = title;
        this.ArtistId = artistId;
    }

    // Getters and setters

    /**
     * @return The album's unique identifier
     */
    public Integer getAlbumId() {
        return AlbumId;
    }

    /**
     * @param albumId The album's unique identifier to set
     */
    public void setAlbumId(Integer albumId) {
        this.AlbumId = albumId;
    }

    /**
     * @return The album's title
     */
    public String getTitle() {
        return Title;
    }

    /**
     * @param title The album's title to set
     */
    public void setTitle(String title) {
        this.Title = title;
    }

    /**
     * @return The ID of the artist who created this album
     */
    public Integer getArtistId() {
        return ArtistId;
    }

    /**
     * @param artistId The ID of the artist to set
     */
    public void setArtistId(Integer artistId) {
        this.ArtistId = artistId;
    }

    @Override
    public String toString() {
        return "Album{" +
                "AlbumId=" + AlbumId +
                ", Title='" + Title + '\'' +
                ", ArtistId=" + ArtistId +
                '}';
    }
}