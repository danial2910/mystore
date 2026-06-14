package com.store.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import com.store.model.Item;
import com.store.model.ItemStatus;
import com.store.model.ServiceRecord;
import com.store.model.ServiceRecordStatus;
import com.store.model.User;
import com.store.service.BillingCalculator;
import com.store.service.ItemRepository;
import com.store.service.ServiceRecordRepository;
import com.store.service.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ServiceRecordRepository serviceRecordRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AdminController adminController;

    private Model model;

    @BeforeEach
    void setUp() {
        model = new ExtendedModelMap();
        ReflectionTestUtils.setField(adminController, "ratePerHour", 5);

        BillingCalculator billingCalculator = new BillingCalculator();
        ReflectionTestUtils.setField(billingCalculator, "ratePerHour", 5);
        ReflectionTestUtils.setField(billingCalculator, "lateFeeMultiplier", 1.5);
        ReflectionTestUtils.setField(adminController, "billingCalculator", billingCalculator);
    }

    // ── Show pending items ────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void showPendingItems_returnsViewWithPendingItemsPage() {
        List<Item> items = List.of(new Item());
        Page<Item> page = new PageImpl<>(items, PageRequest.of(0, 10), 1);
        when(itemRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(itemRepository.findDistinctCategoriesByStatus(ItemStatus.PENDING)).thenReturn(List.of("electronics"));

        String view = adminController.showPendingItems(model, null, null, 0);

        assertThat(view).isEqualTo("admin/PendingItems");
        assertThat(model.getAttribute("items")).isEqualTo(items);
        assertThat(model.getAttribute("currentPage")).isEqualTo(0);
        assertThat(model.getAttribute("totalPages")).isEqualTo(1);
        assertThat(model.getAttribute("totalElements")).isEqualTo(1L);
        assertThat(model.getAttribute("categories")).isEqualTo(List.of("electronics"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void showPendingItems_withCategoryAndSearch_passesFiltersToModel() {
        Page<Item> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(itemRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(itemRepository.findDistinctCategoriesByStatus(ItemStatus.PENDING)).thenReturn(List.of());

        String view = adminController.showPendingItems(model, "electronics", "widget", 0);

        assertThat(view).isEqualTo("admin/PendingItems");
        assertThat(model.getAttribute("category")).isEqualTo("electronics");
        assertThat(model.getAttribute("search")).isEqualTo("widget");
    }

    // ── Approve ───────────────────────────────────────────────────────────────

    @Test
    void approveItem_existingItem_setsStatusApprovedAndRedirects() {
        User owner = new User();
        User admin = new User();
        admin.setUsername("admin");
        Item item = new Item();
        item.setStatus(ItemStatus.PENDING);
        item.setOwner(owner);
        when(itemRepository.findById(1)).thenReturn(Optional.of(item));
        when(authentication.getName()).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        String view = adminController.approveItem(1, authentication);

        assertThat(view).isEqualTo("redirect:/admin/items");
        assertThat(item.getStatus()).isEqualTo(ItemStatus.APPROVED);
        assertThat(item.getApprovedAt()).isNotNull();
        assertThat(item.getApprovedBy()).isEqualTo(admin);
        verify(itemRepository).save(item);
    }

    @Test
    void approveItem_existingItem_createsActiveServiceRecord() {
        User owner = new User();
        User admin = new User();
        admin.setUsername("admin");
        Item item = new Item();
        item.setStatus(ItemStatus.PENDING);
        item.setOwner(owner);
        when(itemRepository.findById(1)).thenReturn(Optional.of(item));
        when(authentication.getName()).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        adminController.approveItem(1, authentication);

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

        String view = adminController.approveItem(1, authentication);

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
        assertThat(item.getRejectedAt()).isNotNull();
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
    void rejectItem_withOverdueActiveServiceRecord_appliesLateFee() {
        LocalDateTime now = LocalDateTime.now();

        Item item = new Item();
        item.setId(1);
        item.setStatus(ItemStatus.PENDING);
        item.setStorageEndTime(now.minusHours(2));
        when(itemRepository.findById(1)).thenReturn(Optional.of(item));

        ServiceRecord record = new ServiceRecord();
        record.setStartTime(now.minusHours(5));
        record.setStatus(ServiceRecordStatus.ACTIVE);
        when(serviceRecordRepository.findByItemIdAndStatus(1, ServiceRecordStatus.ACTIVE))
                .thenReturn(Optional.of(record));

        adminController.rejectItem(1);

        ArgumentCaptor<ServiceRecord> captor = ArgumentCaptor.forClass(ServiceRecord.class);
        verify(serviceRecordRepository).save(captor.capture());
        ServiceRecord saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ServiceRecordStatus.ENDED);
        // 3 normal hrs * RM5 + 2 late hrs * RM5 * 1.5 = 15 + 15 = 30
        assertThat(saved.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(30));
    }

    @Test
    void rejectItem_itemNotFound_redirectsWithoutSaving() {
        when(itemRepository.findById(1)).thenReturn(Optional.empty());

        String view = adminController.rejectItem(1);

        assertThat(view).isEqualTo("redirect:/admin/items");
        verify(itemRepository, never()).save(any(Item.class));
    }

    // ── Extension requests ───────────────────────────────────────────────────

    @Test
    void showExtensionRequests_returnsViewWithItems() {
        Item item = new Item();
        item.setRequestedStorageEndTime(LocalDateTime.now().plusDays(1));
        List<Item> items = List.of(item);
        when(itemRepository.findByRequestedStorageEndTimeIsNotNull()).thenReturn(items);

        String view = adminController.showExtensionRequests(model);

        assertThat(view).isEqualTo("admin/ExtensionRequests");
        assertThat(model.getAttribute("items")).isEqualTo(items);
    }

    @Test
    void approveExtension_existingItem_appliesRequestedEndTimeAndClearsRequest() {
        LocalDateTime currentEnd = LocalDateTime.now().plusDays(1);
        LocalDateTime requestedEnd = LocalDateTime.now().plusDays(3);

        Item item = new Item();
        item.setStorageEndTime(currentEnd);
        item.setRequestedStorageEndTime(requestedEnd);
        when(itemRepository.findById(1)).thenReturn(Optional.of(item));

        String view = adminController.approveExtension(1);

        assertThat(view).isEqualTo("redirect:/admin/extensions");
        assertThat(item.getStorageEndTime()).isEqualTo(requestedEnd);
        assertThat(item.getRequestedStorageEndTime()).isNull();
        verify(itemRepository).save(item);
    }

    @Test
    void approveExtension_itemNotFound_redirectsWithoutSaving() {
        when(itemRepository.findById(1)).thenReturn(Optional.empty());

        String view = adminController.approveExtension(1);

        assertThat(view).isEqualTo("redirect:/admin/extensions");
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void rejectExtension_existingItem_clearsRequestWithoutChangingStorageEndTime() {
        LocalDateTime currentEnd = LocalDateTime.now().plusDays(1);
        LocalDateTime requestedEnd = LocalDateTime.now().plusDays(3);

        Item item = new Item();
        item.setStorageEndTime(currentEnd);
        item.setRequestedStorageEndTime(requestedEnd);
        when(itemRepository.findById(1)).thenReturn(Optional.of(item));

        String view = adminController.rejectExtension(1);

        assertThat(view).isEqualTo("redirect:/admin/extensions");
        assertThat(item.getStorageEndTime()).isEqualTo(currentEnd);
        assertThat(item.getRequestedStorageEndTime()).isNull();
        verify(itemRepository).save(item);
    }

    @Test
    void rejectExtension_itemNotFound_redirectsWithoutSaving() {
        when(itemRepository.findById(1)).thenReturn(Optional.empty());

        String view = adminController.rejectExtension(1);

        assertThat(view).isEqualTo("redirect:/admin/extensions");
        verify(itemRepository, never()).save(any(Item.class));
    }

    // ── Billing ───────────────────────────────────────────────────────────────

    @Test
    void showBilling_returnsViewWithRecordsAndRate() {
        ServiceRecord record = new ServiceRecord();
        record.setItem(new Item());
        record.setStartTime(LocalDateTime.now());
        List<ServiceRecord> records = List.of(record);
        when(serviceRecordRepository.findAllByOrderByStartTimeDesc()).thenReturn(records);

        String view = adminController.showBilling(model);

        assertThat(view).isEqualTo("admin/Billing");
        assertThat(model.getAttribute("records")).isEqualTo(records);
        assertThat(model.getAttribute("ratePerHour")).isEqualTo(5);
    }

    @Test
    @SuppressWarnings("unchecked")
    void showBilling_withActiveRecord_computesLiveHoursAndAmount() {
        ServiceRecord record = new ServiceRecord();
        record.setId(7);
        record.setItem(new Item());
        record.setStartTime(LocalDateTime.now().minusHours(2));
        record.setStatus(ServiceRecordStatus.ACTIVE);

        when(serviceRecordRepository.findAllByOrderByStartTimeDesc()).thenReturn(List.of(record));

        adminController.showBilling(model);

        Map<Integer, Long> liveHours = (Map<Integer, Long>) model.getAttribute("liveHoursMap");
        Map<Integer, BigDecimal> liveAmounts = (Map<Integer, BigDecimal>) model.getAttribute("liveAmountMap");

        assertThat(liveHours.get(7)).isGreaterThanOrEqualTo(2L);
        assertThat(liveAmounts.get(7)).isGreaterThanOrEqualTo(BigDecimal.valueOf(10));
    }

    @Test
    void showBilling_addsLateFeeMultiplierToModel() {
        when(serviceRecordRepository.findAllByOrderByStartTimeDesc()).thenReturn(List.of());

        adminController.showBilling(model);

        assertThat(model.getAttribute("lateFeeMultiplier")).isEqualTo(1.5);
    }

    @Test
    @SuppressWarnings("unchecked")
    void showBilling_activeRecordPastStorageEndTime_flaggedOverdueWithLateFee() {
        LocalDateTime now = LocalDateTime.now();

        Item item = new Item();
        item.setStorageEndTime(now.minusHours(2));

        ServiceRecord record = new ServiceRecord();
        record.setId(8);
        record.setItem(item);
        record.setStartTime(now.minusHours(5));
        record.setStatus(ServiceRecordStatus.ACTIVE);

        when(serviceRecordRepository.findAllByOrderByStartTimeDesc()).thenReturn(List.of(record));

        adminController.showBilling(model);

        Map<Integer, Boolean> overdueMap = (Map<Integer, Boolean>) model.getAttribute("overdueMap");
        Map<Integer, BigDecimal> liveAmounts = (Map<Integer, BigDecimal>) model.getAttribute("liveAmountMap");

        assertThat(overdueMap.get(8)).isTrue();
        // 3 normal hrs * RM5 + 2 late hrs * RM5 * 1.5 = 15 + 15 = 30
        assertThat(liveAmounts.get(8)).isEqualByComparingTo(BigDecimal.valueOf(30));
    }

    @Test
    @SuppressWarnings("unchecked")
    void showBilling_endedRecordPastStorageEndTime_flaggedOverdue() {
        Item item = new Item();
        item.setStorageEndTime(LocalDateTime.now().minusHours(1));

        ServiceRecord record = new ServiceRecord();
        record.setId(9);
        record.setItem(item);
        record.setStartTime(LocalDateTime.now().minusHours(4));
        record.setEndTime(LocalDateTime.now());
        record.setTotalHours(4);
        record.setTotalAmount(BigDecimal.valueOf(20));
        record.setStatus(ServiceRecordStatus.ENDED);

        when(serviceRecordRepository.findAllByOrderByStartTimeDesc()).thenReturn(List.of(record));

        adminController.showBilling(model);

        Map<Integer, Boolean> overdueMap = (Map<Integer, Boolean>) model.getAttribute("overdueMap");
        assertThat(overdueMap.get(9)).isTrue();
    }

    @Test
    void markBillPaid_existingRecord_setsPaidAndRedirects() {
        ServiceRecord record = new ServiceRecord();
        record.setId(5);
        when(serviceRecordRepository.findById(5)).thenReturn(Optional.of(record));

        String view = adminController.markBillPaid(5);

        assertThat(view).isEqualTo("redirect:/admin/billing");
        assertThat(record.isPaid()).isTrue();
        assertThat(record.getPaidAt()).isNotNull();
        verify(serviceRecordRepository).save(record);
    }

    @Test
    void markBillPaid_recordNotFound_redirectsWithoutSaving() {
        when(serviceRecordRepository.findById(5)).thenReturn(Optional.empty());

        String view = adminController.markBillPaid(5);

        assertThat(view).isEqualTo("redirect:/admin/billing");
        verify(serviceRecordRepository, never()).save(any(ServiceRecord.class));
    }

    @Test
    void markBillUnpaid_existingRecord_clearsPaidAndRedirects() {
        ServiceRecord record = new ServiceRecord();
        record.setId(6);
        record.setPaid(true);
        record.setPaidAt(LocalDateTime.now());
        when(serviceRecordRepository.findById(6)).thenReturn(Optional.of(record));

        String view = adminController.markBillUnpaid(6);

        assertThat(view).isEqualTo("redirect:/admin/billing");
        assertThat(record.isPaid()).isFalse();
        assertThat(record.getPaidAt()).isNull();
        verify(serviceRecordRepository).save(record);
    }

    @Test
    void markBillUnpaid_recordNotFound_redirectsWithoutSaving() {
        when(serviceRecordRepository.findById(6)).thenReturn(Optional.empty());

        String view = adminController.markBillUnpaid(6);

        assertThat(view).isEqualTo("redirect:/admin/billing");
        verify(serviceRecordRepository, never()).save(any(ServiceRecord.class));
    }

    // ── Dashboard ────────────────────────────────────────────────────────────

    @Test
    void showDashboard_returnsViewWithCounts() {
        when(itemRepository.countByStatus(ItemStatus.PENDING)).thenReturn(3L);
        when(itemRepository.countByStatus(ItemStatus.APPROVED)).thenReturn(7L);
        when(serviceRecordRepository.countByStatus(ServiceRecordStatus.ACTIVE)).thenReturn(4L);
        when(itemRepository.countByRequestedStorageEndTimeIsNotNull()).thenReturn(2L);
        when(userRepository.count()).thenReturn(10L);
        when(serviceRecordRepository.findAllByOrderByStartTimeDesc()).thenReturn(List.of());

        String view = adminController.showDashboard(model);

        assertThat(view).isEqualTo("admin/Dashboard");
        assertThat(model.getAttribute("pendingItemsCount")).isEqualTo(3L);
        assertThat(model.getAttribute("approvedItemsCount")).isEqualTo(7L);
        assertThat(model.getAttribute("activeRentalsCount")).isEqualTo(4L);
        assertThat(model.getAttribute("pendingExtensionsCount")).isEqualTo(2L);
        assertThat(model.getAttribute("totalUsers")).isEqualTo(10L);
        assertThat(model.getAttribute("totalOutstanding")).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void showDashboard_totalOutstanding_sumsUnpaidActiveAndEndedRecords() {
        ServiceRecord activeUnpaid = new ServiceRecord();
        activeUnpaid.setItem(new Item());
        activeUnpaid.setStartTime(LocalDateTime.now().minusHours(2));
        activeUnpaid.setStatus(ServiceRecordStatus.ACTIVE);

        ServiceRecord activePaid = new ServiceRecord();
        activePaid.setItem(new Item());
        activePaid.setStartTime(LocalDateTime.now().minusHours(2));
        activePaid.setStatus(ServiceRecordStatus.ACTIVE);
        activePaid.setPaid(true);

        ServiceRecord endedUnpaid = new ServiceRecord();
        endedUnpaid.setStatus(ServiceRecordStatus.ENDED);
        endedUnpaid.setTotalAmount(BigDecimal.valueOf(20));

        ServiceRecord endedPaid = new ServiceRecord();
        endedPaid.setStatus(ServiceRecordStatus.ENDED);
        endedPaid.setTotalAmount(BigDecimal.valueOf(50));
        endedPaid.setPaid(true);

        when(itemRepository.countByStatus(ItemStatus.PENDING)).thenReturn(0L);
        when(itemRepository.countByStatus(ItemStatus.APPROVED)).thenReturn(0L);
        when(serviceRecordRepository.countByStatus(ServiceRecordStatus.ACTIVE)).thenReturn(2L);
        when(userRepository.count()).thenReturn(1L);
        when(serviceRecordRepository.findAllByOrderByStartTimeDesc())
                .thenReturn(List.of(activeUnpaid, activePaid, endedUnpaid, endedPaid));

        adminController.showDashboard(model);

        BigDecimal totalOutstanding = (BigDecimal) model.getAttribute("totalOutstanding");

        // activeUnpaid: 2 hrs * RM5 = RM10, endedUnpaid: RM20 => RM30
        assertThat(totalOutstanding).isEqualByComparingTo(BigDecimal.valueOf(30));
    }

    @Test
    void showDashboard_overdueRentalsCount_countsOnlyOverdueActiveRecords() {
        Item overdueItem = new Item();
        overdueItem.setStorageEndTime(LocalDateTime.now().minusHours(1));
        ServiceRecord overdueActive = new ServiceRecord();
        overdueActive.setItem(overdueItem);
        overdueActive.setStartTime(LocalDateTime.now().minusHours(3));
        overdueActive.setStatus(ServiceRecordStatus.ACTIVE);

        Item onTimeItem = new Item();
        onTimeItem.setStorageEndTime(LocalDateTime.now().plusHours(4));
        ServiceRecord onTimeActive = new ServiceRecord();
        onTimeActive.setItem(onTimeItem);
        onTimeActive.setStartTime(LocalDateTime.now().minusHours(1));
        onTimeActive.setStatus(ServiceRecordStatus.ACTIVE);

        ServiceRecord endedRecord = new ServiceRecord();
        endedRecord.setItem(new Item());
        endedRecord.setStatus(ServiceRecordStatus.ENDED);
        endedRecord.setTotalAmount(BigDecimal.valueOf(20));

        when(itemRepository.countByStatus(ItemStatus.PENDING)).thenReturn(0L);
        when(itemRepository.countByStatus(ItemStatus.APPROVED)).thenReturn(0L);
        when(serviceRecordRepository.countByStatus(ServiceRecordStatus.ACTIVE)).thenReturn(2L);
        when(userRepository.count()).thenReturn(1L);
        when(serviceRecordRepository.findAllByOrderByStartTimeDesc())
                .thenReturn(List.of(overdueActive, onTimeActive, endedRecord));

        adminController.showDashboard(model);

        assertThat(model.getAttribute("overdueRentalsCount")).isEqualTo(1L);
    }
}
