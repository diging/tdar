create table user_info (
    id  bigserial not null,
    affilliation varchar(255),
    contributor boolean default FALSE not null,
    contributor_agreement_version int default 0 not null,
    contributor_reason varchar(512),
    last_login timestamp,
    penultimate_login timestamp,
    proxy_note text,
    tos_version int default 0 not null,
    total_login int8,
    proxyInstitution_id int8 references institution,
    user_id int8 references person,
    primary key (id)
);

insert into user_info (affilliation, contributor, contributor_reason, contributor_agreement_version,last_login, penultimate_login, proxy_note, tos_version, total_login, proxyInstitution_id, user_id) 
    select affilliation, contributor, contributor_reason, contributor_agreement_version,last_login, penultimate_login, proxy_note, tos_version, total_login, proxyInstitution_id, id from person where username is not null;  
    
alter table person drop column  affilliation;
alter table person drop column  contributor;
alter table person drop column  contributor_agreement_version;
alter table person drop column  contributor_reason;
alter table person drop column  last_login;
alter table person drop column  penultimate_login;
alter table person drop column  proxy_note;
alter table person drop column  tos_version;
alter table person drop column  total_login;
alter table person drop column  proxyInstitution_id;

create table tdar_user(
    id int8 references person,
    username varchar(255),
    primary key (id)
);
insert into tdar_user select id,username from person where username is not null;