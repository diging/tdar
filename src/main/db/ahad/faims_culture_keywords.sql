-- The grand plan is: 
--    1) backup database:   
--          pg_dump -U postgres -o tdardata > tdardata.sql
--          pg_dump -U postgres -o tdarmetadata > tdarmetadata.sql
--          pg_dumpall > outfile    ... dumps everthing in one hit
--    2) drop database:   what about simply connecting to new one?
--    3) import ahad db (which should have the AHAD keywords base)
--          createdb -U postgres -O tdar -T template0 faimsdata
--          psql -U postgres faimsdata < tdardata.sql
--          createdb -U postgres -O tdar -T template0 faimsmetadata
--          psql -U postgres faimsmetadata < tdarmetadata.sql
--          psql -f infile postgres   ... restores from a dump all...
--    4) run the update.sql's that need to be run
--    5) add the new terms to the culture keyword table 
--    6) migrate existing culture keyword terms to the new one: "Historical Archaeology (incl. Industrial Archaeology) (FOR 210108)"
--    7) drop the current culture keyword terms
--    Steps 5, 6 & 7: (this file)
--          psql -U tdar -f faims_culture_keywords.sql faimsmetadata >> log.txt

-- Step 5) add the new new terms to the culture keyword table
--
-- Data for Name: culture_keyword; Type: TABLE DATA; Schema: public; Owner: tdar
--

--insert into culture_keyword (id, label, approved, index, selectable) values (2201, 'Archaeology', true, 2201, true);
--insert into culture_keyword (id, label, approved, index, selectable) values (2102, 'Curatorial and Related Studies', true, 2102, true);
--insert into culture_keyword (id, label, approved, index, selectable) values (2103, 'Historical Studies', true, 2103, true);
--insert into culture_keyword (id, label, approved, index, selectable) values (2199, 'Other History and Archaeology', true, 2199, false);

insert into culture_keyword (id, label, approved, index, selectable) values (210101, 'Aboriginal and Torres Strait Islander Archaeology (FOR 210101)', true, '10101', true);

insert into culture_keyword (id, label, approved, index, selectable) values (210102, 'Archaeological Science (FOR 210102)', true, '10102', false);
insert into culture_keyword (id, label, approved, index, selectable, parent_id) values (300000, 'Geochronology', true, '10102.1', true, 210102);
insert into culture_keyword (id, label, approved, index, selectable, parent_id) values (300001, 'Zooarchaeology', true, '10102.2', true, 210102);
insert into culture_keyword (id, label, approved, index, selectable, parent_id) values (300002, 'Bioarchaeology', true, '10102.3', true, 210102);

insert into culture_keyword (id, label, approved, index, selectable) values (210103, 'Archaeology of Asia, Africa and the Americas (FOR 210103)', true, '10103', false);
insert into culture_keyword (id, label, approved, index, selectable, parent_id) values (300003, 'Archaeology of Asia', true, '10103.1', true, 210103);
insert into culture_keyword (id, label, approved, index, selectable, parent_id) values (300004, 'Archaeology of Africa', true, '10103.2', true, 210103);
insert into culture_keyword (id, label, approved, index, selectable, parent_id) values (300005, 'Archaeology of the Americas', true, '10103.3', true, 210103);

insert into culture_keyword (id, label, approved, index, selectable) values (210104, 'Archaeology of Australia (excl. Aboriginal and Torres Strait Islander) (FOR 210104)', true, '10104', true);
insert into culture_keyword (id, label, approved, index, selectable) values (210105, 'Archaeology of Europe, the Mediterranean and the Levant (FOR 210105)', true, '10105', true);
insert into culture_keyword (id, label, approved, index, selectable) values (210106, 'Archaeology of New Guinea and Pacific Islands (excl. New Zealand) (FOR 210106)', true, '10106', true);
insert into culture_keyword (id, label, approved, index, selectable) values (210107, 'Archaeology of New Zealand (excl. Maori) (FOR 210107)', true, '10107', true);
insert into culture_keyword (id, label, approved, index, selectable) values (210108, 'Historical Archaeology (incl. Industrial Archaeology) (FOR 210108)', true, '10108', true);
insert into culture_keyword (id, label, approved, index, selectable) values (210109, 'Maori Archaeology (FOR 210109)', true, '10109', true);
insert into culture_keyword (id, label, approved, index, selectable) values (210110, 'Maritime Archaeology (FOR 210110)', true, '10110', true);
insert into culture_keyword (id, label, approved, index, selectable) values (210199, 'Archaeology not elsewhere classified (FOR 210199)', true, '10199', true);

insert into culture_keyword (id, label, approved, index, selectable) values (210201, 'Archival, Repository and Related Studies (FOR 210201)', true, '10201', true);
insert into culture_keyword (id, label, approved, index, selectable) values (210202, 'Heritage and Cultural Conservation (FOR 210202)', true, '10202', true);
insert into culture_keyword (id, label, approved, index, selectable) values (210203, 'Materials Conservation (FOR 210203)', true, '10203', true);
insert into culture_keyword (id, label, approved, index, selectable) values (210204, 'Museum Studies (FOR 210204)', true, '10204', true);
insert into culture_keyword (id, label, approved, index, selectable) values (210205, 'Curatorial and Related Studies not elsewhere classified (FOR 210205)', true, '102025', true);

insert into culture_keyword (id, label, approved, index, selectable) values (210301, 'Aboriginal and Torres Strait Islander History (FOR 210301)', true, '10301', true);
insert into culture_keyword (id, label, approved, index, selectable) values (210302, 'Asian History (FOR 210302)', true, '10302', true);
insert into culture_keyword (id, label, approved, index, selectable) values (210303, 'Australian History (excl. Aboriginal and Torres Strait Islander History) (FOR 210303)', true, '10303', true);
insert into culture_keyword (id, label, approved, index, selectable) values (210304, 'Biography (FOR 210304)', true, '10304', true);
insert into culture_keyword (id, label, approved, index, selectable) values (210305, 'British History (FOR 210305)', true, '10305', true);
insert into culture_keyword (id, label, approved, index, selectable) values (210306, 'Classical Greek and Roman History (FOR 210306)', true, '10306', true);
insert into culture_keyword (id, label, approved, index, selectable) values (210307, 'European History (excl. British, Classical Greek and Roman) (FOR 210307)', true, '10307', true);
insert into culture_keyword (id, label, approved, index, selectable) values (210308, 'Latin American History (FOR 210308)', true, '10308', true);
insert into culture_keyword (id, label, approved, index, selectable) values (210309, 'Maori History (FOR 210309)', true, '10309', true);
insert into culture_keyword (id, label, approved, index, selectable) values (210310, 'Middle Eastern and African History (FOR 210310)', true, '10310', true);
insert into culture_keyword (id, label, approved, index, selectable) values (210311, 'New Zealand History (FOR 210311)', true, '10311', true);
insert into culture_keyword (id, label, approved, index, selectable) values (210312, 'North American History (FOR 210312)', true, '10312', true);
insert into culture_keyword (id, label, approved, index, selectable) values (210313, 'Pacific History (excl. New Zealand and Maori) (FOR 210313)', true, '10313', true);
insert into culture_keyword (id, label, approved, index, selectable) values (210314, 'Historical Studies not elsewhere classified (FOR 210314)', true, '10314', true);

insert into culture_keyword (id, label, approved, index, selectable) values (219999, 'History and Archaeology not elsewhere classified (FOR 219999)', true, '19999', true);


-- Step 6) Change the existing culture keywords to the new ones
-- this is not as simple as it seems as duplicate keys will result, which will cause problems. So following will fail
-- update resource_culture_keyword set culture_keyword_id = 210108;

-- rather, assume no entries point to new culture keywords
insert into resource_culture_keyword (resource_id, culture_keyword_id) SELECT DISTINCT resource_id, 210108 FROM resource_culture_keyword;
delete from resource_culture_keyword where culture_keyword_id <> 210108;


-- Step 7) Remove the existing culture keywords
delete from culture_keyword_synonym where culturekeyword_id < 210101; 
delete from culture_keyword where id < 210101;
