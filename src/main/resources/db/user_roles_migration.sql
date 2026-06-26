update users
set role = 'CUSTOMER'
where role = 'USER' or role is null or trim(role) = '';

update users
set role = 'CUSTOMER'
where upper(trim(role)) = 'DRIVER';

update users
set role = 'MANAGER'
where upper(trim(role)) in ('MANAGER', 'PARKING_MANAGER', 'PARKINGMANAGER');

update users
set role = 'ADMIN'
where upper(trim(role)) in ('ADMIN', 'SYSTEM_ADMIN', 'SYSTEM_ADMINISTRATOR', 'SYSTEMADMIN', 'SYS_ADMIN');

alter table users
    alter column role set default 'CUSTOMER';

alter table users
    drop constraint if exists chk_users_role;

alter table users
    add constraint chk_users_role
    check (role in ('MANAGER', 'ADMIN', 'STAFF', 'CUSTOMER'));
