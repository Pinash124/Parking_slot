update users
set role = 'CUSTOMER'
where role = 'USER' or role is null or trim(role) = '';

alter table users
    alter column role set default 'CUSTOMER';

alter table users
    drop constraint if exists chk_users_role;

alter table users
    add constraint chk_users_role
    check (role in ('MANAGER', 'STAFF', 'CUSTOMER'));
