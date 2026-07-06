# BE report cho FE - các thay đổi từ hôm qua tới giờ

Ghi chú quan trọng: FE hiện tại không bắt buộc sửa ngay. Các endpoint cũ vẫn được giữ để tránh gãy kết nối.

## 1. Tách khu/slot ô tô theo mục đích

BE/DB đã tách khu ô tô thành 2 loại:

- `CAR_NORMAL`: dành cho đặt trước thường và xe vãng lai ô tô.
- `CAR_MONTHLY`: dành riêng cho vé tháng ô tô.

Slot ô tô được chia:

- 1/3 slot đầu mỗi tầng: vé tháng.
- 2/3 slot còn lại: thường/vãng lai.

Ví dụ code slot:

```text
F1-CAR-MONTHLY-001..010
F1-CAR-NORMAL-011..030
```

## 2. API catalog có `purpose`

FE nên dùng `purpose` để lấy đúng zone/slot, tránh chọn nhầm.

### Lấy zone

```http
GET /api/user/zones?floorId={floorId}&purpose=RESERVATION
GET /api/user/zones?floorId={floorId}&purpose=MONTHLY
GET /api/user/zones?floorId={floorId}&purpose=PARKING
```

Ý nghĩa:

- `RESERVATION`: chỉ trả zone `CAR_NORMAL`.
- `MONTHLY`: chỉ trả zone `CAR_MONTHLY`.
- `PARKING`: trả zone dùng cho xe vào bãi thường/vãng lai.

### Lấy slot trống

```http
GET /api/parking-info/available-slots?zoneId={zoneId}&purpose=MONTHLY
GET /api/parking-info/available-slots?zoneId={zoneId}&purpose=PARKING
```

Ý nghĩa:

- `MONTHLY`: chỉ trả slot tháng.
- `PARKING`: trả slot dùng cho xe vào bãi.
- Đặt trước thường hiện tại không cần chọn slot.

## 3. Luồng đặt trước thường

Chỉ ô tô được đặt trước.

Đặt trước thường không giữ slot cụ thể nữa. BE chỉ giữ “1 suất” trong zone thường.

Endpoint cũ vẫn dùng:

```http
POST /api/user/reservations
```

Payload giữ như cũ:

```json
{
  "vehicleId": 1,
  "zoneId": 10,
  "startTime": "2026-07-06T10:00:00.000Z",
  "endTime": "2026-07-06T12:00:00.000Z"
}
```

Response có thể có:

```json
{
  "reservedSlotId": null,
  "reservedSlotCode": null
}
```

Đây là đúng logic mới, không phải lỗi.

Khi xe vào đúng khung giờ, BE tự random slot trống trong `CAR_NORMAL`.

Quy tắc giờ:

- Được vào sớm tối đa 30 phút.
- Được trễ tối đa 20 phút.
- Ngoài khung này, booking bị hủy và xe xử lý như vãng lai nếu còn chỗ.

## 4. Luồng xe vãng lai

Xe vãng lai không có booking, không có vé tháng.

Khi check-in:

- Ô tô được random slot trong `CAR_NORMAL`.
- Xe máy được random slot khu xe máy.
- Không được dùng slot `CAR_MONTHLY`.

BE có bảo vệ capacity cho booking:

- Nếu còn slot trống nhưng slot đó đang cần giữ cho booking sắp tới/đang trong khung giờ, xe vãng lai sẽ bị chặn.

## 5. Luồng vé tháng

Chỉ ô tô được đăng ký vé tháng.

Vé tháng bắt buộc chọn:

- xe
- zone tháng `CAR_MONTHLY`
- slot tháng cụ thể

Endpoint cũ:

```http
POST /api/user/monthly-passes
```

Payload:

```json
{
  "vehicleId": 1,
  "slotId": 20,
  "startDate": "2026-07-06",
  "months": 1,
  "note": "Căn hộ A1205"
}
```

Sau khi tạo:

- pass status: `PENDING_PAYMENT`
- paymentStatus: `PENDING`
- slot status: `MONTHLY_HELD`

Sau khi manager/staff xác nhận thanh toán:

- pass status: `ACTIVE` hoặc `SCHEDULED`
- paymentStatus: `PAID`
- slot status: `MONTHLY_RESERVED`

Khi xe tháng vào:

- BE dùng đúng slot riêng của vé tháng.
- slot status: `MONTHLY_OCCUPIED`

Khi xe tháng ra:

- không tính phí lượt gửi.
- slot quay về `MONTHLY_RESERVED`.

## 6. Giá vé tháng ô tô

Giá chốt:

```text
500000 VND / tháng
```

BE đã chốt giá này khi tính vé tháng.

DB Supabase đã đồng bộ:

- `vehicle_types.monthly_rate = 500000` cho car.
- `pricing_policies.monthly_rate = 500000` cho car.

## 7. Field mới trong vé tháng

Response vé tháng có thêm:

```json
{
  "autoRenew": false
}
```

Ý nghĩa:

- `true`: tự động gia hạn.
- `false`: không tự động gia hạn.

Field này là additive, FE cũ không dùng vẫn không sao.

## 8. Thanh toán vé tháng - endpoint mới

Các endpoint này là mới, FE hiện tại chưa bắt buộc dùng.

### Online QR

```http
POST /api/user/monthly-passes/{id}/payment/online-qr
```

Body:

```json
{
  "autoRenew": true
}
```

Ghi chú:

- Online cho phép user chọn tự động gia hạn hoặc không.
- `autoRenew = true`: tự gia hạn.
- `autoRenew = false`: chỉ thanh toán kỳ này.

Response chính:

```json
{
  "paymentMethod": "ONLINE_QR",
  "paymentReference": "MTHQR-1-ABCDEFGH",
  "amount": 500000,
  "autoRenew": true,
  "qrContent": "MONTHLY_PASS|passId=1|ref=..."
}
```

FE có thể render QR từ `qrContent`.

### Tiền mặt

```http
POST /api/user/monthly-passes/{id}/payment/cash-bill
```

Body: không cần.

Ghi chú:

- Tiền mặt mặc định `autoRenew = true`.
- BE trả bill text và QR content để staff quét.

Response chính:

```json
{
  "paymentMethod": "CASH",
  "paymentReference": "MTHCASH-1-ABCDEFGH",
  "amount": 500000,
  "autoRenew": true,
  "qrContent": "MONTHLY_PASS|passId=1|ref=...",
  "billContent": "BILL VE THANG..."
}
```

### Manager/staff xác nhận thanh toán bằng QR

```http
POST /api/manager/monthly-passes/confirm-payment/scan
```

Body:

```json
{
  "qrContent": "MONTHLY_PASS|passId=1|ref=...",
  "paymentMethod": "CASH",
  "referenceCode": "MTHCASH-1-ABCDEFGH"
}
```

Kết quả:

- vé tháng chuyển `PAID`
- slot chuyển `MONTHLY_RESERVED`
- pass chuyển `ACTIVE` hoặc `SCHEDULED`

## 9. Endpoint cũ vẫn giữ

FE hiện tại vẫn dùng được:

```http
GET /api/user/monthly-passes
POST /api/user/monthly-passes
POST /api/manager/monthly-passes/{id}/confirm-payment
GET /api/user/reservations
POST /api/user/reservations
PATCH /api/user/reservations/{id}/cancel
```

## 10. DB đã thay đổi

Đã thêm cột:

```sql
auto_renew boolean default false
```

trong bảng:

```text
monthly_parking_passes
```

## 11. Test

BE đã chạy:

```bash
.\mvnw.cmd -q test
```

Kết quả: pass.

FE không bị chỉnh trong đợt note này.
