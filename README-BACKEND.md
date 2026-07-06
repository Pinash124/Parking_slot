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

- User dang ky ve thang tai `POST /api/user/monthly-passes` voi `vehicleId`, `slotId`, `startDate`, `months`
- User xem cac ve thang cua minh tai `GET /api/user/monthly-passes`
- Response tra ve ngay `slotId`, `slotCode`, `slotStatus` de FE hien thi cho da chon
- Ve moi co trang thai `PENDING_PAYMENT`, slot duoc giu o `MONTHLY_HELD`
- Manager xac nhan tra truoc tai `POST /api/manager/monthly-passes/{id}/confirm-payment`; luc nay slot chuyen `MONTHLY_RESERVED`
- Manager huy ve tai `POST /api/manager/monthly-passes/{id}/cancel`; slot duoc tra ve `AVAILABLE` neu xe chua vao bai
- Neu xe co ve thang con hieu luc, he thong tu tinh `parkingFee = 0` khi checkout
- Neu tong phi = 0, backend tu tao payment cash 0 dong de barrier/validate khong bi ket

## Quy tac vao bai va dat truoc

- Chi xe 4 banh/car duoc dat truoc va dang ky ve thang co cho doc quyen; xe 2 banh vao nhu xe thuong/vang lai
- Moi tang chia khu car thanh `CAR_MONTHLY` va `CAR_NORMAL`: slot thang = `floor(tong slot car / 3)`, slot thuong nhan phan con lai
- Ma slot car duoc danh lai lien tuc: `F1-CAR-MONTHLY-001..010` la 1/3 dau, `F1-CAR-NORMAL-011..030` la 2/3 con lai
- Booking thuong chi duoc chon `CAR_NORMAL`; ve thang chi duoc chon `CAR_MONTHLY`; xe car vang lai cung chi duoc xep vao `CAR_NORMAL`
- Catalog tach hoan toan bang query `purpose`: `GET /api/user/zones?purpose=RESERVATION|MONTHLY` va `GET /api/parking-info/available-slots?purpose=PARKING|RESERVATION|MONTHLY`; moi purpose chi tra zone/slot dung luong cua no
- Khi them/xoa slot car, backend tu can bang lai ty le bang cac slot `AVAILABLE`; co the goi lai thu cong `POST /api/manager/zones/rebalance-car?floorId=...`
- Dat truoc duoc vao som toi da 30 phut va tre toi da 20 phut so voi `startTime`
- Ngoai cua so tren, booking bi huy, slot da giu duoc giai phong va xe duoc xu ly nhu vang lai
- Neu khong co booking/ve thang va khong truyen `slotId`, backend tu chon ngau nhien mot slot `AVAILABLE` dung loai xe
- Response tao booking luon co `reservedSlotId`, `reservedSlotCode`, `reservedSlotStatus`

## Bieu phi ngay/dem

- Ban ngay `07:00-21:59`: 2 banh 5.000 VND/luot, 4 banh 10.000 VND/luot
- Ban dem `22:00-06:59`: cong 3.000 VND/gio cho 2 banh, 5.000 VND/gio cho 4 banh
- Co 10 phut grace khi vuot moc bieu phi; gio dem sau grace duoc lam tron len theo gio bat dau
- Cac gia tri va moc gio co the doi bang bien moi truong `DAY_TARIFF_START`, `NIGHT_TARIFF_START`, `TARIFF_GRACE_MINUTES`, `TWO_WHEEL_DAY_TURN`, `FOUR_WHEEL_DAY_TURN`, `TWO_WHEEL_NIGHT_HOURLY`, `FOUR_WHEEL_NIGHT_HOURLY`

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

