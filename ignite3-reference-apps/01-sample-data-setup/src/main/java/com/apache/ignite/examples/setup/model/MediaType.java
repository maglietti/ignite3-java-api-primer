package com.apache.ignite.examples.setup.model;

import org.apache.ignite.catalog.annotations.Column;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Zone;

/**
 * MediaType entity representing audio file formats (MP3, AAC, FLAC, WAV, etc.).
 * 
 * Another example of the reference data pattern, similar to Genre:
 * - High Replication: 3 replicas for fast access from any cluster node
 * - Lookup Table: Small, stable dataset that changes infrequently
 * - Join Performance: Available locally on more nodes for Track foreign key joins
 * - Read-Heavy Workload: Media types are read frequently but rarely modified
 * 
 * This pattern is essential for distributed systems where lookup data needs to be
 * readily available without cross-node network calls during joins.
 */
@Table(
    // Same replicated zone strategy as Genre for consistent reference data handling
    // Higher replication factor ensures media type lookups are always fast
    zone = @Zone(value = "MusicStoreReplicated", storageProfiles = "default")
)
public class MediaType {
    
    /**
     * Media type identifier for efficient foreign key references.
     * Used by Track entities to specify audio format.
     */
    @Id
    private Integer MediaTypeId;
    
    /**
     * Media type description (MP3, AAC, FLAC, Protected AAC, etc.).
     * Describes the audio file format and encoding.
     */
    @Column(value = "Name", length = 120, nullable = true)
    private String Name;
    
    /**
     * Default constructor required by Ignite 3.
     */
    public MediaType() {
    }
    
    /**
     * Constructor for creating MediaType instances.
     * 
     * @param mediaTypeId Unique media type identifier
     * @param name Media type description/name
     */
    public MediaType(Integer mediaTypeId, String name) {
        this.MediaTypeId = mediaTypeId;
        this.Name = name;
    }
    
    public Integer getMediaTypeId() {
        return MediaTypeId;
    }
    
    public void setMediaTypeId(Integer mediaTypeId) {
        this.MediaTypeId = mediaTypeId;
    }
    
    public String getName() {
        return Name;
    }
    
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