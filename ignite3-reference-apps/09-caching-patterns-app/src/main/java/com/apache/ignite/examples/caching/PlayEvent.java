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

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Play event entity representing music streaming events.
 * 
 * Used for demonstrating write-behind patterns with high-throughput
 * analytics data recording.
 */
public class PlayEvent {
    
    private int customerId;
    private int trackId;
    private Instant timestamp;
    private String activityType;
    private Map<String, Object> metadata;
    private int durationSeconds;
    private boolean completed;
    
    public PlayEvent() {
        // Default constructor required for Ignite
    }
    
    private PlayEvent(Builder builder) {
        this.customerId = builder.customerId;
        this.trackId = builder.trackId;
        this.timestamp = builder.timestamp;
        this.activityType = builder.activityType;
        this.metadata = builder.metadata;
        this.durationSeconds = builder.durationSeconds;
        this.completed = builder.completed;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public Builder toBuilder() {
        return new Builder()
            .customerId(this.customerId)
            .trackId(this.trackId)
            .timestamp(this.timestamp)
            .activityType(this.activityType)
            .metadata(this.metadata)
            .durationSeconds(this.durationSeconds)
            .completed(this.completed);
    }
    
    // Getters and setters
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    
    public int getTrackId() { return trackId; }
    public void setTrackId(int trackId) { this.trackId = trackId; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    
    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
    
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayEvent playEvent = (PlayEvent) o;
        return customerId == playEvent.customerId && 
               trackId == playEvent.trackId && 
               Objects.equals(timestamp, playEvent.timestamp);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(customerId, trackId, timestamp);
    }
    
    @Override
    public String toString() {
        return String.format("PlayEvent{customer=%d, track=%d, timestamp=%s, type='%s', completed=%s}", 
            customerId, trackId, timestamp, activityType, completed);
    }
    
    public static class Builder {
        private int customerId;
        private int trackId;
        private Instant timestamp = Instant.now();
        private String activityType = "play";
        private Map<String, Object> metadata;
        private int durationSeconds;
        private boolean completed = false;
        
        public Builder customerId(int customerId) {
            this.customerId = customerId;
            return this;
        }
        
        public Builder trackId(int trackId) {
            this.trackId = trackId;
            return this;
        }
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder activityType(String activityType) {
            this.activityType = activityType;
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public Builder durationSeconds(int durationSeconds) {
            this.durationSeconds = durationSeconds;
            return this;
        }
        
        public Builder completed(boolean completed) {
            this.completed = completed;
            return this;
        }
        
        public PlayEvent build() {
            return new PlayEvent(this);
        }
    }
}