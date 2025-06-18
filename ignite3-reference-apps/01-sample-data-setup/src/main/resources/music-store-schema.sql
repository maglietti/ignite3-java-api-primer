-- Apache Ignite 3 Music Store Sample Database Schema
-- Distribution Zones and Table Definitions

-- Create Distribution Zones
CREATE ZONE MusicStore WITH REPLICAS=2, PARTITIONS=25, STORAGE_PROFILES='default';
CREATE ZONE MusicStoreReplicated WITH REPLICAS=3, PARTITIONS=25, STORAGE_PROFILES='default';

-- Reference Data Tables (MusicStoreReplicated Zone)

CREATE TABLE Genre (
    GenreId INTEGER PRIMARY KEY,
    Name VARCHAR(120)
) WITH PRIMARY_ZONE='MusicStoreReplicated';

CREATE TABLE MediaType (
    MediaTypeId INTEGER PRIMARY KEY,
    Name VARCHAR(120)
) WITH PRIMARY_ZONE='MusicStoreReplicated';

-- Music Entity Tables (MusicStore Zone)

CREATE TABLE Artist (
    ArtistId INTEGER PRIMARY KEY,
    Name VARCHAR(120) NOT NULL
) WITH PRIMARY_ZONE='MusicStore';

CREATE TABLE Album (
    AlbumId INTEGER,
    ArtistId INTEGER,
    Title VARCHAR(160) NOT NULL,
    PRIMARY KEY (AlbumId, ArtistId)
) WITH PRIMARY_ZONE='MusicStore', COLOCATION_COLUMNS='ArtistId';

CREATE INDEX IFK_AlbumArtistId ON Album (ArtistId);

CREATE TABLE Track (
    TrackId INTEGER,
    AlbumId INTEGER,
    Name VARCHAR(200) NOT NULL,
    MediaTypeId INTEGER NOT NULL,
    GenreId INTEGER,
    Composer VARCHAR(220),
    Milliseconds INTEGER NOT NULL,
    Bytes INTEGER,
    UnitPrice DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (TrackId, AlbumId)
) WITH PRIMARY_ZONE='MusicStore', COLOCATION_COLUMNS='AlbumId';

CREATE INDEX IFK_TrackAlbumId ON Track (AlbumId);
CREATE INDEX IFK_TrackGenreId ON Track (GenreId);
CREATE INDEX IFK_TrackMediaTypeId ON Track (MediaTypeId);

-- Business Entity Tables (MusicStore Zone)

CREATE TABLE Customer (
    CustomerId INTEGER PRIMARY KEY,
    FirstName VARCHAR(40) NOT NULL,
    LastName VARCHAR(20) NOT NULL,
    Company VARCHAR(80),
    Address VARCHAR(70),
    City VARCHAR(40),
    State VARCHAR(40),
    Country VARCHAR(40),
    PostalCode VARCHAR(10),
    Phone VARCHAR(24),
    Fax VARCHAR(24),
    Email VARCHAR(60) NOT NULL,
    SupportRepId INTEGER
) WITH PRIMARY_ZONE='MusicStore';

CREATE INDEX IFK_CustomerSupportRepId ON Customer (SupportRepId);

CREATE TABLE Employee (
    EmployeeId INTEGER PRIMARY KEY,
    LastName VARCHAR(20) NOT NULL,
    FirstName VARCHAR(20) NOT NULL,
    Title VARCHAR(30),
    ReportsTo INTEGER,
    BirthDate DATE,
    HireDate DATE,
    Address VARCHAR(70),
    City VARCHAR(40),
    State VARCHAR(40),
    Country VARCHAR(40),
    PostalCode VARCHAR(10),
    Phone VARCHAR(24),
    Fax VARCHAR(24),
    Email VARCHAR(60)
) WITH PRIMARY_ZONE='MusicStore';

CREATE INDEX IFK_EmployeeReportsTo ON Employee (ReportsTo);

CREATE TABLE Invoice (
    InvoiceId INTEGER,
    CustomerId INTEGER,
    InvoiceDate DATE NOT NULL,
    BillingAddress VARCHAR(70),
    BillingCity VARCHAR(40),
    BillingState VARCHAR(40),
    BillingCountry VARCHAR(40),
    BillingPostalCode VARCHAR(10),
    Total DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (InvoiceId, CustomerId)
) WITH PRIMARY_ZONE='MusicStore', COLOCATION_COLUMNS='CustomerId';

CREATE INDEX IFK_InvoiceCustomerId ON Invoice (CustomerId);

CREATE TABLE InvoiceLine (
    InvoiceLineId INTEGER,
    InvoiceId INTEGER,
    TrackId INTEGER NOT NULL,
    UnitPrice DECIMAL(10,2) NOT NULL,
    Quantity INTEGER NOT NULL,
    PRIMARY KEY (InvoiceLineId, InvoiceId)
) WITH PRIMARY_ZONE='MusicStore', COLOCATION_COLUMNS='InvoiceId';

CREATE INDEX IFK_InvoiceLineInvoiceId ON InvoiceLine (InvoiceId);
CREATE INDEX IFK_InvoiceLineTrackId ON InvoiceLine (TrackId);

-- Playlist Tables (MusicStore Zone)

CREATE TABLE Playlist (
    PlaylistId INTEGER PRIMARY KEY,
    Name VARCHAR(120)
) WITH PRIMARY_ZONE='MusicStore';

CREATE TABLE PlaylistTrack (
    PlaylistId INTEGER,
    TrackId INTEGER,
    PRIMARY KEY (PlaylistId, TrackId)
) WITH PRIMARY_ZONE='MusicStore', COLOCATION_COLUMNS='PlaylistId';

CREATE INDEX IFK_PlaylistTrackPlaylistId ON PlaylistTrack (PlaylistId);
CREATE INDEX IFK_PlaylistTrackTrackId ON PlaylistTrack (TrackId);