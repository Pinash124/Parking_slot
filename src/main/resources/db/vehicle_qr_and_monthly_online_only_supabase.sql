-- PostgreSQL / Supabase migration for vehicle QR codes and monthly-pass online-only payment.
-- Run this once on Supabase if Hibernate ddl-auto=update is not allowed in production.

alter table if exists vehicles
    add column if not exists qr_code varchar(255);

update vehicles
set qr_code = 'VEHICLE|vehicleId=' || vehicle_id || '|plate=' || upper(coalesce(plate_number, ''))
where qr_code is null or trim(qr_code) = '';

create unique index if not exists uq_vehicles_qr_code
    on vehicles(qr_code)
    where qr_code is not null;

-- Monthly passes are transfer/online QR only.
-- Keep old payment_method values for audit, but normalize pending/new rows away from CASH.
update monthly_parking_passes
set payment_method = null
where upper(coalesce(payment_status, '')) <> 'PAID'
  and upper(coalesce(payment_method, '')) = 'CASH';
