create table if not exists parking_buildings (
    building_id bigserial primary key,
    name varchar(255) not null,
    address varchar(500),
    description text,
    status varchar(50),
    created_at timestamp,
    updated_at timestamp
);

create table if not exists vehicle_types (
    vehicle_type_id bigserial primary key,
    code varchar(50) not null unique,
    name varchar(255) not null,
    description text,
    status varchar(50),
    created_at timestamp,
    updated_at timestamp
);

create table if not exists parking_zones (
    zone_id bigserial primary key,
    building_id bigint references parking_buildings(building_id) on delete set null,
    vehicle_type_id bigint references vehicle_types(vehicle_type_id) on delete set null,
    name varchar(255) not null,
    floor_number integer,
    description text,
    status varchar(50),
    created_at timestamp,
    updated_at timestamp
);

create table if not exists parking_slots (
    slot_id bigserial primary key,
    zone_id bigint references parking_zones(zone_id) on delete set null,
    vehicle_type_id bigint references vehicle_types(vehicle_type_id) on delete set null,
    slot_code varchar(100) not null unique,
    status varchar(50) not null default 'AVAILABLE',
    note text,
    created_at timestamp,
    updated_at timestamp
);

create table if not exists pricing_policies (
    pricing_policy_id bigserial primary key,
    vehicle_type_id bigint references vehicle_types(vehicle_type_id) on delete set null,
    name varchar(255) not null,
    price_amount numeric(12, 2) not null,
    pricing_unit varchar(50),
    peak_start_time time,
    peak_end_time time,
    peak_multiplier numeric(8, 2),
    rules text,
    status varchar(50),
    created_at timestamp,
    updated_at timestamp
);

create table if not exists parking_incidents (
    incident_id bigserial primary key,
    session_id bigint,
    incident_type varchar(80),
    description text,
    penalty_amount numeric(12, 2),
    status varchar(50),
    created_by bigint,
    resolved_by bigint,
    created_at timestamp,
    resolved_at timestamp
);

create table if not exists system_configs (
    config_id bigserial primary key,
    config_key varchar(150) not null unique,
    config_value text,
    description text,
    updated_at timestamp
);

create index if not exists idx_parking_slots_status on parking_slots(status);
create index if not exists idx_parking_slots_vehicle_type on parking_slots(vehicle_type_id);
create index if not exists idx_parking_sessions_entry_time on parking_sessions(entry_time);
create index if not exists idx_parking_sessions_exit_time on parking_sessions(exit_time);
