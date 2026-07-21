
CREATE DATABASE SmartParking;
GO
USE SmartParking;
GO

CREATE TABLE Users(
    user_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    full_name NVARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE());

CREATE TABLE VehicleTypes(
    vehicle_type_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description NVARCHAR(255),
    default_hourly_fee DECIMAL(18,2)
);

CREATE TABLE Vehicles(
    vehicle_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    vehicle_type_id BIGINT NOT NULL,
    plate_number VARCHAR(20) UNIQUE NOT NULL,
    brand VARCHAR(50),
    color VARCHAR(30),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    FOREIGN KEY(user_id) REFERENCES Users(user_id),
    FOREIGN KEY(vehicle_type_id) REFERENCES VehicleTypes(vehicle_type_id)
);

CREATE TABLE Buildings(
    building_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100),
    address NVARCHAR(255),
    description NVARCHAR(255),
    status VARCHAR(20) DEFAULT 'ACTIVE'
);

CREATE TABLE Floors(
    floor_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    building_id BIGINT NOT NULL,
    floor_name VARCHAR(50),
    floor_number INT,
    FOREIGN KEY(building_id) REFERENCES Buildings(building_id)
);

CREATE TABLE Zones(
    zone_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    floor_id BIGINT NOT NULL,
    vehicle_type_id BIGINT NOT NULL,
    zone_name VARCHAR(50),
    FOREIGN KEY(floor_id) REFERENCES Floors(floor_id),
    FOREIGN KEY(vehicle_type_id) REFERENCES VehicleTypes(vehicle_type_id)
);

CREATE TABLE ParkingSlots(
    slot_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    zone_id BIGINT NOT NULL,
    slot_code VARCHAR(20) UNIQUE NOT NULL,
    status VARCHAR(20) DEFAULT 'AVAILABLE',
    FOREIGN KEY(zone_id) REFERENCES Zones(zone_id)
);

CREATE TABLE Gates(
    gate_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    building_id BIGINT NOT NULL,
    gate_name VARCHAR(50),
    gate_type VARCHAR(20),
    FOREIGN KEY(building_id) REFERENCES Buildings(building_id)
);

CREATE TABLE PricingPolicies(
    policy_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    vehicle_type_id BIGINT NOT NULL,
    policy_name NVARCHAR(100),
    hourly_rate DECIMAL(18,2),
    daily_rate DECIMAL(18,2),
    lost_ticket_fee DECIMAL(18,2),
    overtime_fee DECIMAL(18,2),
    effective_from DATETIME2,
    effective_to DATETIME2,
    status VARCHAR(20),
    FOREIGN KEY(vehicle_type_id) REFERENCES VehicleTypes(vehicle_type_id)
);

CREATE TABLE Reservations(
    reservation_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    vehicle_id BIGINT NOT NULL,
    zone_id BIGINT NOT NULL,
    start_time DATETIME2,
    end_time DATETIME2,
    status VARCHAR(20),
    created_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY(user_id) REFERENCES Users(user_id),
    FOREIGN KEY(vehicle_id) REFERENCES Vehicles(vehicle_id),
    FOREIGN KEY(zone_id) REFERENCES Zones(zone_id)
);

CREATE TABLE ParkingSessions(
    session_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    reservation_id BIGINT NULL,
    vehicle_id BIGINT NOT NULL,
    slot_id BIGINT NOT NULL,
    entry_staff_id BIGINT NULL,
    exit_staff_id BIGINT NULL,
    entry_gate_id BIGINT NULL,
    exit_gate_id BIGINT NULL,
    ticket_code VARCHAR(50),
    entry_time DATETIME2,
    exit_time DATETIME2,
    parking_fee DECIMAL(18,2),
    penalty_fee DECIMAL(18,2),
    total_fee DECIMAL(18,2),
    status VARCHAR(20),
    FOREIGN KEY(reservation_id) REFERENCES Reservations(reservation_id),
    FOREIGN KEY(vehicle_id) REFERENCES Vehicles(vehicle_id),
    FOREIGN KEY(slot_id) REFERENCES ParkingSlots(slot_id),
    FOREIGN KEY(entry_staff_id) REFERENCES Users(user_id),
    FOREIGN KEY(exit_staff_id) REFERENCES Users(user_id),
    FOREIGN KEY(entry_gate_id) REFERENCES Gates(gate_id),
    FOREIGN KEY(exit_gate_id) REFERENCES Gates(gate_id)
);

CREATE TABLE Payments(
    payment_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    session_id BIGINT NOT NULL,
    amount DECIMAL(18,2),
    payment_method VARCHAR(30),
    payment_time DATETIME2 DEFAULT GETDATE(),
    status VARCHAR(20),
    FOREIGN KEY(session_id) REFERENCES ParkingSessions(session_id)
);

CREATE TABLE Transactions(
    transaction_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    reference_code VARCHAR(100),
    gateway VARCHAR(50),
    status VARCHAR(20),
    FOREIGN KEY(payment_id) REFERENCES Payments(payment_id)
);

CREATE TABLE Violations(
    violation_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    session_id BIGINT NOT NULL,
    violation_type VARCHAR(100),
    description NVARCHAR(MAX),
    penalty_amount DECIMAL(18,2),
    status VARCHAR(20),
    created_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY(session_id) REFERENCES ParkingSessions(session_id)
);

CREATE TABLE IncidentReports(
    incident_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    session_id BIGINT NOT NULL,
    reported_by BIGINT NULL,
    incident_type VARCHAR(100),
    description NVARCHAR(MAX),
    status VARCHAR(20),
    created_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY(session_id) REFERENCES ParkingSessions(session_id),
    FOREIGN KEY(reported_by) REFERENCES Users(user_id)
);

CREATE TABLE Feedbacks(
    feedback_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    rating INT,
    content NVARCHAR(MAX),
    created_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY(user_id) REFERENCES Users(user_id),
    FOREIGN KEY(session_id) REFERENCES ParkingSessions(session_id)
);

CREATE TABLE Notifications(
    notification_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title NVARCHAR(200),
    message NVARCHAR(MAX),
    is_read BIT DEFAULT 0,
    created_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY(user_id) REFERENCES Users(user_id)
);

CREATE TABLE AuditLogs(
    log_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    action VARCHAR(255),
    entity_name VARCHAR(100),
    entity_id BIGINT,
    created_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY(user_id) REFERENCES Users(user_id)
);

CREATE TABLE SystemConfigs(
    config_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    config_key VARCHAR(150) NOT NULL UNIQUE,
    config_value NVARCHAR(MAX),
    description NVARCHAR(500),
    updated_at DATETIME2 DEFAULT GETDATE()
);

CREATE TABLE LicensePlateScans(
    scan_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    session_id BIGINT NOT NULL,
    plate_number VARCHAR(20),
    image_url NVARCHAR(500),
    confidence_score DECIMAL(5,2),
    scan_time DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY(session_id) REFERENCES ParkingSessions(session_id)
);

CREATE TABLE MonthlyParkingPasses(
    pass_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    vehicle_id BIGINT NOT NULL,
    vehicle_type_id BIGINT NOT NULL,
    months INT,
    monthly_rate DECIMAL(18,2),
    total_amount DECIMAL(18,2),
    start_date DATE,
    end_date DATE,
    status VARCHAR(20),
    note NVARCHAR(500),
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY(user_id) REFERENCES Users(user_id),
    FOREIGN KEY(vehicle_id) REFERENCES Vehicles(vehicle_id),
    FOREIGN KEY(vehicle_type_id) REFERENCES VehicleTypes(vehicle_type_id)
);

ALTER TABLE Users
ADD role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER';

ALTER TABLE Users
ADD CONSTRAINT CK_Users_Role
CHECK (role IN ('ADMIN', 'MANAGER', 'STAFF', 'CUSTOMER'));
