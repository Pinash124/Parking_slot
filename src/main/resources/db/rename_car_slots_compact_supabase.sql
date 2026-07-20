-- PostgreSQL / Supabase migration: compact car slot names.
-- Slot type is determined by zones.zone_type, not by MONTHLY/NORMAL text in slot_code.
-- Example:
--   CAR_MONTHLY zone: F4-CAR-001 .. F4-CAR-010
--   CAR_NORMAL  zone: F4-CAR-011 .. F4-CAR-030

with ranked_slots as (
    select slot.slot_id,
           zone.zone_type,
           regexp_replace(zone.zone_name, '-CAR-(MONTHLY|NORMAL|ZONE)$', '', 'i') as floor_code,
           row_number() over (partition by zone.zone_id order by slot.slot_code, slot.slot_id) as zone_rank,
           count(*) filter (where zone.zone_type = 'CAR_MONTHLY') over (
               partition by zone.floor_id, zone.vehicle_type_id
           ) as monthly_count
    from parking_slots slot
    join zones zone on zone.zone_id = slot.zone_id
    where zone.zone_type in ('CAR_NORMAL', 'CAR_MONTHLY')
), tmp as (
    update parking_slots slot
    set slot_code = 'TMP-CAR-' || slot.slot_id
    from ranked_slots ranked
    where slot.slot_id = ranked.slot_id
    returning slot.slot_id
)
update parking_slots slot
set slot_code = case
    when ranked.zone_type = 'CAR_MONTHLY'
        then ranked.floor_code || '-CAR-' || lpad(ranked.zone_rank::text, 3, '0')
    else ranked.floor_code || '-CAR-' || lpad((ranked.monthly_count + ranked.zone_rank)::text, 3, '0')
end
from ranked_slots ranked
where slot.slot_id = ranked.slot_id;
