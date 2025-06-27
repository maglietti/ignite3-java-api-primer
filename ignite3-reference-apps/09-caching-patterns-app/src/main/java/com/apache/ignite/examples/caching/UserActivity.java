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
 * User activity entity representing various user interactions.
 * 
 * Used for demonstrating write-behind patterns with user behavior tracking.
 */
public class UserActivity {
    
    private int customerId;
    private String activityType;
    private Instant timestamp;
    private Map<String, Object> metadata;
    private String sessionId;
    private String deviceType;
    
    public UserActivity() {
        // Default constructor required for Ignite
    }
    
    private UserActivity(Builder builder) {
        this.customerId = builder.customerId;
        this.activityType = builder.activityType;
        this.timestamp = builder.timestamp;
        this.metadata = builder.metadata;
        this.sessionId = builder.sessionId;
        this.deviceType = builder.deviceType;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public Builder toBuilder() {
        return new Builder()
            .customerId(this.customerId)
            .activityType(this.activityType)
            .timestamp(this.timestamp)
            .metadata(this.metadata)
            .sessionId(this.sessionId)
            .deviceType(this.deviceType);
    }
    
    // Getters and setters
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    
    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserActivity that = (UserActivity) o;
        return customerId == that.customerId && 
               Objects.equals(activityType, that.activityType) && 
               Objects.equals(timestamp, that.timestamp);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(customerId, activityType, timestamp);
    }
    
    @Override
    public String toString() {
        return String.format("UserActivity{customer=%d, type='%s', timestamp=%s, device='%s'}", 
            customerId, activityType, timestamp, deviceType);
    }
    
    public static class Builder {
        private int customerId;
        private String activityType;
        private Instant timestamp = Instant.now();
        private Map<String, Object> metadata;
        private String sessionId;
        private String deviceType;
        
        public Builder customerId(int customerId) {
            this.customerId = customerId;
            return this;
        }
        
        public Builder activityType(String activityType) {
            this.activityType = activityType;
            return this;
        }
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public Builder deviceType(String deviceType) {
            this.deviceType = deviceType;
            return this;
        }
        
        public UserActivity build() {
            return new UserActivity(this);
        }
    }
}