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

package com.apache.ignite.examples.filestreaming;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates realistic sample music event CSV files for file streaming demonstrations.
 * 
 * Creates music streaming event data that simulates user interactions with a music
 * platform: track plays, pauses, skips, likes, and playlist operations. The data
 * includes temporal patterns and realistic user behavior for effective backpressure
 * testing where file reading speed often exceeds cluster processing capacity.
 * 
 * CSV Format:
 * EventId,UserId,TrackId,EventType,EventTime,Duration,PlaylistId
 * 
 * Data characteristics:
 * - Sequential event IDs for consistent ordering
 * - Realistic user ID range (1000-4999) for colocation testing
 * - Track ID range (1-500) matching sample music store data
 * - Event types representing real music streaming interactions
 * - Temporal sequencing with logical event progression
 * - Variable duration for TRACK_END events
 * - Playlist associations for related events
 */
public class SampleDataGenerator {
    
    private static final String[] EVENT_TYPES = {
        "TRACK_START", "TRACK_END", "TRACK_PAUSE", "TRACK_RESUME",
        "TRACK_SKIP", "TRACK_LIKE", "TRACK_UNLIKE", "PLAYLIST_ADD",
        "PLAYLIST_REMOVE", "TRACK_SHARE"
    };
    
    private static final int MIN_USER_ID = 1000;
    private static final int MAX_USER_ID = 4999;
    private static final int MIN_TRACK_ID = 1;
    private static final int MAX_TRACK_ID = 500;
    private static final int MIN_PLAYLIST_ID = 2000;
    private static final int MAX_PLAYLIST_ID = 2999;
    
    /**
     * Generates a sample CSV file with specified number of music events.
     * Creates realistic temporal sequences where TRACK_START events are
     * often followed by related TRACK_END events with appropriate durations.
     * 
     * @param filePath path where CSV file will be created
     * @param eventCount number of events to generate
     * @return Path to the created file
     * @throws IOException if file creation fails
     */
    public static Path generateMusicEventFile(String filePath, int eventCount) throws IOException {
        Path path = Paths.get(filePath);
        
        // Ensure parent directories exist
        Files.createDirectories(path.getParent());
        
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            // Write CSV header
            writer.write("EventId,UserId,TrackId,EventType,EventTime,Duration,PlaylistId");
            writer.newLine();
            
            Random random = ThreadLocalRandom.current();
            long baseTime = System.currentTimeMillis() - (eventCount * 1000L); // Start in the past
            
            // Track active sessions for realistic event sequences
            int activeUserId = -1;
            int activeTrackId = -1;
            long trackStartTime = 0;
            
            for (int i = 1; i <= eventCount; i++) {
                long eventId = i;
                long eventTime = baseTime + (i * random.nextInt(500, 2000)); // 0.5-2 sec between events
                
                // Generate realistic event sequences
                String eventType;
                int userId;
                int trackId;
                long duration = 0;
                Integer playlistId = null;
                
                // 60% chance to continue with active session, 40% chance new session
                if (activeUserId != -1 && random.nextDouble() < 0.6) {
                    userId = activeUserId;
                    trackId = activeTrackId;
                    
                    // Choose appropriate follow-up event
                    String[] followUpEvents = {"TRACK_END", "TRACK_PAUSE", "TRACK_SKIP", "TRACK_LIKE"};
                    eventType = followUpEvents[random.nextInt(followUpEvents.length)];
                    
                    // Calculate duration for TRACK_END events
                    if ("TRACK_END".equals(eventType)) {
                        duration = eventTime - trackStartTime;
                        activeUserId = -1; // End session
                    } else if ("TRACK_SKIP".equals(eventType)) {
                        duration = eventTime - trackStartTime;
                        activeUserId = -1; // End session
                    }
                    
                    // Add playlist context for some events
                    if (random.nextDouble() < 0.3) {
                        playlistId = random.nextInt(MIN_PLAYLIST_ID, MAX_PLAYLIST_ID + 1);
                    }
                    
                } else {
                    // Start new session
                    userId = random.nextInt(MIN_USER_ID, MAX_USER_ID + 1);
                    trackId = random.nextInt(MIN_TRACK_ID, MAX_TRACK_ID + 1);
                    
                    // Favor TRACK_START for new sessions, but allow other events
                    if (random.nextDouble() < 0.7) {
                        eventType = "TRACK_START";
                        activeUserId = userId;
                        activeTrackId = trackId;
                        trackStartTime = eventTime;
                    } else {
                        eventType = EVENT_TYPES[random.nextInt(EVENT_TYPES.length)];
                    }
                    
                    // Add playlist context for playlist-related events
                    if (eventType.contains("PLAYLIST") || random.nextDouble() < 0.2) {
                        playlistId = random.nextInt(MIN_PLAYLIST_ID, MAX_PLAYLIST_ID + 1);
                    }
                }
                
                // Write CSV record
                writer.write(String.format("%d,%d,%d,%s,%d,%d,%s",
                    eventId, userId, trackId, eventType, eventTime, duration,
                    playlistId != null ? playlistId.toString() : ""));
                writer.newLine();
                
                // Periodic progress for large files
                if (i % 10000 == 0) {
                    System.out.printf("Generated %d events...%n", i);
                }
            }
        }
        
        return path;
    }
    
    /**
     * Generates a high-velocity event file designed to create backpressure scenarios.
     * Events are tightly packed in time to simulate peak load conditions where
     * file reading significantly outpaces cluster processing capacity.
     * 
     * @param filePath path where CSV file will be created
     * @param eventCount number of events to generate
     * @return Path to the created file
     * @throws IOException if file creation fails
     */
    public static Path generateHighVelocityEventFile(String filePath, int eventCount) throws IOException {
        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());
        
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            // Write CSV header
            writer.write("EventId,UserId,TrackId,EventType,EventTime,Duration,PlaylistId");
            writer.newLine();
            
            Random random = ThreadLocalRandom.current();
            long baseTime = System.currentTimeMillis();
            
            for (int i = 1; i <= eventCount; i++) {
                long eventId = i;
                int userId = random.nextInt(MIN_USER_ID, MAX_USER_ID + 1);
                int trackId = random.nextInt(MIN_TRACK_ID, MAX_TRACK_ID + 1);
                String eventType = EVENT_TYPES[random.nextInt(EVENT_TYPES.length)];
                
                // High-velocity timing: events every 10-50ms
                long eventTime = baseTime + (i * random.nextInt(10, 50));
                
                // Random duration for applicable events
                long duration = "TRACK_END".equals(eventType) || "TRACK_SKIP".equals(eventType)
                    ? random.nextInt(30000, 300000) // 30sec to 5min
                    : 0;
                
                // Occasional playlist association
                Integer playlistId = random.nextDouble() < 0.3
                    ? random.nextInt(MIN_PLAYLIST_ID, MAX_PLAYLIST_ID + 1)
                    : null;
                
                writer.write(String.format("%d,%d,%d,%s,%d,%d,%s",
                    eventId, userId, trackId, eventType, eventTime, duration,
                    playlistId != null ? playlistId.toString() : ""));
                writer.newLine();
            }
        }
        
        return path;
    }
    
    /**
     * Gets the file size in MB for display purposes.
     * 
     * @param path file path to check
     * @return file size in megabytes
     */
    public static double getFileSizeMB(Path path) {
        try {
            long bytes = Files.size(path);
            return bytes / (1024.0 * 1024.0);
        } catch (IOException e) {
            return 0.0;
        }
    }
    
    /**
     * Counts the number of lines in a file for validation.
     * 
     * @param path file path to check
     * @return number of lines (including header)
     */
    public static long countLines(Path path) {
        try {
            return Files.lines(path).count();
        } catch (IOException e) {
            return 0;
        }
    }
    
    /**
     * Deletes a generated file for cleanup.
     * 
     * @param path file path to delete
     * @return true if file was deleted successfully
     */
    public static boolean deleteFile(Path path) {
        try {
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            return false;
        }
    }
}