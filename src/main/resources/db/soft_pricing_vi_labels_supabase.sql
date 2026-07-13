-- Soft pricing defaults for demo/staging data.
-- Run this once on existing Supabase/PostgreSQL databases after deploying the code defaults.

update "VehicleTypes"
set name = 'Xe 2 bánh',
    description = coalesce(description, 'Xe hai bánh'),
    daily_rate = 20000,
    monthly_rate = 100000,
    wheel_count = 2
where upper(name) like '%MOTOR%'
   or upper(name) like '%BIKE%'
   or lower(name) like '%máy%'
   or wheel_count = 2;

update "VehicleTypes"
set name = 'Ô tô',
    description = coalesce(description, 'Xe bốn bánh'),
    daily_rate = 50000,
    monthly_rate = 300000,
    wheel_count = 4
where upper(name) like '%CAR%'
   or lower(name) like '%ô tô%'
   or wheel_count = 4;

update "PricingPolicies" p
set policy_name = 'Chính sách xe 2 bánh',
    hourly_rate = 3000,
    daily_rate = 20000,
    monthly_rate = 100000,
    lost_ticket_fee = 50000,
    overtime_fee = 5000
from "VehicleTypes" vt
where p.vehicle_type_id = vt.vehicle_type_id
  and (upper(vt.name) like '%MOTOR%' or upper(vt.name) like '%BIKE%' or lower(vt.name) like '%máy%' or vt.wheel_count = 2);

update "PricingPolicies" p
set policy_name = 'Chính sách ô tô',
    hourly_rate = 5000,
    daily_rate = 50000,
    monthly_rate = 300000,
    lost_ticket_fee = 100000,
    overtime_fee = 10000
from "VehicleTypes" vt
where p.vehicle_type_id = vt.vehicle_type_id
  and (upper(vt.name) like '%CAR%' or lower(vt.name) like '%ô tô%' or vt.wheel_count = 4);
