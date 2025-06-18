-- Apache Ignite 3 Music Store Sample Data
-- Basic sample data for all entities

-- Reference Data
INSERT INTO Genre (GenreId, Name) VALUES (1, 'Rock');
INSERT INTO Genre (GenreId, Name) VALUES (2, 'Jazz');
INSERT INTO Genre (GenreId, Name) VALUES (3, 'Metal');
INSERT INTO Genre (GenreId, Name) VALUES (4, 'Alternative & Punk');
INSERT INTO Genre (GenreId, Name) VALUES (5, 'Rock And Roll');

INSERT INTO MediaType (MediaTypeId, Name) VALUES (1, 'MPEG audio file');
INSERT INTO MediaType (MediaTypeId, Name) VALUES (2, 'Protected AAC audio file');
INSERT INTO MediaType (MediaTypeId, Name) VALUES (3, 'Protected MPEG-4 video file');
INSERT INTO MediaType (MediaTypeId, Name) VALUES (4, 'Purchased AAC audio file');
INSERT INTO MediaType (MediaTypeId, Name) VALUES (5, 'AAC audio file');

-- Music Entities
INSERT INTO Artist (ArtistId, Name) VALUES (1, 'AC/DC');
INSERT INTO Artist (ArtistId, Name) VALUES (2, 'Accept');
INSERT INTO Artist (ArtistId, Name) VALUES (3, 'Aerosmith');
INSERT INTO Artist (ArtistId, Name) VALUES (4, 'Alanis Morissette');
INSERT INTO Artist (ArtistId, Name) VALUES (5, 'Alice In Chains');

INSERT INTO Album (AlbumId, ArtistId, Title) VALUES (1, 1, 'For Those About To Rock We Salute You');
INSERT INTO Album (AlbumId, ArtistId, Title) VALUES (2, 2, 'Balls to the Wall');
INSERT INTO Album (AlbumId, ArtistId, Title) VALUES (3, 2, 'Restless and Wild');
INSERT INTO Album (AlbumId, ArtistId, Title) VALUES (4, 1, 'Let There Be Rock');
INSERT INTO Album (AlbumId, ArtistId, Title) VALUES (5, 3, 'Big Ones');

INSERT INTO Track (TrackId, AlbumId, Name, MediaTypeId, GenreId, Composer, Milliseconds, Bytes, UnitPrice) 
VALUES (1, 1, 'For Those About To Rock (We Salute You)', 1, 1, 'Angus Young, Malcolm Young, Brian Johnson', 343719, 11170334, 0.99);

INSERT INTO Track (TrackId, AlbumId, Name, MediaTypeId, GenreId, Composer, Milliseconds, Bytes, UnitPrice) 
VALUES (2, 1, 'Put The Finger On You', 1, 1, 'Angus Young, Malcolm Young, Brian Johnson', 205662, 6713451, 0.99);

INSERT INTO Track (TrackId, AlbumId, Name, MediaTypeId, GenreId, Composer, Milliseconds, Bytes, UnitPrice) 
VALUES (3, 1, 'Let''s Get It Up', 1, 1, 'Angus Young, Malcolm Young, Brian Johnson', 233926, 7636561, 0.99);

INSERT INTO Track (TrackId, AlbumId, Name, MediaTypeId, GenreId, Composer, Milliseconds, Bytes, UnitPrice) 
VALUES (4, 1, 'Inject The Venom', 1, 1, 'Angus Young, Malcolm Young, Brian Johnson', 210834, 6852860, 0.99);

INSERT INTO Track (TrackId, AlbumId, Name, MediaTypeId, GenreId, Composer, Milliseconds, Bytes, UnitPrice) 
VALUES (5, 1, 'Snowballed', 1, 1, 'Angus Young, Malcolm Young, Brian Johnson', 203102, 6599424, 0.99);

-- Business Entities
INSERT INTO Employee (EmployeeId, LastName, FirstName, Title, ReportsTo, BirthDate, HireDate, Address, City, State, Country, PostalCode, Phone, Fax, Email) 
VALUES (1, 'Adams', 'Andrew', 'General Manager', NULL, '1962-02-18', '2002-08-14', '11120 Jasper Ave NW', 'Edmonton', 'AB', 'Canada', 'T5K 2N1', '+1 (780) 428-9482', '+1 (780) 428-3457', 'andrew@musicstore.com');

INSERT INTO Employee (EmployeeId, LastName, FirstName, Title, ReportsTo, BirthDate, HireDate, Address, City, State, Country, PostalCode, Phone, Fax, Email) 
VALUES (2, 'Edwards', 'Nancy', 'Sales Manager', 1, '1958-12-08', '2002-05-01', '825 8 Ave SW', 'Calgary', 'AB', 'Canada', 'T2P 2T3', '+1 (403) 262-3443', '+1 (403) 262-3322', 'nancy@musicstore.com');

INSERT INTO Customer (CustomerId, FirstName, LastName, Company, Address, City, State, Country, PostalCode, Phone, Fax, Email, SupportRepId) 
VALUES (1, 'Luís', 'Gonçalves', 'Embraer - Empresa Brasileira de Aeronáutica S.A.', 'Av. Brigadeiro Faria Lima, 2170', 'São José dos Campos', 'SP', 'Brazil', '12227-000', '+55 (12) 3923-5555', '+55 (12) 3923-5566', 'luisg@embraer.com.br', 3);

INSERT INTO Customer (CustomerId, FirstName, LastName, Company, Address, City, State, Country, PostalCode, Phone, Fax, Email, SupportRepId) 
VALUES (2, 'Leonie', 'Köhler', NULL, 'Theodor-Heuss-Straße 34', 'Stuttgart', NULL, 'Germany', '70174', '+49 0711 2842222', NULL, 'leonekohler@surfeu.de', 5);

INSERT INTO Invoice (InvoiceId, CustomerId, InvoiceDate, BillingAddress, BillingCity, BillingState, BillingCountry, BillingPostalCode, Total) 
VALUES (1, 1, '2009-01-01', 'Av. Brigadeiro Faria Lima, 2170', 'São José dos Campos', 'SP', 'Brazil', '12227-000', 1.98);

INSERT INTO Invoice (InvoiceId, CustomerId, InvoiceDate, BillingAddress, BillingCity, BillingState, BillingCountry, BillingPostalCode, Total) 
VALUES (2, 2, '2009-01-02', 'Theodor-Heuss-Straße 34', 'Stuttgart', NULL, 'Germany', '70174', 3.96);

INSERT INTO InvoiceLine (InvoiceLineId, InvoiceId, TrackId, UnitPrice, Quantity) VALUES (1, 1, 1, 0.99, 1);
INSERT INTO InvoiceLine (InvoiceLineId, InvoiceId, TrackId, UnitPrice, Quantity) VALUES (2, 1, 2, 0.99, 1);
INSERT INTO InvoiceLine (InvoiceLineId, InvoiceId, TrackId, UnitPrice, Quantity) VALUES (3, 2, 3, 0.99, 1);
INSERT INTO InvoiceLine (InvoiceLineId, InvoiceId, TrackId, UnitPrice, Quantity) VALUES (4, 2, 4, 0.99, 1);

-- Playlist Data
INSERT INTO Playlist (PlaylistId, Name) VALUES (1, 'Music');
INSERT INTO Playlist (PlaylistId, Name) VALUES (2, 'Movies');
INSERT INTO Playlist (PlaylistId, Name) VALUES (3, 'TV Shows');
INSERT INTO Playlist (PlaylistId, Name) VALUES (4, 'Audiobooks');
INSERT INTO Playlist (PlaylistId, Name) VALUES (5, '90''s Music');

INSERT INTO PlaylistTrack (PlaylistId, TrackId) VALUES (1, 1);
INSERT INTO PlaylistTrack (PlaylistId, TrackId) VALUES (1, 2);
INSERT INTO PlaylistTrack (PlaylistId, TrackId) VALUES (1, 3);
INSERT INTO PlaylistTrack (PlaylistId, TrackId) VALUES (5, 1);
INSERT INTO PlaylistTrack (PlaylistId, TrackId) VALUES (5, 2);