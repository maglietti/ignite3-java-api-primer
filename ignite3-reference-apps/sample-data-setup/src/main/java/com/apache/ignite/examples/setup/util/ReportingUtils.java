package com.apache.ignite.examples.setup.util;

import java.math.BigDecimal;

import org.apache.ignite.client.IgniteClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportingUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportingUtils.class);
    
    public static void generateSampleReports(IgniteClient client) {
        logger.info("Generating sample reports from music store data...");
        
        displayArtistSummary(client);
        displayAlbumSummary(client);
        displayTrackSummary(client);
        displayCustomerSummary(client);
        displaySalesReport(client);
        displayMostPopularGenres(client);
    }
    
    public static void displayArtistSummary(IgniteClient client) {
        logger.info("\n=== Artist Summary ===");
        
        try {
            var resultSet = client.sql().execute(null,
                "SELECT a.Name as ArtistName, COUNT(al.AlbumId) as AlbumCount " +
                "FROM Artist a " +
                "LEFT JOIN Album al ON a.ArtistId = al.ArtistId " +
                "GROUP BY a.ArtistId, a.Name " +
                "ORDER BY AlbumCount DESC, a.Name");
            
            resultSet.forEachRemaining(row -> {
                String artistName = row.stringValue("ArtistName");
                long albumCount = row.longValue("AlbumCount");
                logger.info("Artist: {} - Albums: {}", artistName, albumCount);
            });
            
        } catch (Exception e) {
            logger.error("Error generating artist summary: {}", e.getMessage());
        }
    }
    
    public static void displayAlbumSummary(IgniteClient client) {
        logger.info("\n=== Album Summary ===");
        
        try {
            var resultSet = client.sql().execute(null,
                "SELECT a.Name as ArtistName, al.Title as AlbumTitle, COUNT(t.TrackId) as TrackCount " +
                "FROM Artist a " +
                "JOIN Album al ON a.ArtistId = al.ArtistId " +
                "LEFT JOIN Track t ON al.AlbumId = t.AlbumId " +
                "GROUP BY a.ArtistId, a.Name, al.AlbumId, al.Title " +
                "ORDER BY a.Name, al.Title");
            
            resultSet.forEachRemaining(row -> {
                String artistName = row.stringValue("ArtistName");
                String albumTitle = row.stringValue("AlbumTitle");
                long trackCount = row.longValue("TrackCount");
                logger.info("Album: {} by {} - Tracks: {}", albumTitle, artistName, trackCount);
            });
            
        } catch (Exception e) {
            logger.error("Error generating album summary: {}", e.getMessage());
        }
    }
    
    public static void displayTrackSummary(IgniteClient client) {
        logger.info("\n=== Track Summary ===");
        
        try {
            var resultSet = client.sql().execute(null,
                "SELECT t.Name as TrackName, a.Name as ArtistName, al.Title as AlbumTitle, " +
                "g.Name as GenreName, t.UnitPrice, t.Milliseconds " +
                "FROM Track t " +
                "JOIN Album al ON t.AlbumId = al.AlbumId " +
                "JOIN Artist a ON al.ArtistId = a.ArtistId " +
                "LEFT JOIN Genre g ON t.GenreId = g.GenreId " +
                "ORDER BY a.Name, al.Title, t.TrackId " +
                "LIMIT 10");
            
            resultSet.forEachRemaining(row -> {
                String trackName = row.stringValue("TrackName");
                String artistName = row.stringValue("ArtistName");
                String albumTitle = row.stringValue("AlbumTitle");
                String genreName = row.stringValue("GenreName");
                BigDecimal unitPrice = row.decimalValue("UnitPrice");
                int milliseconds = row.intValue("Milliseconds");
                int seconds = milliseconds / 1000;
                int minutes = seconds / 60;
                seconds = seconds % 60;
                
                logger.info("Track: {} by {} (Album: {}) - Genre: {} - Price: ${} - Duration: {}:{:02d}", 
                           trackName, artistName, albumTitle, genreName, unitPrice, minutes, seconds);
            });
            
        } catch (Exception e) {
            logger.error("Error generating track summary: {}", e.getMessage());
        }
    }
    
    public static void displayCustomerSummary(IgniteClient client) {
        logger.info("\n=== Customer Summary ===");
        
        try {
            var resultSet = client.sql().execute(null,
                "SELECT c.FirstName, c.LastName, c.Country, c.City, COUNT(i.InvoiceId) as InvoiceCount " +
                "FROM Customer c " +
                "LEFT JOIN Invoice i ON c.CustomerId = i.CustomerId " +
                "GROUP BY c.CustomerId, c.FirstName, c.LastName, c.Country, c.City " +
                "ORDER BY InvoiceCount DESC, c.LastName");
            
            resultSet.forEachRemaining(row -> {
                String firstName = row.stringValue("FirstName");
                String lastName = row.stringValue("LastName");
                String country = row.stringValue("Country");
                String city = row.stringValue("City");
                long invoiceCount = row.longValue("InvoiceCount");
                
                logger.info("Customer: {} {} from {}, {} - Invoices: {}", 
                           firstName, lastName, city, country, invoiceCount);
            });
            
        } catch (Exception e) {
            logger.error("Error generating customer summary: {}", e.getMessage());
        }
    }
    
    public static void displaySalesReport(IgniteClient client) {
        logger.info("\n=== Sales Report ===");
        
        try {
            var resultSet = client.sql().execute(null,
                "SELECT i.InvoiceDate, c.FirstName, c.LastName, i.Total, " +
                "COUNT(il.InvoiceLineId) as LineItemCount " +
                "FROM Invoice i " +
                "JOIN Customer c ON i.CustomerId = c.CustomerId " +
                "LEFT JOIN InvoiceLine il ON i.InvoiceId = il.InvoiceId " +
                "GROUP BY i.InvoiceId, i.InvoiceDate, c.FirstName, c.LastName, i.Total " +
                "ORDER BY i.InvoiceDate DESC");
            
            resultSet.forEachRemaining(row -> {
                var invoiceDate = row.dateValue("InvoiceDate");
                String firstName = row.stringValue("FirstName");
                String lastName = row.stringValue("LastName");
                BigDecimal total = row.decimalValue("Total");
                long lineItemCount = row.longValue("LineItemCount");
                
                logger.info("Sale: {} - Customer: {} {} - Total: ${} - Items: {}", 
                           invoiceDate, firstName, lastName, total, lineItemCount);
            });
            
        } catch (Exception e) {
            logger.error("Error generating sales report: {}", e.getMessage());
        }
    }
    
    public static void displayMostPopularGenres(IgniteClient client) {
        logger.info("\n=== Most Popular Genres ===");
        
        try {
            var resultSet = client.sql().execute(null,
                "SELECT g.Name as GenreName, COUNT(t.TrackId) as TrackCount " +
                "FROM Genre g " +
                "LEFT JOIN Track t ON g.GenreId = t.GenreId " +
                "GROUP BY g.GenreId, g.Name " +
                "ORDER BY TrackCount DESC");
            
            resultSet.forEachRemaining(row -> {
                String genreName = row.stringValue("GenreName");
                long trackCount = row.longValue("TrackCount");
                logger.info("Genre: {} - Tracks: {}", genreName, trackCount);
            });
            
        } catch (Exception e) {
            logger.error("Error generating genre popularity report: {}", e.getMessage());
        }
    }
    
    public static void displayDetailedTrackInfo(IgniteClient client, String artistName) {
        logger.info("\n=== Detailed Track Info for Artist: {} ===", artistName);
        
        try {
            var resultSet = client.sql().execute(null,
                "SELECT t.Name as TrackName, al.Title as AlbumTitle, g.Name as GenreName, " +
                "mt.Name as MediaTypeName, t.Composer, t.Milliseconds, t.Bytes, t.UnitPrice " +
                "FROM Track t " +
                "JOIN Album al ON t.AlbumId = al.AlbumId " +
                "JOIN Artist a ON al.ArtistId = a.ArtistId " +
                "LEFT JOIN Genre g ON t.GenreId = g.GenreId " +
                "LEFT JOIN MediaType mt ON t.MediaTypeId = mt.MediaTypeId " +
                "WHERE a.Name = ? " +
                "ORDER BY al.Title, t.TrackId", artistName);
            
            if (!resultSet.hasNext()) {
                logger.info("No tracks found for artist: {}", artistName);
                return;
            }
            
            resultSet.forEachRemaining(row -> {
                String trackName = row.stringValue("TrackName");
                String albumTitle = row.stringValue("AlbumTitle");
                String genreName = row.stringValue("GenreName");
                String mediaTypeName = row.stringValue("MediaTypeName");
                String composer = row.stringValue("Composer");
                int milliseconds = row.intValue("Milliseconds");
                Integer bytes = row.intValue("Bytes");
                BigDecimal unitPrice = row.decimalValue("UnitPrice");
                
                int seconds = milliseconds / 1000;
                int minutes = seconds / 60;
                seconds = seconds % 60;
                
                logger.info("  Track: {} (Album: {})", trackName, albumTitle);
                logger.info("    Genre: {} | Media: {} | Composer: {}", genreName, mediaTypeName, composer);
                logger.info("    Duration: {}:{:02d} | Size: {} bytes | Price: ${}", minutes, seconds, bytes, unitPrice);
                logger.info("");
            });
            
        } catch (Exception e) {
            logger.error("Error generating detailed track info for {}: {}", artistName, e.getMessage());
        }
    }
    
    public static void runComplexQueries(IgniteClient client) {
        logger.info("\n=== Complex Query Examples ===");
        
        displayAverageAlbumLength(client);
        displayCustomerPurchaseHistory(client);
        displayTopSellingTracks(client);
    }
    
    private static void displayAverageAlbumLength(IgniteClient client) {
        try {
            var resultSet = client.sql().execute(null,
                "SELECT al.Title, a.Name as ArtistName, " +
                "COUNT(t.TrackId) as TrackCount, " +
                "AVG(t.Milliseconds) as AvgTrackLength, " +
                "SUM(t.Milliseconds) as TotalAlbumLength " +
                "FROM Album al " +
                "JOIN Artist a ON al.ArtistId = a.ArtistId " +
                "LEFT JOIN Track t ON al.AlbumId = t.AlbumId " +
                "GROUP BY al.AlbumId, al.Title, a.Name " +
                "HAVING COUNT(t.TrackId) > 0 " +
                "ORDER BY TotalAlbumLength DESC");
            
            logger.info("Average album lengths:");
            resultSet.forEachRemaining(row -> {
                String albumTitle = row.stringValue("Title");
                String artistName = row.stringValue("ArtistName");
                long trackCount = row.longValue("TrackCount");
                long avgLength = row.longValue("AvgTrackLength");
                long totalLength = row.longValue("TotalAlbumLength");
                
                int avgMinutes = (int) (avgLength / 60000);
                int avgSeconds = (int) ((avgLength % 60000) / 1000);
                int totalMinutes = (int) (totalLength / 60000);
                int totalSecondsOnly = (int) ((totalLength % 60000) / 1000);
                
                logger.info("  {} by {} - {} tracks, Avg: {}:{:02d}, Total: {}:{:02d}", 
                           albumTitle, artistName, trackCount, avgMinutes, avgSeconds, totalMinutes, totalSecondsOnly);
            });
            
        } catch (Exception e) {
            logger.error("Error in average album length query: {}", e.getMessage());
        }
    }
    
    private static void displayCustomerPurchaseHistory(IgniteClient client) {
        try {
            var resultSet = client.sql().execute(null,
                "SELECT c.FirstName, c.LastName, " +
                "COUNT(DISTINCT i.InvoiceId) as TotalInvoices, " +
                "COUNT(il.InvoiceLineId) as TotalItems, " +
                "SUM(i.Total) as TotalSpent " +
                "FROM Customer c " +
                "LEFT JOIN Invoice i ON c.CustomerId = i.CustomerId " +
                "LEFT JOIN InvoiceLine il ON i.InvoiceId = il.InvoiceId " +
                "GROUP BY c.CustomerId, c.FirstName, c.LastName " +
                "HAVING SUM(i.Total) > 0 " +
                "ORDER BY TotalSpent DESC");
            
            logger.info("Customer purchase history:");
            resultSet.forEachRemaining(row -> {
                String firstName = row.stringValue("FirstName");
                String lastName = row.stringValue("LastName");
                long totalInvoices = row.longValue("TotalInvoices");
                long totalItems = row.longValue("TotalItems");
                BigDecimal totalSpent = row.decimalValue("TotalSpent");
                
                logger.info("  {} {} - {} invoices, {} items, ${} spent", 
                           firstName, lastName, totalInvoices, totalItems, totalSpent);
            });
            
        } catch (Exception e) {
            logger.error("Error in customer purchase history query: {}", e.getMessage());
        }
    }
    
    private static void displayTopSellingTracks(IgniteClient client) {
        try {
            var resultSet = client.sql().execute(null,
                "SELECT t.Name as TrackName, a.Name as ArtistName, al.Title as AlbumTitle, " +
                "COUNT(il.InvoiceLineId) as TimesSold, " +
                "SUM(il.Quantity * il.UnitPrice) as TotalRevenue " +
                "FROM Track t " +
                "JOIN Album al ON t.AlbumId = al.AlbumId " +
                "JOIN Artist a ON al.ArtistId = a.ArtistId " +
                "LEFT JOIN InvoiceLine il ON t.TrackId = il.TrackId " +
                "GROUP BY t.TrackId, t.Name, a.Name, al.Title " +
                "HAVING COUNT(il.InvoiceLineId) > 0 " +
                "ORDER BY TimesSold DESC, TotalRevenue DESC " +
                "LIMIT 10");
            
            logger.info("Top selling tracks:");
            resultSet.forEachRemaining(row -> {
                String trackName = row.stringValue("TrackName");
                String artistName = row.stringValue("ArtistName");
                String albumTitle = row.stringValue("AlbumTitle");
                long timesSold = row.longValue("TimesSold");
                BigDecimal totalRevenue = row.decimalValue("TotalRevenue");
                
                logger.info("  {} by {} (Album: {}) - Sold {} times, Revenue: ${}", 
                           trackName, artistName, albumTitle, timesSold, totalRevenue);
            });
            
        } catch (Exception e) {
            logger.error("Error in top selling tracks query: {}", e.getMessage());
        }
    }
}