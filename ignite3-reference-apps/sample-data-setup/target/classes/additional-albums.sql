-- Apache Ignite 3 Music Store Extended Sample Data
-- Additional artists, albums, and tracks for richer examples

-- Additional Artists
INSERT INTO Artist (ArtistId, Name) VALUES (6, 'Black Sabbath');
INSERT INTO Artist (ArtistId, Name) VALUES (7, 'Chico Buarque');
INSERT INTO Artist (ArtistId, Name) VALUES (8, 'Deep Purple');
INSERT INTO Artist (ArtistId, Name) VALUES (9, 'Metallica');
INSERT INTO Artist (ArtistId, Name) VALUES (10, 'Led Zeppelin');
INSERT INTO Artist (ArtistId, Name) VALUES (11, 'The Beatles');
INSERT INTO Artist (ArtistId, Name) VALUES (12, 'Pink Floyd');
INSERT INTO Artist (ArtistId, Name) VALUES (13, 'Queen');
INSERT INTO Artist (ArtistId, Name) VALUES (14, 'The Rolling Stones');
INSERT INTO Artist (ArtistId, Name) VALUES (15, 'Iron Maiden');

-- Additional Albums
INSERT INTO Album (AlbumId, ArtistId, Title) VALUES (6, 6, 'Paranoid');
INSERT INTO Album (AlbumId, ArtistId, Title) VALUES (7, 7, 'Minha Historia');
INSERT INTO Album (AlbumId, ArtistId, Title) VALUES (8, 8, 'Machine Head');
INSERT INTO Album (AlbumId, ArtistId, Title) VALUES (9, 9, 'Master of Puppets');
INSERT INTO Album (AlbumId, ArtistId, Title) VALUES (10, 10, 'Led Zeppelin IV');
INSERT INTO Album (AlbumId, ArtistId, Title) VALUES (11, 11, 'Abbey Road');
INSERT INTO Album (AlbumId, ArtistId, Title) VALUES (12, 12, 'The Dark Side of the Moon');
INSERT INTO Album (AlbumId, ArtistId, Title) VALUES (13, 13, 'A Night at the Opera');
INSERT INTO Album (AlbumId, ArtistId, Title) VALUES (14, 14, 'Sticky Fingers');
INSERT INTO Album (AlbumId, ArtistId, Title) VALUES (15, 15, 'The Number of the Beast');

-- Additional Tracks
INSERT INTO Track (TrackId, AlbumId, Name, MediaTypeId, GenreId, Composer, Milliseconds, Bytes, UnitPrice) 
VALUES (6, 6, 'Paranoid', 1, 3, 'Tony Iommi, Ozzy Osbourne, Geezer Butler, Bill Ward', 170333, 5566397, 0.99);

INSERT INTO Track (TrackId, AlbumId, Name, MediaTypeId, GenreId, Composer, Milliseconds, Bytes, UnitPrice) 
VALUES (7, 6, 'Iron Man', 1, 3, 'Tony Iommi, Ozzy Osbourne, Geezer Butler, Bill Ward', 356666, 11673881, 0.99);

INSERT INTO Track (TrackId, AlbumId, Name, MediaTypeId, GenreId, Composer, Milliseconds, Bytes, UnitPrice) 
VALUES (8, 8, 'Highway Star', 1, 1, 'Ritchie Blackmore, Ian Gillan, Roger Glover, Jon Lord, Ian Paice', 386679, 12641711, 0.99);

INSERT INTO Track (TrackId, AlbumId, Name, MediaTypeId, GenreId, Composer, Milliseconds, Bytes, UnitPrice) 
VALUES (9, 9, 'Master of Puppets', 1, 3, 'James Hetfield, Lars Ulrich, Kirk Hammett, Cliff Burton', 515333, 16863326, 0.99);

INSERT INTO Track (TrackId, AlbumId, Name, MediaTypeId, GenreId, Composer, Milliseconds, Bytes, UnitPrice) 
VALUES (10, 10, 'Stairway to Heaven', 1, 1, 'Jimmy Page, Robert Plant', 482333, 15783426, 0.99);

INSERT INTO Track (TrackId, AlbumId, Name, MediaTypeId, GenreId, Composer, Milliseconds, Bytes, UnitPrice) 
VALUES (11, 11, 'Come Together', 1, 1, 'John Lennon, Paul McCartney', 259333, 8490426, 0.99);

INSERT INTO Track (TrackId, AlbumId, Name, MediaTypeId, GenreId, Composer, Milliseconds, Bytes, UnitPrice) 
VALUES (12, 12, 'Time', 1, 1, 'Roger Waters, David Gilmour, Richard Wright, Nick Mason', 412333, 13493426, 0.99);

INSERT INTO Track (TrackId, AlbumId, Name, MediaTypeId, GenreId, Composer, Milliseconds, Bytes, UnitPrice) 
VALUES (13, 13, 'Bohemian Rhapsody', 1, 1, 'Freddie Mercury', 355333, 11633426, 0.99);

INSERT INTO Track (TrackId, AlbumId, Name, MediaTypeId, GenreId, Composer, Milliseconds, Bytes, UnitPrice) 
VALUES (14, 14, 'Wild Horses', 1, 1, 'Mick Jagger, Keith Richards', 341333, 11173426, 0.99);

INSERT INTO Track (TrackId, AlbumId, Name, MediaTypeId, GenreId, Composer, Milliseconds, Bytes, UnitPrice) 
VALUES (15, 15, 'The Number of the Beast', 1, 3, 'Steve Harris', 292333, 9573426, 0.99);

-- Additional Genres for diversity
INSERT INTO Genre (GenreId, Name) VALUES (6, 'Classical');
INSERT INTO Genre (GenreId, Name) VALUES (7, 'Country');
INSERT INTO Genre (GenreId, Name) VALUES (8, 'Electronic');
INSERT INTO Genre (GenreId, Name) VALUES (9, 'Blues');
INSERT INTO Genre (GenreId, Name) VALUES (10, 'Reggae');

-- Additional MediaTypes
INSERT INTO MediaType (MediaTypeId, Name) VALUES (6, 'FLAC audio file');
INSERT INTO MediaType (MediaTypeId, Name) VALUES (7, 'WAV audio file');

-- Additional Business Data
INSERT INTO Employee (EmployeeId, LastName, FirstName, Title, ReportsTo, BirthDate, HireDate, Address, City, State, Country, PostalCode, Phone, Fax, Email) 
VALUES (3, 'Peacock', 'Jane', 'Sales Support Agent', 2, '1973-08-29', '2002-04-01', '1111 6 Ave SW', 'Calgary', 'AB', 'Canada', 'T2P 5M5', '+1 (403) 262-3443', '+1 (403) 262-6712', 'jane@musicstore.com');

INSERT INTO Customer (CustomerId, FirstName, LastName, Company, Address, City, State, Country, PostalCode, Phone, Fax, Email, SupportRepId) 
VALUES (3, 'François', 'Tremblay', NULL, '1498 rue Bélanger', 'Montréal', 'QC', 'Canada', 'H2G 1A7', '+1 (514) 721-4711', NULL, 'ftremblay@gmail.com', 3);

INSERT INTO Customer (CustomerId, FirstName, LastName, Company, Address, City, State, Country, PostalCode, Phone, Fax, Email, SupportRepId) 
VALUES (4, 'Bjørn', 'Hansen', NULL, 'Ullevålsveien 14', 'Oslo', NULL, 'Norway', '0171', '+47 22 44 22 22', NULL, 'bjorn.hansen@yahoo.no', 4);

-- Additional Playlists
INSERT INTO Playlist (PlaylistId, Name) VALUES (6, 'Heavy Metal Classics');
INSERT INTO Playlist (PlaylistId, Name) VALUES (7, 'Classic Rock Essentials');
INSERT INTO Playlist (PlaylistId, Name) VALUES (8, 'Progressive Rock');

-- Additional Playlist Tracks
INSERT INTO PlaylistTrack (PlaylistId, TrackId) VALUES (6, 6);
INSERT INTO PlaylistTrack (PlaylistId, TrackId) VALUES (6, 7);
INSERT INTO PlaylistTrack (PlaylistId, TrackId) VALUES (6, 9);
INSERT INTO PlaylistTrack (PlaylistId, TrackId) VALUES (6, 15);

INSERT INTO PlaylistTrack (PlaylistId, TrackId) VALUES (7, 1);
INSERT INTO PlaylistTrack (PlaylistId, TrackId) VALUES (7, 8);
INSERT INTO PlaylistTrack (PlaylistId, TrackId) VALUES (7, 10);
INSERT INTO PlaylistTrack (PlaylistId, TrackId) VALUES (7, 11);
INSERT INTO PlaylistTrack (PlaylistId, TrackId) VALUES (7, 13);
INSERT INTO PlaylistTrack (PlaylistId, TrackId) VALUES (7, 14);

INSERT INTO PlaylistTrack (PlaylistId, TrackId) VALUES (8, 12);
INSERT INTO PlaylistTrack (PlaylistId, TrackId) VALUES (8, 13);