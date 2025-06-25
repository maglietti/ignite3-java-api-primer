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

package com.apache.ignite.examples.setup.model;

import org.apache.ignite.catalog.annotations.ColumnRef;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Index;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Zone;

@Table(
    zone = @Zone(value = "MusicStore", storageProfiles = "default"),
    colocateBy = @ColumnRef("PlaylistId"),
    indexes = {
        @Index(value = "IFK_PlaylistTrackPlaylistId", columns = { @ColumnRef("PlaylistId") }),
        @Index(value = "IFK_PlaylistTrackTrackId", columns = { @ColumnRef("TrackId") })
    }
)
public class PlaylistTrack {
    
    @Id
    private Integer PlaylistId;
    
    @Id
    private Integer TrackId;
    
    public PlaylistTrack() {
    }
    
    public PlaylistTrack(Integer playlistId, Integer trackId) {
        this.PlaylistId = playlistId;
        this.TrackId = trackId;
    }
    
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