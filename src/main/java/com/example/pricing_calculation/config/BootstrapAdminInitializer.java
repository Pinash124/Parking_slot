package com.example.pricing_calculation.config;

import com.example.pricing_calculation.domain.*;
import com.example.pricing_calculation.repository.*;
import com.example.pricing_calculation.service.PasswordHashService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapAdminInitializer implements CommandLineRunner {
    private final UserAccountRepository users;
    private final PasswordHashService passwords;
    private final BuildingRepository buildings;
    private final FloorRepository floors;
    private final PaymentModuleVehicleTypeRepository vehicleTypes;
    private final ZoneRepository zones;
    private final String email;
    private final String password;

    public BootstrapAdminInitializer(
            UserAccountRepository users,
            PasswordHashService passwords,
            BuildingRepository buildings,
            FloorRepository floors,
            PaymentModuleVehicleTypeRepository vehicleTypes,
            ZoneRepository zones,
            @Value("${parking.bootstrap.admin-email:admin@smartparking.local}") String email,
            @Value("${parking.bootstrap.admin-password:Admin@12345}") String password) {
        this.users = users;
        this.passwords = passwords;
        this.buildings = buildings;
        this.floors = floors;
        this.vehicleTypes = vehicleTypes;
        this.zones = zones;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        // Seed Admin Account
        String normalizedEmail = email.trim().toLowerCase();
        if (!users.existsByEmailIgnoreCase(normalizedEmail)) {
            UserAccount admin = new UserAccount();
            admin.setFullName("System Administrator");
            admin.setEmail(normalizedEmail);
            admin.setPasswordHash(passwords.hash(password));
            admin.setStatus("ACTIVE");
            admin.setRole(UserRole.ADMINISTRATOR.code());
            admin.setCreatedAt(java.time.LocalDateTime.now());
            users.save(admin);
        }

        // Seed Building
        Building b = null;
        if (buildings.count() == 0) {
            b = new Building();
            b.setName("Tòa nhà A");
            b.setAddress("Khu đô thị Smart City");
            b.setStatus("ACTIVE");
            b = buildings.save(b);
        } else {
            b = buildings.findAll().get(0);
        }

        // Seed Floors
        Floor f1 = null;
        Floor f2 = null;
        if (floors.count() == 0) {
            f1 = new Floor();
            f1.setBuilding(b);
            f1.setFloorName("Tầng 1");
            f1.setFloorNumber(1);
            f1 = floors.save(f1);

            f2 = new Floor();
            f2.setBuilding(b);
            f2.setFloorName("Tầng 2");
            f2.setFloorNumber(2);
            f2 = floors.save(f2);
        } else {
            List<Floor> allFloors = floors.findAll();
            f1 = allFloors.get(0);
            if (allFloors.size() > 1) {
                f2 = allFloors.get(1);
            } else {
                f2 = f1;
            }
        }

        // Seed Vehicle Types
        VehicleTypeEntity motorbike = null;
        VehicleTypeEntity car = null;
        if (vehicleTypes.count() == 0) {
            motorbike = new VehicleTypeEntity();
            motorbike.setName("Xe máy");
            motorbike.setDescription("Xe hai bánh của cư dân");
            motorbike.setDefaultHourlyFee(BigDecimal.ZERO);
            motorbike.setDailyRate(BigDecimal.valueOf(10000));
            motorbike.setMonthlyRate(BigDecimal.valueOf(150000));
            motorbike = vehicleTypes.save(motorbike);

            car = new VehicleTypeEntity();
            car.setName("Ô tô");
            car.setDescription("Xe bốn bánh của cư dân");
            car.setDefaultHourlyFee(BigDecimal.ZERO);
            car.setDailyRate(BigDecimal.valueOf(50000));
            car.setMonthlyRate(BigDecimal.valueOf(500000));
            car = vehicleTypes.save(car);
        } else {
            List<VehicleTypeEntity> allTypes = vehicleTypes.findAll();
            motorbike = allTypes.stream()
                    .filter(t -> t.getName().contains("máy") || t.getName().contains("Motor"))
                    .findFirst()
                    .orElse(allTypes.get(0));
            car = allTypes.stream()
                    .filter(t -> t.getName().contains("tô") || t.getName().contains("Car"))
                    .findFirst()
                    .orElse(allTypes.get(allTypes.size() - 1));
        }

        // Seed Zones
        if (zones.count() == 0) {
            Zone zA = new Zone();
            zA.setFloor(f1);
            zA.setVehicleType(car);
            zA.setZoneName("Khu A (Ô tô)");
            zones.save(zA);

            Zone zB = new Zone();
            zB.setFloor(f1);
            zB.setVehicleType(motorbike);
            zB.setZoneName("Khu B (Xe máy)");
            zones.save(zB);

            Zone zC = new Zone();
            zC.setFloor(f2);
            zC.setVehicleType(car);
            zC.setZoneName("Khu C (Ô tô)");
            zones.save(zC);

            Zone zD = new Zone();
            zD.setFloor(f2);
            zD.setVehicleType(motorbike);
            zD.setZoneName("Khu D (Xe máy)");
            zones.save(zD);
        }
    }
}
