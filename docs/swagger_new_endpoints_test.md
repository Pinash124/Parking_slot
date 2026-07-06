# Swagger checklist - API mới/cần test

Backend Swagger local:

- `http://localhost:8080/swagger-ui/index.html`

Yêu cầu chung:

- Các API `/api/user/**` dùng token user role `PARKING_USER`.
- Các API `/api/manager/**` dùng token manager/admin.
- Header: `Authorization: Bearer <token>`.
- Không cần test FE trong checklist này.

## 1. Parking info - lọc slot theo mục đích

### 1.1 Lấy slot cho khách vãng lai

`GET /api/parking-info/available-slots?purpose=PARKING`

Expected:

- Trả slot còn trống cho xe vào trực tiếp/vãng lai.
- Không trả slot khu tháng đang giữ/đã reserved cho vé tháng.

### 1.2 Lấy slot cho đặt trước

`GET /api/parking-info/available-slots?purpose=RESERVATION`

Expected:

- Chỉ trả slot/khu thường của car.
- Không thấy slot/khu `MONTHLY`.
- Nếu khu thường hết sức chứa thì không còn slot phù hợp để đặt/vào.

### 1.3 Lấy slot cho vé tháng

`GET /api/parking-info/available-slots?purpose=MONTHLY`

Expected:

- Chỉ trả slot/khu tháng của car.
- Không thấy slot/khu thường.
- Slot tháng đầu khu car là phần 1/3 số slot theo rule đã chốt.

### 1.4 Lọc thêm theo zone/vehicle type

`GET /api/parking-info/available-slots?purpose=MONTHLY&zoneId=<zoneId>`

`GET /api/parking-info/available-slots?purpose=RESERVATION&vehicleTypeId=<carVehicleTypeId>`

Expected:

- Response có thêm các field để FE phân biệt khu:

```json
{
  "slotId": 1,
  "slotCode": "F1-CAR-MONTHLY-001",
  "status": "AVAILABLE",
  "zoneId": 1,
  "zoneName": "Car Monthly",
  "zoneType": "CAR_MONTHLY",
  "vehicleTypeId": 1,
  "vehicleTypeName": "Car",
  "floorId": 1,
  "floorName": "F1",
  "buildingId": 1,
  "buildingName": "Main"
}
```

## 2. User zones - lọc khu theo mục đích

### 2.1 Khu cho đặt trước

`GET /api/user/zones?purpose=RESERVATION`

Expected:

- Chỉ thấy zone car thường, ví dụ `CAR_NORMAL`.
- Không thấy `CAR_MONTHLY`.

### 2.2 Khu cho vé tháng

`GET /api/user/zones?purpose=MONTHLY`

Expected:

- Chỉ thấy zone car tháng, ví dụ `CAR_MONTHLY`.
- Không thấy `CAR_NORMAL`.

### 2.3 Khu cho vãng lai

`GET /api/user/zones?purpose=PARKING`

Expected:

- Dùng cho xe vào trực tiếp.
- Không cho vãng lai chiếm slot tháng.

## 3. Đặt trước - chỉ car, không chọn slot cụ thể

### 3.1 Tạo đặt trước hợp lệ

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

- Xe phải là car.
- Zone phải là zone thường của car.
- Đặt trước chỉ giữ sức chứa/khu, không khóa slot cụ thể ngay lúc đặt.
- Response có thể trả thông tin zone đã chọn; slot đặt trước mới sẽ không phải là slot tháng.

### 3.2 Đặt trước bằng motor

`POST /api/user/reservations`

Body giống trên nhưng `vehicleId` là motor.

Expected:

- Bị reject.
- Lý do: chỉ car mới được đặt trước.

### 3.3 Đặt trước vào zone tháng

`POST /api/user/reservations`

Body dùng `zoneId` thuộc `CAR_MONTHLY`.

Expected:

- Bị reject.
- Không cho đặt trước lẫn vào khu tháng.

### 3.4 Check rule sớm/trễ

Luồng cần test qua API xe vào/check-in hiện có:

- Sớm trong 30 phút so với `startTime`: hợp lệ.
- Sớm hơn 30 phút: xem như không đạt đặt trước, không dùng booking đó.
- Trễ trong 20 phút: hợp lệ.
- Trễ hơn 20 phút: hủy/không dùng booking, xe xử lý như không đặt trước nếu còn chỗ vãng lai.

## 4. Vé tháng - chọn slot car tháng

### 4.1 Tạo vé tháng hợp lệ

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

- Chỉ car được đăng ký vé tháng.
- Slot phải thuộc zone `CAR_MONTHLY`.
- Slot chuyển sang trạng thái giữ/tháng.
- Response trả lại chỗ đã chọn:

```json
{
  "id": 10,
  "slotId": 1,
  "slotCode": "F1-CAR-MONTHLY-001",
  "slotStatus": "MONTHLY_HELD",
  "monthlyRate": 500000,
  "totalAmount": 500000,
  "paymentStatus": "PENDING_PAYMENT",
  "autoRenew": false
}
```

### 4.2 Tạo vé tháng bằng slot thường

`POST /api/user/monthly-passes`

Body dùng `slotId` thuộc `CAR_NORMAL`.

Expected:

- Bị reject.
- Không cho lẫn slot thường vào vé tháng.

### 4.3 Tạo vé tháng bằng motor

`POST /api/user/monthly-passes`

Body dùng `vehicleId` là motor.

Expected:

- Bị reject.
- Lý do: chỉ car có vé tháng/chọn slot tháng.

## 5. Thanh toán vé tháng

### 5.1 Online QR - có hoặc không tự động gia hạn

`POST /api/user/monthly-passes/{id}/payment/online-qr`

Body:

```json
{
  "autoRenew": true
}
```

Expected:

- Trả QR content để thanh toán online.
- `autoRenew` đúng theo body.
- Chưa cần manager xác nhận ở bước này.

Response cần kiểm:

```json
{
  "paymentMethod": "ONLINE_QR",
  "paymentReference": "MP-...",
  "amount": 500000,
  "autoRenew": true,
  "qrContent": "...",
  "billContent": null
}
```

Test thêm body:

```json
{
  "autoRenew": false
}
```

Expected:

- `autoRenew = false`.

### 5.2 Tiền mặt - bill cho staff quét

`POST /api/user/monthly-passes/{id}/payment/cash-bill`

Expected:

- Trả bill có thông tin vé tháng + QR/bill content cho staff quét.
- Backend set `autoRenew = true` cho luồng tiền mặt.

Response cần kiểm:

```json
{
  "paymentMethod": "CASH",
  "paymentReference": "MP-...",
  "amount": 500000,
  "autoRenew": true,
  "qrContent": "...",
  "billContent": "..."
}
```

### 5.3 Manager xác nhận thanh toán theo id

`POST /api/manager/monthly-passes/{id}/confirm-payment`

Body:

```json
{
  "paymentMethod": "ONLINE_QR",
  "referenceCode": "TEST-PAID-001",
  "autoRenew": true
}
```

Expected:

- `paymentStatus = PAID`.
- Vé chuyển `ACTIVE` nếu đã tới ngày bắt đầu, hoặc `SCHEDULED` nếu start date ở tương lai.
- Slot chuyển sang `MONTHLY_RESERVED`.

### 5.4 Manager xác nhận bằng QR scan

`POST /api/manager/monthly-passes/confirm-payment/scan`

Body:

```json
{
  "qrContent": "copy-qr-content-tu-buoc-5.1-hoac-5.2",
  "paymentMethod": "CASH",
  "referenceCode": "STAFF-SCAN-001"
}
```

Expected:

- Tìm đúng vé tháng từ QR content.
- Xác nhận thanh toán thành công.
- Slot thành `MONTHLY_RESERVED`.

## 6. Manager - quản lý khu/slot mới

### 6.1 Rebalance car zone theo rule 1/3 tháng, 2/3 thường

`POST /api/manager/zones/rebalance-car?floorId=<floorId>`

Expected:

- Chia lại slot car trên floor:
  - 1/3 slot đầu là zone tháng.
  - 2/3 slot còn lại là zone thường.
- Slot code/name được đổi theo khu tháng/thường.
- Không làm mất liên kết dữ liệu đang dùng.

### 6.2 Xem zone có zoneType

`GET /api/manager/zones?floorId=<floorId>`

Expected:

- Response có `zoneType`, ví dụ:
  - `CAR_MONTHLY`
  - `CAR_NORMAL`
  - zone motor giữ logic riêng.

### 6.3 Xem slot có zoneType

`GET /api/manager/slots?zoneId=<zoneId>`

Expected:

- Response có:
  - `zoneType`
  - `slotCode`
  - `status`

## 7. Giá cần xác nhận

### 7.1 Giá vé tháng car

Test bằng:

- `GET /api/user/vehicle-types`
- hoặc tạo vé tháng 1 tháng ở mục 4.1.

Expected:

- Car monthly rate = `500000`.
- Vé tháng 1 tháng total = `500000`.

### 7.2 Giá lượt thường/vãng lai

Luồng tính tiền khi xe ra:

- Ngày: `07:00 - 21:59`
  - 2 bánh: `5000/lượt`
  - 4 bánh: `10000/lượt`
- Đêm: `22:00 - 06:59`
  - 2 bánh: `+3000/giờ`
  - 4 bánh: `+5000/giờ`
- Grace time: `10 phút`.
- Vé tháng: không tính tiền lượt khi xe ra vì đã thanh toán tháng.

## 8. Checklist pass/fail nhanh

- [ ] `purpose=RESERVATION` không thấy slot/zone tháng.
- [ ] `purpose=MONTHLY` không thấy slot/zone thường.
- [ ] Motor không đặt trước được.
- [ ] Motor không làm vé tháng được.
- [ ] Đặt trước car không khóa slot cụ thể lúc đặt.
- [ ] Khi xe đặt trước check-in hợp lệ thì được xếp slot random trong khu thường.
- [ ] Vãng lai chỉ vào khi còn sức chứa; full thì khóa bãi/không nhận thêm.
- [ ] Vé tháng trả `slotId`, `slotCode`, `slotStatus`.
- [ ] Online QR cho chọn `autoRenew=true/false`.
- [ ] Tiền mặt trả bill/QR cho staff quét và tự gia hạn.
- [ ] Manager scan QR xác nhận được payment.
- [ ] Car vé tháng giá `500000/tháng`.
