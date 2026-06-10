package com.store.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import com.store.model.Item;
import com.store.model.ItemStatus;
import com.store.service.ItemRepository;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private AdminController adminController;

    private Model model;

    @BeforeEach
    void setUp() {
        model = new ExtendedModelMap();
    }

    @Test
    void showPendingItems_returnsViewWithPendingItems() {
        List<Item> items = List.of(new Item());
        when(itemRepository.findByStatus(ItemStatus.PENDING)).thenReturn(items);

        String view = adminController.showPendingItems(model);

        assertThat(view).isEqualTo("admin/PendingItems");
        assertThat(model.getAttribute("items")).isEqualTo(items);
    }

    @Test
    void approveItem_existingItem_setsStatusApprovedAndRedirects() {
        Item item = new Item();
        item.setStatus(ItemStatus.PENDING);
        when(itemRepository.findById(1)).thenReturn(Optional.of(item));

        String view = adminController.approveItem(1);

        assertThat(view).isEqualTo("redirect:/admin/items");
        assertThat(item.getStatus()).isEqualTo(ItemStatus.APPROVED);
        verify(itemRepository).save(item);
    }

    @Test
    void approveItem_itemNotFound_redirectsWithoutSaving() {
        when(itemRepository.findById(1)).thenReturn(Optional.empty());

        String view = adminController.approveItem(1);

        assertThat(view).isEqualTo("redirect:/admin/items");
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void rejectItem_existingItem_setsStatusRejectedAndRedirects() {
        Item item = new Item();
        item.setStatus(ItemStatus.PENDING);
        when(itemRepository.findById(1)).thenReturn(Optional.of(item));

        String view = adminController.rejectItem(1);

        assertThat(view).isEqualTo("redirect:/admin/items");
        assertThat(item.getStatus()).isEqualTo(ItemStatus.REJECTED);
        verify(itemRepository).save(item);
    }

    @Test
    void rejectItem_itemNotFound_redirectsWithoutSaving() {
        when(itemRepository.findById(1)).thenReturn(Optional.empty());

        String view = adminController.rejectItem(1);

        assertThat(view).isEqualTo("redirect:/admin/items");
        verify(itemRepository, never()).save(any(Item.class));
    }
}
