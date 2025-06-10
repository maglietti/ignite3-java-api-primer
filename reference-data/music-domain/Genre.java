package com.example.model;

import org.apache.ignite.catalog.annotations.Column;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Zone;

/**
 * Represents a music genre in the Chinook database.
 * This class maps to the Genre table which contains information about music genres.
 */
@Table(
        zone = @Zone(value = "ChinookReplicated", storageProfiles = "default")
)
public class Genre {
    // Primary key field
    @Id
    @Column(value = "GenreId", nullable = false)
    private Integer GenreId;

    @Column(value = "Name", nullable = true)
    private String Name;

    /**
     * Default constructor required for serialization
     */
    public Genre() { }

    /**
     * Constructs a Genre with specified details
     *
     * @param genreId The unique identifier for the genre
     * @param name The name of the genre
     */
    public Genre(Integer genreId, String name) {
        this.GenreId = genreId;
        this.Name = name;
    }

    // Getters and setters

    /**
     * @return The genre's unique identifier
     */
    public Integer getGenreId() {
        return GenreId;
    }

    /**
     * @param genreId The genre's unique identifier to set
     */
    public void setGenreId(Integer genreId) {
        this.GenreId = genreId;
    }

    /**
     * @return The genre's name
     */
    public String getName() {
        return Name;
    }

    /**
     * @param name The genre's name to set
     */
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