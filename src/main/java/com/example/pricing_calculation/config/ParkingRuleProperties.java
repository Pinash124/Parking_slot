package com.example.pricing_calculation.config;

import java.math.BigDecimal;
import java.time.LocalTime;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "parking.rules")
public class ParkingRuleProperties {

    private int reservationEarlyMinutes = 30;
    private int reservationLateMinutes = 20;
    private int tariffGraceMinutes = 10;
    private LocalTime dayStart = LocalTime.of(7, 0);
    private LocalTime nightStart = LocalTime.of(22, 0);
    private BigDecimal twoWheelDayTurn = BigDecimal.valueOf(8000);
    private BigDecimal fourWheelDayTurn = BigDecimal.valueOf(35000);
    private BigDecimal twoWheelNightHourly = BigDecimal.valueOf(3000);
    private BigDecimal fourWheelNightHourly = BigDecimal.valueOf(5000);

    public int getReservationEarlyMinutes() { return reservationEarlyMinutes; }
    public void setReservationEarlyMinutes(int value) { this.reservationEarlyMinutes = value; }
    public int getReservationLateMinutes() { return reservationLateMinutes; }
    public void setReservationLateMinutes(int value) { this.reservationLateMinutes = value; }
    public int getTariffGraceMinutes() { return tariffGraceMinutes; }
    public void setTariffGraceMinutes(int value) { this.tariffGraceMinutes = value; }
    public LocalTime getDayStart() { return dayStart; }
    public void setDayStart(LocalTime value) { this.dayStart = value; }
    public LocalTime getNightStart() { return nightStart; }
    public void setNightStart(LocalTime value) { this.nightStart = value; }
    public BigDecimal getTwoWheelDayTurn() { return twoWheelDayTurn; }
    public void setTwoWheelDayTurn(BigDecimal value) { this.twoWheelDayTurn = value; }
    public BigDecimal getFourWheelDayTurn() { return fourWheelDayTurn; }
    public void setFourWheelDayTurn(BigDecimal value) { this.fourWheelDayTurn = value; }
    public BigDecimal getTwoWheelNightHourly() { return twoWheelNightHourly; }
    public void setTwoWheelNightHourly(BigDecimal value) { this.twoWheelNightHourly = value; }
    public BigDecimal getFourWheelNightHourly() { return fourWheelNightHourly; }
    public void setFourWheelNightHourly(BigDecimal value) { this.fourWheelNightHourly = value; }
}
