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

package com.apache.ignite.examples.setup.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apache.ignite.examples.setup.model.Album;
import com.apache.ignite.examples.setup.model.Artist;
import com.apache.ignite.examples.setup.model.Customer;
import com.apache.ignite.examples.setup.model.Employee;
import com.apache.ignite.examples.setup.model.Genre;
import com.apache.ignite.examples.setup.model.Invoice;
import com.apache.ignite.examples.setup.model.InvoiceLine;
import com.apache.ignite.examples.setup.model.MediaType;
import com.apache.ignite.examples.setup.model.Playlist;
import com.apache.ignite.examples.setup.model.PlaylistTrack;
import com.apache.ignite.examples.setup.model.Track;

/**
 * Optimized SQL data loader for Apache Ignite 3 music store dataset.
 * 
 * Uses native Ignite batch APIs and parallel processing to achieve maximum
 * loading performance by avoiding character-by-character SQL parsing and
 * leveraging structured data extraction patterns.
 */
public class OptimizedSqlLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(OptimizedSqlLoader.class);
    
    
    /**
     * Loads data from SQL script using optimized batch processing.
     * 
     * @param client Connected Ignite client
     * @param scriptPath Path to SQL script in resources
     */
    public static void loadFromScript(IgniteClient client, String scriptPath) {
        logger.info(">>> Reading SQL script with optimized parser: {}", scriptPath);
        
        try {
            InputStream scriptStream = OptimizedSqlLoader.class.getResourceAsStream("/" + scriptPath);
            if (scriptStream == null) {
                throw new RuntimeException("SQL script not found: " + scriptPath);
            }
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(scriptStream, StandardCharsets.UTF_8))) {
                String content = readFullContent(reader);
                
                logger.info(">>> Extracting INSERT statements with improved parsing");
                List<TableData> tableDataList = extractTableData(content);
                
                logger.info("<<< Found {} table datasets for batch loading", tableDataList.size());
                
                if (tableDataList.isEmpty()) {
                    logger.warn("!!! No table data extracted - check SQL parsing logic");
                    return;
                }
                
                loadDataInParallel(client, tableDataList);
            }
            
        } catch (Exception e) {
            logger.error("<<< Optimized SQL loading failed: {}", e.getMessage());
            throw new RuntimeException("Optimized SQL loading failed", e);
        }
    }
    
    private static String readFullContent(BufferedReader reader) throws Exception {
        StringBuilder content = new StringBuilder();
        String line;
        boolean insideMultilineComment = false;
        
        while ((line = reader.readLine()) != null) {
            if (line.contains("/*") && !line.contains("*/")) {
                insideMultilineComment = true;
                continue;
            }
            
            if (insideMultilineComment) {
                if (line.contains("*/")) {
                    insideMultilineComment = false;
                }
                continue;
            }
            
            line = line.trim();
            if (line.isEmpty() || line.startsWith("--")) {
                continue;
            }
            
            content.append(line).append("\n");
        }
        
        return content.toString();
    }
    
    private static List<TableData> extractTableData(String content) {
        List<TableData> tableDataList = new ArrayList<>();
        
        String[] statements = content.split(";");
        
        for (String statement : statements) {
            statement = statement.trim();
            if (statement.isEmpty()) continue;
            
            String upperStatement = statement.toUpperCase();
            if (upperStatement.startsWith("INSERT INTO")) {
                
                Pattern tableNamePattern = Pattern.compile("INSERT INTO\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
                Matcher tableNameMatcher = tableNamePattern.matcher(statement);
                
                if (tableNameMatcher.find()) {
                    String tableName = tableNameMatcher.group(1);
                    
                    if (isTargetTable(tableName)) {
                        int valuesIndex = upperStatement.indexOf("VALUES");
                        if (valuesIndex != -1) {
                            String valuesSection = statement.substring(valuesIndex + 6).trim();
                            List<String> valueRows = extractValueRows(valuesSection);
                            
                            if (!valueRows.isEmpty()) {
                                // Find existing table data or create new one
                                TableData existingData = null;
                                for (TableData data : tableDataList) {
                                    if (data.tableName.equalsIgnoreCase(tableName)) {
                                        existingData = data;
                                        break;
                                    }
                                }
                                
                                if (existingData != null) {
                                    existingData.valueRows.addAll(valueRows);
                                    logger.info(">>> Added {} more records to table: {} (total: {})", 
                                               valueRows.size(), tableName, existingData.valueRows.size());
                                } else {
                                    tableDataList.add(new TableData(tableName, valueRows));
                                    logger.info("<<< Extracted {} records for table: {}", valueRows.size(), tableName);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return tableDataList;
    }
    
    private static boolean isTargetTable(String tableName) {
        return tableName.equalsIgnoreCase("Genre") || 
               tableName.equalsIgnoreCase("MediaType") ||
               tableName.equalsIgnoreCase("Artist") ||
               tableName.equalsIgnoreCase("Album") ||
               tableName.equalsIgnoreCase("Track") ||
               tableName.equalsIgnoreCase("Customer") ||
               tableName.equalsIgnoreCase("Employee") ||
               tableName.equalsIgnoreCase("Invoice") ||
               tableName.equalsIgnoreCase("InvoiceLine") ||
               tableName.equalsIgnoreCase("Playlist") ||
               tableName.equalsIgnoreCase("PlaylistTrack");
    }
    
    private static List<String> extractValueRows(String valuesSection) {
        List<String> rows = new ArrayList<>();
        
        int i = 0;
        while (i < valuesSection.length()) {
            if (valuesSection.charAt(i) == '(') {
                int start = i + 1;
                int end = findMatchingParen(valuesSection, i);
                
                if (end != -1) {
                    String valueRow = valuesSection.substring(start, end);
                    rows.add(valueRow);
                    i = end + 1;
                } else {
                    break;
                }
            } else {
                i++;
            }
        }
        
        return rows;
    }
    
    private static int findMatchingParen(String text, int startIndex) {
        int parenCount = 1;
        boolean inQuote = false;
        
        for (int i = startIndex + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (c == '\'' && (i == 0 || text.charAt(i-1) != '\\')) {
                inQuote = !inQuote;
            } else if (!inQuote) {
                if (c == '(') {
                    parenCount++;
                } else if (c == ')') {
                    parenCount--;
                    if (parenCount == 0) {
                        return i;
                    }
                }
            }
        }
        
        return -1;
    }
    
    private static void loadDataInParallel(IgniteClient client, List<TableData> tableDataList) {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        
        try {
            logger.info("--- Phase 1: Loading reference tables in parallel");
            
            List<CompletableFuture<Void>> referenceFutures = new ArrayList<>();
            for (TableData tableData : tableDataList) {
                if (isReferenceTable(tableData.tableName)) {
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> 
                        loadTableData(client, tableData), executor);
                    referenceFutures.add(future);
                }
            }
            
            CompletableFuture.allOf(referenceFutures.toArray(new CompletableFuture[0])).join();
            logger.info("<<< Reference tables loaded successfully");
            
            logger.info("--- Phase 2: Loading core entities in dependency order");
            
            for (TableData tableData : tableDataList) {
                if (isCoreEntity(tableData.tableName)) {
                    loadTableData(client, tableData);
                }
            }
            
            logger.info("<<< Core entities loaded successfully");
            
            logger.info("--- Phase 3: Loading business entities in parallel");
            
            List<CompletableFuture<Void>> businessFutures = new ArrayList<>();
            for (TableData tableData : tableDataList) {
                if (isBusinessEntity(tableData.tableName)) {
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> 
                        loadTableData(client, tableData), executor);
                    businessFutures.add(future);
                }
            }
            
            CompletableFuture.allOf(businessFutures.toArray(new CompletableFuture[0])).join();
            logger.info("<<< All data loaded successfully");
            
        } finally {
            executor.shutdown();
        }
    }
    
    private static boolean isReferenceTable(String tableName) {
        return tableName.equalsIgnoreCase("Genre") || tableName.equalsIgnoreCase("MediaType");
    }
    
    private static boolean isCoreEntity(String tableName) {
        return tableName.equalsIgnoreCase("Artist") || 
               tableName.equalsIgnoreCase("Album") || 
               tableName.equalsIgnoreCase("Track");
    }
    
    private static boolean isBusinessEntity(String tableName) {
        return tableName.equalsIgnoreCase("Customer") ||
               tableName.equalsIgnoreCase("Employee") ||
               tableName.equalsIgnoreCase("Invoice") ||
               tableName.equalsIgnoreCase("InvoiceLine") ||
               tableName.equalsIgnoreCase("Playlist") ||
               tableName.equalsIgnoreCase("PlaylistTrack");
    }
    
    private static void loadTableData(IgniteClient client, TableData tableData) {
        String tableName = tableData.tableName;
        List<String> valueRows = tableData.valueRows;
        
        logger.info(">>> Loading {} records into {} table", valueRows.size(), tableName);
        
        try {
            switch (tableName.toLowerCase()) {
                case "genre":
                    loadGenreData(client, valueRows);
                    break;
                case "mediatype":
                    loadMediaTypeData(client, valueRows);
                    break;
                case "artist":
                    loadArtistData(client, valueRows);
                    break;
                case "album":
                    loadAlbumData(client, valueRows);
                    break;
                case "track":
                    loadTrackData(client, valueRows);
                    break;
                case "customer":
                    loadCustomerData(client, valueRows);
                    break;
                case "employee":
                    loadEmployeeData(client, valueRows);
                    break;
                case "invoice":
                    loadInvoiceData(client, valueRows);
                    break;
                case "invoiceline":
                    loadInvoiceLineData(client, valueRows);
                    break;
                case "playlist":
                    loadPlaylistData(client, valueRows);
                    break;
                case "playlisttrack":
                    loadPlaylistTrackData(client, valueRows);
                    break;
                default:
                    logger.warn("* Unknown table: {}", tableName);
            }
            
            logger.info("<<< Completed loading {} table ({} records)", tableName, valueRows.size());
            
        } catch (Exception e) {
            logger.error("<<< Failed to load {} table: {}", tableName, e.getMessage());
            throw new RuntimeException("Failed to load " + tableName + " table", e);
        }
    }
    
    private static void loadGenreData(IgniteClient client, List<String> valueRows) {
        Table table = client.tables().table("Genre");
        RecordView<Genre> view = table.recordView(Genre.class);
        
        List<Genre> genres = new ArrayList<>();
        for (String row : valueRows) {
            String[] values = parseValueRow(row);
            genres.add(new Genre(Integer.parseInt(values[0]), cleanString(values[1])));
        }
        
        view.upsertAll(null, genres);
    }
    
    private static void loadMediaTypeData(IgniteClient client, List<String> valueRows) {
        Table table = client.tables().table("MediaType");
        RecordView<MediaType> view = table.recordView(MediaType.class);
        
        List<MediaType> mediaTypes = new ArrayList<>();
        for (String row : valueRows) {
            String[] values = parseValueRow(row);
            mediaTypes.add(new MediaType(Integer.parseInt(values[0]), cleanString(values[1])));
        }
        
        view.upsertAll(null, mediaTypes);
    }
    
    private static void loadArtistData(IgniteClient client, List<String> valueRows) {
        Table table = client.tables().table("Artist");
        RecordView<Artist> view = table.recordView(Artist.class);
        
        List<Artist> artists = new ArrayList<>();
        for (String row : valueRows) {
            String[] values = parseValueRow(row);
            artists.add(new Artist(Integer.parseInt(values[0]), cleanString(values[1])));
        }
        
        view.upsertAll(null, artists);
    }
    
    private static void loadAlbumData(IgniteClient client, List<String> valueRows) {
        Table table = client.tables().table("Album");
        RecordView<Album> view = table.recordView(Album.class);
        
        List<Album> albums = new ArrayList<>();
        for (String row : valueRows) {
            String[] values = parseValueRow(row);
            
            if (values.length < 3) {
                logger.warn("Skipping Album row with insufficient values: {}", row);
                continue;
            }
            
            albums.add(new Album(
                Integer.parseInt(values[0]),  // AlbumId
                Integer.parseInt(values[2]),  // ArtistId
                cleanString(values[1])        // Title
            ));
        }
        
        view.upsertAll(null, albums);
    }
    
    private static void loadTrackData(IgniteClient client, List<String> valueRows) {
        Table table = client.tables().table("Track");
        RecordView<Track> view = table.recordView(Track.class);
        
        List<Track> tracks = new ArrayList<>();
        for (String row : valueRows) {
            String[] values = parseValueRow(row);
            // SQL: (TrackId, Name, AlbumId, MediaTypeId, GenreId, Composer, Milliseconds, Bytes, UnitPrice)
            // Constructor: (trackId, albumId, name, mediaTypeId, genreId, composer, milliseconds, bytes, unitPrice)
            tracks.add(new Track(
                Integer.parseInt(values[0]),  // TrackId
                values[2] != null && !values[2].equals("NULL") ? Integer.parseInt(values[2]) : null,  // AlbumId
                cleanString(values[1]),       // Name
                Integer.parseInt(values[3]),  // MediaTypeId
                values[4] != null && !values[4].equals("NULL") ? Integer.parseInt(values[4]) : null,  // GenreId
                values[5] != null && !values[5].equals("NULL") ? cleanString(values[5]) : null,       // Composer
                Integer.parseInt(values[6]),  // Milliseconds
                values[7] != null && !values[7].equals("NULL") ? Integer.parseInt(values[7]) : null,  // Bytes
                new BigDecimal(values[8])     // UnitPrice
            ));
        }
        
        view.upsertAll(null, tracks);
    }
    
    private static void loadCustomerData(IgniteClient client, List<String> valueRows) {
        Table table = client.tables().table("Customer");
        RecordView<Customer> view = table.recordView(Customer.class);
        
        List<Customer> customers = new ArrayList<>();
        for (String row : valueRows) {
            String[] values = parseValueRow(row);
            customers.add(new Customer(
                Integer.parseInt(values[0]),
                cleanString(values[1]),
                cleanString(values[2]),
                values[3] != null && !values[3].equals("NULL") ? cleanString(values[3]) : null,
                values[4] != null && !values[4].equals("NULL") ? cleanString(values[4]) : null,
                values[5] != null && !values[5].equals("NULL") ? cleanString(values[5]) : null,
                values[6] != null && !values[6].equals("NULL") ? cleanString(values[6]) : null,
                values[7] != null && !values[7].equals("NULL") ? cleanString(values[7]) : null,
                values[8] != null && !values[8].equals("NULL") ? cleanString(values[8]) : null,
                values[9] != null && !values[9].equals("NULL") ? cleanString(values[9]) : null,
                values[10] != null && !values[10].equals("NULL") ? cleanString(values[10]) : null,
                cleanString(values[11]),
                values[12] != null && !values[12].equals("NULL") ? Integer.parseInt(values[12]) : null
            ));
        }
        
        view.upsertAll(null, customers);
    }
    
    private static void loadEmployeeData(IgniteClient client, List<String> valueRows) {
        Table table = client.tables().table("Employee");
        RecordView<Employee> view = table.recordView(Employee.class);
        
        List<Employee> employees = new ArrayList<>();
        for (String row : valueRows) {
            String[] values = parseValueRow(row);
            employees.add(new Employee(
                Integer.parseInt(values[0]),
                cleanString(values[1]),
                cleanString(values[2]),
                values[3] != null && !values[3].equals("NULL") ? cleanString(values[3]) : null,
                values[4] != null && !values[4].equals("NULL") ? Integer.parseInt(values[4]) : null,
                values[5] != null && !values[5].equals("NULL") ? LocalDate.parse(cleanString(values[5])) : null,
                values[6] != null && !values[6].equals("NULL") ? LocalDate.parse(cleanString(values[6])) : null,
                values[7] != null && !values[7].equals("NULL") ? cleanString(values[7]) : null,
                values[8] != null && !values[8].equals("NULL") ? cleanString(values[8]) : null,
                values[9] != null && !values[9].equals("NULL") ? cleanString(values[9]) : null,
                values[10] != null && !values[10].equals("NULL") ? cleanString(values[10]) : null,
                values[11] != null && !values[11].equals("NULL") ? cleanString(values[11]) : null,
                values[12] != null && !values[12].equals("NULL") ? cleanString(values[12]) : null,
                values[13] != null && !values[13].equals("NULL") ? cleanString(values[13]) : null,
                values[14] != null && !values[14].equals("NULL") ? cleanString(values[14]) : null
            ));
        }
        
        view.upsertAll(null, employees);
    }
    
    private static void loadInvoiceData(IgniteClient client, List<String> valueRows) {
        Table table = client.tables().table("Invoice");
        RecordView<Invoice> view = table.recordView(Invoice.class);
        
        List<Invoice> invoices = new ArrayList<>();
        for (String row : valueRows) {
            String[] values = parseValueRow(row);
            invoices.add(new Invoice(
                Integer.parseInt(values[0]),
                Integer.parseInt(values[1]),
                LocalDate.parse(cleanString(values[2])),
                values[3] != null && !values[3].equals("NULL") ? cleanString(values[3]) : null,
                values[4] != null && !values[4].equals("NULL") ? cleanString(values[4]) : null,
                values[5] != null && !values[5].equals("NULL") ? cleanString(values[5]) : null,
                values[6] != null && !values[6].equals("NULL") ? cleanString(values[6]) : null,
                values[7] != null && !values[7].equals("NULL") ? cleanString(values[7]) : null,
                new BigDecimal(values[8])
            ));
        }
        
        view.upsertAll(null, invoices);
    }
    
    private static void loadInvoiceLineData(IgniteClient client, List<String> valueRows) {
        Table table = client.tables().table("InvoiceLine");
        RecordView<InvoiceLine> view = table.recordView(InvoiceLine.class);
        
        List<InvoiceLine> invoiceLines = new ArrayList<>();
        for (String row : valueRows) {
            String[] values = parseValueRow(row);
            invoiceLines.add(new InvoiceLine(
                Integer.parseInt(values[0]),
                Integer.parseInt(values[1]),
                Integer.parseInt(values[2]),
                new BigDecimal(values[3]),
                Integer.parseInt(values[4])
            ));
        }
        
        view.upsertAll(null, invoiceLines);
    }
    
    private static void loadPlaylistData(IgniteClient client, List<String> valueRows) {
        Table table = client.tables().table("Playlist");
        RecordView<Playlist> view = table.recordView(Playlist.class);
        
        List<Playlist> playlists = new ArrayList<>();
        for (String row : valueRows) {
            String[] values = parseValueRow(row);
            playlists.add(new Playlist(
                Integer.parseInt(values[0]),
                values[1] != null && !values[1].equals("NULL") ? cleanString(values[1]) : null
            ));
        }
        
        view.upsertAll(null, playlists);
    }
    
    private static void loadPlaylistTrackData(IgniteClient client, List<String> valueRows) {
        Table table = client.tables().table("PlaylistTrack");
        RecordView<PlaylistTrack> view = table.recordView(PlaylistTrack.class);
        
        List<PlaylistTrack> playlistTracks = new ArrayList<>();
        for (String row : valueRows) {
            String[] values = parseValueRow(row);
            playlistTracks.add(new PlaylistTrack(
                Integer.parseInt(values[0]),
                Integer.parseInt(values[1])
            ));
        }
        
        view.upsertAll(null, playlistTracks);
    }
    
    private static String[] parseValueRow(String row) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < row.length(); i++) {
            char c = row.charAt(i);
            
            if (c == '\'' && (i == 0 || row.charAt(i-1) != '\\')) {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        
        values.add(current.toString().trim());
        return values.toArray(new String[0]);
    }
    
    private static String cleanString(String value) {
        if (value == null || value.equals("NULL")) {
            return null;
        }
        
        value = value.trim();
        
        // Handle SQL date literals like "date '2020-01-01'"
        if (value.startsWith("date '") && value.endsWith("'")) {
            value = value.substring(6, value.length() - 1);
        } else if (value.startsWith("'") && value.endsWith("'")) {
            value = value.substring(1, value.length() - 1);
        }
        
        // Fix malformed dates in the SQL data - must be done after quote removal
        if (value.matches("\\d{4}-0\\d{2}-\\d{2}")) {
            // Fix dates like "1958-012-08" -> "1958-12-08"
            value = value.replaceFirst("-(0)(\\d{2})-", "-$2-");
        }
        if (value.matches("\\d{4}-\\d{2}-0\\d{2}")) {
            // Fix dates like "1962-02-018" -> "1962-02-18"  
            value = value.replaceFirst("-(\\d{2})-(0)(\\d{2})$", "-$1-$3");
        }
        
        return value.replace("''", "'");
    }
    
    private static class TableData {
        final String tableName;
        final List<String> valueRows;
        
        TableData(String tableName, List<String> valueRows) {
            this.tableName = tableName;
            this.valueRows = new ArrayList<>(valueRows);  // Make it mutable
        }
    }
}