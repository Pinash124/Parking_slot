package com.example.pricing_calculation.service;

import com.example.pricing_calculation.config.ParkingRuleProperties;
import com.example.pricing_calculation.repository.SystemSettingRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TimeBandParkingFeeCalculator {

    public record Result(BigDecimal total, BigDecimal dayTurnRate, BigDecimal nightHourlyRate,
                         long dayTurns, long nightHours) { }

    private static final String DAY_START_KEY = "pricing.dayStart";
    private static final String NIGHT_START_KEY = "pricing.nightStart";

    private final ParkingRuleProperties rules;
    private final SystemSettingRepository settings;

    @Autowired
    public TimeBandParkingFeeCalculator(ParkingRuleProperties rules, SystemSettingRepository settings) {
        this.rules = rules;
        this.settings = settings;
    }

    public TimeBandParkingFeeCalculator(ParkingRuleProperties rules) {
        this(rules, null);
    }

    public Result calculate(int wheelCount, LocalDateTime entry, LocalDateTime exit) {
        return calculate(wheelCount, entry, exit, null, null);
    }

    public Result calculate(int wheelCount, LocalDateTime entry, LocalDateTime exit,
                            BigDecimal dayTurnRateOverride, BigDecimal nightHourlyRateOverride) {
        return calculate(wheelCount, entry, exit,
                dayTurnRateOverride, "PER_TURN", 1,
                nightHourlyRateOverride, "PER_HOUR", 1);
    }

    public Result calculate(int wheelCount, LocalDateTime entry, LocalDateTime exit,
                            BigDecimal dayRateOverride, String dayBillingMode, Integer dayBlockHours,
                            BigDecimal nightRateOverride, String nightBillingMode, Integer nightBlockHours) {
        if (entry == null || exit == null || !exit.isAfter(entry)) {
            throw new BadRequestException("exitTime must be after entryTime");
        }
        BigDecimal defaultDayRate = wheelCount <= 2 ? rules.getTwoWheelDayTurn() : rules.getFourWheelDayTurn();
        BigDecimal defaultNightRate = wheelCount <= 2 ? rules.getTwoWheelNightHourly() : rules.getFourWheelNightHourly();
        BigDecimal dayRate = positiveOrDefault(dayRateOverride, defaultDayRate);
        BigDecimal nightRate = positiveOrDefault(nightRateOverride, defaultNightRate);
        String dayMode = billingMode(dayBillingMode, "PER_TURN");
        String nightMode = billingMode(nightBillingMode, "PER_HOUR");
        int dayBlock = validBlockHours(dayBlockHours);
        int nightBlock = validBlockHours(nightBlockHours);
        LocalTime dayStartTime = timeSetting(DAY_START_KEY, rules.getDayStart());
        LocalTime nightStartTime = timeSetting(NIGHT_START_KEY, rules.getNightStart());
        long dayUnits = 0;
        long nightUnits = 0;

        LocalDate date = entry.toLocalDate().minusDays(1);
        LocalDate lastDate = exit.toLocalDate();
        while (!date.isAfter(lastDate)) {
            LocalDateTime dayStart = date.atTime(dayStartTime);
            LocalDateTime nightStart = date.atTime(nightStartTime);
            long dayMinutes = overlapMinutes(entry, exit, dayStart, nightStart);
            boolean crossedIntoDay = entry.isBefore(dayStart) && exit.isAfter(dayStart);
            long billableDayMinutes = crossedIntoDay && dayMinutes <= rules.getTariffGraceMinutes()
                    ? 0
                    : dayMinutes;
            if (billableDayMinutes > 0) {
                dayUnits += billedUnits(billableDayMinutes, dayMode, dayBlock);
            }

            LocalDateTime nightEnd = date.plusDays(1).atTime(dayStartTime);
            long nightMinutes = overlapMinutes(entry, exit, nightStart, nightEnd);
            boolean crossedIntoNight = entry.isBefore(nightStart) && exit.isAfter(nightStart);
            long billableNightMinutes = crossedIntoNight
                    ? Math.max(0, nightMinutes - rules.getTariffGraceMinutes())
                    : nightMinutes;
            if (billableNightMinutes > 0) {
                nightUnits += billedUnits(billableNightMinutes, nightMode, nightBlock);
            }
            date = date.plusDays(1);
        }

        BigDecimal total = dayRate.multiply(BigDecimal.valueOf(dayUnits))
                .add(nightRate.multiply(BigDecimal.valueOf(nightUnits)));
        return new Result(total, dayRate, nightRate, dayUnits, nightUnits);
    }

    private long overlapMinutes(LocalDateTime entry, LocalDateTime exit,
                                LocalDateTime bandStart, LocalDateTime bandEnd) {
        LocalDateTime start = entry.isAfter(bandStart) ? entry : bandStart;
        LocalDateTime end = exit.isBefore(bandEnd) ? exit : bandEnd;
        return end.isAfter(start) ? Duration.between(start, end).toMinutes() : 0;
    }

    private long divideCeiling(long value, long divisor) {
        return value <= 0 ? 0 : (value + divisor - 1) / divisor;
    }

    private BigDecimal positiveOrDefault(BigDecimal value, BigDecimal fallback) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0 ? value : fallback;
    }

    private long billedUnits(long minutes, String mode, int blockHours) {
        if (minutes <= 0) {
            return 0;
        }
        return switch (mode) {
            case "PER_TURN" -> 1;
            case "PER_BLOCK" -> divideCeiling(minutes, blockHours * 60L);
            default -> divideCeiling(minutes, 60);
        };
    }

    private String billingMode(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String mode = value.trim().toUpperCase();
        return switch (mode) {
            case "PER_TURN", "PER_HOUR", "PER_BLOCK" -> mode;
            default -> fallback;
        };
    }

    private int validBlockHours(Integer value) {
        return value != null && value > 0 ? value : 1;
    }

    private LocalTime timeSetting(String key, LocalTime fallback) {
        if (settings == null) {
            return fallback;
        }
        return settings.findById(key)
                .map(setting -> {
                    try {
                        return LocalTime.parse(setting.getValue());
                    } catch (Exception ignored) {
                        return fallback;
                    }
                })
                .orElse(fallback);
    }
}
