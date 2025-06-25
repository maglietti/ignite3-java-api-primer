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
 * Artist entity representing music artists in the catalog.
 * 
 * Used for demonstrating cache-aside patterns with read-heavy
 * catalog operations in music streaming applications.
 */
public class Artist {
    
    private int artistId;
    private String name;
    private String genre;
    private String country;
    private int albumCount;
    
    public Artist() {
        // Default constructor required for Ignite
    }
    
    private Artist(Builder builder) {
        this.artistId = builder.artistId;
        this.name = builder.name;
        this.genre = builder.genre;
        this.country = builder.country;
        this.albumCount = builder.albumCount;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public Builder toBuilder() {
        return new Builder()
            .artistId(this.artistId)
            .name(this.name)
            .genre(this.genre)
            .country(this.country)
            .albumCount(this.albumCount);
    }
    
    // Getters and setters
    public int getArtistId() { return artistId; }
    public void setArtistId(int artistId) { this.artistId = artistId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    
    public int getAlbumCount() { return albumCount; }
    public void setAlbumCount(int albumCount) { this.albumCount = albumCount; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Artist artist = (Artist) o;
        return artistId == artist.artistId;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(artistId);
    }
    
    @Override
    public String toString() {
        return String.format("Artist{id=%d, name='%s', genre='%s', country='%s', albums=%d}", 
            artistId, name, genre, country, albumCount);
    }
    
    public static class Builder {
        private int artistId;
        private String name;
        private String genre;
        private String country;
        private int albumCount;
        
        public Builder artistId(int artistId) {
            this.artistId = artistId;
            return this;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder genre(String genre) {
            this.genre = genre;
            return this;
        }
        
        public Builder country(String country) {
            this.country = country;
            return this;
        }
        
        public Builder albumCount(int albumCount) {
            this.albumCount = albumCount;
            return this;
        }
        
        public Artist build() {
            return new Artist(this);
        }
    }
}