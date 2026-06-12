package com.store.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import com.store.model.Item;
import com.store.model.ItemStatus;
import com.store.model.ServiceRecord;
import com.store.model.ServiceRecordStatus;
import com.store.model.User;
import com.store.service.ItemRepository;
import com.store.service.ServiceRecordRepository;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ServiceRecordRepository serviceRecordRepository;

    @InjectMocks
    private AdminController adminController;

    private Model model;

    @BeforeEach
    void setUp() {
        model = new ExtendedModelMap();
    }

    // ── Show pending items ────────────────────────────────────────────────────

    @Test
    void showPendingItems_returnsViewWithPendingItems() {
        List<Item> items = List.of(new Item());
        when(itemRepository.findByStatus(ItemStatus.PENDING)).thenReturn(items);

        String view = adminController.showPendingItems(model);

        assertThat(view).isEqualTo("admin/PendingItems");
        assertThat(model.getAttribute("items")).isEqualTo(items);
    }

    // ── Approve ───────────────────────────────────────────────────────────────

    @Test
    void approveItem_existingItem_setsStatusApprovedAndRedirects() {
        User owner = new User();
        Item item = new Item();
        item.setStatus(ItemStatus.PENDING);
        item.setOwner(owner);
        when(itemRepository.findById(1)).thenReturn(Optional.of(item));

        String view = adminController.approveItem(1);

        assertThat(view).isEqualTo("redirect:/admin/items");
        assertThat(item.getStatus()).isEqualTo(ItemStatus.APPROVED);
        verify(itemRepository).save(item);
    }

    @Test
    void approveItem_existingItem_createsActiveServiceRecord() {
        User owner = new User();
        Item item = new Item();
        item.setStatus(ItemStatus.PENDING);
        item.setOwner(owner);
        when(itemRepository.findById(1)).thenReturn(Optional.of(item));

        adminController.approveItem(1);

        ArgumentCaptor<ServiceRecord> captor = ArgumentCaptor.forClass(ServiceRecord.class);
        verify(serviceRecordRepository).save(captor.capture());
        ServiceRecord record = captor.getValue();
        assertThat(record.getItem()).isEqualTo(item);
        assertThat(record.getOwner()).isEqualTo(owner);
        assertThat(record.getStatus()).isEqualTo(ServiceRecordStatus.ACTIVE);
        assertThat(record.getStartTime()).isNotNull();
    }

    @Test
    void approveItem_itemNotFound_redirectsWithoutSaving() {
        when(itemRepository.findById(1)).thenReturn(Optional.empty());

        String view = adminController.approveItem(1);

        assertThat(view).isEqualTo("redirect:/admin/items");
        verify(itemRepository, never()).save(any(Item.class));
        verify(serviceRecordRepository, never()).save(any(ServiceRecord.class));
    }

    // ── Reject ────────────────────────────────────────────────────────────────

    @Test
    void rejectItem_existingItem_setsStatusRejectedAndRedirects() {
        Item item = new Item();
        item.setId(1);
        item.setStatus(ItemStatus.PENDING);
        when(itemRepository.findById(1)).thenReturn(Optional.of(item));
        when(serviceRecordRepository.findByItemIdAndStatus(1, ServiceRecordStatus.ACTIVE))
                .thenReturn(Optional.empty());

        String view = adminController.rejectItem(1);

        assertThat(view).isEqualTo("redirect:/admin/items");
        assertThat(item.getStatus()).isEqualTo(ItemStatus.REJECTED);
        verify(itemRepository).save(item);
    }

    @Test
    void rejectItem_withActiveServiceRecord_closesRecord() {
        Item item = new Item();
        item.setId(1);
        item.setStatus(ItemStatus.PENDING);
        when(itemRepository.findById(1)).thenReturn(Optional.of(item));

        ServiceRecord record = new ServiceRecord();
        record.setStartTime(LocalDateTime.now().minusHours(3));
        record.setStatus(ServiceRecordStatus.ACTIVE);
        when(serviceRecordRepository.findByItemIdAndStatus(1, ServiceRecordStatus.ACTIVE))
                .thenReturn(Optional.of(record));

        adminController.rejectItem(1);

        ArgumentCaptor<ServiceRecord> captor = ArgumentCaptor.forClass(ServiceRecord.class);
        verify(serviceRecordRepository).save(captor.capture());
        ServiceRecord saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ServiceRecordStatus.ENDED);
        assertThat(saved.getEndTime()).isNotNull();
        assertThat(saved.getTotalHours()).isGreaterThanOrEqualTo(1);
        assertThat(saved.getTotalAmount()).isNotNull();
    }

    @Test
    void rejectItem_itemNotFound_redirectsWithoutSaving() {
        when(itemRepository.findById(1)).thenReturn(Optional.empty());

        String view = adminController.rejectItem(1);

        assertThat(view).isEqualTo("redirect:/admin/items");
        verify(itemRepository, never()).save(any(Item.class));
    }
}
