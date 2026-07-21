package com.example.pricing_calculation.service;

import com.example.pricing_calculation.config.ParkingRuleProperties;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class TimeBandParkingFeeCalculator {

    public record Result(BigDecimal total, BigDecimal dayTurnRate, BigDecimal nightHourlyRate,
                         long dayTurns, long nightHours) { }

    private final ParkingRuleProperties rules;

    public TimeBandParkingFeeCalculator(ParkingRuleProperties rules) {
        this.rules = rules;
    }

    public Result calculate(int wheelCount, LocalDateTime entry, LocalDateTime exit) {
        if (entry == null || exit == null || !exit.isAfter(entry)) {
            throw new BadRequestException("exitTime must be after entryTime");
        }
        BigDecimal dayRate = wheelCount <= 2 ? rules.getTwoWheelDayTurn() : rules.getFourWheelDayTurn();
        BigDecimal nightRate = wheelCount <= 2 ? rules.getTwoWheelNightHourly() : rules.getFourWheelNightHourly();
        long dayTurns = 0;
        long nightHours = 0;

        LocalDate date = entry.toLocalDate().minusDays(1);
        LocalDate lastDate = exit.toLocalDate();
        while (!date.isAfter(lastDate)) {
            LocalDateTime dayStart = date.atTime(rules.getDayStart());
            LocalDateTime nightStart = date.atTime(rules.getNightStart());
            long dayMinutes = overlapMinutes(entry, exit, dayStart, nightStart);
            boolean crossedIntoDay = entry.isBefore(dayStart) && exit.isAfter(dayStart);
            if (dayMinutes > 0 && !(crossedIntoDay && dayMinutes <= rules.getTariffGraceMinutes())) {
                dayTurns++;
            }

            LocalDateTime nightEnd = date.plusDays(1).atTime(rules.getDayStart());
            long nightMinutes = overlapMinutes(entry, exit, nightStart, nightEnd);
            boolean crossedIntoNight = entry.isBefore(nightStart) && exit.isAfter(nightStart);
            long billableNightMinutes = crossedIntoNight
                    ? Math.max(0, nightMinutes - rules.getTariffGraceMinutes())
                    : nightMinutes;
            if (billableNightMinutes > 0) {
                nightHours += divideCeiling(billableNightMinutes, 60);
            }
            date = date.plusDays(1);
        }

        BigDecimal total = dayRate.multiply(BigDecimal.valueOf(dayTurns))
                .add(nightRate.multiply(BigDecimal.valueOf(nightHours)));
        return new Result(total, dayRate, nightRate, dayTurns, nightHours);
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
}
