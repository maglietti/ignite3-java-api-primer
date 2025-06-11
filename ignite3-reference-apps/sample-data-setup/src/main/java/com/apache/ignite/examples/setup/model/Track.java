package com.apache.ignite.examples.setup.model;

import java.math.BigDecimal;
import org.apache.ignite.catalog.annotations.Column;
import org.apache.ignite.catalog.annotations.ColumnRef;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Index;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Zone;

/**
 * Track entity representing individual music tracks/songs in the music store.
 * 
 * This is the most complex entity in the hierarchy demonstrating advanced Ignite 3 features:
 * - Multi-level Colocation: Tracks are colocated with their Albums (which are colocated with Artists)
 * - Composite Primary Keys: TrackId + AlbumId for distributed uniqueness
 * - Multiple Foreign Keys: References to Album, Genre, and MediaType
 * - Rich Metadata: Complete track information including duration, file size, pricing
 * - Multiple Indexes: Foreign key indexes for efficient joins across relationships
 * 
 * Colocation Chain: Artist → Album (by ArtistId) → Track (by AlbumId)
 * This means tracks, their albums, and artists are all stored on the same cluster nodes.
 */
@Table(
    // Same zone as Artist and Album for consistency
    zone = @Zone(value = "MusicStore", storageProfiles = "default"),
    
    // CRITICAL: Colocate tracks with their albums for maximum query performance
    // Since Albums are colocated by ArtistId, this creates a 3-level hierarchy:
    // Artist → Album (by ArtistId) → Track (by AlbumId)
    // This enables extremely fast queries like "get all tracks by artist"
    colocateBy = @ColumnRef("AlbumId"),
    
    // Multiple indexes for efficient foreign key lookups
    // Essential for join performance in distributed environments
    indexes = {
        @Index(value = "IFK_TrackAlbumId", columns = { @ColumnRef("AlbumId") }),      // Album relationship
        @Index(value = "IFK_TrackGenreId", columns = { @ColumnRef("GenreId") }),      // Genre classification
        @Index(value = "IFK_TrackMediaTypeId", columns = { @ColumnRef("MediaTypeId") }) // Media format
    }
)
public class Track {
    
    /**
     * Track identifier - first part of composite primary key.
     * Unique within the scope of an album.
     */
    @Id
    private Integer TrackId;
    
    /**
     * Album identifier - second part of composite primary key AND colocation key.
     * This serves triple duty:
     * 1. Part of the composite primary key for uniqueness
     * 2. Foreign key reference to the Album table
     * 3. Colocation key to ensure tracks are stored with their albums
     */
    @Id
    private Integer AlbumId;
    
    /**
     * Track name/title.
     * Length 200 accommodates long song titles.
     */
    @Column(value = "Name", length = 200, nullable = false)
    private String Name;
    
    /**
     * Media type reference (MP3, AAC, FLAC, etc.).
     * Required field - every track must have a format.
     */
    @Column(value = "MediaTypeId", nullable = false)
    private Integer MediaTypeId;
    
    /**
     * Genre classification reference (Rock, Jazz, Classical, etc.).
     * Optional - some tracks may not have genre classification.
     */
    @Column(value = "GenreId", nullable = true)
    private Integer GenreId;
    
    /**
     * Track composer/songwriter information.
     * Optional field with generous length for multiple composers.
     */
    @Column(value = "Composer", length = 220, nullable = true)
    private String Composer;
    
    /**
     * Track duration in milliseconds.
     * Required for playback and playlist calculations.
     */
    @Column(value = "Milliseconds", nullable = false)
    private Integer Milliseconds;
    
    /**
     * File size in bytes.
     * Optional - may not be available for all tracks.
     */
    @Column(value = "Bytes", nullable = true)
    private Integer Bytes;
    
    /**
     * Track price with precise decimal handling.
     * Uses DECIMAL(10,2) for exact monetary calculations.
     * Essential for e-commerce accuracy.
     */
    @Column(value = "UnitPrice", precision = 10, scale = 2, nullable = false)
    private BigDecimal UnitPrice;
    
    /**
     * Default constructor required by Ignite 3 for object deserialization.
     */
    public Track() {
    }
    
    /**
     * Comprehensive constructor for creating Track instances.
     * 
     * @param trackId Unique track identifier within album
     * @param albumId Album containing this track (also colocation key)
     * @param name Track title/name
     * @param mediaTypeId Audio format type reference
     * @param genreId Music genre classification reference (optional)
     * @param composer Songwriter/composer information (optional)
     * @param milliseconds Track duration in milliseconds
     * @param bytes File size in bytes (optional)
     * @param unitPrice Track purchase price
     */
    public Track(Integer trackId, Integer albumId, String name, Integer mediaTypeId, 
                 Integer genreId, String composer, Integer milliseconds, Integer bytes, BigDecimal unitPrice) {
        this.TrackId = trackId;
        this.AlbumId = albumId;
        this.Name = name;
        this.MediaTypeId = mediaTypeId;
        this.GenreId = genreId;
        this.Composer = composer;
        this.Milliseconds = milliseconds;
        this.Bytes = bytes;
        this.UnitPrice = unitPrice;
    }
    
    public Integer getTrackId() {
        return TrackId;
    }
    
    public void setTrackId(Integer trackId) {
        this.TrackId = trackId;
    }
    
    public Integer getAlbumId() {
        return AlbumId;
    }
    
    public void setAlbumId(Integer albumId) {
        this.AlbumId = albumId;
    }
    
    public String getName() {
        return Name;
    }
    
    public void setName(String name) {
        this.Name = name;
    }
    
    public Integer getMediaTypeId() {
        return MediaTypeId;
    }
    
    public void setMediaTypeId(Integer mediaTypeId) {
        this.MediaTypeId = mediaTypeId;
    }
    
    public Integer getGenreId() {
        return GenreId;
    }
    
    public void setGenreId(Integer genreId) {
        this.GenreId = genreId;
    }
    
    public String getComposer() {
        return Composer;
    }
    
    public void setComposer(String composer) {
        this.Composer = composer;
    }
    
    public Integer getMilliseconds() {
        return Milliseconds;
    }
    
    public void setMilliseconds(Integer milliseconds) {
        this.Milliseconds = milliseconds;
    }
    
    public Integer getBytes() {
        return Bytes;
    }
    
    public void setBytes(Integer bytes) {
        this.Bytes = bytes;
    }
    
    public BigDecimal getUnitPrice() {
        return UnitPrice;
    }
    
    public void setUnitPrice(BigDecimal unitPrice) {
        this.UnitPrice = unitPrice;
    }
    
    @Override
    public String toString() {
        return "Track{" +
                "TrackId=" + TrackId +
                ", AlbumId=" + AlbumId +
                ", Name='" + Name + '\'' +
                ", MediaTypeId=" + MediaTypeId +
                ", GenreId=" + GenreId +
                ", Composer='" + Composer + '\'' +
                ", Milliseconds=" + Milliseconds +
                ", Bytes=" + Bytes +
                ", UnitPrice=" + UnitPrice +
                '}';
    }
}