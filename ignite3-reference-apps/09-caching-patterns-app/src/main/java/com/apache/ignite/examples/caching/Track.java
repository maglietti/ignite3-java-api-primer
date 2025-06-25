/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.apache.ignite.examples.caching;

import java.util.Objects;

/**
 * Track entity representing individual music tracks.
 * 
 * Used for demonstrating cache-aside patterns with catalog operations.
 */
public class Track {
    
    private int trackId;
    private String name;
    private int artistId;
    private int albumId;
    private String genre;
    private int durationSeconds;
    private String streamingUrl;
    
    public Track() {
        // Default constructor required for Ignite
    }
    
    private Track(Builder builder) {
        this.trackId = builder.trackId;
        this.name = builder.name;
        this.artistId = builder.artistId;
        this.albumId = builder.albumId;
        this.genre = builder.genre;
        this.durationSeconds = builder.durationSeconds;
        this.streamingUrl = builder.streamingUrl;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public Builder toBuilder() {
        return new Builder()
            .trackId(this.trackId)
            .name(this.name)
            .artistId(this.artistId)
            .albumId(this.albumId)
            .genre(this.genre)
            .durationSeconds(this.durationSeconds)
            .streamingUrl(this.streamingUrl);
    }
    
    // Getters and setters
    public int getTrackId() { return trackId; }
    public void setTrackId(int trackId) { this.trackId = trackId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public int getArtistId() { return artistId; }
    public void setArtistId(int artistId) { this.artistId = artistId; }
    
    public int getAlbumId() { return albumId; }
    public void setAlbumId(int albumId) { this.albumId = albumId; }
    
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    
    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
    
    public String getStreamingUrl() { return streamingUrl; }
    public void setStreamingUrl(String streamingUrl) { this.streamingUrl = streamingUrl; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Track track = (Track) o;
        return trackId == track.trackId;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(trackId);
    }
    
    @Override
    public String toString() {
        return String.format("Track{id=%d, name='%s', artistId=%d, albumId=%d, duration=%ds}", 
            trackId, name, artistId, albumId, durationSeconds);
    }
    
    public static class Builder {
        private int trackId;
        private String name;
        private int artistId;
        private int albumId;
        private String genre;
        private int durationSeconds;
        private String streamingUrl;
        
        public Builder trackId(int trackId) {
            this.trackId = trackId;
            return this;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder artistId(int artistId) {
            this.artistId = artistId;
            return this;
        }
        
        public Builder albumId(int albumId) {
            this.albumId = albumId;
            return this;
        }
        
        public Builder genre(String genre) {
            this.genre = genre;
            return this;
        }
        
        public Builder durationSeconds(int durationSeconds) {
            this.durationSeconds = durationSeconds;
            return this;
        }
        
        public Builder streamingUrl(String streamingUrl) {
            this.streamingUrl = streamingUrl;
            return this;
        }
        
        public Track build() {
            return new Track(this);
        }
    }
}