# Parking Payment System Backend

Backend Spring Boot cho há»‡ thá»‘ng quáº£n lÃ½ bÃ£i Ä‘á»— xe, táº­p trung vÃ o cÃ¡c module Ä‘Æ°á»£c giao trong `PBMS_Sheet_Full.xlsx`: reservation, pricing, parking session, payment, WebSocket vÃ  dashboard.

## 1. Tráº¡ng thÃ¡i triá»ƒn khai

CÃ¡c chá»©c nÄƒng hiá»‡n Ä‘Ã£ cÃ³ trong repository:

- Äáº·t chá»—, tÃ¬m kiáº¿m, phÃª duyá»‡t vÃ  há»§y reservation.
- Check-in, táº¡o ticket, checkout vÃ  cáº­p nháº­t tráº¡ng thÃ¡i parking slot.
- TÃ­nh phÃ­ theo giá»/ngÃ y, phÃ­ máº¥t vÃ© vÃ  phÃ­ quÃ¡ giá».
- Thanh toÃ¡n Cash vÃ  VNPAY sandbox.
- Tra cá»©u checkout theo biá»ƒn sá»‘, theo dÃµi tráº¡ng thÃ¡i payment vÃ  thá»i háº¡n 15 phÃºt rá»i bÃ£i.
- XÃ¡c thá»±c payment táº¡i cá»•ng ra vÃ  tráº£ quyáº¿t Ä‘á»‹nh má»Ÿ barrier.
- LÆ°u, tÃ¬m kiáº¿m vÃ  tá»•ng há»£p lá»‹ch sá»­ giao dá»‹ch.
- PhÃ¡t sá»± kiá»‡n realtime báº±ng STOMP WebSocket.
- Dashboard tá»•ng há»£p reservation, session, slot, payment, doanh thu vÃ  transaction.
- Unit test, functional test, smoke bot vÃ  concurrent load test cho payment flow.

## 2. CÃ´ng nghá»‡ vÃ  mÃ´i trÆ°á»ng

| ThÃ nh pháº§n | CÃ´ng nghá»‡ |
|---|---|
| Runtime | Java 17+ |
| Framework | Spring Boot 4.1.0 |
| REST API | Spring Web MVC |
| Persistence | Spring Data JPA / Hibernate |
| Database | Microsoft SQL Server hoáº·c MySQL / database `SmartParking` |
| Realtime | STOMP WebSocket |
| Build | Maven Wrapper |
| Test | JUnit 5, Mockito, Spring Boot Test, Java HTTP Client |

CÃ¡c dependency vÃ  plugin Ä‘Æ°á»£c khai bÃ¡o táº¡i [`pom.xml`](pom.xml). SQL Server lÃ  profile máº·c Ä‘á»‹nh; MySQL Ä‘Æ°á»£c há»— trá»£ qua profile `mysql`.

## 3. Khá»Ÿi Ä‘á»™ng nhanh

### Äiá»u kiá»‡n

- Java 17 trá»Ÿ lÃªn.
- SQL Server láº¯ng nghe táº¡i `localhost:1433`.
- Database `SmartParking` Ä‘Ã£ Ä‘Æ°á»£c khá»Ÿi táº¡o.
- TÃ i khoáº£n SQL hiá»‡n táº¡i: `sa`.

### Cháº¡y á»©ng dá»¥ng

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Server máº·c Ä‘á»‹nh cháº¡y táº¡i `http://localhost:8080`. Endpoint kiá»ƒm tra Ä‘Æ¡n giáº£n lÃ  `GET /hello`, Ä‘Æ°á»£c xá»­ lÃ½ bá»Ÿi [`HelloController.java`](src/main/java/com/example/pricing_calculation/HelloController.java).

### Swagger / OpenAPI

Há»‡ thá»‘ng dÃ¹ng Springdoc OpenAPI Ä‘á»ƒ tá»± Ä‘á»™ng quÃ©t toÃ n bá»™ REST controller. Sau khi á»©ng dá»¥ng khá»Ÿi Ä‘á»™ng, má»Ÿ:

- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- OpenAPI YAML: [http://localhost:8080/v3/api-docs.yaml](http://localhost:8080/v3/api-docs.yaml)

Test Ä‘Äƒng kÃ½ vÃ  Ä‘Äƒng nháº­p ngay trÃªn Swagger UI:

1. Má»Ÿ nhÃ³m `Authentication`.
2. Chá»n `POST /api/auth/register`, báº¥m **Try it out**, nháº­p JSON vÃ  báº¥m **Execute**.
3. Chá»n `POST /api/auth/login`, nháº­p email/password Ä‘Ã£ Ä‘Äƒng kÃ½ rá»“i sao chÃ©p `accessToken` tá»« response.
4. Báº¥m **Authorize** á»Ÿ Ä‘áº§u trang, nháº­p access token vÃ  xÃ¡c nháº­n.
5. Gá»i `POST /api/auth/logout`; Swagger tá»± gá»­i header `Authorization: Bearer <token>`.

Metadata OpenAPI vÃ  Bearer scheme náº±m táº¡i [`OpenApiConfig.java`](src/main/java/com/example/pricing_calculation/config/OpenApiConfig.java). Cháº¡y test tá»± Ä‘á»™ng cho Swagger:

```powershell
.\mvnw.cmd "-Dtest=SwaggerDocumentationTest" test
```

Káº¿t quáº£ kiá»ƒm tra gáº§n nháº¥t: `2` test, `0` failure, `0` error, `BUILD SUCCESS`.

### ÄÃ³ng gÃ³i

```powershell
.\mvnw.cmd -DskipTests package
java -jar target\pricing-calculation-0.0.1-SNAPSHOT.jar
```

Entry point cá»§a á»©ng dá»¥ng lÃ  [`PricingCalculationApplication.java`](src/main/java/com/example/pricing_calculation/PricingCalculationApplication.java).

### Cháº¡y trÃªn mÃ¡y chá»‰ cÃ³ MySQL

KhÃ´ng cáº§n cÃ i SSMS vÃ  khÃ´ng sá»­ dá»¥ng hai file MDF/LDF. YÃªu cáº§u MySQL 8 trá»Ÿ lÃªn.

1. Khá»Ÿi táº¡o schema báº±ng [`SmartParkingSystem.mysql.sql`](database/SmartParkingSystem.mysql.sql):

```powershell
cmd /c "mysql -u root -p < database\SmartParkingSystem.mysql.sql"
```

Script nÃ y táº¡o database náº¿u chÆ°a tá»“n táº¡i vÃ  tÃ¡i táº¡o 19 báº£ng. KhÃ´ng cháº¡y trÃªn database `SmartParking` Ä‘ang chá»©a dá»¯ liá»‡u cáº§n giá»¯ láº¡i.

2. Khai bÃ¡o tÃ i khoáº£n MySQL trong terminal:

```powershell
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="your-password"
```

3. Cháº¡y á»©ng dá»¥ng vá»›i profile MySQL:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=mysql"
```

Hoáº·c cháº¡y file jar:

```powershell
java -jar target\pricing-calculation-0.0.1-SNAPSHOT.jar --spring.profiles.active=mysql
```

Cáº¥u hÃ¬nh profile náº±m táº¡i [`application-mysql.properties`](src/main/resources/application-mysql.properties). CÃ³ thá»ƒ ghi Ä‘Ã¨ toÃ n bá»™ JDBC URL báº±ng biáº¿n `MYSQL_URL`.

### Táº¡o ZIP Ä‘á»ƒ gá»­i Ä‘i

KhÃ´ng zip trá»±c tiáº¿p MDF/LDF khi SQL Server Ä‘ang cháº¡y. DÃ¹ng script sau Ä‘á»ƒ táº¡o gÃ³i portable bÃªn ngoÃ i project:

```powershell
.\package-portable.ps1
```

GÃ³i ZIP tá»± Ä‘á»™ng loáº¡i `target`, `SmartParking.mdf` vÃ  `SmartParking_log.ldf`, nhÆ°ng váº«n chá»©a source, schema SQL Server vÃ  schema MySQL. NgÆ°á»i nháº­n dÃ¹ng MySQL chá»‰ cáº§n lÃ m theo má»¥c phÃ­a trÃªn.

## 4. Kiáº¿n trÃºc xá»­ lÃ½

```mermaid
flowchart LR
    Client["Web / Mobile / Test Bot"] --> Controller["REST Controller"]
    Controller --> Service["Business Service"]
    Service --> Repository["Spring Data Repository"]
    Repository --> DB["SQL Server - SmartParking"]
    Service --> Realtime["RealtimeEventService"]
    Realtime --> WS["STOMP /topic/*"]
```

Quy Æ°á»›c liÃªn káº¿t file:

1. `web/*Controller`: nháº­n HTTP request, parse DTO vÃ  tráº£ HTTP response.
2. `service/*Service`: kiá»ƒm tra dá»¯ liá»‡u vÃ  thá»±c hiá»‡n nghiá»‡p vá»¥.
3. `repository/*Repository`: truy váº¥n entity báº±ng Spring Data JPA.
4. `domain/*`: Ã¡nh xáº¡ entity tá»›i báº£ng SQL Server.
5. `dto/*`: contract request/response, khÃ´ng lÆ°u trá»±c tiáº¿p vÃ o database.

Lá»—i nghiá»‡p vá»¥ Ä‘Æ°á»£c nÃ©m báº±ng [`BadRequestException.java`](src/main/java/com/example/pricing_calculation/service/BadRequestException.java) hoáº·c [`ResourceNotFoundException.java`](src/main/java/com/example/pricing_calculation/service/ResourceNotFoundException.java), sau Ä‘Ã³ Ä‘Æ°á»£c chuáº©n hÃ³a thÃ nh HTTP 400/404 bá»Ÿi [`ApiExceptionHandler.java`](src/main/java/com/example/pricing_calculation/web/ApiExceptionHandler.java).

## 5. Database

### Nguá»“n khá»Ÿi táº¡o

- Script khá»Ÿi táº¡o: [`database/SmartParkingSystem.sql`](database/SmartParkingSystem.sql).
- Database logic: `SmartParking` trÃªn SQL Server Express táº¡i `localhost:1433`.
- File dá»¯ liá»‡u: `D:\New folder\Parking Payment System\database\SmartParking.mdf`.
- File transaction log: `D:\New folder\Parking Payment System\database\SmartParking_log.ldf`.
- Cáº¥u hÃ¬nh káº¿t ná»‘i: [`application.properties`](src/main/resources/application.properties).

`spring.jpa.hibernate.ddl-auto=none`, vÃ¬ váº­y Java khÃ´ng tá»± táº¡o hoáº·c thay Ä‘á»•i schema. Khi database chÆ°a tá»“n táº¡i, cháº¡y `SmartParkingSystem.sql` má»™t láº§n trong SSMS 2022 trÆ°á»›c khi khá»Ÿi Ä‘á»™ng backend.

Máº­t kháº©u SQL hiá»‡n Ä‘ang náº±m trá»±c tiáº¿p trong `application.properties`. Khi triá»ƒn khai tháº­t, cáº§n chuyá»ƒn username/password sang environment variables hoáº·c secret manager.

### Entity vÃ  báº£ng Ä‘Æ°á»£c sá»­ dá»¥ng

| Entity | Báº£ng | Quan há»‡ chÃ­nh | File |
|---|---|---|---|
| `UserAccount` | `Users` | Má»™t user cÃ³ nhiá»u vehicle/reservation | [`UserAccount.java`](src/main/java/com/example/pricing_calculation/domain/UserAccount.java) |
| `VehicleTypeEntity` | `VehicleTypes` | ÄÆ°á»£c dÃ¹ng bá»Ÿi vehicle, zone, pricing policy | [`VehicleTypeEntity.java`](src/main/java/com/example/pricing_calculation/domain/VehicleTypeEntity.java) |
| `Vehicle` | `Vehicles` | Thuá»™c user vÃ  vehicle type | [`Vehicle.java`](src/main/java/com/example/pricing_calculation/domain/Vehicle.java) |
| `Building` | `Buildings` | CÃ³ nhiá»u floor | [`Building.java`](src/main/java/com/example/pricing_calculation/domain/Building.java) |
| `Floor` | `Floors` | Thuá»™c building, cÃ³ nhiá»u zone | [`Floor.java`](src/main/java/com/example/pricing_calculation/domain/Floor.java) |
| `Zone` | `Zones` | Thuá»™c floor vÃ  giá»›i háº¡n theo vehicle type | [`Zone.java`](src/main/java/com/example/pricing_calculation/domain/Zone.java) |
| `ParkingSlot` | `ParkingSlots` | Thuá»™c zone; tráº¡ng thÃ¡i `AVAILABLE`, `RESERVED`, `OCCUPIED` | [`ParkingSlot.java`](src/main/java/com/example/pricing_calculation/domain/ParkingSlot.java) |
| `PricingPolicy` | `PricingPolicies` | ChÃ­nh sÃ¡ch phÃ­ theo vehicle type vÃ  thá»i gian hiá»‡u lá»±c | [`PricingPolicy.java`](src/main/java/com/example/pricing_calculation/domain/PricingPolicy.java) |
| `Reservation` | `Reservations` | LiÃªn káº¿t user, vehicle vÃ  zone | [`Reservation.java`](src/main/java/com/example/pricing_calculation/domain/Reservation.java) |
| `ParkingSession` | `ParkingSessions` | LiÃªn káº¿t reservation, vehicle vÃ  slot | [`ParkingSession.java`](src/main/java/com/example/pricing_calculation/domain/ParkingSession.java) |
| `Payment` | `Payments` | Thuá»™c má»™t parking session | [`Payment.java`](src/main/java/com/example/pricing_calculation/domain/Payment.java) |
| `TransactionHistory` | `Transactions` | Thuá»™c má»™t payment, lÆ°u gateway/reference/status | [`TransactionHistory.java`](src/main/java/com/example/pricing_calculation/domain/TransactionHistory.java) |

Script SQL cÃ²n táº¡o `Gates`, `Violations`, `IncidentReports`, `Feedbacks`, `Notifications`, `AuditLogs` vÃ  `LicensePlateScans`. Repository hiá»‡n chÆ°a cÃ³ entity/API cho cÃ¡c báº£ng nÃ y.

## 6. Chá»©c nÄƒng há»‡ thá»‘ng

### Authentication API (khÃ´ng cáº§n frontend)

Backend cung cáº¥p Ä‘áº§y Ä‘á»§ API Ä‘Äƒng kÃ½, Ä‘Äƒng nháº­p vÃ  Ä‘Äƒng xuáº¥t. CÃ³ thá»ƒ gá»i trá»±c tiáº¿p báº±ng Postman, `curl` hoáº·c báº¥t ká»³ á»©ng dá»¥ng web/mobile nÃ o; dá»± Ã¡n khÃ´ng phá»¥ thuá»™c vÃ o mÃ£ nguá»“n frontend.

| API | Chá»©c nÄƒng | Káº¿t quáº£ chÃ­nh |
|---|---|---|
| `POST /api/auth/register` | Táº¡o tÃ i khoáº£n `CUSTOMER` Ä‘ang hoáº¡t Ä‘á»™ng | HTTP 201 vÃ  thÃ´ng tin tÃ i khoáº£n, khÃ´ng tráº£ máº­t kháº©u |
| `POST /api/auth/login` | Kiá»ƒm tra email/máº­t kháº©u | Bearer access token cÃ³ hiá»‡u lá»±c 8 giá» |
| `POST /api/auth/logout` | Thu há»“i access token hiá»‡n táº¡i | XÃ³a phiÃªn Ä‘Äƒng nháº­p phÃ­a backend |

VÃ­ dá»¥ Ä‘Äƒng kÃ½:

```powershell
$body = @{
    fullName = "Nguyen Van A"
    email = "customer@example.com"
    phone = "0901234567"
    password = "safe-password-123"
} | ConvertTo-Json

Invoke-RestMethod -Method Post `
    -Uri "http://localhost:8080/api/auth/register" `
    -ContentType "application/json" `
    -Body $body
```

VÃ­ dá»¥ Ä‘Äƒng nháº­p vÃ  Ä‘Äƒng xuáº¥t:

```powershell
$loginBody = @{
    email = "customer@example.com"
    password = "safe-password-123"
} | ConvertTo-Json

$login = Invoke-RestMethod -Method Post `
    -Uri "http://localhost:8080/api/auth/login" `
    -ContentType "application/json" `
    -Body $loginBody

Invoke-RestMethod -Method Post `
    -Uri "http://localhost:8080/api/auth/logout" `
    -Headers @{ Authorization = "Bearer $($login.accessToken)" }
```

Email Ä‘Æ°á»£c chuáº©n hÃ³a vá» chá»¯ thÆ°á»ng; password dÃ i 8â€“128 kÃ½ tá»± vÃ  chá»‰ Ä‘Æ°á»£c lÆ°u dÆ°á»›i dáº¡ng PBKDF2-SHA256 kÃ¨m salt. Backend tá»± gÃ¡n `status=ACTIVE` vÃ  `role=CUSTOMER`, nÃªn client khÃ´ng thá»ƒ tá»± cáº¥p quyá»n cho mÃ¬nh. PhiÃªn Ä‘Äƒng nháº­p hiá»‡n Ä‘Æ°á»£c giá»¯ trong bá»™ nhá»› vÃ  sáº½ bá»‹ xÃ³a khi á»©ng dá»¥ng khá»Ÿi Ä‘á»™ng láº¡i.

### 6.1 Reservation

| API | Chá»©c nÄƒng |
|---|---|
| `POST /api/reservations` | Táº¡o reservation vÃ  tá»± Ä‘áº·t tráº¡ng thÃ¡i `APPROVED` |
| `GET /api/reservations` | Lá»c theo user, vehicle, zone, status vÃ  khoáº£ng thá»i gian |
| `GET /api/reservations/{id}` | Xem chi tiáº¿t |
| `PATCH /api/reservations/{id}/approve` | PhÃª duyá»‡t |
| `PATCH /api/reservations/{id}/cancel` | Há»§y |

File xá»­ lÃ½ vÃ  liÃªn káº¿t:

- [`ReservationController.java`](src/main/java/com/example/pricing_calculation/web/ReservationController.java) Ä‘á»‹nh nghÄ©a endpoint.
- [`ReservationService.java`](src/main/java/com/example/pricing_calculation/service/ReservationService.java) kiá»ƒm tra user/vehicle/zone, quyá»n sá»Ÿ há»¯u vehicle vÃ  sá»©c chá»©a zone.
- [`ReservationRepository.java`](src/main/java/com/example/pricing_calculation/repository/ReservationRepository.java) Ä‘áº¿m reservation trÃ¹ng thá»i gian.
- `UserAccountRepository`, `VehicleRepository`, `ZoneRepository` cung cáº¥p dá»¯ liá»‡u tham chiáº¿u.
- [`ParkingSlotRepository.java`](src/main/java/com/example/pricing_calculation/repository/ParkingSlotRepository.java) cung cáº¥p tá»•ng sá»‘ slot Ä‘á»ƒ kiá»ƒm tra capacity.
- [`Reservation.java`](src/main/java/com/example/pricing_calculation/domain/Reservation.java) Ã¡nh xáº¡ báº£ng `Reservations`.
- `ReservationCreateRequest`, `ReservationResponse`, `PageResponse` lÃ  DTO request/response.
- `RealtimeEventService` phÃ¡t sá»± kiá»‡n reservation tá»›i `/topic/reservations`.

### 6.2 Pricing calculation

| API | Chá»©c nÄƒng |
|---|---|
| `GET /api/pricing/estimate` | Æ¯á»›c tÃ­nh phÃ­ theo vehicle type, entry/exit time, máº¥t vÃ© vÃ  sá»‘ phÃºt quÃ¡ giá» |

Tham sá»‘: `vehicleTypeId`, `entryTime`, `exitTime`, `lostTicket`, `overtimeMinutes`.

File xá»­ lÃ½ vÃ  liÃªn káº¿t:

- [`PricingController.java`](src/main/java/com/example/pricing_calculation/web/PricingController.java) nháº­n query parameter.
- [`PricingService.java`](src/main/java/com/example/pricing_calculation/service/PricingService.java) chá»n policy cÃ²n hiá»‡u lá»±c vÃ  tÃ­nh phÃ­.
- [`PricingPolicyRepository.java`](src/main/java/com/example/pricing_calculation/repository/PricingPolicyRepository.java) tÃ¬m policy `ACTIVE` theo thá»i Ä‘iá»ƒm entry.
- [`VehicleTypeRepository.java`](src/main/java/com/example/pricing_calculation/repository/VehicleTypeRepository.java) cung cáº¥p default hourly fee khi khÃ´ng cÃ³ policy.
- [`PricingPolicy.java`](src/main/java/com/example/pricing_calculation/domain/PricingPolicy.java) vÃ  `VehicleTypeEntity` Ã¡nh xáº¡ dá»¯ liá»‡u giÃ¡.
- [`PricingQuoteResponse.java`](src/main/java/com/example/pricing_calculation/dto/PricingQuoteResponse.java) tráº£ chi tiáº¿t tá»«ng thÃ nh pháº§n phÃ­ vÃ  currency `VND`.

Quy táº¯c chÃ­nh:

- Thá»i gian Ä‘Æ°á»£c lÃ m trÃ²n lÃªn theo giá», tá»‘i thiá»ƒu má»™t giá».
- Tá»« 24 giá» trá»Ÿ lÃªn dÃ¹ng `dailyRate`, sau Ä‘Ã³ cá»™ng sá»‘ giá» dÆ° theo `hourlyRate`.
- `lostTicket=true` cá»™ng `lostTicketFee`.
- Overtime lÃ m trÃ²n lÃªn theo giá» rá»“i nhÃ¢n `overtimeFee`.

### 6.3 Parking session

| API | Chá»©c nÄƒng |
|---|---|
| `POST /api/parking-sessions/check-in` | Táº¡o session/ticket vÃ  chuyá»ƒn slot sang `OCCUPIED` |
| `GET /api/parking-sessions/{id}` | Xem session |
| `POST /api/parking-sessions/{id}/checkout` | TÃ­nh phÃ­, chuyá»ƒn session sang `CHECKED_OUT` vÃ  tráº£ slot vá» `AVAILABLE` |

File xá»­ lÃ½ vÃ  liÃªn káº¿t:

- [`ParkingSessionController.java`](src/main/java/com/example/pricing_calculation/web/ParkingSessionController.java) Ä‘á»‹nh nghÄ©a endpoint.
- [`ParkingSessionService.java`](src/main/java/com/example/pricing_calculation/service/ParkingSessionService.java) xÃ¡c thá»±c vehicle/slot/reservation, táº¡o ticket vÃ  gá»i `PricingService` khi checkout.
- `ParkingSessionRepository`, `ReservationRepository`, `VehicleRepository`, `ParkingSlotRepository` Ä‘á»c vÃ  ghi dá»¯ liá»‡u.
- [`ParkingSession.java`](src/main/java/com/example/pricing_calculation/domain/ParkingSession.java) liÃªn káº¿t `Reservation`, `Vehicle`, `ParkingSlot`.
- `SessionCheckInRequest`, `SessionCheckoutRequest`, `ParkingSessionResponse` lÃ  DTO.
- Sá»± kiá»‡n session/slot Ä‘Æ°á»£c phÃ¡t tá»›i `/topic/parking-sessions` vÃ  `/topic/parking-slots`.

### 6.4 Payment cÆ¡ báº£n

| API | Chá»©c nÄƒng |
|---|---|
| `POST /api/payments` | Táº¡o payment trá»±c tiáº¿p cho má»™t session |
| `GET /api/payments/{id}` | Xem payment |
| `PATCH /api/payments/{id}/status` | Cáº­p nháº­t tráº¡ng thÃ¡i vÃ  cÃ³ thá»ƒ táº¡o transaction gateway |

File xá»­ lÃ½ vÃ  liÃªn káº¿t:

- [`PaymentController.java`](src/main/java/com/example/pricing_calculation/web/PaymentController.java) nháº­n request.
- [`PaymentService.java`](src/main/java/com/example/pricing_calculation/service/PaymentService.java) láº¥y `totalFee` tá»« session khi request khÃ´ng truyá»n amount, lÆ°u payment vÃ  táº¡o transaction náº¿u cÃ³ gateway/reference.
- `PaymentRepository`, `ParkingSessionRepository`, `TransactionHistoryRepository` thá»±c hiá»‡n persistence.
- [`Payment.java`](src/main/java/com/example/pricing_calculation/domain/Payment.java) Ã¡nh xáº¡ `Payments`; `TransactionHistory` Ã¡nh xáº¡ `Transactions`.
- `PaymentCreateRequest`, `PaymentStatusUpdateRequest`, `PaymentResponse` lÃ  DTO.
- Payment event Ä‘Æ°á»£c phÃ¡t tá»›i `/topic/payments`.

### 6.5 Payment gateway

| API | Chá»©c nÄƒng |
|---|---|
| `POST /api/payment-gateways/cash` | Táº¡o payment `COMPLETED` ngay |
| `POST /api/payment-gateways/vnpay` | Create a `PENDING` payment and signed VNPay Sandbox URL; `qrContent` contains the same URL |
| `GET /api/payment-gateways/vnpay/return` | Verify VNPay return signature, amount and transaction before updating payment |
| `GET /api/payment-gateways/vnpay/ipn` | Verified VNPay IPN endpoint returning `RspCode` and `Message` |

File xá»­ lÃ½ vÃ  liÃªn káº¿t:

- [`PaymentGatewayController.java`](src/main/java/com/example/pricing_calculation/web/PaymentGatewayController.java) cung cáº¥p API gateway.
- [`PaymentGatewayService.java`](src/main/java/com/example/pricing_calculation/service/PaymentGatewayService.java) sinh reference code, gá»i `PaymentService`, Ä‘á»“ng bá»™ status cá»§a `Payments` vÃ  `Transactions`, Ä‘á»“ng thá»i táº¡o `exitDeadline = paymentTime + 15 phÃºt`.
- `PaymentRepository` vÃ  `TransactionHistoryRepository` tra cá»©u/cáº­p nháº­t payment cÃ¹ng transaction.
- `PaymentGatewayRequest`, `PaymentGatewayConfirmRequest`, `PaymentGatewayResponse` lÃ  DTO.

VNPay Sandbox uses merchant configuration from environment variables, HMAC-SHA512 request signing and verified Return/IPN callbacks.

### 6.6 Payment checkout vÃ  barrier

| API | Chá»©c nÄƒng |
|---|---|
| `POST /api/payment-checkout/prepare` | TÃ¬m session má»›i nháº¥t theo biá»ƒn sá»‘; náº¿u Ä‘ang `ACTIVE` thÃ¬ checkout vÃ  tÃ­nh phÃ­ |
| `GET /api/payment-checkout/sessions/{sessionId}/status` | Tráº£ payment status, `paid`, `paidAt`, `exitDeadline` vÃ  cá»­a sá»• 15 phÃºt |
| `POST /api/payment-checkout/validate-exit` | Kiá»ƒm tra xe Ä‘Ã£ tráº£ tiá»n vÃ  cÃ²n trong thá»i háº¡n Ä‘á»ƒ quyáº¿t Ä‘á»‹nh má»Ÿ barrier |

File xá»­ lÃ½ vÃ  liÃªn káº¿t:

- [`PaymentCheckoutController.java`](src/main/java/com/example/pricing_calculation/web/PaymentCheckoutController.java) cung cáº¥p ba endpoint.
- [`PaymentCheckoutService.java`](src/main/java/com/example/pricing_calculation/service/PaymentCheckoutService.java) tÃ¬m session báº±ng `ParkingSessionRepository`, gá»i `ParkingSessionService` Ä‘á»ƒ tÃ­nh phÃ­ vÃ  dÃ¹ng `PaymentRepository` Ä‘á»ƒ xÃ¡c minh payment.
- `PaymentCheckoutPrepareRequest`, `PaymentCheckoutResponse`, `PaymentExitValidationRequest`, `PaymentExitValidationResponse` lÃ  DTO.
- Káº¿t quáº£ barrier lÃ  `OPEN_PAYMENT_VERIFIED`, `DENY_PAYMENT_REQUIRED` hoáº·c `DENY_EXIT_WINDOW_EXPIRED`.
- Quyáº¿t Ä‘á»‹nh Ä‘Æ°á»£c phÃ¡t realtime tá»›i `/topic/parking-sessions`.

Backend chá»‰ tráº£ quyáº¿t Ä‘á»‹nh `openBarrier`; chÆ°a tÃ­ch há»£p thiáº¿t bá»‹/barrier API tháº­t.

### 6.7 Transaction history

| API | Chá»©c nÄƒng |
|---|---|
| `GET /api/transaction-history` | TÃ¬m kiáº¿m cÃ³ phÃ¢n trang vÃ  filter |
| `GET /api/transaction-history/summary` | Tá»•ng há»£p sá»‘ lÆ°á»£ng, amount vÃ  tráº¡ng thÃ¡i |
| `GET /api/transaction-history/recent` | Láº¥y transaction gáº§n nháº¥t |
| `GET /api/transaction-history/code/{code}` | TÃ¬m theo reference code |
| `GET /api/transaction-history/{id}` | TÃ¬m theo ID |
| `POST /api/transaction-history` | Táº¡o transaction cho payment cÃ³ sáºµn |
| `PUT /api/transaction-history/{id}` | Cáº­p nháº­t transaction |
| `PATCH /api/transaction-history/{id}/status` | Äá»•i tráº¡ng thÃ¡i |
| `DELETE /api/transaction-history/{id}` | XÃ³a transaction |

Filter há»— trá»£ `keyword`, `type`, `status`, `paymentMethod`, `licensePlate`, `reservationCode`, thá»i gian, khoáº£ng amount, paging vÃ  sorting.

- [`TransactionHistoryController.java`](src/main/java/com/example/pricing_calculation/web/TransactionHistoryController.java) Ä‘á»‹nh nghÄ©a API.
- [`TransactionHistoryService.java`](src/main/java/com/example/pricing_calculation/service/TransactionHistoryService.java) dá»±ng JPA specification vÃ  summary.
- [`TransactionHistoryRepository.java`](src/main/java/com/example/pricing_calculation/repository/TransactionHistoryRepository.java) há»— trá»£ CRUD, specification, reference lookup vÃ  recent query.
- `TransactionHistory` liÃªn káº¿t tá»›i `Payment`, sau Ä‘Ã³ láº§n theo `ParkingSession`, `Vehicle`, `Reservation` vÃ  `UserAccount` Ä‘á»ƒ dá»±ng response.
- `TransactionStatus`, `TransactionType`, `PaymentMethod` Ä‘á»‹nh nghÄ©a cÃ¡c enum há»— trá»£ filter.

### 6.8 Realtime WebSocket

- STOMP endpoint: `/ws`.
- Broker prefix: `/topic`.
- Application prefix: `/app`.
- Topic hiá»‡n dÃ¹ng: `/topic/reservations`, `/topic/parking-sessions`, `/topic/parking-slots`, `/topic/payments`.

[`WebSocketConfig.java`](src/main/java/com/example/pricing_calculation/config/WebSocketConfig.java) cáº¥u hÃ¬nh broker; [`RealtimeEventService.java`](src/main/java/com/example/pricing_calculation/service/RealtimeEventService.java) Ä‘Ã³ng gÃ³i payload báº±ng `WebSocketEvent` vÃ  phÃ¡t tá»« cÃ¡c service nghiá»‡p vá»¥.

`setAllowedOriginPatterns("*")` phÃ¹ há»£p mÃ´i trÆ°á»ng phÃ¡t triá»ƒn nhÆ°ng cáº§n giá»›i háº¡n domain khi triá»ƒn khai production.

### 6.9 Dashboard

| API | Chá»©c nÄƒng |
|---|---|
| `GET /api/dashboard/overview` | Tráº£ sá»‘ liá»‡u tá»•ng quan hiá»‡n táº¡i |

Response gá»“m tá»•ng/pending/approved reservations, active sessions, available/occupied/reserved slots, pending/completed payments, doanh thu hÃ´m nay vÃ  tá»•ng transactions.

- [`DashboardController.java`](src/main/java/com/example/pricing_calculation/web/DashboardController.java) cung cáº¥p endpoint.
- [`DashboardService.java`](src/main/java/com/example/pricing_calculation/service/DashboardService.java) tá»•ng há»£p tá»« `ReservationRepository`, `ParkingSessionRepository`, `ParkingSlotRepository`, `PaymentRepository` vÃ  `TransactionHistoryRepository`.
- [`DashboardOverviewResponse.java`](src/main/java/com/example/pricing_calculation/dto/DashboardOverviewResponse.java) lÃ  contract response.

## 7. Authentication bot testing

[`AuthFlowBot.java`](src/test/java/com/example/pricing_calculation/authtest/AuthFlowBot.java) dÃ¹ng Java HTTP Client gá»i REST API tháº­t trÃªn má»™t cá»•ng ngáº«u nhiÃªn. Bot tá»± táº¡o email duy nháº¥t vÃ  kiá»ƒm tra toÃ n bá»™ chuá»—i sau:

| BÆ°á»›c | API | Káº¿t quáº£ báº¯t buá»™c |
|---|---|---|
| ÄÄƒng kÃ½ | `POST /api/auth/register` | HTTP 201, `ACTIVE`, role `CUSTOMER`, response khÃ´ng lá»™ password |
| ÄÄƒng nháº­p sai | `POST /api/auth/login` | HTTP 401 vÃ  khÃ´ng cáº¥p token |
| ÄÄƒng nháº­p Ä‘Ãºng | `POST /api/auth/login` | HTTP 200, tráº£ Bearer token vÃ  thá»i gian háº¿t háº¡n |
| ÄÄƒng xuáº¥t | `POST /api/auth/logout` | HTTP 200, token bá»‹ thu há»“i |
| DÃ¹ng láº¡i token | `POST /api/auth/logout` | HTTP 401, chá»©ng minh token cÅ© khÃ´ng cÃ²n hiá»‡u lá»±c |

[`AuthFlowBotTest.java`](src/test/java/com/example/pricing_calculation/authtest/AuthFlowBotTest.java) khá»Ÿi Ä‘á»™ng Spring Boot, controller, service vÃ  JPA vá»›i database H2 in-memory riÃªng. Test khÃ´ng cáº§n frontend, khÃ´ng cáº§n SQL Server vÃ  tá»± xÃ³a tÃ i khoáº£n bot sau khi hoÃ n táº¥t.

Cháº¡y riÃªng bot auth:

```powershell
.\mvnw.cmd "-Dtest=AuthFlowBotTest" test
```

Log cháº¡y thá»±c táº¿ gáº§n nháº¥t ngÃ y `2026-06-18`:

```text
[auth-bot] result=PASS durationMs=1384 register=201 invalidLogin=401 login=200 logout=200 reusedToken=401
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

ToÃ n bá»™ test suite sau khi thÃªm bot vÃ  Swagger: `24` test, `0` failure, `0` error, `2` payment smoke/load test Ä‘Æ°á»£c skip theo cáº¥u hÃ¬nh máº·c Ä‘á»‹nh; káº¿t quáº£ `BUILD SUCCESS`.

CÃ¡c unit test auth bá»• sung náº±m táº¡i [`AuthServiceTest.java`](src/test/java/com/example/pricing_calculation/service/AuthServiceTest.java), kiá»ƒm tra chuáº©n hÃ³a dá»¯ liá»‡u, hash máº­t kháº©u, email trÃ¹ng, credential sai vÃ  thu há»“i token.

## 8. Payment testing

Má»¥c nÃ y há»£p nháº¥t ná»™i dung trÆ°á»›c Ä‘Ã¢y cá»§a `PAYMENT_TESTING.md`. Pháº¡m vi test lÃ  backend payment; website chá»‰ Ä‘Æ°á»£c dÃ¹ng lÃ m tÃ i liá»‡u xÃ¡c Ä‘á»‹nh luá»“ng, khÃ´ng kiá»ƒm thá»­ UI.

### Functional test cases

| ID | TÃ¡c vá»¥ | Test method | Káº¿t quáº£ mong Ä‘á»£i |
|---|---|---|---|
| PAY-001 | Nháº­p biá»ƒn sá»‘ | `prepareByPlateChecksOutActiveSessionAndReturnsCalculatedFee` | TÃ¬m session vÃ  chuyá»ƒn `ACTIVE` sang `CHECKED_OUT` |
| PAY-002 | TÃ­nh phÃ­ giá»/máº¥t vÃ©/overtime | `calculatesHourlyLostTicketAndOvertimeFees` | TÃ­nh Ä‘Ãºng parking fee vÃ  penalty |
| PAY-003 | TÃ­nh phÃ­ ngÃ y | `appliesDailyRateThenRemainingHourlyRate` | DÃ¹ng daily rate vÃ  giá» cÃ²n láº¡i |
| PAY-004 | Cash | `cashCompletesImmediatelyAndReturnsExitDeadline` | `COMPLETED` vÃ  cÃ³ deadline 15 phÃºt |
| PAY-006 | VNPAY | `vnpayCreatesPendingPaymentUrlAndQrContent` | `PENDING`, URL vÃ  QR content |
| PAY-007 | Callback thÃ nh cÃ´ng | `successfulCallbackCompletesPaymentAndStartsFifteenMinuteWindow` | Payment/transaction thÃ nh `COMPLETED` |
| PAY-008 | Callback sai gateway | `callbackRejectsReferenceFromAnotherGateway` | Tá»« chá»‘i cáº­p nháº­t |
| PAY-009 | Tráº¡ng thÃ¡i payment | `completedPaymentStatusIncludesFifteenMinuteDeadline` | `paid=true`, deadline vÃ  window 15 phÃºt |
| PAY-010 | Xe ra Ä‘Ãºng háº¡n | `exitValidationOpensBarrierInsidePaymentWindow` | `openBarrier=true` |
| PAY-011 | Xe ra quÃ¡ háº¡n | `exitValidationRejectsExpiredPaymentWindow` | `DENY_EXIT_WINDOW_EXPIRED` |
| PAY-012 | Xe chÆ°a tráº£ tiá»n | `exitValidationRejectsUnpaidVehicle` | `DENY_PAYMENT_REQUIRED` |

CÃ¡c unit/functional test náº±m táº¡i:

- [`PricingServiceTest.java`](src/test/java/com/example/pricing_calculation/service/PricingServiceTest.java).
- [`PaymentGatewayServiceTest.java`](src/test/java/com/example/pricing_calculation/service/PaymentGatewayServiceTest.java).
- [`PaymentCheckoutServiceTest.java`](src/test/java/com/example/pricing_calculation/service/PaymentCheckoutServiceTest.java).
- [`PricingCalculationApplicationTests.java`](src/test/java/com/example/pricing_calculation/PricingCalculationApplicationTests.java).

Cháº¡y suite máº·c Ä‘á»‹nh:

```powershell
.\mvnw.cmd test
```

### Smoke bot

[`PaymentFlowBot.java`](src/test/java/com/example/pricing_calculation/paymenttest/PaymentFlowBot.java) dÃ¹ng Java HTTP Client gá»i API tháº­t qua controller/service/JPA/SQL Server. Má»™t bot thá»±c hiá»‡n reservation, check-in, tÃ­nh phÃ­, payment, callback, kiá»ƒm tra deadline vÃ  validate barrier.

[`PaymentFlowSmokeTest.java`](src/test/java/com/example/pricing_calculation/paymenttest/PaymentFlowSmokeTest.java) cháº¡y tuáº§n tá»± hai luá»“ng `CASH`, `VNPAY`.

```powershell
.\mvnw.cmd "-Dpayment.smoke=true" "-Dtest=PaymentFlowSmokeTest" test
```

### Load test

[`PaymentLoadTest.java`](src/test/java/com/example/pricing_calculation/paymenttest/PaymentLoadTest.java) dÃ¹ng `ExecutorService` vÃ  `CountDownLatch` Ä‘á»ƒ cháº¡y bot Ä‘á»“ng thá»i. Sá»‘ user tá»‘i Ä‘a Ä‘Æ°á»£c giá»›i háº¡n á»Ÿ 200.

```powershell
.\mvnw.cmd "-Dpayment.load=true" "-Dpayment.load.users=30" "-Dpayment.load.concurrency=10" "-Dpayment.load.p95-ms=15000" "-Dtest=PaymentLoadTest" test
```

Káº¿t quáº£ kiá»ƒm tra gáº§n nháº¥t trÃªn mÃ¡y local:

```text
[payment-load] users=30 concurrency=10 successful=30 totalMs=1047 p95Ms=796 flowsPerSecond=28.65
```

[`PaymentTestDataFactory.java`](src/test/java/com/example/pricing_calculation/paymenttest/PaymentTestDataFactory.java) táº¡o dá»¯ liá»‡u marker riÃªng trong SQL Server vÃ  xÃ³a `Transactions`, `Payments`, `ParkingSessions`, `Reservations` cÃ¹ng toÃ n bá»™ dá»¯ liá»‡u ná»n sau má»—i test. Smoke/load test Ä‘á»u assert khÃ´ng cÃ²n marker.

## 9. Cáº¥u trÃºc thÆ° má»¥c

```text
src/main/java/com/example/pricing_calculation/
|-- config/       WebSocket configuration
|-- domain/       JPA entities vÃ  enums
|-- dto/          Request/response contracts
|-- repository/   Spring Data JPA access
|-- service/      Business logic
`-- web/          REST controllers vÃ  exception handler

src/test/java/com/example/pricing_calculation/
|-- authtest/     HTTP bot cho register, login vÃ  logout
|-- service/      Unit/functional tests
`-- paymenttest/  HTTP bot, smoke test, load test vÃ  test data factory
```

## 10. Luá»“ng payment hoÃ n chá»‰nh

```mermaid
sequenceDiagram
    participant User as Driver/Test Bot
    participant API as Spring REST API
    participant Pricing as PricingService
    participant Gateway as PaymentGatewayService
    participant DB as SQL Server
    participant WS as WebSocket

    User->>API: Create reservation and check-in
    API->>DB: Save reservation/session and occupy slot
    User->>API: Prepare checkout by license plate
    API->>Pricing: Calculate parking fee
    Pricing->>DB: Read active pricing policy
    API->>DB: Save checkout fee and release slot
    User->>Gateway: Select Cash/VNPAY
    Gateway->>DB: Save payment and transaction
    Gateway->>WS: Publish payment event
    User->>Gateway: Confirm online payment
    Gateway->>DB: Mark payment/transaction completed
    User->>API: Validate vehicle exit
    API-->>User: OPEN_PAYMENT_VERIFIED or deny decision
```


