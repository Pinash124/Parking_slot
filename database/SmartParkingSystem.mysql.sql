CREATE DATABASE IF NOT EXISTS SmartParking
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE SmartParking;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS LicensePlateScans;
DROP TABLE IF EXISTS SystemConfigs;
DROP TABLE IF EXISTS AuditLogs;
DROP TABLE IF EXISTS Notifications;
DROP TABLE IF EXISTS Feedbacks;
DROP TABLE IF EXISTS IncidentReports;
DROP TABLE IF EXISTS Violations;
DROP TABLE IF EXISTS MonthlyParkingPasses;
DROP TABLE IF EXISTS Transactions;
DROP TABLE IF EXISTS Payments;
DROP TABLE IF EXISTS ParkingSessions;
DROP TABLE IF EXISTS Reservations;
DROP TABLE IF EXISTS PricingPolicies;
DROP TABLE IF EXISTS Gates;
DROP TABLE IF EXISTS ParkingSlots;
DROP TABLE IF EXISTS Zones;
DROP TABLE IF EXISTS Floors;
DROP TABLE IF EXISTS Buildings;
DROP TABLE IF EXISTS Vehicles;
DROP TABLE IF EXISTS VehicleTypes;
DROP TABLE IF EXISTS Users;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE Users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    CONSTRAINT CK_Users_Role
        CHECK (role IN ('ADMIN', 'MANAGER', 'STAFF', 'CUSTOMER'))
) ENGINE=InnoDB;

CREATE TABLE VehicleTypes (
    vehicle_type_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    default_hourly_fee DECIMAL(18,2)
) ENGINE=InnoDB;

CREATE TABLE Vehicles (
    vehicle_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    vehicle_type_id BIGINT NOT NULL,
    plate_number VARCHAR(20) UNIQUE NOT NULL,
    brand VARCHAR(50),
    color VARCHAR(30),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    CONSTRAINT FK_Vehicles_Users
        FOREIGN KEY (user_id) REFERENCES Users(user_id),
    CONSTRAINT FK_Vehicles_VehicleTypes
        FOREIGN KEY (vehicle_type_id) REFERENCES VehicleTypes(vehicle_type_id)
) ENGINE=InnoDB;

CREATE TABLE Buildings (
    building_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    address VARCHAR(255),
    description VARCHAR(255),
    status VARCHAR(20) DEFAULT 'ACTIVE'
) ENGINE=InnoDB;

CREATE TABLE Floors (
    floor_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    building_id BIGINT NOT NULL,
    floor_name VARCHAR(50),
    floor_number INT,
    CONSTRAINT FK_Floors_Buildings
        FOREIGN KEY (building_id) REFERENCES Buildings(building_id)
) ENGINE=InnoDB;

CREATE TABLE Zones (
    zone_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    floor_id BIGINT NOT NULL,
    vehicle_type_id BIGINT NOT NULL,
    zone_name VARCHAR(50),
    CONSTRAINT FK_Zones_Floors
        FOREIGN KEY (floor_id) REFERENCES Floors(floor_id),
    CONSTRAINT FK_Zones_VehicleTypes
        FOREIGN KEY (vehicle_type_id) REFERENCES VehicleTypes(vehicle_type_id)
) ENGINE=InnoDB;

CREATE TABLE ParkingSlots (
    slot_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    zone_id BIGINT NOT NULL,
    slot_code VARCHAR(20) UNIQUE NOT NULL,
    status VARCHAR(20) DEFAULT 'AVAILABLE',
    CONSTRAINT FK_ParkingSlots_Zones
        FOREIGN KEY (zone_id) REFERENCES Zones(zone_id)
) ENGINE=InnoDB;

CREATE TABLE Gates (
    gate_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    building_id BIGINT NOT NULL,
    gate_name VARCHAR(50),
    gate_type VARCHAR(20),
    CONSTRAINT FK_Gates_Buildings
        FOREIGN KEY (building_id) REFERENCES Buildings(building_id)
) ENGINE=InnoDB;

CREATE TABLE PricingPolicies (
    policy_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_type_id BIGINT NOT NULL,
    policy_name VARCHAR(100),
    hourly_rate DECIMAL(18,2),
    daily_rate DECIMAL(18,2),
    lost_ticket_fee DECIMAL(18,2),
    overtime_fee DECIMAL(18,2),
    effective_from DATETIME(6),
    effective_to DATETIME(6),
    status VARCHAR(20),
    CONSTRAINT FK_PricingPolicies_VehicleTypes
        FOREIGN KEY (vehicle_type_id) REFERENCES VehicleTypes(vehicle_type_id)
) ENGINE=InnoDB;

CREATE TABLE Reservations (
    reservation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    vehicle_id BIGINT NOT NULL,
    zone_id BIGINT NOT NULL,
    start_time DATETIME(6),
    end_time DATETIME(6),
    status VARCHAR(20),
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT FK_Reservations_Users
        FOREIGN KEY (user_id) REFERENCES Users(user_id),
    CONSTRAINT FK_Reservations_Vehicles
        FOREIGN KEY (vehicle_id) REFERENCES Vehicles(vehicle_id),
    CONSTRAINT FK_Reservations_Zones
        FOREIGN KEY (zone_id) REFERENCES Zones(zone_id)
) ENGINE=InnoDB;

CREATE TABLE ParkingSessions (
    session_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NULL,
    vehicle_id BIGINT NOT NULL,
    slot_id BIGINT NOT NULL,
    entry_staff_id BIGINT NULL,
    exit_staff_id BIGINT NULL,
    entry_gate_id BIGINT NULL,
    exit_gate_id BIGINT NULL,
    ticket_code VARCHAR(50),
    entry_time DATETIME(6),
    exit_time DATETIME(6),
    parking_fee DECIMAL(18,2),
    penalty_fee DECIMAL(18,2),
    total_fee DECIMAL(18,2),
    status VARCHAR(20),
    CONSTRAINT FK_ParkingSessions_Reservations
        FOREIGN KEY (reservation_id) REFERENCES Reservations(reservation_id),
    CONSTRAINT FK_ParkingSessions_Vehicles
        FOREIGN KEY (vehicle_id) REFERENCES Vehicles(vehicle_id),
    CONSTRAINT FK_ParkingSessions_ParkingSlots
        FOREIGN KEY (slot_id) REFERENCES ParkingSlots(slot_id),
    CONSTRAINT FK_ParkingSessions_EntryStaff
        FOREIGN KEY (entry_staff_id) REFERENCES Users(user_id),
    CONSTRAINT FK_ParkingSessions_ExitStaff
        FOREIGN KEY (exit_staff_id) REFERENCES Users(user_id),
    CONSTRAINT FK_ParkingSessions_EntryGates
        FOREIGN KEY (entry_gate_id) REFERENCES Gates(gate_id),
    CONSTRAINT FK_ParkingSessions_ExitGates
        FOREIGN KEY (exit_gate_id) REFERENCES Gates(gate_id)
) ENGINE=InnoDB;

CREATE TABLE Payments (
    payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    amount DECIMAL(18,2),
    payment_method VARCHAR(30),
    payment_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    status VARCHAR(20),
    CONSTRAINT FK_Payments_ParkingSessions
        FOREIGN KEY (session_id) REFERENCES ParkingSessions(session_id)
) ENGINE=InnoDB;

CREATE TABLE Transactions (
    transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    reference_code VARCHAR(100),
    gateway VARCHAR(50),
    status VARCHAR(20),
    CONSTRAINT FK_Transactions_Payments
        FOREIGN KEY (payment_id) REFERENCES Payments(payment_id)
) ENGINE=InnoDB;

CREATE TABLE Violations (
    violation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    violation_type VARCHAR(100),
    description LONGTEXT,
    penalty_amount DECIMAL(18,2),
    status VARCHAR(20),
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT FK_Violations_ParkingSessions
        FOREIGN KEY (session_id) REFERENCES ParkingSessions(session_id)
) ENGINE=InnoDB;

CREATE TABLE IncidentReports (
    incident_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    reported_by BIGINT NULL,
    incident_type VARCHAR(100),
    description LONGTEXT,
    status VARCHAR(20),
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT FK_IncidentReports_ParkingSessions
        FOREIGN KEY (session_id) REFERENCES ParkingSessions(session_id),
    CONSTRAINT FK_IncidentReports_Users
        FOREIGN KEY (reported_by) REFERENCES Users(user_id)
) ENGINE=InnoDB;

CREATE TABLE Feedbacks (
    feedback_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    rating INT,
    content LONGTEXT,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT FK_Feedbacks_Users
        FOREIGN KEY (user_id) REFERENCES Users(user_id),
    CONSTRAINT FK_Feedbacks_ParkingSessions
        FOREIGN KEY (session_id) REFERENCES ParkingSessions(session_id)
) ENGINE=InnoDB;

CREATE TABLE Notifications (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200),
    message LONGTEXT,
    is_read BOOLEAN DEFAULT FALSE,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT FK_Notifications_Users
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
) ENGINE=InnoDB;

CREATE TABLE AuditLogs (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    action VARCHAR(255),
    entity_name VARCHAR(100),
    entity_id BIGINT,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT FK_AuditLogs_Users
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
) ENGINE=InnoDB;

CREATE TABLE SystemConfigs (
    config_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(150) NOT NULL UNIQUE,
    config_value TEXT,
    description VARCHAR(500),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE LicensePlateScans (
    scan_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    plate_number VARCHAR(20),
    image_url VARCHAR(500),
    confidence_score DECIMAL(5,2),
    scan_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT FK_LicensePlateScans_ParkingSessions
        FOREIGN KEY (session_id) REFERENCES ParkingSessions(session_id)
) ENGINE=InnoDB;

CREATE TABLE MonthlyParkingPasses (
    pass_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    vehicle_id BIGINT NOT NULL,
    vehicle_type_id BIGINT NOT NULL,
    months INT,
    monthly_rate DECIMAL(18,2),
    total_amount DECIMAL(18,2),
    start_date DATE,
    end_date DATE,
    status VARCHAR(20),
    note VARCHAR(500),
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT FK_MonthlyParkingPasses_Users
        FOREIGN KEY (user_id) REFERENCES Users(user_id),
    CONSTRAINT FK_MonthlyParkingPasses_Vehicles
        FOREIGN KEY (vehicle_id) REFERENCES Vehicles(vehicle_id),
    CONSTRAINT FK_MonthlyParkingPasses_VehicleTypes
        FOREIGN KEY (vehicle_type_id) REFERENCES VehicleTypes(vehicle_type_id)
) ENGINE=InnoDB;
