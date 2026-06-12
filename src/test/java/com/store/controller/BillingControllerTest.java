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
    void myBills_withEndedRecord_notIncludedInTotalOwed() {
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
}
