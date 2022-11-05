create table address_aud (id int8 not null, rev int4 not null, revtype int2, city varchar(255), country varchar(255), street varchar(255), zip varchar(255), primary key (id, rev));
create table airport_aud (id int8 not null, rev int4 not null, revtype int2, iata varchar(255), name varchar(255), address_id int8, primary key (id, rev));
create table flight_aud (id int8 not null, rev int4 not null, revtype int2, delay int4, flight_number varchar(255), takeoff_time timestamp, landing_id int8, takeoff_id int8, primary key (id, rev));
create table revinfo (rev int4 not null, revtstmp int8, primary key (rev));
alter table if exists address_aud add constraint FK_address_aud_rev foreign key (rev) references revinfo;
alter table if exists airport_aud add constraint FK_airport_aud_rev foreign key (rev) references revinfo;
alter table if exists flight_aud add constraint FK_flight_aud_rev foreign key (rev) references revinfo;