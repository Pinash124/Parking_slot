# Backend changes for FE - parking rules

File nay tom tat ngan gon nhung thay doi BE de FE/tester biet API nao can dung.

## 1. Khong dung auto-renew nua

Chot moi:

- Bo flow tu dong gia han ve thang.
- Khong lien ket bank/tai khoan de tru tien.
- Moi lan gia han/thanh toan la mot ky rieng.
- BE chi nhac user truoc khi het han 3 ngay.

## 2. Zone/slot car tach thang va thuong

Car co 2 loai zone:

- `CAR_MONTHLY`: slot ve thang.
- `CAR_NORMAL`: slot thuong/dat truoc/vang lai.

Rule chia slot car:

- 1/3 slot dau la slot thang.
- 2/3 slot con lai la slot thuong.

## 3. Endpoint loc zone/slot theo muc dich

### Zone user

```http
GET /api/user/zones?purpose=RESERVATION
GET /api/user/zones?purpose=MONTHLY
GET /api/user/zones?purpose=PARKING
```

Expected:

- `RESERVATION`: chi zone car thuong.
- `MONTHLY`: chi zone car thang.
- `PARKING`: dung cho xe vao truc tiep, khong lay slot thang cho vang lai.

### Slot trong

```http
GET /api/parking-info/available-slots?purpose=RESERVATION
GET /api/parking-info/available-slots?purpose=MONTHLY
GET /api/parking-info/available-slots?purpose=PARKING
```

Response co them:

```json
{
  "zoneType": "CAR_MONTHLY",
  "zoneName": "Car Monthly",
  "slotCode": "F1-CAR-001"
}
```

## 4. Dat truoc

```http
POST /api/user/reservations
```

Body:

```json
{
  "vehicleId": 1,
  "zoneId": 2,
  "startTime": "2026-07-07T08:00:00",
  "endTime": "2026-07-07T10:00:00"
}
```

Luật:

- Chi car duoc dat truoc.
- Zone phai la `CAR_NORMAL`.
- Dat truoc khong khoa slot cu the luc dat.
- Check-in hop le moi random slot trong khu thuong.
- Som toi da 30 phut, tre toi da 20 phut.

## 5. Ve thang

```http
POST /api/user/monthly-passes
GET /api/user/monthly-passes
```

Body tao ve:

```json
{
  "vehicleId": 1,
  "slotId": 1,
  "startDate": "2026-07-07",
  "months": 1,
  "note": "Dang ky ve thang"
}
```

Luật:

- Chi car duoc lam ve thang.
- Slot phai thuoc `CAR_MONTHLY`.
- Gia car thang: `500000 VND/thang`.
- Tao ve xong slot thanh `MONTHLY_HELD`, cho thanh toan.

Response co them field de FE hien chỗ va reminder:

```json
{
  "slotId": 1,
  "slotCode": "F1-CAR-001",
  "slotStatus": "MONTHLY_HELD",
  "monthlyRate": 500000,
  "totalAmount": 500000,
  "daysUntilExpiry": 30,
  "expiryReminderDue": false,
  "expiryReminderMessage": null
}
```

## 6. Thanh toan ve thang

Chot moi:

- Ve thang chi thanh toan bang chuyen khoan/ONLINE_QR.
- Khong su dung tien mat cho ve thang.
- Tien mat chi dung cho xe vao theo luot/vang lai khi xe ra.

### Online QR

```http
POST /api/user/monthly-passes/{id}/payment/online-qr
```

Body: khong can.

Response:

```json
{
  "paymentMethod": "ONLINE_QR",
  "paymentReference": "MTHQR-1-ABCDEFGH",
  "amount": 500000,
  "qrContent": "MONTHLY_PASS|passId=1|ref=...",
  "billContent": null
}
```

### Tien mat

```http
POST /api/user/monthly-passes/{id}/payment/cash-bill
```

Endpoint nay khong dung nua cho ve thang. BE se reject:

```text
Monthly pass only supports ONLINE_QR transfer payment
```

### Manager/nhan vien xac nhan bang QR

```http
POST /api/manager/monthly-passes/confirm-payment/scan
```

Body:

```json
{
  "qrContent": "MONTHLY_PASS|passId=1|ref=...",
  "paymentMethod": "ONLINE_QR",
  "referenceCode": "BANK-TXN-001"
}
```

Ket qua:

- `paymentStatus = PAID`
- Pass thanh `ACTIVE` hoac `SCHEDULED`
- Slot thanh `MONTHLY_RESERVED`

## 6A. QR rieng cho tung phuong tien

Khi user tao/them phuong tien:

```http
POST /api/user/vehicles
GET /api/user/vehicles
```

Response vehicle co them:

```json
{
  "id": 10,
  "plateNumber": "59A-12345",
  "qrCode": "VEHICLE|vehicleId=10|plate=59A-12345"
}
```

FE render QR tu field `qrCode`. QR nay dung cho nhan vien bai xe (`PARKING_STAFF`) quet xe vao/ra.

Nhan vien scan QR phuong tien:

```http
POST /api/staff/parking-sessions/check-in/scan-qr?entryGateCode=GATE_IN_01
POST /api/payment-checkout/prepare/scan-qr
POST /api/payment-checkout/validate-exit/scan-qr
```

Ghi chu:

- QR phuong tien co format `VEHICLE|vehicleId=...|plate=...`.
- Bien so text cu van duoc ho tro de khong pha flow cu.
- Reservation id QR cu van duoc ho tro.

## 7. Reminder het han ve thang truoc 3 ngay

FE chi can doc field trong `GET /api/user/monthly-passes`.

Neu ve da paid va con 0-3 ngay toi `endDate`:

```json
{
  "daysUntilExpiry": 3,
  "expiryReminderDue": true,
  "expiryReminderMessage": "Ve thang cua xe 59A-12345 se het han sau 3 ngay. Vui long thanh toan ky moi neu muon tiep tuc giu cho."
}
```

Neu khong can nhac:

```json
{
  "expiryReminderDue": false,
  "expiryReminderMessage": null
}
```

## 8. DB note

- Backend hien khong dung `auto_renew`.
- Neu DB da tung them cot `auto_renew` thi co the de nguyen, khong anh huong.
- Khong can migration lien ket bank.
- Them cot `vehicles.qr_code` de luu QR rieng cho tung phuong tien.
- File migration: `src/main/resources/db/vehicle_qr_and_monthly_online_only_supabase.sql`.

## 9. Test

BE da chay:

```bash
.\mvnw.cmd -q test
```

FE khong bi chinh trong dot note nay.
