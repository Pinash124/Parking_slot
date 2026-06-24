package com.example.pricing_calculation.paymenttest;

import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class PaymentTestDataFactory {

    private final JdbcTemplate jdbc;

    PaymentTestDataFactory(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    PaymentBotSeed createSeed(String runPrefix, int index) {
        String code = (runPrefix + String.format(Locale.ROOT, "%03d", index)).toUpperCase(Locale.ROOT);
        String email = code.toLowerCase(Locale.ROOT) + "@example.com";
        Long userId = insertId(
                """
                INSERT INTO Users(full_name,email,phone,password_hash,status)
                OUTPUT INSERTED.user_id
                VALUES (?, ?, '0900000000', 'test-hash', 'ACTIVE')
                """,
                "Payment Bot " + code,
                email
        );
        Long vehicleTypeId = insertId(
                """
                INSERT INTO VehicleTypes(name,description,default_hourly_fee)
                OUTPUT INSERTED.vehicle_type_id
                VALUES (?, 'Payment load test vehicle', 12000)
                """,
                "VT-" + code
        );
        Long vehicleId = insertId(
                """
                INSERT INTO Vehicles(user_id,vehicle_type_id,plate_number,brand,color,status)
                OUTPUT INSERTED.vehicle_id
                VALUES (?, ?, ?, 'Test', 'Blue', 'ACTIVE')
                """,
                userId,
                vehicleTypeId,
                code
        );
        Long buildingId = insertId(
                """
                INSERT INTO Buildings(name,address,description,status)
                OUTPUT INSERTED.building_id
                VALUES (?, 'Payment test address', 'Payment test building', 'ACTIVE')
                """,
                "B-" + code
        );
        Long floorId = insertId(
                """
                INSERT INTO Floors(building_id,floor_name,floor_number)
                OUTPUT INSERTED.floor_id
                VALUES (?, ?, 1)
                """,
                buildingId,
                "F-" + code
        );
        Long zoneId = insertId(
                """
                INSERT INTO Zones(floor_id,vehicle_type_id,zone_name)
                OUTPUT INSERTED.zone_id
                VALUES (?, ?, ?)
                """,
                floorId,
                vehicleTypeId,
                "Z-" + code
        );
        Long slotId = insertId(
                """
                INSERT INTO ParkingSlots(zone_id,slot_code,status)
                OUTPUT INSERTED.slot_id
                VALUES (?, ?, 'AVAILABLE')
                """,
                zoneId,
                "S" + code
        );
        insertId(
                """
                INSERT INTO PricingPolicies(
                    vehicle_type_id,policy_name,hourly_rate,daily_rate,
                    lost_ticket_fee,overtime_fee,effective_from,effective_to,status)
                OUTPUT INSERTED.policy_id
                VALUES (?, ?, 15000, 200000, 50000, 7000,
                        DATEADD(day,-1,SYSDATETIME()),DATEADD(day,1,SYSDATETIME()),'ACTIVE')
                """,
                vehicleTypeId,
                "POL-" + code
        );
        return new PaymentBotSeed(code, email, userId, vehicleId, vehicleTypeId, zoneId, slotId);
    }

    void cleanup(String runPrefix) {
        String platePattern = runPrefix.toUpperCase(Locale.ROOT) + "%";
        String typedPattern = "VT-" + platePattern;
        jdbc.update("""
                DELETE T FROM Transactions T
                JOIN Payments P ON P.payment_id = T.payment_id
                JOIN ParkingSessions S ON S.session_id = P.session_id
                JOIN Vehicles V ON V.vehicle_id = S.vehicle_id
                WHERE V.plate_number LIKE ?
                """, platePattern);
        jdbc.update("""
                DELETE P FROM Payments P
                JOIN ParkingSessions S ON S.session_id = P.session_id
                JOIN Vehicles V ON V.vehicle_id = S.vehicle_id
                WHERE V.plate_number LIKE ?
                """, platePattern);
        jdbc.update("""
                DELETE S FROM ParkingSessions S
                JOIN Vehicles V ON V.vehicle_id = S.vehicle_id
                WHERE V.plate_number LIKE ?
                """, platePattern);
        jdbc.update("""
                DELETE R FROM Reservations R
                JOIN Vehicles V ON V.vehicle_id = R.vehicle_id
                WHERE V.plate_number LIKE ?
                """, platePattern);
        jdbc.update("""
                DELETE FROM PricingPolicies
                WHERE vehicle_type_id IN (
                    SELECT vehicle_type_id FROM VehicleTypes WHERE name LIKE ?)
                """, typedPattern);
        jdbc.update("DELETE FROM ParkingSlots WHERE slot_code LIKE ?", "S" + platePattern);
        jdbc.update("DELETE FROM Zones WHERE zone_name LIKE ?", "Z-" + platePattern);
        jdbc.update("""
                DELETE F FROM Floors F
                JOIN Buildings B ON B.building_id = F.building_id
                WHERE B.name LIKE ?
                """, "B-" + platePattern);
        jdbc.update("DELETE FROM Buildings WHERE name LIKE ?", "B-" + platePattern);
        jdbc.update("DELETE FROM Vehicles WHERE plate_number LIKE ?", platePattern);
        jdbc.update("DELETE FROM VehicleTypes WHERE name LIKE ?", typedPattern);
        jdbc.update("DELETE FROM Users WHERE email LIKE ?", runPrefix.toLowerCase(Locale.ROOT) + "%@example.com");
    }

    int countMarkers(String runPrefix) {
        String platePattern = runPrefix.toUpperCase(Locale.ROOT) + "%";
        return jdbc.queryForObject(
                """
                SELECT
                    (SELECT COUNT(*) FROM Users WHERE email LIKE ?) +
                    (SELECT COUNT(*) FROM VehicleTypes WHERE name LIKE ?) +
                    (SELECT COUNT(*) FROM Vehicles WHERE plate_number LIKE ?) +
                    (SELECT COUNT(*) FROM Buildings WHERE name LIKE ?) +
                    (SELECT COUNT(*) FROM ParkingSlots WHERE slot_code LIKE ?)
                """,
                Integer.class,
                runPrefix.toLowerCase(Locale.ROOT) + "%@example.com",
                "VT-" + platePattern,
                platePattern,
                "B-" + platePattern,
                "S" + platePattern
        );
    }

    static String newRunPrefix(String kind) {
        return ("PT" + kind + UUID.randomUUID().toString().replace("-", "").substring(0, 6))
                .toUpperCase(Locale.ROOT);
    }

    private Long insertId(String sql, Object... arguments) {
        return jdbc.queryForObject(sql, Long.class, arguments);
    }
}
