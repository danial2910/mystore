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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.store.model.Item;
import com.store.model.ItemDTO;
import com.store.model.ItemStatus;
import com.store.model.User;
import com.store.service.ItemRepository;
import com.store.service.UserRepository;

@ExtendWith(MockitoExtension.class)
class ItemsControllerTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserRepository userRepository;

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
    }

    @Test
    void showItemList_returnsIndexViewWithApprovedItems() {
        List<Item> items = List.of(new Item());
        when(itemRepository.findByStatus(ItemStatus.APPROVED)).thenReturn(items);

        String view = itemsController.showItemList(model);

        assertThat(view).isEqualTo("items/index");
        assertThat(model.getAttribute("items")).isEqualTo(items);
    }

    @Test
    void showMyItems_returnsMyItemsViewWithOwnerItems() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        List<Item> items = List.of(new Item());
        when(itemRepository.findByOwnerOrderByCreatedAtDesc(user)).thenReturn(items);

        String view = itemsController.showMyItems(model, authentication);

        assertThat(view).isEqualTo("items/MyItems");
        assertThat(model.getAttribute("items")).isEqualTo(items);
    }

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
    void createItem_validData_savesPendingItemOwnedByUserAndRedirects() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        ItemDTO itemDTO = new ItemDTO();
        itemDTO.setName("Widget");
        itemDTO.setBrand("Acme");
        itemDTO.setCategory("electronics");
        itemDTO.setQuantity(5);
        itemDTO.setDescription("A widget that does things and stuff for testing.");
        itemDTO.setImageFile(new MockMultipartFile("imageFile", "", "image/png", new byte[0]));

        BindingResult result = new BeanPropertyBindingResult(itemDTO, "itemDTO");

        String view = itemsController.createItem(itemDTO, result, authentication);

        assertThat(view).isEqualTo("redirect:/items/my");

        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(itemRepository).save(captor.capture());
        Item saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Widget");
        assertThat(saved.getBrand()).isEqualTo("Acme");
        assertThat(saved.getCategory()).isEqualTo("electronics");
        assertThat(saved.getQuantity()).isEqualTo(5);
        assertThat(saved.getStatus()).isEqualTo(ItemStatus.PENDING);
        assertThat(saved.getOwner()).isEqualTo(user);
        assertThat(saved.getImageFileName()).isNull();
    }

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

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = itemsController.showEditItem(model, 1, authentication, redirectAttributes);

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
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = itemsController.updateItem(1, itemDTO, result, model, authentication, redirectAttributes);

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
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = itemsController.updateItem(1, itemDTO, result, model, authentication, redirectAttributes);

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
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = itemsController.updateItem(1, itemDTO, result, model, authentication, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/items/my");
        assertThat(item.getName()).isEqualTo("New Name");
        assertThat(item.getBrand()).isEqualTo("New Brand");
        assertThat(item.getCategory()).isEqualTo("books");
        assertThat(item.getQuantity()).isEqualTo(10);
        verify(itemRepository).save(item);
    }

    @Test
    void deleteItem_notPending_redirectsWithError() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        Item item = new Item();
        item.setStatus(ItemStatus.APPROVED);
        when(itemRepository.findByIdAndOwner(1, user)).thenReturn(Optional.of(item));

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = itemsController.deleteItem(1, authentication, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/items/my");
        verify(itemRepository, never()).delete(any(Item.class));
    }

    @Test
    void deleteItem_pendingItem_deletesAndRedirects() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        Item item = new Item();
        item.setStatus(ItemStatus.PENDING);
        when(itemRepository.findByIdAndOwner(1, user)).thenReturn(Optional.of(item));

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = itemsController.deleteItem(1, authentication, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/items/my");
        verify(itemRepository).delete(item);
    }

    @Test
    void cancelItem_invalidStatus_redirectsWithError() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        Item item = new Item();
        item.setStatus(ItemStatus.REJECTED);
        when(itemRepository.findByIdAndOwner(1, user)).thenReturn(Optional.of(item));

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = itemsController.cancelItem(1, authentication, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/items/my");
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void cancelItem_approvedItem_setsCancelledAndRedirects() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        Item item = new Item();
        item.setStatus(ItemStatus.APPROVED);
        when(itemRepository.findByIdAndOwner(1, user)).thenReturn(Optional.of(item));

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = itemsController.cancelItem(1, authentication, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/items/my");
        assertThat(item.getStatus()).isEqualTo(ItemStatus.CANCELLED);
        verify(itemRepository).save(item);
    }
}
