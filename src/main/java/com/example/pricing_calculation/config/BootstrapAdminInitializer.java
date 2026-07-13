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
    private static final BigDecimal MOTORBIKE_HOURLY = BigDecimal.valueOf(3000);
    private static final BigDecimal MOTORBIKE_DAILY = BigDecimal.valueOf(20000);
    private static final BigDecimal MOTORBIKE_MONTHLY = BigDecimal.valueOf(100000);
    private static final BigDecimal MOTORBIKE_LOST_TICKET = BigDecimal.valueOf(50000);
    private static final BigDecimal MOTORBIKE_OVERTIME = BigDecimal.valueOf(5000);

    private static final BigDecimal CAR_HOURLY = BigDecimal.valueOf(5000);
    private static final BigDecimal CAR_DAILY = BigDecimal.valueOf(50000);
    private static final BigDecimal CAR_MONTHLY = BigDecimal.valueOf(300000);
    private static final BigDecimal CAR_LOST_TICKET = BigDecimal.valueOf(100000);
    private static final BigDecimal CAR_OVERTIME = BigDecimal.valueOf(10000);

    private final UserAccountRepository users;
    private final PasswordHashService passwords;
    private final BuildingRepository buildings;
    private final FloorRepository floors;
    private final PaymentModuleVehicleTypeRepository vehicleTypes;
    private final PaymentModulePricingPolicyRepository pricingPolicies;
    private final ZoneRepository zones;
    private final String email;
    private final String password;

    public BootstrapAdminInitializer(
            UserAccountRepository users,
            PasswordHashService passwords,
            BuildingRepository buildings,
            FloorRepository floors,
            PaymentModuleVehicleTypeRepository vehicleTypes,
            PaymentModulePricingPolicyRepository pricingPolicies,
            ZoneRepository zones,
            @Value("${parking.bootstrap.admin-email:admin@smartparking.local}") String email,
            @Value("${parking.bootstrap.admin-password:Admin@12345}") String password) {
        this.users = users;
        this.passwords = passwords;
        this.buildings = buildings;
        this.floors = floors;
        this.vehicleTypes = vehicleTypes;
        this.pricingPolicies = pricingPolicies;
        this.zones = zones;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(String... args) {
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

        Building building;
        if (buildings.count() == 0) {
            building = new Building();
            building.setName("Tòa nhà A");
            building.setAddress("Khu đô thị Smart City");
            building.setStatus("ACTIVE");
            building = buildings.save(building);
        } else {
            building = buildings.findAll().get(0);
        }

        Floor firstFloor;
        Floor secondFloor;
        if (floors.count() == 0) {
            firstFloor = new Floor();
            firstFloor.setBuilding(building);
            firstFloor.setFloorName("Tầng 1");
            firstFloor.setFloorNumber(1);
            firstFloor = floors.save(firstFloor);

            secondFloor = new Floor();
            secondFloor.setBuilding(building);
            secondFloor.setFloorName("Tầng 2");
            secondFloor.setFloorNumber(2);
            secondFloor = floors.save(secondFloor);
        } else {
            List<Floor> allFloors = floors.findAll();
            firstFloor = allFloors.get(0);
            secondFloor = allFloors.size() > 1 ? allFloors.get(1) : firstFloor;
        }

        VehicleTypeEntity motorbike;
        VehicleTypeEntity car;
        if (vehicleTypes.count() == 0) {
            motorbike = new VehicleTypeEntity();
            car = new VehicleTypeEntity();
        } else {
            List<VehicleTypeEntity> allTypes = vehicleTypes.findAll();
            motorbike = allTypes.stream()
                    .filter(this::isMotorbikeType)
                    .findFirst()
                    .orElse(allTypes.get(0));
            car = allTypes.stream()
                    .filter(this::isCarType)
                    .findFirst()
                    .orElse(allTypes.get(allTypes.size() - 1));
        }

        normalizeMotorbikeType(motorbike);
        normalizeCarType(car);
        vehicleTypes.saveAll(List.of(motorbike, car));
        normalizePricingPolicies(motorbike, car);

        if (zones.count() == 0) {
            Zone carZoneFirstFloor = new Zone();
            carZoneFirstFloor.setFloor(firstFloor);
            carZoneFirstFloor.setVehicleType(car);
            carZoneFirstFloor.setZoneName("Khu A (Ô tô)");
            zones.save(carZoneFirstFloor);

            Zone motorbikeZoneFirstFloor = new Zone();
            motorbikeZoneFirstFloor.setFloor(firstFloor);
            motorbikeZoneFirstFloor.setVehicleType(motorbike);
            motorbikeZoneFirstFloor.setZoneName("Khu B (Xe 2 bánh)");
            zones.save(motorbikeZoneFirstFloor);

            Zone carZoneSecondFloor = new Zone();
            carZoneSecondFloor.setFloor(secondFloor);
            carZoneSecondFloor.setVehicleType(car);
            carZoneSecondFloor.setZoneName("Khu C (Ô tô)");
            zones.save(carZoneSecondFloor);

            Zone motorbikeZoneSecondFloor = new Zone();
            motorbikeZoneSecondFloor.setFloor(secondFloor);
            motorbikeZoneSecondFloor.setVehicleType(motorbike);
            motorbikeZoneSecondFloor.setZoneName("Khu D (Xe 2 bánh)");
            zones.save(motorbikeZoneSecondFloor);
        }
    }

    private void normalizeMotorbikeType(VehicleTypeEntity type) {
        type.setName("Xe 2 bánh");
        type.setDescription("Xe hai bánh của cư dân");
        type.setDefaultHourlyFee(MOTORBIKE_HOURLY);
        type.setDailyRate(MOTORBIKE_DAILY);
        type.setMonthlyRate(MOTORBIKE_MONTHLY);
        type.setWheelCount(2);
    }

    private void normalizeCarType(VehicleTypeEntity type) {
        type.setName("Ô tô");
        type.setDescription("Xe bốn bánh của cư dân");
        type.setDefaultHourlyFee(CAR_HOURLY);
        type.setDailyRate(CAR_DAILY);
        type.setMonthlyRate(CAR_MONTHLY);
        type.setWheelCount(4);
    }

    private void normalizePricingPolicies(VehicleTypeEntity motorbike, VehicleTypeEntity car) {
        List<PaymentModulePricingPolicy> policies = pricingPolicies.findAll();
        if (policies == null) {
            return;
        }
        for (PaymentModulePricingPolicy policy : policies) {
            if (policy.getVehicleType() == null) {
                continue;
            }
            Long vehicleTypeId = policy.getVehicleType().getId();
            if (vehicleTypeId.equals(motorbike.getId())) {
                policy.setPolicyName("Chính sách xe 2 bánh");
                policy.setHourlyRate(MOTORBIKE_HOURLY);
                policy.setDailyRate(MOTORBIKE_DAILY);
                policy.setMonthlyRate(MOTORBIKE_MONTHLY);
                policy.setLostTicketFee(MOTORBIKE_LOST_TICKET);
                policy.setOvertimeFee(MOTORBIKE_OVERTIME);
            } else if (vehicleTypeId.equals(car.getId())) {
                policy.setPolicyName("Chính sách ô tô");
                policy.setHourlyRate(CAR_HOURLY);
                policy.setDailyRate(CAR_DAILY);
                policy.setMonthlyRate(CAR_MONTHLY);
                policy.setLostTicketFee(CAR_LOST_TICKET);
                policy.setOvertimeFee(CAR_OVERTIME);
            }
        }
        pricingPolicies.saveAll(policies);
    }

    private boolean isMotorbikeType(VehicleTypeEntity type) {
        if (type == null) {
            return false;
        }
        if (Integer.valueOf(2).equals(type.getWheelCount())) {
            return true;
        }
        String name = normalize(type.getName());
        return name.contains("MOTOR") || name.contains("BIKE") || name.contains("XE MAY") || name.contains("2 BANH");
    }

    private boolean isCarType(VehicleTypeEntity type) {
        if (type == null) {
            return false;
        }
        if (Integer.valueOf(4).equals(type.getWheelCount())) {
            return true;
        }
        String name = normalize(type.getName());
        return name.contains("CAR") || name.contains("OTO") || name.contains("O TO") || name.contains("4 BANH");
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase();
    }
}
