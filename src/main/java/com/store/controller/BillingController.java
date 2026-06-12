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

    @GetMapping("/my")
    public String myBills(Authentication authentication, Model model) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        List<ServiceRecord> records = serviceRecordRepository.findByOwnerOrderByStartTimeDesc(user);

        LocalDateTime now = LocalDateTime.now();

        Map<Integer, Long> liveHoursMap = new HashMap<>();
        Map<Integer, BigDecimal> liveAmountMap = new HashMap<>();
        Map<Integer, Long> plannedHoursMap = new HashMap<>();

        BigDecimal totalOwed = BigDecimal.ZERO;

        for (ServiceRecord r : records) {
            // planned hours from item storage window
            if (r.getItem().getStorageStartTime() != null && r.getItem().getStorageEndTime() != null) {
                long planned = ChronoUnit.HOURS.between(r.getItem().getStorageStartTime(), r.getItem().getStorageEndTime());
                plannedHoursMap.put(r.getId(), Math.max(1, planned));
            }

            if (r.getStatus() == ServiceRecordStatus.ACTIVE) {
                long hours = ChronoUnit.HOURS.between(r.getStartTime(), now);
                if (hours < 1) hours = 1;
                BigDecimal amount = BigDecimal.valueOf((long) ratePerHour * hours);
                liveHoursMap.put(r.getId(), hours);
                liveAmountMap.put(r.getId(), amount);
                totalOwed = totalOwed.add(amount);
            }
        }

        model.addAttribute("records", records);
        model.addAttribute("ratePerHour", ratePerHour);
        model.addAttribute("liveHoursMap", liveHoursMap);
        model.addAttribute("liveAmountMap", liveAmountMap);
        model.addAttribute("plannedHoursMap", plannedHoursMap);
        model.addAttribute("totalOwed", totalOwed);
        return "billing/MyBills";
    }
}
