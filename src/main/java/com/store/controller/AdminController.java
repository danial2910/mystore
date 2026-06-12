package com.store.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.store.model.Item;
import com.store.model.ItemStatus;
import com.store.model.ServiceRecord;
import com.store.model.ServiceRecordStatus;
import com.store.service.ItemRepository;
import com.store.service.ServiceRecordRepository;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Value("${app.rental.rate-per-hour}")
    private int ratePerHour;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ServiceRecordRepository serviceRecordRepository;

    @GetMapping("/items")
    public String showPendingItems(Model model) {
        List<Item> items = itemRepository.findByStatus(ItemStatus.PENDING);
        model.addAttribute("items", items);
        return "admin/PendingItems";
    }

    @PostMapping("/items/{id}/approve")
    public String approveItem(@PathVariable int id) {
        itemRepository.findById(id).ifPresent(item -> {
            item.setStatus(ItemStatus.APPROVED);
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

    @PostMapping("/items/{id}/reject")
    public String rejectItem(@PathVariable int id) {
        itemRepository.findById(id).ifPresent(item -> {
            item.setStatus(ItemStatus.REJECTED);
            itemRepository.save(item);

            serviceRecordRepository.findByItemIdAndStatus(item.getId(), ServiceRecordStatus.ACTIVE)
                    .ifPresent(record -> {
                        LocalDateTime end = LocalDateTime.now();
                        long hours = ChronoUnit.HOURS.between(record.getStartTime(), end);
                        if (hours < 1) hours = 1;
                        record.setEndTime(end);
                        record.setTotalHours(hours);
                        record.setTotalAmount(BigDecimal.valueOf((long) ratePerHour * hours));
                        record.setStatus(ServiceRecordStatus.ENDED);
                        serviceRecordRepository.save(record);
                    });
        });
        return "redirect:/admin/items";
    }
}
