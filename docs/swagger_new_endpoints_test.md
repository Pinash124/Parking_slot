# Swagger checklist - API moi/can test

Swagger local:

- `http://localhost:8080/swagger-ui/index.html`

Yeu cau chung:

- `/api/user/**`: dung token `PARKING_USER`.
- `/api/manager/**`: dung token manager/admin.
- Header: `Authorization: Bearer <token>`.
- Checklist nay chi test BE/Swagger, khong dung FE.

## 1. Parking info - loc slot theo muc dich

### 1.1 Slot cho vang lai

`GET /api/parking-info/available-slots?purpose=PARKING`

Expected:

- Tra slot con trong cho xe vao truc tiep/vang lai.
- Khong cho vang lai chiem slot thang.

### 1.2 Slot cho dat truoc

`GET /api/parking-info/available-slots?purpose=RESERVATION`

Expected:

- Chi thay slot/khu thuong cua car.
- Khong thay `CAR_MONTHLY`.

### 1.3 Slot cho ve thang

`GET /api/parking-info/available-slots?purpose=MONTHLY`

Expected:

- Chi thay slot/khu thang cua car.
- Khong thay `CAR_NORMAL`.
- 1/3 slot dau cua khu car la slot thang.

## 2. User zones - loc khu theo muc dich

### 2.1 Khu cho dat truoc

`GET /api/user/zones?purpose=RESERVATION`

Expected:

- Chi thay zone car thuong, vi du `CAR_NORMAL`.
- Khong thay `CAR_MONTHLY`.

### 2.2 Khu cho ve thang

`GET /api/user/zones?purpose=MONTHLY`

Expected:

- Chi thay zone car thang, vi du `CAR_MONTHLY`.
- Khong thay `CAR_NORMAL`.

### 2.3 Khu cho vang lai

`GET /api/user/zones?purpose=PARKING`

Expected:

- Dung cho xe vao truc tiep.
- Khong cho vang lai chiem slot thang.

## 3. Dat truoc - chi car, khong khoa slot cu the

### 3.1 Tao dat truoc hop le

`POST /api/user/reservations`

Body:

```json
{
  "vehicleId": 1,
  "zoneId": 2,
  "startTime": "2026-07-07T08:00:00",
  "endTime": "2026-07-07T10:00:00"
}
```

Expected:

- Xe phai la car.
- Zone phai la `CAR_NORMAL`.
- Dat truoc chi giu suc chua/khu, khong khoa slot cu the luc dat.
- Khi check-in hop le, BE moi random slot trong khu thuong.

### 3.2 Dat truoc bang motor

Expected:

- Reject.
- Ly do: chi car moi duoc dat truoc.

### 3.3 Dat truoc vao zone thang

Expected:

- Reject.
- Khong cho dat truoc lan vao `CAR_MONTHLY`.

### 3.4 Rule som/tre

Can test qua API xe vao/check-in hien co:

- Som trong 30 phut so voi `startTime`: hop le.
- Som hon 30 phut: khong dung booking do.
- Tre trong 20 phut: hop le.
- Tre hon 20 phut: huy/khong dung booking, xe xu ly nhu khong dat truoc neu con cho.

## 4. Ve thang - chon slot car thang

### 4.1 Tao ve thang hop le

`POST /api/user/monthly-passes`

Body:

```json
{
  "vehicleId": 1,
  "slotId": 1,
  "startDate": "2026-07-07",
  "months": 1,
  "note": "Dang ky ve thang"
}
```

Expected:

- Chi car duoc dang ky ve thang.
- Slot phai thuoc `CAR_MONTHLY`.
- Slot chuyen sang `MONTHLY_HELD`.
- Gia car thang la `500000/thang`.
- Response tra lai cho da chon va field reminder:

```json
{
  "slotId": 1,
  "slotCode": "F1-CAR-MONTHLY-001",
  "slotStatus": "MONTHLY_HELD",
  "monthlyRate": 500000,
  "totalAmount": 500000,
  "paymentStatus": "PENDING",
  "daysUntilExpiry": 30,
  "expiryReminderDue": false,
  "expiryReminderMessage": null
}
```

### 4.2 Tao ve thang bang slot thuong

Expected:

- Reject.
- Khong cho lan slot thuong vao ve thang.

### 4.3 Tao ve thang bang motor

Expected:

- Reject.
- Ly do: chi car co ve thang/chon slot thang.

## 5. Thanh toan ve thang

Luu y chot moi:

- Khong con auto-renew/tu dong gia han.
- Khong can lien ket bank/tai khoan de tru tien.
- Moi lan gia han/thanh toan la mot ky rieng.

### 5.1 Online QR - thanh toan ky hien tai

`POST /api/user/monthly-passes/{id}/payment/online-qr`

Body: khong can.

Expected:

- Tra QR content de user thanh toan.
- QR chi dai dien cho ky ve thang hien tai.
- Response khong co `autoRenew`.

Response mau:

```json
{
  "paymentMethod": "ONLINE_QR",
  "paymentReference": "MTHQR-1-ABCDEFGH",
  "amount": 500000,
  "qrContent": "MONTHLY_PASS|passId=1|ref=...",
  "billContent": null
}
```

### 5.2 Tien mat - bill cho staff quet

`POST /api/user/monthly-passes/{id}/payment/cash-bill`

Body: khong can.

Expected:

- Tra bill text + QR content cho staff quet/xac nhan.
- Chi thanh toan ky hien tai, khong tu gia han.
- Response khong co `autoRenew`.

Response mau:

```json
{
  "paymentMethod": "CASH",
  "paymentReference": "MTHCASH-1-ABCDEFGH",
  "amount": 500000,
  "qrContent": "MONTHLY_PASS|passId=1|ref=...",
  "billContent": "BILL VE THANG..."
}
```

### 5.3 Manager xac nhan thanh toan theo id

`POST /api/manager/monthly-passes/{id}/confirm-payment`

Body:

```json
{
  "paymentMethod": "ONLINE_QR",
  "referenceCode": "TEST-PAID-001"
}
```

Expected:

- `paymentStatus = PAID`.
- Ve chuyen `ACTIVE` neu toi ngay bat dau, hoac `SCHEDULED` neu start date o tuong lai.
- Slot chuyen sang `MONTHLY_RESERVED`.

### 5.4 Manager xac nhan bang QR scan

`POST /api/manager/monthly-passes/confirm-payment/scan`

Body:

```json
{
  "qrContent": "MONTHLY_PASS|passId=1|ref=...",
  "paymentMethod": "CASH",
  "referenceCode": "STAFF-SCAN-001"
}
```

Expected:

- Tim dung ve thang tu QR content.
- Xac nhan thanh toan thanh cong.
- Slot thanh `MONTHLY_RESERVED`.

## 6. Nhac ve thang sap het han truoc 3 ngay

### 6.1 User xem danh sach ve thang

`GET /api/user/monthly-passes`

Expected:

- Voi ve da thanh toan va con 0-3 ngay toi `endDate`, response co:

```json
{
  "daysUntilExpiry": 3,
  "expiryReminderDue": true,
  "expiryReminderMessage": "Ve thang cua xe 59A-12345 se het han sau 3 ngay. Vui long thanh toan ky moi neu muon tiep tuc giu cho."
}
```

- Neu con hon 3 ngay, da het han, hoac chua thanh toan:

```json
{
  "expiryReminderDue": false,
  "expiryReminderMessage": null
}
```

Ghi chu:

- Backend chi nhac, khong tu gia han.
- Neu khach muon tiep tuc giu cho, tao/thanh toan ky ve thang moi.

## 7. Manager - quan ly khu/slot moi

### 7.1 Rebalance car zone theo rule 1/3 thang, 2/3 thuong

`POST /api/manager/zones/rebalance-car?floorId=<floorId>`

Expected:

- Chia lai slot car tren floor:
  - 1/3 slot dau la zone thang.
  - 2/3 slot con lai la zone thuong.
- Slot code/name doi theo khu thang/thuong.
- Khong lam mat lien ket du lieu dang dung.

### 7.2 Xem zone co zoneType

`GET /api/manager/zones?floorId=<floorId>`

Expected:

- Response co `zoneType`, vi du:
  - `CAR_MONTHLY`
  - `CAR_NORMAL`

### 7.3 Xem slot co zoneType

`GET /api/manager/slots?zoneId=<zoneId>`

Expected:

- Response co `zoneType`, `slotCode`, `status`.

## 8. Gia can xac nhan

### 8.1 Gia ve thang car

Test bang:

- `GET /api/user/vehicle-types`
- Hoac tao ve thang 1 thang.

Expected:

- Car monthly rate = `500000`.
- Ve thang 1 thang total = `500000`.

### 8.2 Gia luot thuong/vang lai

- Ngay `07:00 - 21:59`:
  - 2 banh: `5000/luot`
  - 4 banh: `10000/luot`
- Dem `22:00 - 06:59`:
  - 2 banh: `+3000/gio`
  - 4 banh: `+5000/gio`
- Grace time: `10 phut`.
- Ve thang: khong tinh tien luot khi xe ra vi da thanh toan thang.

## 9. Checklist pass/fail nhanh

- [ ] `purpose=RESERVATION` khong thay slot/zone thang.
- [ ] `purpose=MONTHLY` khong thay slot/zone thuong.
- [ ] Motor khong dat truoc duoc.
- [ ] Motor khong lam ve thang duoc.
- [ ] Dat truoc car khong khoa slot cu the luc dat.
- [ ] Check-in booking hop le random slot trong khu thuong.
- [ ] Vang lai chi vao khi con suc chua; full thi khong nhan them.
- [ ] Ve thang tra `slotId`, `slotCode`, `slotStatus`.
- [ ] Online QR khong can body auto-renew.
- [ ] Cash bill khong tu gia han.
- [ ] Manager scan QR xac nhan duoc payment.
- [ ] Car ve thang gia `500000/thang`.
- [ ] Ve thang con <= 3 ngay het han tra `expiryReminderDue=true`.
