package com.store.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.store.model.Item;
import com.store.model.ItemStatus;
import com.store.service.ItemRepository;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private ItemRepository itemRepository;

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
        });
        return "redirect:/admin/items";
    }

    @PostMapping("/items/{id}/reject")
    public String rejectItem(@PathVariable int id) {
        itemRepository.findById(id).ifPresent(item -> {
            item.setStatus(ItemStatus.REJECTED);
            itemRepository.save(item);
        });
        return "redirect:/admin/items";
    }
}
