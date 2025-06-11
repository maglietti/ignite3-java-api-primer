# 12. Integration Patterns

This section demonstrates how to integrate Apache Ignite 3 with popular frameworks and enterprise patterns commonly used in production environments.

## Spring Framework Integration

### Spring Boot Auto-Configuration

#### Creating a Spring Boot Starter

```java
// IgniteAutoConfiguration.java
@Configuration
@EnableConfigurationProperties(IgniteProperties.class)
@ConditionalOnClass(IgniteClient.class)
public class IgniteAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public IgniteClient igniteClient(IgniteProperties properties) {
        IgniteClient.Builder builder = IgniteClient.builder()
            .addresses(properties.getAddresses().toArray(new String[0]))
            .connectTimeout(properties.getConnectTimeout())
            .operationTimeout(properties.getOperationTimeout())
            .heartbeatInterval(properties.getHeartbeatInterval());
        
        // Add authentication if configured
        if (properties.getAuth().getUsername() != null) {
            builder.authenticator(BasicAuthenticator.builder()
                .username(properties.getAuth().getUsername())
                .password(properties.getAuth().getPassword())
                .build());
        }
        
        // Add SSL if configured
        if (properties.getSsl().isEnabled()) {
            SslConfiguration sslConfig = SslConfiguration.builder()
                .enabled(true)
                .keyStorePath(properties.getSsl().getKeyStorePath())
                .keyStorePassword(properties.getSsl().getKeyStorePassword())
                .trustStorePath(properties.getSsl().getTrustStorePath())
                .trustStorePassword(properties.getSsl().getTrustStorePassword())
                .build();
            builder.ssl(sslConfig);
        }
        
        return builder.build();
    }
    
    @Bean
    @ConditionalOnBean(IgniteClient.class)
    public IgniteTemplate igniteTemplate(IgniteClient igniteClient) {
        return new IgniteTemplate(igniteClient);
    }
}

// IgniteProperties.java
@ConfigurationProperties(prefix = "ignite")
@Data
public class IgniteProperties {
    private List<String> addresses = Arrays.asList("localhost:10800");
    private int connectTimeout = 5000;
    private int operationTimeout = 30000;
    private int heartbeatInterval = 3000;
    private AuthProperties auth = new AuthProperties();
    private SslProperties ssl = new SslProperties();
    
    @Data
    public static class AuthProperties {
        private String username;
        private String password;
    }
    
    @Data
    public static class SslProperties {
        private boolean enabled = false;
        private String keyStorePath;
        private String keyStorePassword;
        private String trustStorePath;
        private String trustStorePassword;
    }
}
```

#### Spring Configuration

```yaml
# application.yml
ignite:
  addresses:
    - localhost:10800
    - localhost:10801
  connect-timeout: 5000
  operation-timeout: 30000
  auth:
    username: ${IGNITE_USERNAME:}
    password: ${IGNITE_PASSWORD:}
  ssl:
    enabled: false
    key-store-path: /path/to/keystore.jks
    key-store-password: ${KEYSTORE_PASSWORD:}
    trust-store-path: /path/to/truststore.jks
    trust-store-password: ${TRUSTSTORE_PASSWORD:}
```

#### IgniteTemplate for Spring

```java
@Component
public class IgniteTemplate {
    private final IgniteClient client;
    
    public IgniteTemplate(IgniteClient client) {
        this.client = client;
    }
    
    // Generic record operations
    public <T> void save(T record) {
        Class<?> recordClass = record.getClass();
        String tableName = extractTableName(recordClass);
        
        RecordView<T> view = (RecordView<T>) client.tables().table(tableName).recordView(recordClass);
        view.upsert(null, record);
    }
    
    public <T> T findById(Object id, Class<T> recordClass) {
        String tableName = extractTableName(recordClass);
        RecordView<T> view = (RecordView<T>) client.tables().table(tableName).recordView(recordClass);
        
        try {
            T key = recordClass.getDeclaredConstructor().newInstance();
            setIdField(key, id);
            return view.get(null, key);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find record by id", e);
        }
    }
    
    public <T> void saveAll(Collection<T> records) {
        if (records.isEmpty()) return;
        
        Class<?> recordClass = records.iterator().next().getClass();
        String tableName = extractTableName(recordClass);
        
        RecordView<T> view = (RecordView<T>) client.tables().table(tableName).recordView(recordClass);
        view.upsertAll(null, records);
    }
    
    public <T> List<T> findByQuery(String sql, Class<T> resultClass, Object... parameters) {
        try (ResultSet<T> resultSet = client.sql().execute(null, Mapper.of(resultClass), sql, parameters)) {
            List<T> results = new ArrayList<>();
            while (resultSet.hasNext()) {
                results.add(resultSet.next());
            }
            return results;
        }
    }
    
    // Async operations
    public <T> CompletableFuture<Void> saveAsync(T record) {
        Class<?> recordClass = record.getClass();
        String tableName = extractTableName(recordClass);
        
        RecordView<T> view = (RecordView<T>) client.tables().table(tableName).recordView(recordClass);
        return view.upsertAsync(null, record);
    }
    
    public <T> CompletableFuture<AsyncResultSet<T>> findByQueryAsync(String sql, Class<T> resultClass, Object... parameters) {
        return client.sql().executeAsync(null, Mapper.of(resultClass), sql, parameters);
    }
    
    // Utility methods
    private String extractTableName(Class<?> recordClass) {
        Table tableAnnotation = recordClass.getAnnotation(Table.class);
        if (tableAnnotation != null && !tableAnnotation.value().isEmpty()) {
            return tableAnnotation.value();
        }
        return recordClass.getSimpleName();
    }
    
    private void setIdField(Object instance, Object id) throws Exception {
        Field[] fields = instance.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Id.class)) {
                field.setAccessible(true);
                field.set(instance, id);
                return;
            }
        }
        throw new IllegalArgumentException("No @Id field found in " + instance.getClass());
    }
}
```

### Spring Data Integration

#### Custom Repository Implementation

```java
// Base repository interface
public interface IgniteRepository<T, ID> {
    void save(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    void delete(T entity);
    void deleteById(ID id);
    boolean existsById(ID id);
    long count();
}

// Abstract base implementation
public abstract class AbstractIgniteRepository<T, ID> implements IgniteRepository<T, ID> {
    protected final IgniteTemplate igniteTemplate;
    protected final Class<T> entityClass;
    
    public AbstractIgniteRepository(IgniteTemplate igniteTemplate, Class<T> entityClass) {
        this.igniteTemplate = igniteTemplate;
        this.entityClass = entityClass;
    }
    
    @Override
    public void save(T entity) {
        igniteTemplate.save(entity);
    }
    
    @Override
    public Optional<T> findById(ID id) {
        T entity = igniteTemplate.findById(id, entityClass);
        return Optional.ofNullable(entity);
    }
    
    @Override
    public List<T> findAll() {
        String tableName = extractTableName(entityClass);
        return igniteTemplate.findByQuery("SELECT * FROM " + tableName, entityClass);
    }
    
    @Override
    public void delete(T entity) {
        try {
            ID id = extractId(entity);
            deleteById(id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete entity", e);
        }
    }
    
    @Override
    public void deleteById(ID id) {
        String tableName = extractTableName(entityClass);
        igniteTemplate.client.sql().execute(null, "DELETE FROM " + tableName + " WHERE id = ?", id);
    }
    
    @Override
    public boolean existsById(ID id) {
        return findById(id).isPresent();
    }
    
    @Override
    public long count() {
        String tableName = extractTableName(entityClass);
        var result = igniteTemplate.client.sql().execute(null, "SELECT COUNT(*) as cnt FROM " + tableName);
        return result.hasNext() ? result.next().longValue("cnt") : 0;
    }
    
    // Utility methods
    protected abstract ID extractId(T entity) throws Exception;
    
    protected String extractTableName(Class<?> entityClass) {
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation != null && !tableAnnotation.value().isEmpty()) {
            return tableAnnotation.value();
        }
        return entityClass.getSimpleName();
    }
}

// Concrete repository implementation for Artist
@Repository
public class ArtistRepository extends AbstractIgniteRepository<Artist, Integer> {
    
    public ArtistRepository(IgniteTemplate igniteTemplate) {
        super(igniteTemplate, Artist.class);
    }
    
    @Override
    protected Integer extractId(Artist entity) {
        return entity.getArtistId();
    }
    
    // Custom query methods for music domain
    public List<Artist> findByNameContaining(String namePart) {
        return igniteTemplate.findByQuery(
            "SELECT * FROM Artist WHERE Name LIKE ?", 
            Artist.class, 
            "%" + namePart + "%"
        );
    }
    
    public Artist findByName(String name) {
        List<Artist> artists = igniteTemplate.findByQuery(
            "SELECT * FROM Artist WHERE Name = ?", 
            Artist.class, 
            name
        );
        return artists.isEmpty() ? null : artists.get(0);
    }
    
    public List<Artist> findPopularArtists(int limit) {
        return igniteTemplate.findByQuery(
            "SELECT a.* FROM Artist a " +
            "JOIN Album al ON a.ArtistId = al.ArtistId " +
            "GROUP BY a.ArtistId, a.Name " +
            "ORDER BY COUNT(al.AlbumId) DESC LIMIT ?", 
            Artist.class, 
            limit
        );
    }
}
```

#### Spring Service Layer

```java
@Service
@Transactional
public class ArtistService {
    private final ArtistRepository artistRepository;
    private final IgniteTemplate igniteTemplate;
    
    public ArtistService(ArtistRepository artistRepository, IgniteTemplate igniteTemplate) {
        this.artistRepository = artistRepository;
        this.igniteTemplate = igniteTemplate;
    }
    
    public Artist createArtist(Artist artist) {
        // Validate name uniqueness
        if (artistRepository.findByName(artist.getName()) != null) {
            throw new IllegalArgumentException("Artist name already exists: " + artist.getName());
        }
        
        artistRepository.save(artist);
        return artist;
    }
    
    public Optional<Artist> getArtist(Integer artistId) {
        return artistRepository.findById(artistId);
    }
    
    public List<Artist> searchArtists(String namePart) {
        return artistRepository.findByNameContaining(namePart);
    }
    
    public Artist updateArtist(Integer artistId, Artist updatedArtist) {
        return artistRepository.findById(artistId)
            .map(existing -> {
                existing.setName(updatedArtist.getName());
                artistRepository.save(existing);
                return existing;
            })
            .orElseThrow(() -> new EntityNotFoundException("Artist not found: " + artistId));
    }
    
    public void deleteArtist(Integer artistId) {
        if (!artistRepository.existsById(artistId)) {
            throw new EntityNotFoundException("Artist not found: " + artistId);
        }
        artistRepository.deleteById(artistId);
    }
    
    public List<Artist> getPopularArtists(int limit) {
        return artistRepository.findPopularArtists(limit);
    }
    
    // Batch operations for music catalog import
    public void importArtists(List<Artist> artists) {
        igniteTemplate.saveAll(artists);
    }
    
    public List<Album> getArtistAlbums(Integer artistId) {
        return igniteTemplate.findByQuery(
            "SELECT * FROM Album WHERE ArtistId = ? ORDER BY Title", 
            Album.class, 
            artistId
        );
    }
}
```

## JDBC Driver Usage

### JDBC Connection Setup

#### Basic JDBC Configuration

```java
public class IgniteJDBCExample {
    private static final String JDBC_URL = "jdbc:ignite:thin://localhost:10800";
    
    // Basic connection
    public static Connection createConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL);
    }
    
    // Connection with authentication
    public static Connection createAuthenticatedConnection(String username, String password) throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", username);
        props.setProperty("password", password);
        
        return DriverManager.getConnection(JDBC_URL, props);
    }
    
    // Connection with additional properties
    public static Connection createConfiguredConnection() throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", "ignite");
        props.setProperty("password", "ignite");
        props.setProperty("schema", "PUBLIC");
        props.setProperty("socketTimeout", "30000");
        props.setProperty("connectionTimeout", "5000");
        
        return DriverManager.getConnection(JDBC_URL, props);
    }
}
```

### Integration with Connection Pools

#### HikariCP Integration

```java
@Configuration
public class JDBCPoolConfiguration {
    
    @Bean
    @Primary
    public DataSource igniteDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:ignite:thin://localhost:10800");
        config.setUsername("ignite");
        config.setPassword("ignite");
        
        // Pool settings
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        // Ignite-specific settings
        config.addDataSourceProperty("socketTimeout", "30000");
        config.addDataSourceProperty("connectionTimeout", "5000");
        
        return new HikariDataSource(config);
    }
    
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
```

### Integration with Existing JDBC Tools

#### MyBatis Integration

```java
// Artist mapper interface for MyBatis integration
@Mapper
public interface ArtistMapper {
    
    @Select("SELECT * FROM Artist WHERE ArtistId = #{artistId}")
    Artist findById(Integer artistId);
    
    @Select("SELECT * FROM Artist WHERE Name LIKE CONCAT('%', #{namePart}, '%')")
    List<Artist> findByNameContaining(String namePart);
    
    @Insert("INSERT INTO Artist (ArtistId, Name) " +
            "VALUES (#{artistId}, #{name})")
    int insert(Artist artist);
    
    @Update("UPDATE Artist SET Name = #{name} WHERE ArtistId = #{artistId}")
    int update(Artist artist);
    
    @Delete("DELETE FROM Artist WHERE ArtistId = #{artistId}")
    int deleteById(Integer artistId);
    
    // Complex query with join to get artist album statistics
    @Select("""
        SELECT a.*, COUNT(al.AlbumId) as albumCount, COUNT(t.TrackId) as trackCount
        FROM Artist a 
        LEFT JOIN Album al ON a.ArtistId = al.ArtistId 
        LEFT JOIN Track t ON al.AlbumId = t.AlbumId
        WHERE a.Name LIKE CONCAT('%', #{namePart}, '%')
        GROUP BY a.ArtistId, a.Name
        ORDER BY albumCount DESC
        """)
    @Results({
        @Result(property = "artistId", column = "ArtistId"),
        @Result(property = "name", column = "Name"),
        @Result(property = "albumCount", column = "albumCount"),
        @Result(property = "trackCount", column = "trackCount")
    })
    List<ArtistWithStats> findArtistsWithStats(String namePart);
    
    // Get top artists by album sales
    @Select("""
        SELECT a.*, SUM(il.Quantity * il.UnitPrice) as totalSales
        FROM Artist a
        JOIN Album al ON a.ArtistId = al.ArtistId
        JOIN Track t ON al.AlbumId = t.AlbumId
        JOIN InvoiceLine il ON t.TrackId = il.TrackId
        GROUP BY a.ArtistId, a.Name
        ORDER BY totalSales DESC
        LIMIT #{limit}
        """)
    @Results({
        @Result(property = "artistId", column = "ArtistId"),
        @Result(property = "name", column = "Name"),
        @Result(property = "totalSales", column = "totalSales")
    })
    List<ArtistWithSales> findTopSellingArtists(int limit);
}

// Service using MyBatis for music domain
@Service
public class ArtistMyBatisService {
    private final ArtistMapper artistMapper;
    
    public ArtistMyBatisService(ArtistMapper artistMapper) {
        this.artistMapper = artistMapper;
    }
    
    public Artist getArtist(Integer id) {
        return artistMapper.findById(id);
    }
    
    public List<Artist> searchArtists(String namePart) {
        return artistMapper.findByNameContaining(namePart);
    }
    
    public Artist createArtist(Artist artist) {
        artistMapper.insert(artist);
        return artist;
    }
    
    public List<ArtistWithStats> getArtistsWithStats(String namePart) {
        return artistMapper.findArtistsWithStats(namePart);
    }
    
    public List<ArtistWithSales> getTopSellingArtists(int limit) {
        return artistMapper.findTopSellingArtists(limit);
    }
}
```

#### JPA/Hibernate Integration

```java
// JPA Entity for Artist (JDBC-based access)
@Entity
@javax.persistence.Table(name = "Artist")
public class ArtistJPA {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ArtistId")
    private Integer artistId;
    
    @Column(name = "Name", nullable = false)
    private String name;
    
    // Constructors, getters, setters...
    public ArtistJPA() {}
    
    public ArtistJPA(String name) {
        this.name = name;
    }
    
    public Integer getArtistId() {
        return artistId;
    }
    
    public void setArtistId(Integer artistId) {
        this.artistId = artistId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
}

// JPA Entity for Album
@Entity
@javax.persistence.Table(name = "Album")
public class AlbumJPA {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AlbumId")
    private Integer albumId;
    
    @Column(name = "Title", nullable = false)
    private String title;
    
    @Column(name = "ArtistId", nullable = false)
    private Integer artistId;
    
    // JPA relationship mapping
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ArtistId", insertable = false, updatable = false)
    private ArtistJPA artist;
    
    // Constructors, getters, setters...
    public AlbumJPA() {}
    
    public AlbumJPA(String title, Integer artistId) {
        this.title = title;
        this.artistId = artistId;
    }
    
    // Getters and setters...
    public Integer getAlbumId() {
        return albumId;
    }
    
    public void setAlbumId(Integer albumId) {
        this.albumId = albumId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public Integer getArtistId() {
        return artistId;
    }
    
    public void setArtistId(Integer artistId) {
        this.artistId = artistId;
    }
    
    public ArtistJPA getArtist() {
        return artist;
    }
    
    public void setArtist(ArtistJPA artist) {
        this.artist = artist;
    }
}

// JPA Repository for Artist
@Repository
public interface ArtistJPARepository extends JpaRepository<ArtistJPA, Integer> {
    List<ArtistJPA> findByNameContaining(String namePart);
    Optional<ArtistJPA> findByName(String name);
    
    @Query("SELECT a FROM ArtistJPA a WHERE a.name LIKE %:pattern%")
    List<ArtistJPA> searchByNamePattern(@Param("pattern") String pattern);
    
    // Find artists with most albums
    @Query(value = "SELECT a.* FROM Artist a " +
           "JOIN (SELECT ArtistId, COUNT(*) as album_count " +
           "      FROM Album GROUP BY ArtistId " +
           "      ORDER BY album_count DESC LIMIT :limit) top " +
           "ON a.ArtistId = top.ArtistId", 
           nativeQuery = true)
    List<ArtistJPA> findTopArtistsByAlbumCount(@Param("limit") int limit);
}

// JPA Repository for Album
@Repository
public interface AlbumJPARepository extends JpaRepository<AlbumJPA, Integer> {
    List<AlbumJPA> findByArtistId(Integer artistId);
    List<AlbumJPA> findByTitleContaining(String titlePart);
    
    @Query("SELECT al FROM AlbumJPA al JOIN al.artist a WHERE a.name = :artistName")
    List<AlbumJPA> findByArtistName(@Param("artistName") String artistName);
    
    @Query("SELECT COUNT(al) FROM AlbumJPA al WHERE al.artistId = :artistId")
    long countByArtistId(@Param("artistId") Integer artistId);
}

// Hibernate configuration for Ignite
@Configuration
@EnableJpaRepositories
public class HibernateConfiguration {
    
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.example.entity");
        
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        
        Properties properties = new Properties();
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect"); // Close enough for Ignite SQL
        properties.setProperty("hibernate.hbm2ddl.auto", "validate");
        properties.setProperty("hibernate.show_sql", "true");
        em.setJpaProperties(properties);
        
        return em;
    }
    
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
```

## Microservices Patterns

### Service Discovery Integration

#### Consul Integration

```java
@Component
public class IgniteServiceDiscovery {
    private final ConsulClient consulClient;
    private final IgniteProperties igniteProperties;
    
    public IgniteServiceDiscovery(ConsulClient consulClient, IgniteProperties igniteProperties) {
        this.consulClient = consulClient;
        this.igniteProperties = igniteProperties;
    }
    
    // Discover Ignite nodes from Consul
    public List<String> discoverIgniteNodes() {
        try {
            HealthServicesRequest request = HealthServicesRequest.newBuilder()
                .setService("ignite")
                .setPassing(true)
                .build();
            
            Response<List<HealthService>> response = consulClient.getHealthServices(request);
            
            return response.getValue().stream()
                .map(service -> {
                    String address = service.getService().getAddress();
                    int port = service.getService().getPort();
                    return address + ":" + port;
                })
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            System.err.println("Failed to discover Ignite nodes: " + e.getMessage());
            return igniteProperties.getAddresses(); // Fallback to configured addresses
        }
    }
    
    // Create client with dynamic discovery
    @Bean
    @RefreshScope
    public IgniteClient dynamicIgniteClient() {
        List<String> discoveredNodes = discoverIgniteNodes();
        
        System.out.println("Discovered Ignite nodes: " + discoveredNodes);
        
        return IgniteClient.builder()
            .addresses(discoveredNodes.toArray(new String[0]))
            .connectTimeout(igniteProperties.getConnectTimeout())
            .operationTimeout(igniteProperties.getOperationTimeout())
            .build();
    }
}
```

#### Eureka Integration

```java
@Component
public class EurekaIgniteDiscovery {
    private final EurekaClient eurekaClient;
    
    public EurekaIgniteDiscovery(EurekaClient eurekaClient) {
        this.eurekaClient = eurekaClient;
    }
    
    public List<String> discoverIgniteNodes() {
        try {
            Application application = eurekaClient.getApplication("IGNITE");
            if (application != null) {
                return application.getInstances().stream()
                    .filter(instance -> instance.getStatus() == InstanceInfo.InstanceStatus.UP)
                    .map(instance -> instance.getHostName() + ":" + instance.getPort())
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            System.err.println("Failed to discover Ignite nodes via Eureka: " + e.getMessage());
        }
        
        return Collections.emptyList();
    }
}
```

### Configuration Management

#### Spring Cloud Config Integration

```java
@Component
@RefreshScope
public class DynamicIgniteConfiguration {
    
    @Value("${ignite.addresses:localhost:10800}")
    private String addresses;
    
    @Value("${ignite.connect-timeout:5000}")
    private int connectTimeout;
    
    @Value("${ignite.operation-timeout:30000}")
    private int operationTimeout;
    
    @Value("${ignite.auth.username:}")
    private String username;
    
    @Value("${ignite.auth.password:}")
    private String password;
    
    @EventListener(RefreshScopeRefreshedEvent.class)
    public void onConfigurationRefresh() {
        System.out.println("Ignite configuration refreshed");
        // Here you could recreate the Ignite client with new configuration
        // Note: This would require careful handling of existing connections
    }
    
    public IgniteClient createConfiguredClient() {
        IgniteClient.Builder builder = IgniteClient.builder()
            .addresses(addresses.split(","))
            .connectTimeout(connectTimeout)
            .operationTimeout(operationTimeout);
        
        if (StringUtils.hasText(username)) {
            builder.authenticator(BasicAuthenticator.builder()
                .username(username)
                .password(password)
                .build());
        }
        
        return builder.build();
    }
}
```

### Circuit Breaker Integration

#### Resilience4j Integration

```java
@Component
public class ResilientIgniteService {
    private final IgniteClient igniteClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final TimeLimiter timeLimiter;
    
    public ResilientIgniteService(IgniteClient igniteClient) {
        this.igniteClient = igniteClient;
        
        // Configure circuit breaker
        this.circuitBreaker = CircuitBreaker.ofDefaults("ignite");
        
        // Configure retry
        this.retry = Retry.ofDefaults("ignite");
        
        // Configure time limiter
        this.timeLimiter = TimeLimiter.ofDefaults("ignite");
    }
    
    public <T> T executeWithResilience(Supplier<T> operation) {
        Supplier<T> decoratedSupplier = Decorators.ofSupplier(operation)
            .withCircuitBreaker(circuitBreaker)
            .withRetry(retry)
            .withFallback(Arrays.asList(Exception.class), throwable -> {
                System.err.println("Fallback executed due to: " + throwable.getMessage());
                return null;
            })
            .decorate();
        
        return decoratedSupplier.get();
    }
    
    public <T> CompletableFuture<T> executeWithResilienceAsync(Supplier<CompletableFuture<T>> operation) {
        Supplier<CompletableFuture<T>> decoratedSupplier = Decorators.ofSupplier(operation)
            .withCircuitBreaker(circuitBreaker)
            .withRetry(retry)
            .decorate();
        
        return timeLimiter.executeCompletionStage(decoratedSupplier.get()).toCompletableFuture();
    }
    
    // Example usage with music domain
    public Artist getArtistWithResilience(Integer id) {
        return executeWithResilience(() -> {
            RecordView<Artist> view = igniteClient.tables().table("Artist").recordView(Artist.class);
            Artist key = new Artist();
            key.setArtistId(id);
            return view.get(null, key);
        });
    }
    
    public List<Track> getTracksByGenreWithResilience(String genre) {
        return executeWithResilience(() -> {
            try (ResultSet<Track> resultSet = igniteClient.sql().execute(null, Mapper.of(Track.class),
                    "SELECT t.* FROM Track t JOIN Genre g ON t.GenreId = g.GenreId WHERE g.Name = ?", genre)) {
                
                List<Track> tracks = new ArrayList<>();
                while (resultSet.hasNext()) {
                    tracks.add(resultSet.next());
                }
                return tracks;
            }
        });
    }
}
```

### Distributed Tracing

#### OpenTracing Integration

```java
@Component
public class TracingIgniteService {
    private final IgniteClient igniteClient;
    private final Tracer tracer;
    
    public TracingIgniteService(IgniteClient igniteClient, Tracer tracer) {
        this.igniteClient = igniteClient;
        this.tracer = tracer;
    }
    
    public Artist getArtistWithTracing(Integer id) {
        Span span = tracer.nextSpan()
            .name("ignite.get-artist")
            .tag("artist.id", String.valueOf(id))
            .start();
        
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            RecordView<Artist> view = igniteClient.tables().table("Artist").recordView(Artist.class);
            
            Artist key = new Artist();
            key.setArtistId(id);
            
            Artist result = view.get(null, key);
            
            if (result != null) {
                span.tag("artist.found", "true");
                span.tag("artist.name", result.getName());
            } else {
                span.tag("artist.found", "false");
            }
            
            return result;
            
        } catch (Exception e) {
            span.tag("error", e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
    
    public CompletableFuture<List<Track>> getTracksByGenreWithTracing(String genre) {
        Span span = tracer.nextSpan()
            .name("ignite.get-tracks-by-genre")
            .tag("track.genre", genre)
            .start();
        
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            return igniteClient.sql().executeAsync(null, Mapper.of(Track.class), 
                    "SELECT t.* FROM Track t JOIN Genre g ON t.GenreId = g.GenreId WHERE g.Name = ?", genre)
                .thenApply(resultSet -> {
                    List<Track> tracks = new ArrayList<>();
                    while (resultSet.hasNext()) {
                        tracks.add(resultSet.next());
                    }
                    
                    span.tag("tracks.count", String.valueOf(tracks.size()));
                    return tracks;
                })
                .exceptionally(throwable -> {
                    span.tag("error", throwable.getMessage());
                    return Collections.emptyList();
                })
                .whenComplete((result, throwable) -> span.end());
                
        } catch (Exception e) {
            span.tag("error", e.getMessage());
            span.end();
            throw e;
        }
    }
}
```

### Health Checks and Monitoring

#### Spring Boot Actuator Integration

```java
@Component
public class IgniteHealthIndicator implements HealthIndicator {
    private final IgniteClient igniteClient;
    
    public IgniteHealthIndicator(IgniteClient igniteClient) {
        this.igniteClient = igniteClient;
    }
    
    @Override
    public Health health() {
        try {
            // Perform health check queries for music domain
            long startTime = System.currentTimeMillis();
            
            // Basic connectivity test
            igniteClient.sql().execute(null, "SELECT 1 as health_check").next();
            
            // Test access to core music tables
            var artistCount = igniteClient.sql().execute(null, "SELECT COUNT(*) as count FROM Artist").next();
            var albumCount = igniteClient.sql().execute(null, "SELECT COUNT(*) as count FROM Album").next();
            var trackCount = igniteClient.sql().execute(null, "SELECT COUNT(*) as count FROM Track").next();
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            return Health.up()
                .withDetail("responseTime", responseTime + "ms")
                .withDetail("connections", igniteClient.connections().size())
                .withDetail("status", "Connected")
                .withDetail("musicCatalog", Map.of(
                    "artists", artistCount.longValue("count"),
                    "albums", albumCount.longValue("count"),
                    "tracks", trackCount.longValue("count")
                ))
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("status", "Disconnected")
                .build();
        }
    }
}

// Custom metrics
@Component
public class IgniteMetrics {
    private final IgniteClient igniteClient;
    private final MeterRegistry meterRegistry;
    
    private final Counter operationCounter;
    private final Timer operationTimer;
    private final Gauge connectionGauge;
    
    public IgniteMetrics(IgniteClient igniteClient, MeterRegistry meterRegistry) {
        this.igniteClient = igniteClient;
        this.meterRegistry = meterRegistry;
        
        this.operationCounter = Counter.builder("ignite.operations")
            .description("Total number of Ignite operations")
            .register(meterRegistry);
            
        this.operationTimer = Timer.builder("ignite.operation.duration")
            .description("Duration of Ignite operations")
            .register(meterRegistry);
            
        this.connectionGauge = Gauge.builder("ignite.connections")
            .description("Number of active Ignite connections")
            .register(meterRegistry, this, metrics -> metrics.igniteClient.connections().size());
    }
    
    public <T> T recordOperation(Supplier<T> operation) {
        operationCounter.increment();
        return operationTimer.recordCallable(operation::get);
    }
}
```

### Cache Integration Patterns

#### Spring Cache with Ignite 3

```java
@Configuration
@EnableCaching
public class IgniteSpringCacheConfiguration {
    
    @Bean
    public CacheManager cacheManager(IgniteClient igniteClient) {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        
        // Create caches for music domain use cases
        List<Cache> caches = Arrays.asList(
            new IgniteSpringCache("artists", igniteClient, Duration.ofHours(24)),
            new IgniteSpringCache("albums", igniteClient, Duration.ofHours(12)),
            new IgniteSpringCache("tracks", igniteClient, Duration.ofHours(6)),
            new IgniteSpringCache("playlists", igniteClient, Duration.ofHours(2))
        );
        
        cacheManager.setCaches(caches);
        return cacheManager;
    }
    
    // Custom Ignite-backed Spring Cache implementation
    public static class IgniteSpringCache implements Cache {
        private final String name;
        private final IgniteClient igniteClient;
        private final RecordView<CacheEntry> cacheView;
        private final Duration defaultTtl;
        
        public IgniteSpringCache(String name, IgniteClient igniteClient, Duration defaultTtl) {
            this.name = name;
            this.igniteClient = igniteClient;
            this.cacheView = igniteClient.tables().table("CacheEntry").recordView(CacheEntry.class);
            this.defaultTtl = defaultTtl;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public Object getNativeCache() {
            return igniteClient;
        }
        
        @Override
        public ValueWrapper get(Object key) {
            String cacheKey = buildCacheKey(key);
            CacheEntry keyEntry = new CacheEntry();
            keyEntry.setKey(cacheKey);
            CacheEntry cached = cacheView.get(null, keyEntry);
            
            if (cached != null && !cached.isExpired()) {
                // Update access statistics
                cached.recordAccess();
                cacheView.upsert(null, cached);
                
                Object value = deserialize(cached.getValue());
                return () -> value;
            }
            
            return null;
        }
        
        @Override
        public void put(Object key, Object value) {
            String cacheKey = buildCacheKey(key);
            byte[] serializedValue = serialize(value);
            LocalDateTime expiresAt = defaultTtl != null ? LocalDateTime.now().plus(defaultTtl) : null;
            
            CacheEntry entry = new CacheEntry(cacheKey, serializedValue, expiresAt);
            cacheView.upsert(null, entry);
        }
        
        @Override
        public void evict(Object key) {
            String cacheKey = buildCacheKey(key);
            CacheEntry keyEntry = new CacheEntry();
            keyEntry.setKey(cacheKey);
            cacheView.delete(null, keyEntry);
        }
        
        @Override
        public void clear() {
            String sql = "DELETE FROM CacheEntry WHERE cache_key LIKE ?";
            String pattern = name + ":%";
            igniteClient.sql().execute(null, sql, pattern);
        }
        
        private String buildCacheKey(Object key) {
            return name + ":" + key.toString();
        }
        
        private byte[] serialize(Object value) {
            // Implement serialization logic
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                 ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                oos.writeObject(value);
                return bos.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException("Serialization failed", e);
            }
        }
        
        private Object deserialize(byte[] data) {
            // Implement deserialization logic
            try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
                 ObjectInputStream ois = new ObjectInputStream(bis)) {
                return ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException("Deserialization failed", e);
            }
        }
        
        // Cache entry model (should be defined elsewhere)
        @Table(zone = @Zone(value = "cache_zone", storageProfiles = "default"))
        public static class CacheEntry {
            @Id
            @Column(value = "cache_key", nullable = false)
            private String key;
            
            @Column(value = "cache_value", nullable = true)
            private byte[] value;
            
            @Column(value = "expires_at", nullable = true)
            private LocalDateTime expiresAt;
            
            @Column(value = "access_count", nullable = false)
            private Long accessCount = 0L;
            
            @Column(value = "last_accessed", nullable = false)
            private LocalDateTime lastAccessed;
            
            // Constructors, getters, setters...
            public CacheEntry() {}
            
            public CacheEntry(String key, byte[] value, LocalDateTime expiresAt) {
                this.key = key;
                this.value = value;
                this.expiresAt = expiresAt;
                this.lastAccessed = LocalDateTime.now();
                this.accessCount = 0L;
            }
            
            public boolean isExpired() {
                return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
            }
            
            public void recordAccess() {
                this.lastAccessed = LocalDateTime.now();
                this.accessCount++;
            }
            
            // Getters and setters...
        }
    }
}

// Usage in music service classes
@Service
public class MusicCacheService {
    
    @Cacheable("artists")
    public Artist getArtist(Integer artistId) {
        // This method result will be cached in Ignite
        return loadArtistFromDatabase(artistId);
    }
    
    @CacheEvict(value = "artists", key = "#artist.artistId")
    public void updateArtist(Artist artist) {
        saveArtistToDatabase(artist);
    }
    
    @CachePut(value = "artists", key = "#result.artistId")
    public Artist createArtist(Artist artist) {
        return saveArtistToDatabase(artist);
    }
    
    @Cacheable("albums")
    public List<Album> getArtistAlbums(Integer artistId) {
        return loadArtistAlbumsFromDatabase(artistId);
    }
    
    @CacheEvict(value = "albums", key = "#album.artistId")
    public void updateAlbum(Album album) {
        saveAlbumToDatabase(album);
    }
    
    @Cacheable("tracks")
    public List<Track> getAlbumTracks(Integer albumId) {
        return loadAlbumTracksFromDatabase(albumId);
    }
    
    private Artist loadArtistFromDatabase(Integer artistId) {
        // Database loading logic for artist
        return new Artist();
    }
    
    private Artist saveArtistToDatabase(Artist artist) {
        // Database saving logic for artist
        return artist;
    }
    
    private List<Album> loadArtistAlbumsFromDatabase(Integer artistId) {
        // Database loading logic for albums
        return new ArrayList<>();
    }
    
    private void saveAlbumToDatabase(Album album) {
        // Database saving logic for album
    }
    
    private List<Track> loadAlbumTracksFromDatabase(Integer albumId) {
        // Database loading logic for tracks
        return new ArrayList<>();
    }
}

## Music Store Microservices Architecture

### Music Catalog Service

#### Artist Service Microservice

```java
@RestController
@RequestMapping("/api/v1/artists")
public class ArtistController {
    private final ArtistService artistService;
    private final IgniteMetrics igniteMetrics;
    
    public ArtistController(ArtistService artistService, IgniteMetrics igniteMetrics) {
        this.artistService = artistService;
        this.igniteMetrics = igniteMetrics;
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Artist> getArtist(@PathVariable Integer id) {
        return igniteMetrics.recordOperation(() -> {
            Artist artist = artistService.getArtist(id)
                .orElseThrow(() -> new EntityNotFoundException("Artist not found: " + id));
            return ResponseEntity.ok(artist);
        });
    }
    
    @GetMapping
    public ResponseEntity<List<Artist>> searchArtists(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        return igniteMetrics.recordOperation(() -> {
            List<Artist> artists;
            if (name != null && !name.isEmpty()) {
                artists = artistService.searchArtists(name);
            } else {
                artists = artistService.getAllArtists(page, size);
            }
            return ResponseEntity.ok(artists);
        });
    }
    
    @PostMapping
    public ResponseEntity<Artist> createArtist(@Valid @RequestBody CreateArtistRequest request) {
        return igniteMetrics.recordOperation(() -> {
            Artist artist = new Artist(null, request.getName());
            Artist created = artistService.createArtist(artist);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        });
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Artist> updateArtist(
            @PathVariable Integer id, 
            @Valid @RequestBody UpdateArtistRequest request) {
        
        return igniteMetrics.recordOperation(() -> {
            Artist updated = artistService.updateArtist(id, request.getName());
            return ResponseEntity.ok(updated);
        });
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArtist(@PathVariable Integer id) {
        return igniteMetrics.recordOperation(() -> {
            artistService.deleteArtist(id);
            return ResponseEntity.noContent().build();
        });
    }
    
    @GetMapping("/{id}/albums")
    public ResponseEntity<List<Album>> getArtistAlbums(@PathVariable Integer id) {
        return igniteMetrics.recordOperation(() -> {
            List<Album> albums = artistService.getArtistAlbums(id);
            return ResponseEntity.ok(albums);
        });
    }
    
    @GetMapping("/popular")
    public ResponseEntity<List<ArtistWithStats>> getPopularArtists(
            @RequestParam(defaultValue = "10") int limit) {
        
        return igniteMetrics.recordOperation(() -> {
            List<ArtistWithStats> popularArtists = artistService.getPopularArtists(limit);
            return ResponseEntity.ok(popularArtists);
        });
    }
}

// DTOs for API
public record CreateArtistRequest(@NotBlank String name) {}
public record UpdateArtistRequest(@NotBlank String name) {}

public record ArtistWithStats(
    Integer artistId,
    String name,
    long albumCount,
    long trackCount,
    BigDecimal totalSales
) {}
```

#### Music Recommendation Service

```java
@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {
    private final RecommendationService recommendationService;
    private final IgniteMetrics igniteMetrics;
    
    public RecommendationController(RecommendationService recommendationService, IgniteMetrics igniteMetrics) {
        this.recommendationService = recommendationService;
        this.igniteMetrics = igniteMetrics;
    }
    
    @GetMapping("/artists/{artistId}/similar")
    public ResponseEntity<List<Artist>> getSimilarArtists(
            @PathVariable Integer artistId,
            @RequestParam(defaultValue = "5") int limit) {
        
        return igniteMetrics.recordOperation(() -> {
            List<Artist> similarArtists = recommendationService.findSimilarArtists(artistId, limit);
            return ResponseEntity.ok(similarArtists);
        });
    }
    
    @GetMapping("/customers/{customerId}/tracks")
    public ResponseEntity<List<RecommendedTrack>> getRecommendedTracks(
            @PathVariable Integer customerId,
            @RequestParam(defaultValue = "10") int limit) {
        
        return igniteMetrics.recordOperation(() -> {
            List<RecommendedTrack> recommendations = recommendationService
                .getPersonalizedTrackRecommendations(customerId, limit);
            return ResponseEntity.ok(recommendations);
        });
    }
    
    @GetMapping("/genres/{genre}/trending")
    public ResponseEntity<List<TrendingTrack>> getTrendingTracks(
            @PathVariable String genre,
            @RequestParam(defaultValue = "20") int limit) {
        
        return igniteMetrics.recordOperation(() -> {
            List<TrendingTrack> trending = recommendationService.getTrendingTracksByGenre(genre, limit);
            return ResponseEntity.ok(trending);
        });
    }
    
    @PostMapping("/playlists/generate")
    public ResponseEntity<GeneratedPlaylist> generatePlaylist(
            @Valid @RequestBody PlaylistGenerationRequest request) {
        
        return igniteMetrics.recordOperation(() -> {
            GeneratedPlaylist playlist = recommendationService.generatePlaylist(request);
            return ResponseEntity.ok(playlist);
        });
    }
}

@Service
public class RecommendationService {
    private final IgniteClient igniteClient;
    private final ResilientIgniteService resilientService;
    
    public RecommendationService(IgniteClient igniteClient, ResilientIgniteService resilientService) {
        this.igniteClient = igniteClient;
        this.resilientService = resilientService;
    }
    
    public List<Artist> findSimilarArtists(Integer artistId, int limit) {
        return resilientService.executeWithResilience(() -> {
            // Complex query to find similar artists based on genre and customer purchase patterns
            try (ResultSet<Artist> resultSet = igniteClient.sql().execute(null, Mapper.of(Artist.class), """
                    SELECT DISTINCT a2.*
                    FROM Artist a1
                    JOIN Album al1 ON a1.ArtistId = al1.ArtistId
                    JOIN Track t1 ON al1.AlbumId = t1.AlbumId
                    JOIN Track t2 ON t1.GenreId = t2.GenreId
                    JOIN Album al2 ON t2.AlbumId = al2.AlbumId
                    JOIN Artist a2 ON al2.ArtistId = a2.ArtistId
                    WHERE a1.ArtistId = ? AND a2.ArtistId != ?
                    ORDER BY RANDOM() LIMIT ?
                    """, artistId, artistId, limit)) {
                
                List<Artist> similarArtists = new ArrayList<>();
                while (resultSet.hasNext()) {
                    similarArtists.add(resultSet.next());
                }
                return similarArtists;
            }
        });
    }
    
    public List<RecommendedTrack> getPersonalizedTrackRecommendations(Integer customerId, int limit) {
        return resilientService.executeWithResilience(() -> {
            // Recommendation based on customer purchase history and genre preferences
            try (ResultSet<RecommendedTrack> resultSet = igniteClient.sql().execute(
                    null, Mapper.of(RecommendedTrack.class), """
                    SELECT t.TrackId, t.Name as trackName, al.Title as albumTitle, 
                           ar.Name as artistName, t.UnitPrice,
                           COUNT(*) as popularity_score
                    FROM Track t
                    JOIN Album al ON t.AlbumId = al.AlbumId
                    JOIN Artist ar ON al.ArtistId = ar.ArtistId
                    JOIN Genre g ON t.GenreId = g.GenreId
                    WHERE g.GenreId IN (
                        SELECT DISTINCT g2.GenreId
                        FROM InvoiceLine il
                        JOIN Track t2 ON il.TrackId = t2.TrackId
                        JOIN Genre g2 ON t2.GenreId = g2.GenreId
                        JOIN Invoice i ON il.InvoiceId = i.InvoiceId
                        WHERE i.CustomerId = ?
                    )
                    AND t.TrackId NOT IN (
                        SELECT il2.TrackId
                        FROM InvoiceLine il2
                        JOIN Invoice i2 ON il2.InvoiceId = i2.InvoiceId
                        WHERE i2.CustomerId = ?
                    )
                    GROUP BY t.TrackId, t.Name, al.Title, ar.Name, t.UnitPrice
                    ORDER BY popularity_score DESC
                    LIMIT ?
                    """, customerId, customerId, limit)) {
                
                List<RecommendedTrack> recommendations = new ArrayList<>();
                while (resultSet.hasNext()) {
                    recommendations.add(resultSet.next());
                }
                return recommendations;
            }
        });
    }
    
    public GeneratedPlaylist generatePlaylist(PlaylistGenerationRequest request) {
        return resilientService.executeWithResilience(() -> {
            List<Track> tracks = new ArrayList<>();
            
            // Generate playlist based on criteria
            if (request.genres() != null && !request.genres().isEmpty()) {
                tracks.addAll(getTracksByGenres(request.genres(), request.maxTracks()));
            }
            
            if (request.artistIds() != null && !request.artistIds().isEmpty()) {
                tracks.addAll(getTracksByArtists(request.artistIds(), request.maxTracks() / 2));
            }
            
            // Remove duplicates and limit
            tracks = tracks.stream()
                .distinct()
                .limit(request.maxTracks())
                .collect(Collectors.toList());
            
            return new GeneratedPlaylist(
                "Generated Playlist",
                request.description(),
                tracks,
                LocalDateTime.now()
            );
        });
    }
    
    private List<Track> getTracksByGenres(List<String> genres, int limit) {
        String genreList = genres.stream()
            .map(g -> "'" + g + "'")
            .collect(Collectors.joining(","));
        
        try (ResultSet<Track> resultSet = igniteClient.sql().execute(
                null, Mapper.of(Track.class), 
                "SELECT t.* FROM Track t JOIN Genre g ON t.GenreId = g.GenreId " +
                "WHERE g.Name IN (" + genreList + ") ORDER BY RANDOM() LIMIT ?", limit)) {
            
            List<Track> tracks = new ArrayList<>();
            while (resultSet.hasNext()) {
                tracks.add(resultSet.next());
            }
            return tracks;
        }
    }
    
    private List<Track> getTracksByArtists(List<Integer> artistIds, int limit) {
        String artistList = artistIds.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(","));
        
        try (ResultSet<Track> resultSet = igniteClient.sql().execute(
                null, Mapper.of(Track.class),
                "SELECT t.* FROM Track t JOIN Album al ON t.AlbumId = al.AlbumId " +
                "WHERE al.ArtistId IN (" + artistList + ") ORDER BY RANDOM() LIMIT ?", limit)) {
            
            List<Track> tracks = new ArrayList<>();
            while (resultSet.hasNext()) {
                tracks.add(resultSet.next());
            }
            return tracks;
        }
    }
}

// DTOs for recommendations
public record RecommendedTrack(
    Integer trackId,
    String trackName,
    String albumTitle,
    String artistName,
    BigDecimal unitPrice,
    int popularityScore
) {}

public record TrendingTrack(
    Integer trackId,
    String trackName,
    String albumTitle,
    String artistName,
    long purchaseCount,
    BigDecimal avgPrice
) {}

public record PlaylistGenerationRequest(
    List<String> genres,
    List<Integer> artistIds,
    String description,
    @Min(1) @Max(100) int maxTracks
) {}

public record GeneratedPlaylist(
    String name,
    String description,
    List<Track> tracks,
    LocalDateTime createdAt
) {}
```

### Event-Driven Architecture

```java
// Music domain events
public sealed interface MusicEvent permits ArtistCreated, AlbumReleased, TrackPurchased {
    String eventId();
    LocalDateTime timestamp();
}

public record ArtistCreated(
    String eventId,
    LocalDateTime timestamp,
    Integer artistId,
    String artistName
) implements MusicEvent {}

public record AlbumReleased(
    String eventId,
    LocalDateTime timestamp,
    Integer albumId,
    String albumTitle,
    Integer artistId,
    String artistName
) implements MusicEvent {}

public record TrackPurchased(
    String eventId,
    LocalDateTime timestamp,
    Integer trackId,
    String trackName,
    Integer customerId,
    BigDecimal price
) implements MusicEvent {}

// Event publisher using Ignite as event store
@Component
public class MusicEventPublisher {
    private final IgniteClient igniteClient;
    private final ApplicationEventPublisher eventPublisher;
    
    public MusicEventPublisher(IgniteClient igniteClient, ApplicationEventPublisher eventPublisher) {
        this.igniteClient = igniteClient;
        this.eventPublisher = eventPublisher;
    }
    
    public void publishEvent(MusicEvent event) {
        // Store event in Ignite for durability
        storeEvent(event);
        
        // Publish locally for immediate processing
        eventPublisher.publishEvent(event);
    }
    
    private void storeEvent(MusicEvent event) {
        RecordView<EventRecord> eventView = igniteClient.tables().table("MusicEvent").recordView(EventRecord.class);
        
        EventRecord record = new EventRecord(
            event.eventId(),
            event.getClass().getSimpleName(),
            serializeEvent(event),
            event.timestamp()
        );
        
        eventView.upsert(null, record);
    }
    
    private String serializeEvent(MusicEvent event) {
        // Implement JSON serialization
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}

// Event handlers for cross-service coordination
@Component
public class MusicEventHandler {
    private final RecommendationService recommendationService;
    private final AnalyticsService analyticsService;
    
    public MusicEventHandler(RecommendationService recommendationService, AnalyticsService analyticsService) {
        this.recommendationService = recommendationService;
        this.analyticsService = analyticsService;
    }
    
    @EventListener
    public void handleArtistCreated(ArtistCreated event) {
        // Update recommendation models
        recommendationService.refreshArtistModel(event.artistId());
        
        // Update analytics
        analyticsService.recordArtistCreation(event);
    }
    
    @EventListener
    public void handleAlbumReleased(AlbumReleased event) {
        // Update album recommendations
        recommendationService.refreshAlbumRecommendations(event.albumId());
        
        // Generate trending analysis
        analyticsService.analyzeNewRelease(event);
    }
    
    @EventListener
    public void handleTrackPurchased(TrackPurchased event) {
        // Update customer preferences
        recommendationService.updateCustomerPreferences(event.customerId(), event.trackId());
        
        // Update sales analytics
        analyticsService.recordSale(event);
    }
}

@Table(zone = @Zone(value = "events", storageProfiles = "default"))
public class EventRecord {
    @Id
    @Column(value = "event_id", nullable = false)
    private String eventId;
    
    @Column(value = "event_type", nullable = false)
    private String eventType;
    
    @Column(value = "event_data", nullable = false)
    private String eventData;
    
    @Column(value = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    // Constructors, getters, setters...
    public EventRecord() {}
    
    public EventRecord(String eventId, String eventType, String eventData, LocalDateTime timestamp) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.eventData = eventData;
        this.timestamp = timestamp;
    }
    
    // Getters and setters...
}
```
```
