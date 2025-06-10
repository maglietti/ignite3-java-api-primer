package com.example.model;

import org.apache.ignite.catalog.annotations.Column;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Zone;

/**
 * Represents a media type in the Chinook database.
 * This class maps to the MediaType table which contains information about different media formats.
 */
@Table(
        zone = @Zone(value = "ChinookReplicated", storageProfiles = "default")
)
public class MediaType {
    // Primary key field
    @Id
    @Column(value = "MediaTypeId", nullable = false)
    private Integer MediaTypeId;

    @Column(value = "Name", nullable = true)
    private String Name;

    /**
     * Default constructor required for serialization
     */
    public MediaType() { }

    /**
     * Constructs a MediaType with specified details
     *
     * @param mediaTypeId The unique identifier for the media type
     * @param name The name of the media type
     */
    public MediaType(Integer mediaTypeId, String name) {
        this.MediaTypeId = mediaTypeId;
        this.Name = name;
    }

    // Getters and setters

    /**
     * @return The media type's unique identifier
     */
    public Integer getMediaTypeId() {
        return MediaTypeId;
    }

    /**
     * @param mediaTypeId The media type's unique identifier to set
     */
    public void setMediaTypeId(Integer mediaTypeId) {
        this.MediaTypeId = mediaTypeId;
    }

    /**
     * @return The media type's name
     */
    public String getName() {
        return Name;
    }

    /**
     * @param name The media type's name to set
     */
    public void setName(String name) {
        this.Name = name;
    }

    @Override
    public String toString() {
        return "MediaType{" +
                "MediaTypeId=" + MediaTypeId +
                ", Name='" + Name + '\'' +
                '}';
    }
}