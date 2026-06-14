package com.store.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.store.model.Item;
import com.store.model.ItemDTO;
import com.store.model.ItemStatus;
import com.store.model.ServiceRecord;
import com.store.model.ServiceRecordStatus;
import com.store.model.User;
import com.store.service.BillingCalculator;
import com.store.service.ItemRepository;
import com.store.service.ServiceRecordRepository;
import com.store.service.UserRepository;

@ExtendWith(MockitoExtension.class)
class ItemsControllerTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ServiceRecordRepository serviceRecordRepository;

    @InjectMocks
    private ItemsController itemsController;

    private Model model;
    private User user;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        model = new ExtendedModelMap();
        user = new User();
        user.setId(1);
        user.setUsername("testuser");
        authentication = new UsernamePasswordAuthenticationToken("testuser", "password");

        BillingCalculator billingCalculator = new BillingCalculator();
        ReflectionTestUtils.setField(billingCalculator, "ratePerHour", 5);
        ReflectionTestUtils.setField(billingCalculator, "lateFeeMultiplier", 1.5);
        ReflectionTestUtils.setField(itemsController, "billingCalculator", billingCalculator);
    }

    // ── Item list ─────────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void showItemList_returnsIndexViewWithApprovedItemsPage() {
        List<Item> items = List.of(new Item());
        Page<Item> page = new PageImpl<>(items, PageRequest.of(0, 10), 1);
        when(itemRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(itemRepository.findDistinctCategoriesByStatus(ItemStatus.APPROVED)).thenReturn(List.of("electronics"));

        String view = itemsController.showItemList(model, null, null, 0);

        assertThat(view).isEqualTo("items/index");
        assertThat(model.getAttribute("items")).isEqualTo(items);
        assertThat(model.getAttribute("currentPage")).isEqualTo(0);
        assertThat(model.getAttribute("totalPages")).isEqualTo(1);
        assertThat(model.getAttribute("totalElements")).isEqualTo(1L);
        assertThat(model.getAttribute("categories")).isEqualTo(List.of("electronics"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void showItemList_withCategoryAndSearch_passesFiltersToModel() {
        Page<Item> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(itemRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(itemRepository.findDistinctCategoriesByStatus(ItemStatus.APPROVED)).thenReturn(List.of());

        String view = itemsController.showItemList(model, "electronics", "widget", 0);

        assertThat(view).isEqualTo("items/index");
        assertThat(model.getAttribute("category")).isEqualTo("electronics");
        assertThat(model.getAttribute("search")).isEqualTo("widget");
    }

    @Test
    @SuppressWarnings("unchecked")
    void showMyItems_returnsMyItemsViewWithOwnerItemsPage() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        List<Item> items = List.of(new Item());
        Page<Item> page = new PageImpl<>(items, PageRequest.of(0, 10), 1);
        when(itemRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        String view = itemsController.showMyItems(model, authentication, null, 0);

        assertThat(view).isEqualTo("items/MyItems");
        assertThat(model.getAttribute("items")).isEqualTo(items);
        assertThat(model.getAttribute("currentPage")).isEqualTo(0);
        assertThat(model.getAttribute("totalPages")).isEqualTo(1);
        assertThat(model.getAttribute("totalElements")).isEqualTo(1L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void showMyItems_withStatusFilter_passesStatusToModel() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        Page<Item> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(itemRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        String view = itemsController.showMyItems(model, authentication, "PENDING", 0);

        assertThat(view).isEqualTo("items/MyItems");
        assertThat(model.getAttribute("status")).isEqualTo("PENDING");
    }

    @Test
    @SuppressWarnings("unchecked")
    void showMyItems_withInvalidStatusFilter_stillReturnsView() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        Page<Item> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(itemRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        String view = itemsController.showMyItems(model, authentication, "NOT_A_STATUS", 0);

        assertThat(view).isEqualTo("items/MyItems");
        assertThat(model.getAttribute("status")).isEqualTo("NOT_A_STATUS");
    }

    // ── Item detail ──────────────────────────────────────────────────────────

    @Test
    void showItemDetail_itemNotFound_redirectsWithError() {
        when(itemRepository.findById(1)).thenReturn(Optional.empty());

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = itemsController.showItemDetail(1, model, authentication, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/items");
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isNotNull();
    }

    @Test
    void showItemDetail_approvedItem_anyAuthenticatedUserCanView() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        User otherOwner = new User();
        otherOwner.setId(2);

        Item item = new Item();
        item.setId(1);
        item.setStatus(ItemStatus.APPROVED);
        item.setOwner(otherOwner);
        when(itemRepository.findById(1)).thenReturn(Optional.of(item));

        String view = itemsController.showItemDetail(1, model, authentication, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("items/ItemDetail");
        assertThat(model.getAttribute("item")).isEqualTo(item);
        assertThat(model.getAttribute("isOwner")).isEqualTo(false);
    }

    @Test
    void showItemDetail_pendingItemOwnedByUser_returnsDetailView() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        Item item = new Item();
        item.setId(1);
        item.setStatus(ItemStatus.PENDING);
        item.setOwner(user);
        when(itemRepository.findById(1)).thenReturn(Optional.of(item));

        String view = itemsController.showItemDetail(1, model, authentication, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("items/ItemDetail");
        assertThat(model.getAttribute("isOwner")).isEqualTo(true);
    }

    @Test
    void showItemDetail_pendingItemNotOwnedByNonAdmin_redirectsWithError() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        User otherOwner = new User();
        otherOwner.setId(2);

        Item item = new Item();
        item.setId(1);
        item.setStatus(ItemStatus.PENDING);
        item.setOwner(otherOwner);
        when(itemRepository.findById(1)).thenReturn(Optional.of(item));

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = itemsController.showItemDetail(1, model, authentication, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/items");
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isNotNull();
    }

    @Test
    void showItemDetail_pendingItemViewedByAdmin_returnsDetailView() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        User otherOwner = new User();
        otherOwner.setId(2);

        Item item = new Item();
        item.setId(1);
        item.setStatus(ItemStatus.PENDING);
        item.setOwner(otherOwner);
        when(itemRepository.findById(1)).thenReturn(Optional.of(item));

        Authentication adminAuthentication = new UsernamePasswordAuthenticationToken(
                "testuser", "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        String view = itemsController.showItemDetail(1, model, adminAuthentication, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("items/ItemDetail");
        assertThat(model.getAttribute("isOwner")).isEqualTo(false);
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Test
    void showCreateItem_addsItemDTOAndReturnsCreateView() {
        String view = itemsController.showCreateItem(model);

        assertThat(view).isEqualTo("items/CreateItem");
        assertThat(model.getAttribute("itemDTO")).isInstanceOf(ItemDTO.class);
    }

    @Test
    void createItem_withValidationErrors_returnsCreateView() {
        ItemDTO itemDTO = new ItemDTO();
        BindingResult result = new BeanPropertyBindingResult(itemDTO, "itemDTO");
        result.rejectValue("category", "error.category", "Category must not be empty");

        String view = itemsController.createItem(itemDTO, result, authentication);

        assertThat(view).isEqualTo("items/CreateItem");
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void createItem_validData_savesPendingItemWithStorageTimesAndRedirects() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(5);

        ItemDTO itemDTO = new ItemDTO();
        itemDTO.setName("Widget");
        itemDTO.setBrand("Acme");
        itemDTO.setCategory("electronics");
        itemDTO.setQuantity(5);
        itemDTO.setDescription("A widget that does things and stuff for testing.");
        itemDTO.setImageFile(new MockMultipartFile("imageFile", "", "image/png", new byte[0]));
        itemDTO.setStorageStartTime(start);
        itemDTO.setStorageEndTime(end);

        BindingResult result = new BeanPropertyBindingResult(itemDTO, "itemDTO");

        String view = itemsController.createItem(itemDTO, result, authentication);

        assertThat(view).isEqualTo("redirect:/items/my");

        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(itemRepository).save(captor.capture());
        Item saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Widget");
        assertThat(saved.getStatus()).isEqualTo(ItemStatus.PENDING);
        assertThat(saved.getOwner()).isEqualTo(user);
        assertThat(saved.getStorageStartTime()).isEqualTo(start);
        assertThat(saved.getStorageEndTime()).isEqualTo(end);
    }

    // ── Edit ──────────────────────────────────────────────────────────────────

    @Test
    void showEditItem_itemNotFound_redirectsWithError() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(itemRepository.findByIdAndOwner(1, user)).thenReturn(Optional.empty());

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = itemsController.showEditItem(model, 1, authentication, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/items/my");
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isNotNull();
    }

    @Test
    void showEditItem_itemNotPending_redirectsWithError() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        Item item = new Item();
        item.setStatus(ItemStatus.APPROVED);
        when(itemRepository.findByIdAndOwner(1, user)).thenReturn(Optional.of(item));

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = itemsController.showEditItem(model, 1, authentication, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/items/my");
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isNotNull();
    }

    @Test
    void showEditItem_pendingItemOwnedByUser_returnsEditViewWithPopulatedDTO() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        Item item = new Item();
        item.setName("Widget");
        item.setBrand("Acme");
        item.setCategory("electronics");
        item.setQuantity(5);
        item.setDescription("A description that is long enough.");
        item.setStatus(ItemStatus.PENDING);
        when(itemRepository.findByIdAndOwner(1, user)).thenReturn(Optional.of(item));

        String view = itemsController.showEditItem(model, 1, authentication, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("items/EditItem");
        ItemDTO dto = (ItemDTO) model.getAttribute("itemDTO");
        assertThat(dto).isNotNull();
        assertThat(dto.getName()).isEqualTo("Widget");
        assertThat(dto.getBrand()).isEqualTo("Acme");
        assertThat(dto.getCategory()).isEqualTo("electronics");
        assertThat(dto.getQuantity()).isEqualTo(5);
        assertThat(model.getAttribute("id")).isEqualTo(1);
    }

    @Test
    void updateItem_itemNotEditable_redirectsWithError() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(itemRepository.findByIdAndOwner(1, user)).thenReturn(Optional.empty());

        ItemDTO itemDTO = new ItemDTO();
        BindingResult result = new BeanPropertyBindingResult(itemDTO, "itemDTO");

        String view = itemsController.updateItem(1, itemDTO, result, model, authentication, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/items/my");
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void updateItem_validationErrors_returnsEditViewWithId() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        Item item = new Item();
        item.setStatus(ItemStatus.PENDING);
        when(itemRepository.findByIdAndOwner(1, user)).thenReturn(Optional.of(item));

        ItemDTO itemDTO = new ItemDTO();
        BindingResult result = new BeanPropertyBindingResult(itemDTO, "itemDTO");
        result.rejectValue("category", "error.category", "Category must not be empty");

        String view = itemsController.updateItem(1, itemDTO, result, model, authentication, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("items/EditItem");
        assertThat(model.getAttribute("id")).isEqualTo(1);
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void updateItem_validData_updatesFieldsAndRedirects() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        Item item = new Item();
        item.setStatus(ItemStatus.PENDING);
        item.setName("Old Name");
        when(itemRepository.findByIdAndOwner(1, user)).thenReturn(Optional.of(item));

        ItemDTO itemDTO = new ItemDTO();
        itemDTO.setName("New Name");
        itemDTO.setBrand("New Brand");
        itemDTO.setCategory("books");
        itemDTO.setQuantity(10);
        itemDTO.setDescription("An updated description that is long enough.");
        itemDTO.setImageFile(new MockMultipartFile("imageFile", "", "image/png", new byte[0]));

        BindingResult result = new BeanPropertyBindingResult(itemDTO, "itemDTO");

        String view = itemsController.updateItem(1, itemDTO, result, model, authentication, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/items/my");
        assertThat(item.getName()).isEqualTo("New Name");
        assertThat(item.getBrand()).isEqualTo("New Brand");
        assertThat(item.getCategory()).isEqualTo("books");
        assertThat(item.getQuantity()).isEqualTo(10);
        verify(itemRepository).save(item);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Test
    void deleteItem_notPending_redirectsWithError() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        Item item = new Item();
        item.setStatus(ItemStatus.APPROVED);
        when(itemRepository.findByIdAndOwner(1, user)).thenReturn(Optional.of(item));

        String view = itemsController.deleteItem(1, authentication, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/items/my");
        verify(itemRepository, never()).delete(any(Item.class));
    }

    @Test
    void deleteItem_pendingItem_deletesAndRedirects() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        Item item = new Item();
        item.setStatus(ItemStatus.PENDING);
        when(itemRepository.findByIdAndOwner(1, user)).thenReturn(Optional.of(item));

        String view = itemsController.deleteItem(1, authentication, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/items/my");
        verify(itemRepository).delete(item);
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    @Test
    void cancelItem_invalidStatus_redirectsWithError() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        Item item = new Item();
        item.setStatus(ItemStatus.REJECTED);
        when(itemRepository.findByIdAndOwner(1, user)).thenReturn(Optional.of(item));

        String view = itemsController.cancelItem(1, authentication, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/items/my");
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void cancelItem_pendingItem_setsCancelledAndRedirects() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        Item item = new Item();
        item.setStatus(ItemStatus.PENDING);
        when(itemRepository.findByIdAndOwner(1, user)).thenReturn(Optional.of(item));
        when(serviceRecordRepository.findByItemIdAndStatus(0, ServiceRecordStatus.ACTIVE))
                .thenReturn(Optional.empty());

        String view = itemsController.cancelItem(1, authentication, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/items/my");
        assertThat(item.getStatus()).isEqualTo(ItemStatus.CANCELLED);
        verify(itemRepository).save(item);
    }

    @Test
    void cancelItem_approvedItem_setsCancelledAndRedirects() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        Item item = new Item();
        item.setStatus(ItemStatus.APPROVED);
        when(itemRepository.findByIdAndOwner(1, user)).thenReturn(Optional.of(item));
        when(serviceRecordRepository.findByItemIdAndStatus(0, ServiceRecordStatus.ACTIVE))
                .thenReturn(Optional.empty());

        String view = itemsController.cancelItem(1, authentication, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/items/my");
        assertThat(item.getStatus()).isEqualTo(ItemStatus.CANCELLED);
        verify(itemRepository).save(item);
    }

    @Test
    void cancelItem_withActiveServiceRecord_closesRecord() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        Item item = new Item();
        item.setStatus(ItemStatus.APPROVED);
        when(itemRepository.findByIdAndOwner(1, user)).thenReturn(Optional.of(item));

        ServiceRecord record = new ServiceRecord();
        record.setStartTime(LocalDateTime.now().minusHours(2));
        record.setStatus(ServiceRecordStatus.ACTIVE);
        when(serviceRecordRepository.findByItemIdAndStatus(0, ServiceRecordStatus.ACTIVE))
                .thenReturn(Optional.of(record));

        itemsController.cancelItem(1, authentication, new RedirectAttributesModelMap());

        ArgumentCaptor<ServiceRecord> captor = ArgumentCaptor.forClass(ServiceRecord.class);
        verify(serviceRecordRepository).save(captor.capture());
        ServiceRecord saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ServiceRecordStatus.ENDED);
        assertThat(saved.getEndTime()).isNotNull();
        assertThat(saved.getTotalHours()).isGreaterThanOrEqualTo(1);
        assertThat(saved.getTotalAmount()).isNotNull();
    }

    // ── Extension requests ───────────────────────────────────────────────────

    @Test
    void requestExtension_itemNotFound_redirectsWithError() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(itemRepository.findByIdAndOwner(1, user)).thenReturn(Optional.empty());

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = itemsController.requestExtension(1, LocalDateTime.now().plusDays(1), authentication, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/items/my");
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isNotNull();
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void requestExtension_itemNotApproved_redirectsWithError() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        Item item = new Item();
        item.setStatus(ItemStatus.PENDING);
        when(itemRepository.findByIdAndOwner(1, user)).thenReturn(Optional.of(item));

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = itemsController.requestExtension(1, LocalDateTime.now().plusDays(1), authentication, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/items/my");
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isNotNull();
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void requestExtension_newTimeNotAfterCurrentEndTime_redirectsWithError() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        LocalDateTime currentEnd = LocalDateTime.now().plusDays(1);

        Item item = new Item();
        item.setStatus(ItemStatus.APPROVED);
        item.setStorageEndTime(currentEnd);
        when(itemRepository.findByIdAndOwner(1, user)).thenReturn(Optional.of(item));

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = itemsController.requestExtension(1, currentEnd.minusHours(1), authentication, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/items/my");
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isNotNull();
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void requestExtension_validRequest_setsRequestedTimeAndRedirectsWithSuccess() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        LocalDateTime currentEnd = LocalDateTime.now().plusDays(1);
        LocalDateTime newEnd = currentEnd.plusDays(2);

        Item item = new Item();
        item.setStatus(ItemStatus.APPROVED);
        item.setStorageEndTime(currentEnd);
        when(itemRepository.findByIdAndOwner(1, user)).thenReturn(Optional.of(item));

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = itemsController.requestExtension(1, newEnd, authentication, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/items/my");
        assertThat(redirectAttributes.getFlashAttributes().get("success")).isNotNull();
        assertThat(item.getRequestedStorageEndTime()).isEqualTo(newEnd);
        verify(itemRepository).save(item);
    }

    @Test
    void cancelItem_withOverdueActiveServiceRecord_appliesLateFee() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        LocalDateTime now = LocalDateTime.now();

        Item item = new Item();
        item.setStatus(ItemStatus.APPROVED);
        item.setStorageEndTime(now.minusHours(2));
        when(itemRepository.findByIdAndOwner(1, user)).thenReturn(Optional.of(item));

        ServiceRecord record = new ServiceRecord();
        record.setStartTime(now.minusHours(5));
        record.setStatus(ServiceRecordStatus.ACTIVE);
        when(serviceRecordRepository.findByItemIdAndStatus(0, ServiceRecordStatus.ACTIVE))
                .thenReturn(Optional.of(record));

        itemsController.cancelItem(1, authentication, new RedirectAttributesModelMap());

        ArgumentCaptor<ServiceRecord> captor = ArgumentCaptor.forClass(ServiceRecord.class);
        verify(serviceRecordRepository).save(captor.capture());
        ServiceRecord saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ServiceRecordStatus.ENDED);
        // 3 normal hrs * RM5 + 2 late hrs * RM5 * 1.5 = 15 + 15 = 30
        assertThat(saved.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(30));
    }
}
