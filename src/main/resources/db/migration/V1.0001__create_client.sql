create table client (
    id IDENTITY  primary key,
    firstname varchar(255) not null,
    lastname varchar(255) not null,
    birthdate date null,
    address text null,
    phone varchar(50)  not null,
    email varchar(255) not null
);