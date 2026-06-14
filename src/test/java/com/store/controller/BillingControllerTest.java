package com.store.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import com.store.model.Item;
import com.store.model.ServiceRecord;
import com.store.model.ServiceRecordStatus;
import com.store.model.User;
import com.store.service.BillingCalculator;
import com.store.service.ServiceRecordRepository;
import com.store.service.UserRepository;

@ExtendWith(MockitoExtension.class)
class BillingControllerTest {

    @Mock
    private ServiceRecordRepository serviceRecordRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BillingController billingController;

    private User user;
    private Authentication authentication;
    private Model model;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(billingController, "ratePerHour", 5);

        BillingCalculator billingCalculator = new BillingCalculator();
        ReflectionTestUtils.setField(billingCalculator, "ratePerHour", 5);
        ReflectionTestUtils.setField(billingCalculator, "lateFeeMultiplier", 1.5);
        ReflectionTestUtils.setField(billingController, "billingCalculator", billingCalculator);

        user = new User();
        user.setId(1);
        user.setUsername("testuser");
        authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        model = new ExtendedModelMap();
    }

    @Test
    void myBills_noRecords_returnsEmptyListAndZeroOwed() {
        when(serviceRecordRepository.findByOwnerOrderByStartTimeDesc(user)).thenReturn(List.of());

        String view = billingController.myBills(authentication, model);

        assertThat(view).isEqualTo("billing/MyBills");
        assertThat((List<?>) model.getAttribute("records")).isEmpty();
        assertThat(model.getAttribute("totalOwed")).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @SuppressWarnings("unchecked")
    void myBills_withActiveRecord_computesLiveHoursAndAmount() {
        Item item = new Item();

        ServiceRecord record = new ServiceRecord();
        record.setId(10);
        record.setItem(item);
        record.setStartTime(LocalDateTime.now().minusHours(3));
        record.setStatus(ServiceRecordStatus.ACTIVE);

        when(serviceRecordRepository.findByOwnerOrderByStartTimeDesc(user)).thenReturn(List.of(record));

        billingController.myBills(authentication, model);

        Map<Integer, Long> liveHours = (Map<Integer, Long>) model.getAttribute("liveHoursMap");
        Map<Integer, BigDecimal> liveAmounts = (Map<Integer, BigDecimal>) model.getAttribute("liveAmountMap");
        BigDecimal totalOwed = (BigDecimal) model.getAttribute("totalOwed");

        assertThat(liveHours.get(10)).isGreaterThanOrEqualTo(3L);
        assertThat(liveAmounts.get(10)).isGreaterThanOrEqualTo(BigDecimal.valueOf(15));
        assertThat(totalOwed).isGreaterThanOrEqualTo(BigDecimal.valueOf(15));
    }

    @Test
    @SuppressWarnings("unchecked")
    void myBills_withUnpaidEndedRecord_includedInTotalOwed() {
        Item item = new Item();

        ServiceRecord record = new ServiceRecord();
        record.setId(20);
        record.setItem(item);
        record.setStartTime(LocalDateTime.now().minusHours(4));
        record.setEndTime(LocalDateTime.now().minusHours(1));
        record.setTotalHours(3);
        record.setTotalAmount(BigDecimal.valueOf(15));
        record.setStatus(ServiceRecordStatus.ENDED);

        when(serviceRecordRepository.findByOwnerOrderByStartTimeDesc(user)).thenReturn(List.of(record));

        billingController.myBills(authentication, model);

        Map<Integer, Long> liveHours = (Map<Integer, Long>) model.getAttribute("liveHoursMap");
        BigDecimal totalOwed = (BigDecimal) model.getAttribute("totalOwed");

        assertThat(liveHours).doesNotContainKey(20);
        assertThat(totalOwed).isEqualByComparingTo(BigDecimal.valueOf(15));
    }

    @Test
    void myBills_withPaidEndedRecord_notIncludedInTotalOwed() {
        Item item = new Item();

        ServiceRecord record = new ServiceRecord();
        record.setId(21);
        record.setItem(item);
        record.setStartTime(LocalDateTime.now().minusHours(4));
        record.setEndTime(LocalDateTime.now().minusHours(1));
        record.setTotalHours(3);
        record.setTotalAmount(BigDecimal.valueOf(15));
        record.setStatus(ServiceRecordStatus.ENDED);
        record.setPaid(true);

        when(serviceRecordRepository.findByOwnerOrderByStartTimeDesc(user)).thenReturn(List.of(record));

        billingController.myBills(authentication, model);

        BigDecimal totalOwed = (BigDecimal) model.getAttribute("totalOwed");

        assertThat(totalOwed).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void myBills_withPaidActiveRecord_notIncludedInTotalOwed() {
        Item item = new Item();

        ServiceRecord record = new ServiceRecord();
        record.setId(22);
        record.setItem(item);
        record.setStartTime(LocalDateTime.now().minusHours(3));
        record.setStatus(ServiceRecordStatus.ACTIVE);
        record.setPaid(true);

        when(serviceRecordRepository.findByOwnerOrderByStartTimeDesc(user)).thenReturn(List.of(record));

        billingController.myBills(authentication, model);

        BigDecimal totalOwed = (BigDecimal) model.getAttribute("totalOwed");

        assertThat(totalOwed).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @SuppressWarnings("unchecked")
    void myBills_withPlannedStorageTimes_computesPlannedHours() {
        Item item = new Item();
        item.setStorageStartTime(LocalDateTime.now());
        item.setStorageEndTime(LocalDateTime.now().plusHours(6));

        ServiceRecord record = new ServiceRecord();
        record.setId(30);
        record.setItem(item);
        record.setStartTime(LocalDateTime.now());
        record.setStatus(ServiceRecordStatus.ACTIVE);

        when(serviceRecordRepository.findByOwnerOrderByStartTimeDesc(user)).thenReturn(List.of(record));

        billingController.myBills(authentication, model);

        Map<Integer, Long> plannedHours = (Map<Integer, Long>) model.getAttribute("plannedHoursMap");
        assertThat(plannedHours.get(30)).isEqualTo(6L);
    }

    @Test
    void myBills_activeRecordUnderOneHour_chargesMinimumOneHour() {
        Item item = new Item();

        ServiceRecord record = new ServiceRecord();
        record.setId(40);
        record.setItem(item);
        record.setStartTime(LocalDateTime.now().minusMinutes(30));
        record.setStatus(ServiceRecordStatus.ACTIVE);

        when(serviceRecordRepository.findByOwnerOrderByStartTimeDesc(user)).thenReturn(List.of(record));

        billingController.myBills(authentication, model);

        @SuppressWarnings("unchecked")
        Map<Integer, Long> liveHours = (Map<Integer, Long>) model.getAttribute("liveHoursMap");
        assertThat(liveHours.get(40)).isEqualTo(1L);
        assertThat(model.getAttribute("totalOwed")).isEqualTo(BigDecimal.valueOf(5));
    }

    @Test
    void myBills_addsRatePerHourToModel() {
        when(serviceRecordRepository.findByOwnerOrderByStartTimeDesc(user)).thenReturn(List.of());

        billingController.myBills(authentication, model);

        assertThat(model.getAttribute("ratePerHour")).isEqualTo(5);
    }

    @Test
    void myBills_addsLateFeeMultiplierToModel() {
        when(serviceRecordRepository.findByOwnerOrderByStartTimeDesc(user)).thenReturn(List.of());

        billingController.myBills(authentication, model);

        assertThat(model.getAttribute("lateFeeMultiplier")).isEqualTo(1.5);
    }

    @Test
    @SuppressWarnings("unchecked")
    void myBills_activeRecordPastStorageEndTime_flaggedOverdueWithLateFee() {
        LocalDateTime now = LocalDateTime.now();

        Item item = new Item();
        item.setStorageEndTime(now.minusHours(2));

        ServiceRecord record = new ServiceRecord();
        record.setId(50);
        record.setItem(item);
        record.setStartTime(now.minusHours(5));
        record.setStatus(ServiceRecordStatus.ACTIVE);

        when(serviceRecordRepository.findByOwnerOrderByStartTimeDesc(user)).thenReturn(List.of(record));

        billingController.myBills(authentication, model);

        Map<Integer, Boolean> overdueMap = (Map<Integer, Boolean>) model.getAttribute("overdueMap");
        Map<Integer, BigDecimal> liveAmounts = (Map<Integer, BigDecimal>) model.getAttribute("liveAmountMap");

        assertThat(overdueMap.get(50)).isTrue();
        // 3 normal hrs * RM5 + 2 late hrs * RM5 * 1.5 = 15 + 15 = 30
        assertThat(liveAmounts.get(50)).isEqualByComparingTo(BigDecimal.valueOf(30));
        assertThat((BigDecimal) model.getAttribute("totalOwed")).isEqualByComparingTo(BigDecimal.valueOf(30));
    }

    @Test
    @SuppressWarnings("unchecked")
    void myBills_activeRecordBeforeStorageEndTime_notOverdue() {
        Item item = new Item();
        item.setStorageEndTime(LocalDateTime.now().plusHours(4));

        ServiceRecord record = new ServiceRecord();
        record.setId(51);
        record.setItem(item);
        record.setStartTime(LocalDateTime.now().minusHours(2));
        record.setStatus(ServiceRecordStatus.ACTIVE);

        when(serviceRecordRepository.findByOwnerOrderByStartTimeDesc(user)).thenReturn(List.of(record));

        billingController.myBills(authentication, model);

        Map<Integer, Boolean> overdueMap = (Map<Integer, Boolean>) model.getAttribute("overdueMap");
        assertThat(overdueMap.get(51)).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void myBills_endedRecordPastStorageEndTime_flaggedOverdue() {
        Item item = new Item();
        item.setStorageEndTime(LocalDateTime.now().minusHours(1));

        ServiceRecord record = new ServiceRecord();
        record.setId(52);
        record.setItem(item);
        record.setStartTime(LocalDateTime.now().minusHours(4));
        record.setEndTime(LocalDateTime.now());
        record.setTotalHours(4);
        record.setTotalAmount(BigDecimal.valueOf(20));
        record.setStatus(ServiceRecordStatus.ENDED);

        when(serviceRecordRepository.findByOwnerOrderByStartTimeDesc(user)).thenReturn(List.of(record));

        billingController.myBills(authentication, model);

        Map<Integer, Boolean> overdueMap = (Map<Integer, Boolean>) model.getAttribute("overdueMap");
        assertThat(overdueMap.get(52)).isTrue();
    }
}
