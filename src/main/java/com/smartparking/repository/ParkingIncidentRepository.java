package com.smartparking.repository;

import com.smartparking.model.schemas.ParkingIncident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParkingIncidentRepository extends JpaRepository<ParkingIncident, Long> {

    List<ParkingIncident> findByStatus(String status);
}
