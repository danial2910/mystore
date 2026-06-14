package com.store.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.store.model.Item;
import com.store.model.ItemDTO;
import com.store.model.ItemStatus;
import com.store.model.ServiceRecord;
import com.store.model.ServiceRecordStatus;
import com.store.model.User;
import com.store.service.BillingCalculator;
import com.store.service.BillingResult;
import com.store.service.ItemRepository;
import com.store.service.ItemSpecifications;
import com.store.service.ServiceRecordRepository;
import com.store.service.UserRepository;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/items")
public class ItemsController {

    private static final Logger logger = LoggerFactory.getLogger(ItemsController.class);

    private static final int PAGE_SIZE = 10;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceRecordRepository serviceRecordRepository;

    @Autowired
    private BillingCalculator billingCalculator;

    @GetMapping({"", "/"})
    public String showItemList(Model model,
                                @RequestParam(required = false) String category,
                                @RequestParam(required = false) String search,
                                @RequestParam(defaultValue = "0") int page) {
        Specification<Item> spec = Specification.where(ItemSpecifications.hasStatus(ItemStatus.APPROVED))
                .and(ItemSpecifications.hasCategory(category))
                .and(ItemSpecifications.nameContains(search));

        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("createdAt").descending());
        Page<Item> itemPage = itemRepository.findAll(spec, pageable);

        model.addAttribute("items", itemPage.getContent());
        model.addAttribute("currentPage", itemPage.getNumber());
        model.addAttribute("totalPages", itemPage.getTotalPages());
        model.addAttribute("totalElements", itemPage.getTotalElements());
        model.addAttribute("categories", itemRepository.findDistinctCategoriesByStatus(ItemStatus.APPROVED));
        model.addAttribute("category", category);
        model.addAttribute("search", search);
        return "items/index";
    }

    @GetMapping("/my")
    public String showMyItems(Model model, Authentication authentication,
                               @RequestParam(required = false) String status,
                               @RequestParam(defaultValue = "0") int page) {
        User user = currentUser(authentication);

        ItemStatus statusFilter = null;
        if (status != null && !status.isBlank()) {
            try {
                statusFilter = ItemStatus.valueOf(status);
            } catch (IllegalArgumentException ex) {
                statusFilter = null;
            }
        }

        Specification<Item> spec = Specification.where(ItemSpecifications.hasOwner(user))
                .and(ItemSpecifications.hasStatus(statusFilter));

        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("createdAt").descending());
        Page<Item> itemPage = itemRepository.findAll(spec, pageable);

        model.addAttribute("items", itemPage.getContent());
        model.addAttribute("currentPage", itemPage.getNumber());
        model.addAttribute("totalPages", itemPage.getTotalPages());
        model.addAttribute("totalElements", itemPage.getTotalElements());
        model.addAttribute("status", status);
        return "items/MyItems";
    }

    @GetMapping("/{id}")
    public String showItemDetail(@PathVariable int id, Model model, Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        Item item = itemRepository.findById(id).orElse(null);

        if (item == null) {
            redirectAttributes.addFlashAttribute("error", "Item not found.");
            return "redirect:/items";
        }

        User user = currentUser(authentication);
        boolean isOwner = item.getOwner().getId() == user.getId();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        if (item.getStatus() != ItemStatus.APPROVED && !isOwner && !isAdmin) {
            redirectAttributes.addFlashAttribute("error", "You do not have permission to view that item.");
            return "redirect:/items";
        }

        model.addAttribute("item", item);
        model.addAttribute("isOwner", isOwner);
        return "items/ItemDetail";
    }

    @GetMapping("/create")
    public String showCreateItem(Model model) {
        model.addAttribute("itemDTO", new ItemDTO());
        return "items/CreateItem";
    }

    @PostMapping("/create")
    public String createItem(@Valid @ModelAttribute ItemDTO itemDTO, BindingResult result, Authentication authentication) {
        if (result.hasErrors()) {
            return "items/CreateItem";
        }

        User user = currentUser(authentication);

        Item item = new Item();
        item.setName(itemDTO.getName());
        item.setBrand(itemDTO.getBrand());
        item.setCategory(itemDTO.getCategory());
        item.setQuantity(itemDTO.getQuantity());
        item.setDescription(itemDTO.getDescription());
        item.setCreatedAt(new Date());
        item.setStatus(ItemStatus.PENDING);
        item.setOwner(user);
        item.setImageFileName(storeImage(itemDTO.getImageFile()));
        item.setStorageStartTime(itemDTO.getStorageStartTime());
        item.setStorageEndTime(itemDTO.getStorageEndTime());

        itemRepository.save(item);

        return "redirect:/items/my";
    }

    @GetMapping("/edit/{id}")
    public String showEditItem(Model model, @PathVariable int id, Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        Item item = itemRepository.findByIdAndOwner(id, user).orElse(null);

        if (item == null || item.getStatus() != ItemStatus.PENDING) {
            redirectAttributes.addFlashAttribute("error", "That item cannot be edited.");
            return "redirect:/items/my";
        }

        ItemDTO itemDTO = new ItemDTO();
        itemDTO.setName(item.getName());
        itemDTO.setBrand(item.getBrand());
        itemDTO.setCategory(item.getCategory());
        itemDTO.setQuantity(item.getQuantity());
        itemDTO.setDescription(item.getDescription());

        model.addAttribute("itemDTO", itemDTO);
        model.addAttribute("id", id);
        return "items/EditItem";
    }

    @PostMapping("/edit/{id}")
    public String updateItem(@PathVariable int id, @Valid @ModelAttribute ItemDTO itemDTO, BindingResult result,
                              Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        Item item = itemRepository.findByIdAndOwner(id, user).orElse(null);

        if (item == null || item.getStatus() != ItemStatus.PENDING) {
            redirectAttributes.addFlashAttribute("error", "That item cannot be edited.");
            return "redirect:/items/my";
        }

        if (result.hasErrors()) {
            model.addAttribute("id", id);
            return "items/EditItem";
        }

        if (itemDTO.getImageFile() != null && !itemDTO.getImageFile().isEmpty()) {
            deleteImage(item.getImageFileName());
            item.setImageFileName(storeImage(itemDTO.getImageFile()));
        }

        item.setName(itemDTO.getName());
        item.setBrand(itemDTO.getBrand());
        item.setCategory(itemDTO.getCategory());
        item.setQuantity(itemDTO.getQuantity());
        item.setDescription(itemDTO.getDescription());

        itemRepository.save(item);

        return "redirect:/items/my";
    }

    @PostMapping("/delete/{id}")
    public String deleteItem(@PathVariable int id, Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        Item item = itemRepository.findByIdAndOwner(id, user).orElse(null);

        if (item == null || item.getStatus() != ItemStatus.PENDING) {
            redirectAttributes.addFlashAttribute("error", "That item cannot be deleted.");
            return "redirect:/items/my";
        }

        deleteImage(item.getImageFileName());
        itemRepository.delete(item);

        return "redirect:/items/my";
    }

    @PostMapping("/{id}/request-extension")
    public String requestExtension(@PathVariable int id,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newStorageEndTime,
                                    Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        Item item = itemRepository.findByIdAndOwner(id, user).orElse(null);

        if (item == null || item.getStatus() != ItemStatus.APPROVED) {
            redirectAttributes.addFlashAttribute("error", "That item is not eligible for a storage extension.");
            return "redirect:/items/my";
        }

        if (item.getStorageEndTime() != null && !newStorageEndTime.isAfter(item.getStorageEndTime())) {
            redirectAttributes.addFlashAttribute("error", "The new storage end time must be after the current one.");
            return "redirect:/items/my";
        }

        item.setRequestedStorageEndTime(newStorageEndTime);
        itemRepository.save(item);

        redirectAttributes.addFlashAttribute("success", "Extension request submitted for admin approval.");
        return "redirect:/items/my";
    }

    @PostMapping("/cancel/{id}")
    public String cancelItem(@PathVariable int id, Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        Item item = itemRepository.findByIdAndOwner(id, user).orElse(null);

        if (item == null || (item.getStatus() != ItemStatus.PENDING && item.getStatus() != ItemStatus.APPROVED)) {
            redirectAttributes.addFlashAttribute("error", "That item cannot be cancelled.");
            return "redirect:/items/my";
        }

        item.setStatus(ItemStatus.CANCELLED);
        itemRepository.save(item);

        closeServiceRecord(item);

        return "redirect:/items/my";
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + authentication.getName()));
    }

    private String storeImage(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return null;
        }

        String originalFilename = imageFile.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        String storageFileName = UUID.randomUUID() + extension;

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            try (InputStream inputStream = imageFile.getInputStream()) {
                Files.copy(inputStream, uploadPath.resolve(storageFileName), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            logger.error("Failed to store image file {}", storageFileName, ex);
        }

        return storageFileName;
    }

    private void closeServiceRecord(Item item) {
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
    }

    private void deleteImage(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return;
        }

        try {
            Files.deleteIfExists(Paths.get(uploadDir).resolve(fileName));
        } catch (IOException ex) {
            logger.error("Failed to delete image file {}", fileName, ex);
        }
    }
}
