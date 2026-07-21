package com.example.pricing_calculation.service;
import com.example.pricing_calculation.domain.*;
import com.example.pricing_calculation.dto.ManagerReportResponse;
import com.example.pricing_calculation.dto.ManagerReportResponse.VehicleTypeSummary;
import com.example.pricing_calculation.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnifiedReportService {
    private final PaymentModuleParkingSessionRepository sessions;private final PaymentModuleParkingSlotRepository slots;private final PaymentRepository payments;private final PaymentModuleVehicleTypeRepository types;
    public UnifiedReportService(PaymentModuleParkingSessionRepository sessions,PaymentModuleParkingSlotRepository slots,PaymentRepository payments,PaymentModuleVehicleTypeRepository types){this.sessions=sessions;this.slots=slots;this.payments=payments;this.types=types;}
    @Transactional(readOnly=true) public ManagerReportResponse report(LocalDate from,LocalDate to){LocalDateTime start=(from==null?LocalDate.now().minusDays(30):from).atStartOfDay();LocalDateTime end=(to==null?LocalDate.now():to).plusDays(1).atStartOfDay();List<PaymentModuleParkingSession>slist=sessions.findAll();List<PaymentModuleParkingSession>range=slist.stream().filter(s->s.getEntryTime()!=null&&!s.getEntryTime().isBefore(start)&&s.getEntryTime().isBefore(end)).toList();long exits=slist.stream().filter(s->s.getExitTime()!=null&&!s.getExitTime().isBefore(start)&&s.getExitTime().isBefore(end)).count();long current=slist.stream().filter(s->List.of("ACTIVE","PAYMENT_PENDING").contains(up(s.getStatus()))).count();List<PaymentModuleParkingSlot>slotList=slots.findAll();Map<Integer,Long>peak=range.stream().collect(Collectors.groupingBy(s->s.getEntryTime().getHour(),TreeMap::new,Collectors.counting()));List<VehicleTypeSummary>byType=types.findAll().stream().map(t->{long en=range.stream().filter(s->s.getVehicle().getVehicleType().getId().equals(t.getId())).count();long ex=slist.stream().filter(s->s.getExitTime()!=null&&!s.getExitTime().isBefore(start)&&s.getExitTime().isBefore(end)&&s.getVehicle().getVehicleType().getId().equals(t.getId())).count();long parked=slist.stream().filter(s->List.of("ACTIVE","PAYMENT_PENDING").contains(up(s.getStatus()))&&s.getVehicle().getVehicleType().getId().equals(t.getId())).count();List<PaymentModuleParkingSlot>ts=slotList.stream().filter(s->s.getZone().getVehicleType().getId().equals(t.getId())).toList();BigDecimal rev=payments.findAll().stream().filter(p->List.of("COMPLETED","SUCCESS").contains(up(p.getStatus()))&&p.getPaymentTime()!=null&&!p.getPaymentTime().isBefore(start)&&p.getPaymentTime().isBefore(end)&&p.getSession().getVehicle().getVehicleType().getId().equals(t.getId())).map(Payment::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);return new VehicleTypeSummary(t.getId(),t.getName(),en,ex,parked,ts.size(),count(ts,"AVAILABLE"),rev);}).toList();long occupied=count(slotList,"OCCUPIED");return new ManagerReportResponse(start,end,range.size(),exits,current,payments.sumCompletedAmountBetween(start,end),slotList.size(),count(slotList,"AVAILABLE"),occupied,count(slotList,"RESERVED"),count(slotList,"MAINTENANCE"),count(slotList,"LOCKED"),slotList.isEmpty()?0:occupied*100.0/slotList.size(),peak,byType);}
    private long count(List<PaymentModuleParkingSlot>list,String status){return list.stream().filter(s->status.equalsIgnoreCase(s.getStatus())).count();}
    private String up(String s){return s==null?"":s.toUpperCase();}
}
