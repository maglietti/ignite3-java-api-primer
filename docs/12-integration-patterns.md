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

// Service using MyBatis
@Service
public class CustomerMyBatisService {
    private final CustomerMapper customerMapper;
    
    public CustomerMyBatisService(CustomerMapper customerMapper) {
        this.customerMapper = customerMapper;
    }
    
    public Customer getCustomer(Long id) {
        return customerMapper.findById(id);
    }
    
    public List<Customer> getCustomersByCountry(String country) {
        return customerMapper.findByCountry(country);
    }
    
    public Customer createCustomer(Customer customer) {
        customer.setCreatedAt(LocalDateTime.now());
        customerMapper.insert(customer);
        return customer;
    }
    
    public List<CustomerWithOrderCount> getCustomersWithOrderStats(String country) {
        return customerMapper.findCustomersWithOrderCount(country);
    }
}
```

#### JPA/Hibernate Integration

```java
// JPA Entity (for JDBC-based access)
@Entity
@javax.persistence.Table(name = "Customer")
public class CustomerJPA {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "firstName")
    private String firstName;
    
    @Column(name = "lastName")
    private String lastName;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "phoneNumber")
    private String phoneNumber;
    
    @Column(name = "createdAt")
    private LocalDateTime createdAt;
    
    // Constructors, getters, setters...
}

// JPA Repository
@Repository
public interface CustomerJPARepository extends JpaRepository<CustomerJPA, Long> {
    List<CustomerJPA> findByCountry(String country);
    List<CustomerJPA> findByEmailContaining(String emailPart);
    Optional<CustomerJPA> findByEmail(String email);
    
    @Query("SELECT c FROM CustomerJPA c WHERE c.createdAt >= :since")
    List<CustomerJPA> findRecentCustomers(@Param("since") LocalDateTime since);
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
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect"); // Close enough for Ignite
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
    
    // Example usage
    public Customer getCustomerWithResilience(Long id) {
        return executeWithResilience(() -> {
            RecordView<Customer> view = igniteClient.tables().table("Customer").recordView(Customer.class);
            Customer key = new Customer();
            key.setId(id);
            return view.get(null, key);
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
    
    public Customer getCustomerWithTracing(Long id) {
        Span span = tracer.nextSpan()
            .name("ignite.get-customer")
            .tag("customer.id", String.valueOf(id))
            .start();
        
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            RecordView<Customer> view = igniteClient.tables().table("Customer").recordView(Customer.class);
            
            Customer key = new Customer();
            key.setId(id);
            
            Customer result = view.get(null, key);
            
            if (result != null) {
                span.tag("customer.found", "true");
                span.tag("customer.email", result.getEmail());
            } else {
                span.tag("customer.found", "false");
            }
            
            return result;
            
        } catch (Exception e) {
            span.tag("error", e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
    
    public CompletableFuture<List<Customer>> getCustomersByCountryWithTracing(String country) {
        Span span = tracer.nextSpan()
            .name("ignite.get-customers-by-country")
            .tag("customer.country", country)
            .start();
        
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            return igniteClient.sql().executeAsync(null, Mapper.of(Customer.class), 
                    "SELECT * FROM Customer WHERE country = ?", country)
                .thenApply(resultSet -> {
                    List<Customer> customers = new ArrayList<>();
                    while (resultSet.hasNext()) {
                        customers.add(resultSet.next());
                    }
                    
                    span.tag("customers.count", String.valueOf(customers.size()));
                    return customers;
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
            // Perform a simple health check query
            long startTime = System.currentTimeMillis();
            igniteClient.sql().execute(null, "SELECT 1 as health_check").next();
            long responseTime = System.currentTimeMillis() - startTime;
            
            return Health.up()
                .withDetail("responseTime", responseTime + "ms")
                .withDetail("connections", igniteClient.connections().size())
                .withDetail("status", "Connected")
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
        
        // Create caches for different use cases
        List<Cache> caches = Arrays.asList(
            new IgniteSpringCache("users", igniteClient, Duration.ofMinutes(30)),
            new IgniteSpringCache("products", igniteClient, Duration.ofHours(2)),
            new IgniteSpringCache("sessions", igniteClient, Duration.ofMinutes(20))
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

// Usage in service classes
@Service
public class ProductService {
    
    @Cacheable("products")
    public Product getProduct(Long productId) {
        // This method result will be cached in Ignite
        return loadProductFromDatabase(productId);
    }
    
    @CacheEvict(value = "products", key = "#product.id")
    public void updateProduct(Product product) {
        saveProductToDatabase(product);
    }
    
    @CachePut(value = "products", key = "#result.id")
    public Product createProduct(Product product) {
        return saveProductToDatabase(product);
    }
    
    private Product loadProductFromDatabase(Long productId) {
        // Database loading logic
        return new Product();
    }
    
    private Product saveProductToDatabase(Product product) {
        // Database saving logic
        return product;
    }
}
```
