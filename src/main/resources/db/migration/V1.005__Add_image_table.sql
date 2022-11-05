create table image (id int8 not null, data bytea, file_name varchar(255), airport_id int8, primary key (id));
create table image_aud (id int8 not null, rev int4 not null, revtype int2, data bytea, file_name varchar(255), primary key (id, rev));
alter table if exists image add constraint FK_image_airport foreign key (airport_id) references airport;
alter table if exists image_aud add constraint FK_image_aud_rev foreign key (rev) references revinfo;
alter table if exists airport_image_aud add constraint FK_airport_image_aud_rev foreign key (rev) references revinfo;