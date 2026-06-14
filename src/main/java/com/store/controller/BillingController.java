package com.store.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.store.model.ServiceRecord;
import com.store.model.ServiceRecordStatus;
import com.store.model.User;
import com.store.service.BillingCalculator;
import com.store.service.BillingResult;
import com.store.service.ServiceRecordRepository;
import com.store.service.UserRepository;

@Controller
@RequestMapping("/billing")
public class BillingController {

    @Value("${app.rental.rate-per-hour}")
    private int ratePerHour;

    @Autowired
    private ServiceRecordRepository serviceRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BillingCalculator billingCalculator;

    @GetMapping("/my")
    public String myBills(Authentication authentication, Model model) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        List<ServiceRecord> records = serviceRecordRepository.findByOwnerOrderByStartTimeDesc(user);

        LocalDateTime now = LocalDateTime.now();

        Map<Integer, Long> liveHoursMap = new HashMap<>();
        Map<Integer, BigDecimal> liveAmountMap = new HashMap<>();
        Map<Integer, Long> plannedHoursMap = new HashMap<>();
        Map<Integer, Boolean> overdueMap = new HashMap<>();

        BigDecimal totalOwed = BigDecimal.ZERO;

        for (ServiceRecord r : records) {
            LocalDateTime storageEndTime = r.getItem().getStorageEndTime();

            // planned hours from item storage window
            if (r.getItem().getStorageStartTime() != null && storageEndTime != null) {
                long planned = ChronoUnit.HOURS.between(r.getItem().getStorageStartTime(), storageEndTime);
                plannedHoursMap.put(r.getId(), Math.max(1, planned));
            }

            if (r.getStatus() == ServiceRecordStatus.ACTIVE) {
                BillingResult result = billingCalculator.calculate(r.getStartTime(), now, storageEndTime);
                liveHoursMap.put(r.getId(), result.getHours());
                liveAmountMap.put(r.getId(), result.getAmount());
                overdueMap.put(r.getId(), result.isOverdue());
                if (!r.isPaid()) {
                    totalOwed = totalOwed.add(result.getAmount());
                }
            } else {
                overdueMap.put(r.getId(), r.getEndTime() != null && storageEndTime != null
                        && r.getEndTime().isAfter(storageEndTime));
                if (!r.isPaid() && r.getTotalAmount() != null) {
                    totalOwed = totalOwed.add(r.getTotalAmount());
                }
            }
        }

        model.addAttribute("records", records);
        model.addAttribute("ratePerHour", ratePerHour);
        model.addAttribute("lateFeeMultiplier", billingCalculator.getLateFeeMultiplier());
        model.addAttribute("liveHoursMap", liveHoursMap);
        model.addAttribute("liveAmountMap", liveAmountMap);
        model.addAttribute("plannedHoursMap", plannedHoursMap);
        model.addAttribute("overdueMap", overdueMap);
        model.addAttribute("totalOwed", totalOwed);
        return "billing/MyBills";
    }
}
