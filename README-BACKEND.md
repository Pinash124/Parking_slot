# Smart Parking Unified Backend

Backend Spring Boot da duoc quy ve mot runtime duy nhat:

- Entity/repository: `com.example.pricing_calculation.domain` va `repository`
- Authentication: Bearer token tai `/api/auth`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Chay nhanh

Yeu cau Java 17+ va Maven.

```powershell
mvn spring-boot:run
```

Neu dung PostgreSQL, sao chep `.env.example` thanh `.env`, dien dung
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, sau do khoi dong lai backend. Runtime tu
dong nap file `.env` o thu muc goc du an.

Mac dinh he thong dung H2 file tai `./data/smartparking` va tu tao/cap nhat schema. Tai khoan khoi tao:

- Email: `admin@smartparking.local`
- Password: `Admin@12345`

Hay doi hai gia tri nay bang `BOOTSTRAP_ADMIN_EMAIL` va `BOOTSTRAP_ADMIN_PASSWORD` khi trien khai.

## PostgreSQL

```text
DB_URL=jdbc:postgresql://localhost:5432/smartparking
DB_USERNAME=postgres
DB_PASSWORD=your_password
DDL_AUTO=update
```

## Thu tu test Swagger

1. Test nhanh: `POST /api/auth/login/direct`, copy `accessToken`. Luong OTP day du dung `/api/auth/login` roi `/api/auth/login/verify`.
2. Bam **Authorize**, nhap token (khong can go chu Bearer).
3. Manager tao building -> floor -> vehicle type -> zone -> slot -> pricing policy.
4. Admin tao tai khoan `PARKING_STAFF`; user tu dang ky tai `/api/auth/register`.
5. User tao vehicle va reservation. Reservation tu giu mot slot `RESERVED`.
6. Staff check-in tai `/api/staff/parking-sessions/check-in`.
7. User xem `/api/user/parking-sessions/current` va co the them dich vu bo sung.
8. Staff goi checkout de tinh phi; session chuyen `PAYMENT_PENDING` nhung slot van `OCCUPIED`.
9. Thanh toan Cash/VNPay tai `/api/payment-gateways`.
10. Staff goi `complete-exit`; chi cho ra khi payment da hoan tat, sau do slot ve `AVAILABLE`.

## Ve thang

- User dang ky ve thang tai `POST /api/user/monthly-passes`
- User xem cac ve thang cua minh tai `GET /api/user/monthly-passes`
- Neu xe co ve thang con hieu luc, he thong tu tinh `parkingFee = 0` khi checkout
- Neu tong phi = 0, backend tu tao payment cash 0 dong de barrier/validate khong bi ket

## Cong, vi pham va thong bao

- Manager quan ly cong vao/ra tai `GET /api/manager/gates`
- Staff ghi nhan vi pham tai `POST /api/staff/violations`
- User xem thong bao cua minh tai `GET /api/user/notifications`
- Khi tao reservation, dang ky ve thang, check-in va exit hoan tat, he thong tu dong tao notification cho chu xe
- Khi tao/resolve violation, he thong tu dong tao notification cho chu xe lien quan
- Check-in va exit co the validate `entryGateCode` / `exitGateCode` neu gate da duoc cau hinh trong bang `Gates`

VNPay Sandbox uses HMAC-SHA512 signing and verifies the callback signature, amount and transaction reference before completing a payment.

## Authentication day du

- Dang ky OTP: `/api/auth/register` -> `/api/auth/register/verify`
- Dang nhap OTP: `/api/auth/login` -> `/api/auth/login/verify`
- Test nhanh khong OTP: `/api/auth/register/direct`, `/api/auth/login/direct`
- Google OAuth: `/api/auth/google` (cau hinh `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`)
- Quen/reset mat khau: `/api/auth/forgot-password` -> `/api/auth/reset-password`
- Doi mat khau: `/api/auth/change-password`
- Ho so: `/api/auth/me`, `/api/auth/profile`
- Dang xuat: `/api/auth/logout`

Dev mac dinh tra OTP trong truong `developmentOtp`. Production dat `EXPOSE_DEVELOPMENT_OTP=false` va cau hinh SMTP.

