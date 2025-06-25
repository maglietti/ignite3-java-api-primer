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

package com.apache.ignite.examples.schema.model;

import org.apache.ignite.catalog.annotations.Column;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Zone;

/**
 * Genre entity demonstrating reference data patterns with replication zones.
 * 
 * Shows distribution zone configuration for read-heavy reference data,
 * higher replication factors for availability, and lookup table optimization
 * patterns in distributed systems.
 */
@Table(zone = @Zone(value = "MusicStoreReplicated", storageProfiles = "default"))
public class Genre {
    
    @Id
    private Integer GenreId;
    
    @Column(value = "Name", length = 120, nullable = true)
    private String Name;
    
    public Genre() {
    }
    
    public Genre(Integer genreId, String name) {
        this.GenreId = genreId;
        this.Name = name;
    }
    
    public Integer getGenreId() {
        return GenreId;
    }
    
    public void setGenreId(Integer genreId) {
        this.GenreId = genreId;
    }
    
    public String getName() {
        return Name;
    }
    
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