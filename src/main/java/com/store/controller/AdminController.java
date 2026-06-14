package com.store.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.store.model.Item;
import com.store.model.ItemStatus;
import com.store.model.ServiceRecord;
import com.store.model.ServiceRecordStatus;
import com.store.service.BillingCalculator;
import com.store.service.BillingResult;
import com.store.service.ItemRepository;
import com.store.service.ItemSpecifications;
import com.store.service.ServiceRecordRepository;
import com.store.service.UserRepository;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final int PAGE_SIZE = 10;

    @Value("${app.rental.rate-per-hour}")
    private int ratePerHour;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ServiceRecordRepository serviceRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BillingCalculator billingCalculator;

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        List<ServiceRecord> records = serviceRecordRepository.findAllByOrderByStartTimeDesc();
        LocalDateTime now = LocalDateTime.now();

        long overdueRentalsCount = records.stream()
                .filter(r -> r.getStatus() == ServiceRecordStatus.ACTIVE)
                .filter(r -> r.getItem().getStorageEndTime() != null && now.isAfter(r.getItem().getStorageEndTime()))
                .count();

        model.addAttribute("pendingItemsCount", itemRepository.countByStatus(ItemStatus.PENDING));
        model.addAttribute("approvedItemsCount", itemRepository.countByStatus(ItemStatus.APPROVED));
        model.addAttribute("activeRentalsCount", serviceRecordRepository.countByStatus(ServiceRecordStatus.ACTIVE));
        model.addAttribute("overdueRentalsCount", overdueRentalsCount);
        model.addAttribute("pendingExtensionsCount", itemRepository.countByRequestedStorageEndTimeIsNotNull());
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalOutstanding", computeTotalOutstanding(records, now));
        return "admin/Dashboard";
    }

    @GetMapping("/items")
    public String showPendingItems(Model model,
                                    @RequestParam(required = false) String category,
                                    @RequestParam(required = false) String search,
                                    @RequestParam(defaultValue = "0") int page) {
        Specification<Item> spec = Specification.where(ItemSpecifications.hasStatus(ItemStatus.PENDING))
                .and(ItemSpecifications.hasCategory(category))
                .and(ItemSpecifications.nameContains(search));

        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("createdAt").descending());
        Page<Item> itemPage = itemRepository.findAll(spec, pageable);

        model.addAttribute("items", itemPage.getContent());
        model.addAttribute("currentPage", itemPage.getNumber());
        model.addAttribute("totalPages", itemPage.getTotalPages());
        model.addAttribute("totalElements", itemPage.getTotalElements());
        model.addAttribute("categories", itemRepository.findDistinctCategoriesByStatus(ItemStatus.PENDING));
        model.addAttribute("category", category);
        model.addAttribute("search", search);
        return "admin/PendingItems";
    }

    @PostMapping("/items/{id}/approve")
    public String approveItem(@PathVariable int id, Authentication authentication) {
        itemRepository.findById(id).ifPresent(item -> {
            item.setStatus(ItemStatus.APPROVED);
            item.setApprovedAt(LocalDateTime.now());
            userRepository.findByUsername(authentication.getName()).ifPresent(item::setApprovedBy);
            itemRepository.save(item);

            ServiceRecord record = new ServiceRecord();
            record.setItem(item);
            record.setOwner(item.getOwner());
            record.setStartTime(LocalDateTime.now());
            record.setStatus(ServiceRecordStatus.ACTIVE);
            serviceRecordRepository.save(record);
        });
        return "redirect:/admin/items";
    }

    @GetMapping("/extensions")
    public String showExtensionRequests(Model model) {
        List<Item> items = itemRepository.findByRequestedStorageEndTimeIsNotNull();
        model.addAttribute("items", items);
        return "admin/ExtensionRequests";
    }

    @PostMapping("/extensions/{id}/approve")
    public String approveExtension(@PathVariable int id) {
        itemRepository.findById(id).ifPresent(item -> {
            item.setStorageEndTime(item.getRequestedStorageEndTime());
            item.setRequestedStorageEndTime(null);
            itemRepository.save(item);
        });
        return "redirect:/admin/extensions";
    }

    @PostMapping("/extensions/{id}/reject")
    public String rejectExtension(@PathVariable int id) {
        itemRepository.findById(id).ifPresent(item -> {
            item.setRequestedStorageEndTime(null);
            itemRepository.save(item);
        });
        return "redirect:/admin/extensions";
    }

    @PostMapping("/items/{id}/reject")
    public String rejectItem(@PathVariable int id) {
        itemRepository.findById(id).ifPresent(item -> {
            item.setStatus(ItemStatus.REJECTED);
            item.setRejectedAt(LocalDateTime.now());
            itemRepository.save(item);

            serviceRecordRepository.findByItemIdAndStatus(item.getId(), ServiceRecordStatus.ACTIVE)
                    .ifPresent(record -> {
                        LocalDateTime end = LocalDateTime.now();
                        BillingResult result = billingCalculator.calculate(record.getStartTime(), end, item.getStorageEndTime());
                        record.setEndTime(end);
                        record.setTotalHours(result.getHours());
                        record.setTotalAmount(result.getAmount());
                        record.setStatus(ServiceRecordStatus.ENDED);
                        serviceRecordRepository.save(record);
                    });
        });
        return "redirect:/admin/items";
    }

    @GetMapping("/billing")
    public String showBilling(Model model) {
        List<ServiceRecord> records = serviceRecordRepository.findAllByOrderByStartTimeDesc();

        LocalDateTime now = LocalDateTime.now();
        Map<Integer, Long> liveHoursMap = new HashMap<>();
        Map<Integer, BigDecimal> liveAmountMap = new HashMap<>();
        Map<Integer, Boolean> overdueMap = new HashMap<>();

        for (ServiceRecord r : records) {
            LocalDateTime storageEndTime = r.getItem().getStorageEndTime();

            if (r.getStatus() == ServiceRecordStatus.ACTIVE) {
                BillingResult result = billingCalculator.calculate(r.getStartTime(), now, storageEndTime);
                liveHoursMap.put(r.getId(), result.getHours());
                liveAmountMap.put(r.getId(), result.getAmount());
                overdueMap.put(r.getId(), result.isOverdue());
            } else {
                overdueMap.put(r.getId(), r.getEndTime() != null && storageEndTime != null
                        && r.getEndTime().isAfter(storageEndTime));
            }
        }

        model.addAttribute("records", records);
        model.addAttribute("ratePerHour", ratePerHour);
        model.addAttribute("lateFeeMultiplier", billingCalculator.getLateFeeMultiplier());
        model.addAttribute("liveHoursMap", liveHoursMap);
        model.addAttribute("liveAmountMap", liveAmountMap);
        model.addAttribute("overdueMap", overdueMap);
        return "admin/Billing";
    }

    @PostMapping("/billing/{id}/mark-paid")
    public String markBillPaid(@PathVariable int id) {
        serviceRecordRepository.findById(id).ifPresent(record -> {
            record.setPaid(true);
            record.setPaidAt(LocalDateTime.now());
            serviceRecordRepository.save(record);
        });
        return "redirect:/admin/billing";
    }

    @PostMapping("/billing/{id}/mark-unpaid")
    public String markBillUnpaid(@PathVariable int id) {
        serviceRecordRepository.findById(id).ifPresent(record -> {
            record.setPaid(false);
            record.setPaidAt(null);
            serviceRecordRepository.save(record);
        });
        return "redirect:/admin/billing";
    }

    private BigDecimal computeTotalOutstanding(List<ServiceRecord> records, LocalDateTime now) {
        BigDecimal total = BigDecimal.ZERO;

        for (ServiceRecord r : records) {
            if (r.isPaid()) {
                continue;
            }

            if (r.getStatus() == ServiceRecordStatus.ACTIVE) {
                BillingResult result = billingCalculator.calculate(r.getStartTime(), now, r.getItem().getStorageEndTime());
                total = total.add(result.getAmount());
            } else if (r.getTotalAmount() != null) {
                total = total.add(r.getTotalAmount());
            }
        }

        return total;
    }
}
