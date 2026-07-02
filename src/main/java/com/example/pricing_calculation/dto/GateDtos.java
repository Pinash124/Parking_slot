package com.example.pricing_calculation.dto;

import com.example.pricing_calculation.domain.Gate;

public final class GateDtos {

    private GateDtos() {
    }

    public record GateRequest(Long buildingId, String gateName, String gateType) {
    }

    public record GateResponse(Long id, Long buildingId, String buildingName, String gateName, String gateType) {
        public static GateResponse from(Gate gate) {
            return new GateResponse(
                    gate.getId(),
                    gate.getBuilding().getId(),
                    gate.getBuilding().getName(),
                    gate.getGateName(),
                    gate.getGateType()
            );
        }
    }
}
