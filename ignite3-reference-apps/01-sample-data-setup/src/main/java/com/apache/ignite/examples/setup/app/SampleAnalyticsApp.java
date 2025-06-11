package com.apache.ignite.examples.setup.app;

import org.apache.ignite.client.IgniteClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apache.ignite.examples.setup.util.DataSetupUtils;
import com.apache.ignite.examples.setup.util.ReportingUtils;

public class SampleAnalyticsApp {
    
    private static final Logger logger = LoggerFactory.getLogger(SampleAnalyticsApp.class);
    
    public static void main(String[] args) {
        String igniteAddress = args.length > 0 ? args[0] : "127.0.0.1:10800";
        String reportType = args.length > 1 ? args[1] : "all";
        
        logger.info("Starting Music Store Sample Analytics");
        logger.info("Connecting to Ignite cluster at: {}", igniteAddress);
        logger.info("Report type: {}", reportType);
        
        try (IgniteClient client = DataSetupUtils.connectToCluster(igniteAddress)) {
            
            verifyDataExists(client);
            
            switch (reportType.toLowerCase()) {
                case "all":
                    generateAllReports(client);
                    break;
                case "artists":
                    generateArtistReports(client);
                    break;
                case "albums":
                    generateAlbumReports(client);
                    break;
                case "tracks":
                    generateTrackReports(client);
                    break;
                case "customers":
                    generateCustomerReports(client);
                    break;
                case "sales":
                    generateSalesReports(client);
                    break;
                case "complex":
                    generateComplexReports(client);
                    break;
                default:
                    logger.error("Unknown report type: {}. Use: all, artists, albums, tracks, customers, sales, or complex", reportType);
                    printUsage();
                    System.exit(1);
            }
            
            logger.info("Analytics reporting completed successfully");
            
        } catch (Exception e) {
            logger.error("Analytics reporting failed: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
    
    private static void verifyDataExists(IgniteClient client) {
        logger.info("Verifying that sample data exists...");
        
        String[] tables = {"Artist", "Album", "Track", "Customer"};
        boolean hasData = false;
        
        for (String table : tables) {
            long count = DataSetupUtils.getTableRowCount(client, table);
            if (count > 0) {
                hasData = true;
                break;
            }
        }
        
        if (!hasData) {
            logger.warn("No sample data found in music store tables.");
            logger.warn("Run ProjectInitializationApp or DataLoadingApp first to load sample data.");
            logger.info("Proceeding with empty data demonstration...");
        } else {
            logger.info("Sample data verified - proceeding with analytics");
        }
    }
    
    private static void generateAllReports(IgniteClient client) {
        logger.info("Generating all sample reports...");
        
        try {
            ReportingUtils.generateSampleReports(client);
            ReportingUtils.runComplexQueries(client);
            
            String[] artists = {"AC/DC", "Black Sabbath", "Led Zeppelin"};
            for (String artist : artists) {
                ReportingUtils.displayDetailedTrackInfo(client, artist);
            }
            
        } catch (Exception e) {
            logger.error("Error generating all reports: {}", e.getMessage());
        }
    }
    
    private static void generateArtistReports(IgniteClient client) {
        logger.info("Generating artist-focused reports...");
        
        try {
            ReportingUtils.displayArtistSummary(client);
            
            String[] featuredArtists = {"AC/DC", "Accept", "Aerosmith", "Black Sabbath", "Led Zeppelin"};
            for (String artist : featuredArtists) {
                logger.info("\n--- Detailed Analysis for {} ---", artist);
                ReportingUtils.displayDetailedTrackInfo(client, artist);
            }
            
        } catch (Exception e) {
            logger.error("Error generating artist reports: {}", e.getMessage());
        }
    }
    
    private static void generateAlbumReports(IgniteClient client) {
        logger.info("Generating album-focused reports...");
        
        try {
            ReportingUtils.displayAlbumSummary(client);
            
            logger.info("\n--- Album Length Analysis ---");
            var resultSet = client.sql().execute(null,
                "SELECT al.Title, a.Name as ArtistName, COUNT(t.TrackId) as TrackCount, " +
                "AVG(t.Milliseconds) as AvgTrackLength, SUM(t.Milliseconds) as TotalAlbumLength " +
                "FROM Album al " +
                "JOIN Artist a ON al.ArtistId = a.ArtistId " +
                "LEFT JOIN Track t ON al.AlbumId = t.AlbumId " +
                "GROUP BY al.AlbumId, al.Title, a.Name " +
                "HAVING COUNT(t.TrackId) > 0 " +
                "ORDER BY TotalAlbumLength DESC");
            
            resultSet.forEachRemaining(row -> {
                String albumTitle = row.stringValue("Title");
                String artistName = row.stringValue("ArtistName");
                long trackCount = row.longValue("TrackCount");
                long totalLength = row.longValue("TotalAlbumLength");
                int totalMinutes = (int) (totalLength / 60000);
                
                logger.info("Album: {} by {} - {} tracks, Total: {} minutes", 
                           albumTitle, artistName, trackCount, totalMinutes);
            });
            
        } catch (Exception e) {
            logger.error("Error generating album reports: {}", e.getMessage());
        }
    }
    
    private static void generateTrackReports(IgniteClient client) {
        logger.info("Generating track-focused reports...");
        
        try {
            ReportingUtils.displayTrackSummary(client);
            ReportingUtils.displayMostPopularGenres(client);
            
            logger.info("\n--- Longest Tracks ---");
            var resultSet = client.sql().execute(null,
                "SELECT t.Name as TrackName, a.Name as ArtistName, t.Milliseconds " +
                "FROM Track t " +
                "JOIN Album al ON t.AlbumId = al.AlbumId " +
                "JOIN Artist a ON al.ArtistId = a.ArtistId " +
                "ORDER BY t.Milliseconds DESC " +
                "LIMIT 10");
            
            resultSet.forEachRemaining(row -> {
                String trackName = row.stringValue("TrackName");
                String artistName = row.stringValue("ArtistName");
                int milliseconds = row.intValue("Milliseconds");
                int minutes = milliseconds / 60000;
                int seconds = (milliseconds % 60000) / 1000;
                
                logger.info("Track: {} by {} - Duration: {}:{:02d}", 
                           trackName, artistName, minutes, seconds);
            });
            
        } catch (Exception e) {
            logger.error("Error generating track reports: {}", e.getMessage());
        }
    }
    
    private static void generateCustomerReports(IgniteClient client) {
        logger.info("Generating customer-focused reports...");
        
        try {
            ReportingUtils.displayCustomerSummary(client);
            
            logger.info("\n--- Customer Geographic Distribution ---");
            var resultSet = client.sql().execute(null,
                "SELECT Country, COUNT(*) as CustomerCount " +
                "FROM Customer " +
                "GROUP BY Country " +
                "ORDER BY CustomerCount DESC");
            
            resultSet.forEachRemaining(row -> {
                String country = row.stringValue("Country");
                long customerCount = row.longValue("CustomerCount");
                logger.info("Country: {} - Customers: {}", country, customerCount);
            });
            
        } catch (Exception e) {
            logger.error("Error generating customer reports: {}", e.getMessage());
        }
    }
    
    private static void generateSalesReports(IgniteClient client) {
        logger.info("Generating sales-focused reports...");
        
        try {
            ReportingUtils.displaySalesReport(client);
            
            logger.info("\n--- Revenue by Genre ---");
            var resultSet = client.sql().execute(null,
                "SELECT g.Name as GenreName, COUNT(il.InvoiceLineId) as ItemsSold, " +
                "SUM(il.UnitPrice * il.Quantity) as TotalRevenue " +
                "FROM Genre g " +
                "LEFT JOIN Track t ON g.GenreId = t.GenreId " +
                "LEFT JOIN InvoiceLine il ON t.TrackId = il.TrackId " +
                "GROUP BY g.GenreId, g.Name " +
                "HAVING SUM(il.UnitPrice * il.Quantity) > 0 " +
                "ORDER BY TotalRevenue DESC");
            
            resultSet.forEachRemaining(row -> {
                String genreName = row.stringValue("GenreName");
                long itemsSold = row.longValue("ItemsSold");
                var totalRevenue = row.decimalValue("TotalRevenue");
                logger.info("Genre: {} - Items Sold: {}, Revenue: ${}", 
                           genreName, itemsSold, totalRevenue);
            });
            
        } catch (Exception e) {
            logger.error("Error generating sales reports: {}", e.getMessage());
        }
    }
    
    private static void generateComplexReports(IgniteClient client) {
        logger.info("Generating complex analytical reports...");
        
        try {
            ReportingUtils.runComplexQueries(client);
            
            logger.info("\n--- Cross-Entity Analysis ---");
            var resultSet = client.sql().execute(null,
                "SELECT " +
                "  (SELECT COUNT(*) FROM Artist) as ArtistCount, " +
                "  (SELECT COUNT(*) FROM Album) as AlbumCount, " +
                "  (SELECT COUNT(*) FROM Track) as TrackCount, " +
                "  (SELECT COUNT(*) FROM Customer) as CustomerCount, " +
                "  (SELECT COUNT(*) FROM Invoice) as InvoiceCount, " +
                "  (SELECT SUM(Total) FROM Invoice) as TotalRevenue");
            
            if (resultSet.hasNext()) {
                var row = resultSet.next();
                long artistCount = row.longValue("ArtistCount");
                long albumCount = row.longValue("AlbumCount");
                long trackCount = row.longValue("TrackCount");
                long customerCount = row.longValue("CustomerCount");
                long invoiceCount = row.longValue("InvoiceCount");
                var totalRevenue = row.decimalValue("TotalRevenue");
                
                logger.info("Music Store Summary:");
                logger.info("  Artists: {}", artistCount);
                logger.info("  Albums: {}", albumCount);
                logger.info("  Tracks: {}", trackCount);
                logger.info("  Customers: {}", customerCount);
                logger.info("  Invoices: {}", invoiceCount);
                logger.info("  Total Revenue: ${}", totalRevenue != null ? totalRevenue : "0.00");
            }
            
        } catch (Exception e) {
            logger.error("Error generating complex reports: {}", e.getMessage());
        }
    }
    
    private static void printUsage() {
        logger.info("Usage: SampleAnalyticsApp [ignite-address] [report-type]");
        logger.info("  ignite-address: Ignite cluster address (default: 127.0.0.1:10800)");
        logger.info("  report-type: One of the following:");
        logger.info("    all       - Generate all available reports (default)");
        logger.info("    artists   - Artist-focused analysis");
        logger.info("    albums    - Album-focused analysis");
        logger.info("    tracks    - Track-focused analysis");
        logger.info("    customers - Customer-focused analysis");
        logger.info("    sales     - Sales and revenue analysis");
        logger.info("    complex   - Complex cross-entity analysis");
        logger.info("");
        logger.info("Examples:");
        logger.info("  java -jar sample-analytics-app.jar");
        logger.info("  java -jar sample-analytics-app.jar 127.0.0.1:10800 artists");
        logger.info("  java -jar sample-analytics-app.jar 127.0.0.1:10800 sales");
    }
}