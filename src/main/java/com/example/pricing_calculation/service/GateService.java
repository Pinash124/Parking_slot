package com.example.pricing_calculation.service;

import com.example.pricing_calculation.domain.Building;
import com.example.pricing_calculation.domain.Gate;
import com.example.pricing_calculation.dto.GateDtos.GateRequest;
import com.example.pricing_calculation.dto.GateDtos.GateResponse;
import com.example.pricing_calculation.repository.BuildingRepository;
import com.example.pricing_calculation.repository.GateRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GateService {

    private final GateRepository gateRepository;
    private final BuildingRepository buildingRepository;

    public GateService(GateRepository gateRepository, BuildingRepository buildingRepository) {
        this.gateRepository = gateRepository;
        this.buildingRepository = buildingRepository;
    }

    @Transactional(readOnly = true)
    public List<GateResponse> list(Long buildingId) {
        List<Gate> gates = buildingId == null
                ? gateRepository.findAll().stream()
                        .sorted((left, right) -> {
                            int byBuilding = compareLong(left.getBuilding().getId(), right.getBuilding().getId());
                            if (byBuilding != 0) {
                                return byBuilding;
                            }
                            return compareText(left.getGateName(), right.getGateName());
                        })
                        .toList()
                : gateRepository.findByBuildingIdOrderByGateNameAsc(buildingId);
        return gates.stream().map(GateResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public GateResponse getById(Long id) {
        return GateResponse.from(findGate(id));
    }

    @Transactional
    public GateResponse save(Long id, GateRequest request) {
        if (request == null || request.buildingId() == null || request.gateName() == null || request.gateName().isBlank()) {
            throw new BadRequestException("buildingId and gateName are required");
        }
        Building building = buildingRepository.findById(request.buildingId())
                .orElseThrow(() -> new ResourceNotFoundException("Building not found: " + request.buildingId()));

        Gate gate = id == null
                ? new Gate()
                : findGate(id);
        gate.setBuilding(building);
        gate.setGateName(request.gateName());
        gate.setGateType(request.gateType());
        return GateResponse.from(gateRepository.save(gate));
    }

    @Transactional
    public void delete(Long id) {
        gateRepository.delete(findGate(id));
    }

    @Transactional(readOnly = true)
    public void validateGateCode(String gateCode, String expectedType) {
        if (gateCode == null || gateCode.isBlank()) {
            return;
        }
        gateRepository.findByGateNameIgnoreCase(gateCode.trim())
                .ifPresent(gate -> {
                    String gateType = gate.getGateType();
                    if (expectedType != null
                            && gateType != null
                            && !gateType.equalsIgnoreCase(expectedType)
                            && !"BOTH".equalsIgnoreCase(gateType)) {
                        throw new BadRequestException("Gate " + gateCode + " is not configured for " + expectedType.toLowerCase());
                    }
                });
    }

    private Gate findGate(Long id) {
        return gateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gate not found: " + id));
    }

    private int compareLong(Long left, Long right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return Long.compare(left, right);
    }

    private int compareText(String left, String right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareToIgnoreCase(right);
    }
}
