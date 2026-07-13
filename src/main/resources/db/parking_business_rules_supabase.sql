-- PostgreSQL / Supabase migration for reservation windows, wheel tariffs and monthly slots.
-- Hibernate ddl-auto=update can create the same columns; this script is kept for explicit deployments.

alter table if exists vehicle_types
    add column if not exists wheel_count integer;

alter table if exists zones
    add column if not exists zone_type varchar(30);

update vehicle_types
set wheel_count = case
    when lower(name) like '%motor%' or lower(name) like '%bike%'
        or lower(name) like '%xe may%' or lower(name) like '%xe máy%' then 2
    else 4
end
where wheel_count is null;

update zones z
set zone_type = case when vt.wheel_count = 4 then 'CAR_NORMAL' else 'MOTORBIKE' end
from vehicle_types vt
where vt.vehicle_type_id = z.vehicle_type_id
  and z.zone_type is null;

insert into zones(floor_id, vehicle_type_id, zone_name, zone_type)
select z.floor_id,
       z.vehicle_type_id,
       left(regexp_replace(z.zone_name, '-ZONE$', '-MONTHLY', 'i'), 50),
       'CAR_MONTHLY'
from zones z
where z.zone_type = 'CAR_NORMAL'
  and not exists (
      select 1 from zones monthly
      where monthly.floor_id = z.floor_id
        and monthly.vehicle_type_id = z.vehicle_type_id
        and monthly.zone_type = 'CAR_MONTHLY'
  );

with zone_pairs as (
    select normal.zone_id as normal_zone_id,
           monthly.zone_id as monthly_zone_id,
           (select count(*) from parking_slots s where s.zone_id = normal.zone_id) as total_slots,
           (select count(*) from parking_slots s where s.zone_id = monthly.zone_id) as monthly_slots
    from zones normal
    join zones monthly on monthly.floor_id = normal.floor_id
        and monthly.vehicle_type_id = normal.vehicle_type_id
        and monthly.zone_type = 'CAR_MONTHLY'
    where normal.zone_type = 'CAR_NORMAL'
), movable as (
    select slot.slot_id,
           pair.monthly_zone_id,
           row_number() over (partition by pair.normal_zone_id order by slot.slot_code asc) as slot_rank,
           floor(pair.total_slots / 3.0)::integer as monthly_target
    from zone_pairs pair
    join parking_slots slot on slot.zone_id = pair.normal_zone_id
    where pair.monthly_slots = 0
      and upper(slot.status) = 'AVAILABLE'
)
update parking_slots slot
set zone_id = movable.monthly_zone_id
from movable
where slot.slot_id = movable.slot_id
  and movable.slot_rank <= movable.monthly_target;

update zones
set zone_name = regexp_replace(zone_name, '-CAR-(ZONE|NORMAL)$', '-CAR-NORMAL', 'i')
where zone_type = 'CAR_NORMAL';

with ranked_slots as (
    select slot.slot_id,
           zone.zone_type,
           split_part(zone.zone_name, '-CAR-', 1) as floor_code,
           row_number() over (partition by zone.zone_id order by slot.slot_code, slot.slot_id) as zone_rank,
           count(*) filter (where zone.zone_type = 'CAR_MONTHLY') over (
               partition by zone.floor_id, zone.vehicle_type_id
           ) as monthly_count
    from parking_slots slot
    join zones zone on zone.zone_id = slot.zone_id
    where zone.zone_type in ('CAR_NORMAL', 'CAR_MONTHLY')
)
update parking_slots slot
set slot_code = case
    when ranked.zone_type = 'CAR_MONTHLY'
        then ranked.floor_code || '-CAR-' || lpad(ranked.zone_rank::text, 3, '0')
    else ranked.floor_code || '-CAR-' || lpad((ranked.monthly_count + ranked.zone_rank)::text, 3, '0')
end
from ranked_slots ranked
where slot.slot_id = ranked.slot_id;

alter table zones drop constraint if exists chk_zones_zone_type;
alter table zones add constraint chk_zones_zone_type
    check (zone_type in ('CAR_NORMAL', 'CAR_MONTHLY', 'MOTORBIKE'));
alter table zones alter column zone_type set not null;
create index if not exists idx_zones_floor_type on zones(floor_id, zone_type);


alter table if exists reservations
    add column if not exists reserved_slot_id bigint;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'fk_reservation_reserved_slot') then
        alter table reservations
            add constraint fk_reservation_reserved_slot
            foreign key (reserved_slot_id) references parking_slots(slot_id);
    end if;
end $$;

create index if not exists idx_reservation_reserved_slot
    on reservations(reserved_slot_id);
alter table if exists monthly_parking_passes
    add column if not exists reserved_slot_id bigint,
    add column if not exists payment_status varchar(20),
    add column if not exists payment_method varchar(30),
    add column if not exists payment_reference varchar(100),
    add column if not exists paid_at timestamp;

alter table if exists parking_sessions
    add column if not exists monthly_pass_id bigint;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'fk_monthly_pass_reserved_slot') then
        alter table monthly_parking_passes
            add constraint fk_monthly_pass_reserved_slot
            foreign key (reserved_slot_id) references parking_slots(slot_id);
    end if;
    if not exists (select 1 from pg_constraint where conname = 'fk_parking_session_monthly_pass') then
        alter table parking_sessions
            add constraint fk_parking_session_monthly_pass
            foreign key (monthly_pass_id) references monthly_parking_passes(pass_id);
    end if;
end $$;

create index if not exists idx_monthly_pass_reserved_slot
    on monthly_parking_passes(reserved_slot_id);
create index if not exists idx_parking_session_monthly_pass
    on parking_sessions(monthly_pass_id);
