CREATE TABLE Status(
    id serial primary key,
    name varchar(50) not null
);
CREATE TABLE UnitType(
    id serial primary key,
    name varchar(50) not null
);


CREATE TABLE Publication(
    id uuid primary key,
    user_owner_id uuid not null references users(id),
    title varchar(100) not null,
    description text,
    price numeric(12,2) not null check(price>0),
    units int not null check(units > 0),
    units_available int not null check(units_available >=0 AND units_available <= units),
    unit_type_id int not null references UnitType(id),
    location varchar(100),
    available_date date,
    status_id int references status(id),
    date_created timestamp default NOW() not null
);