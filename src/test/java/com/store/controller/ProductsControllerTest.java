package com.store.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import com.store.model.Product;
import com.store.model.ProductDTO;
import com.store.service.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductsControllerTest {

    private static final String UPLOAD_DIR = "public/images/";

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductsController productsController;

    private Model model;

    @BeforeEach
    void setUp() {
        model = new ExtendedModelMap();
    }

    @Test
    void showProductList_addsProductsToModelAndReturnsIndexView() {
        List<Product> products = List.of(new Product(), new Product());
        when(productRepository.findAll()).thenReturn(products);

        String view = productsController.showProductList(model);

        assertThat(view).isEqualTo("products/index");
        assertThat(model.getAttribute("products")).isEqualTo(products);
    }

    @Test
    void showCreateProductForm_addsProductDTOToModelAndReturnsCreateView() {
        String view = productsController.showPCreateproduct(model);

        assertThat(view).isEqualTo("products/CreateProduct");
        assertThat(model.getAttribute("productDTO")).isInstanceOf(ProductDTO.class);
    }

    @Test
    void createProduct_emptyImage_returnsCreateViewWithErrorAndDoesNotSave() {
        ProductDTO productDTO = new ProductDTO();
        productDTO.setName("Sword");
        productDTO.setBrand("Acme");
        productDTO.setCategory("Weapons");
        productDTO.setPrice(9.99);
        productDTO.setDescription("A sharp and shiny sword");
        productDTO.setImageFileName(new MockMultipartFile("imageFileName", new byte[0]));

        BindingResult result = new BeanPropertyBindingResult(productDTO, "productDTO");

        String view = productsController.createProduct(productDTO, result);

        assertThat(view).isEqualTo("products/CreateProduct");
        assertThat(result.hasFieldErrors("imageFileName")).isTrue();
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createProduct_validData_savesProductAndRedirectsToProducts() throws IOException {
        String fileName = "test-upload-" + System.nanoTime() + ".jpg";
        ProductDTO productDTO = new ProductDTO();
        productDTO.setName("Sword");
        productDTO.setBrand("Acme");
        productDTO.setCategory("Weapons");
        productDTO.setPrice(9.99);
        productDTO.setDescription("A sharp and shiny sword");
        productDTO.setImageFileName(new MockMultipartFile("imageFileName", fileName, "image/jpeg",
                "image-bytes".getBytes(StandardCharsets.UTF_8)));

        BindingResult result = new BeanPropertyBindingResult(productDTO, "productDTO");

        try {
            String view = productsController.createProduct(productDTO, result);

            assertThat(view).isEqualTo("redirect:/products");

            ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).save(productCaptor.capture());
            Product saved = productCaptor.getValue();
            assertThat(saved.getName()).isEqualTo("Sword");
            assertThat(saved.getBrand()).isEqualTo("Acme");
            assertThat(saved.getCategory()).isEqualTo("Weapons");
            assertThat(saved.getPrice()).isEqualTo(9.99);
            assertThat(saved.getDescription()).isEqualTo("A sharp and shiny sword");
            assertThat(saved.getImageFileName()).isEqualTo(fileName);
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(Files.exists(Paths.get(UPLOAD_DIR + fileName))).isTrue();
        } finally {
            Files.deleteIfExists(Paths.get(UPLOAD_DIR + fileName));
        }
    }

    @Test
    void createProduct_whenUploadDirDoesNotExist_createsDirectoryAndSaves() {
        String fileName = "sword.jpg";
        ProductDTO productDTO = new ProductDTO();
        productDTO.setName("Sword");
        productDTO.setBrand("Acme");
        productDTO.setCategory("Weapons");
        productDTO.setPrice(9.99);
        productDTO.setDescription("A sharp and shiny sword");
        productDTO.setImageFileName(new MockMultipartFile("imageFileName", fileName, "image/jpeg",
                "image-bytes".getBytes(StandardCharsets.UTF_8)));

        BindingResult result = new BeanPropertyBindingResult(productDTO, "productDTO");

        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(any(Path.class))).thenReturn(false);
            mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(Paths.get(UPLOAD_DIR));
            mockedFiles.when(() -> Files.copy(any(InputStream.class), any(Path.class), any())).thenReturn(0L);

            String view = productsController.createProduct(productDTO, result);

            assertThat(view).isEqualTo("redirect:/products");
            mockedFiles.verify(() -> Files.createDirectories(any(Path.class)));
            verify(productRepository).save(any(Product.class));
        }
    }

    @Test
    void createProduct_fileUploadFails_savesProductAndRedirects() {
        String fileName = "sword.jpg";
        ProductDTO productDTO = new ProductDTO();
        productDTO.setName("Sword");
        productDTO.setBrand("Acme");
        productDTO.setCategory("Weapons");
        productDTO.setPrice(9.99);
        productDTO.setDescription("A sharp and shiny sword");
        productDTO.setImageFileName(new MockMultipartFile("imageFileName", fileName, "image/jpeg",
                "image-bytes".getBytes(StandardCharsets.UTF_8)));

        BindingResult result = new BeanPropertyBindingResult(productDTO, "productDTO");

        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(any(Path.class))).thenReturn(true);
            mockedFiles.when(() -> Files.copy(any(InputStream.class), any(Path.class), any()))
                       .thenThrow(new IOException("disk full"));

            String view = productsController.createProduct(productDTO, result);

            // exception is caught silently; product is still saved
            assertThat(view).isEqualTo("redirect:/products");
            verify(productRepository).save(any(Product.class));
        }
    }

    @Test
    void showEditPage_existingProduct_addsProductDTOAndIdToModelAndReturnsEditView() {
        Product product = new Product();
        product.setId(1);
        product.setName("Sword");
        product.setBrand("Acme");
        product.setCategory("Weapons");
        product.setPrice(19.99);
        product.setDescription("A sharp and shiny sword");

        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        String view = productsController.showEditPage(model, 1);

        assertThat(view).isEqualTo("products/EditProduct");
        assertThat(model.getAttribute("id")).isEqualTo(1);

        ProductDTO productDTO = (ProductDTO) model.getAttribute("productDTO");
        assertThat(productDTO).isNotNull();
        assertThat(productDTO.getName()).isEqualTo("Sword");
        assertThat(productDTO.getBrand()).isEqualTo("Acme");
        assertThat(productDTO.getCategory()).isEqualTo("Weapons");
        assertThat(productDTO.getPrice()).isEqualTo(19.99);
        assertThat(productDTO.getDescription()).isEqualTo("A sharp and shiny sword");
    }

    @Test
    void showEditPage_unknownProduct_redirectsToProducts() {
        when(productRepository.findById(99)).thenReturn(Optional.empty());

        String view = productsController.showEditPage(model, 99);

        assertThat(view).isEqualTo("redirect:/products:");
    }

    @Test
    void updateProduct_validDataWithoutNewImage_updatesProductAndRedirects() {
        Product existing = new Product();
        existing.setId(1);
        existing.setName("Old name");
        existing.setBrand("Old brand");
        existing.setCategory("Old category");
        existing.setPrice(1.0);
        existing.setDescription("Old description");
        existing.setImageFileName("old.jpg");

        when(productRepository.findById(1)).thenReturn(Optional.of(existing));

        ProductDTO productDTO = new ProductDTO();
        productDTO.setName("New name");
        productDTO.setBrand("New brand");
        productDTO.setCategory("New category");
        productDTO.setPrice(2.0);
        productDTO.setDescription("New description");
        productDTO.setImageFileName(new MockMultipartFile("imageFileName", new byte[0]));

        BindingResult result = new BeanPropertyBindingResult(productDTO, "productDTO");

        String view = productsController.updateProduct(model, 1, productDTO, result);

        assertThat(view).isEqualTo("redirect:/products");

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        Product saved = productCaptor.getValue();
        assertThat(saved.getName()).isEqualTo("New name");
        assertThat(saved.getBrand()).isEqualTo("New brand");
        assertThat(saved.getCategory()).isEqualTo("New category");
        assertThat(saved.getPrice()).isEqualTo(2.0);
        assertThat(saved.getDescription()).isEqualTo("New description");
        assertThat(saved.getImageFileName()).isEqualTo("old.jpg");
    }

    @Test
    void updateProduct_validDataWithNewImage_updatesImageAndRedirects() {
        String newFileName = "new-image.jpg";
        Product existing = new Product();
        existing.setId(1);
        existing.setName("Old name");
        existing.setBrand("Old brand");
        existing.setCategory("Old category");
        existing.setPrice(1.0);
        existing.setDescription("Old description");
        existing.setImageFileName("old.jpg");

        when(productRepository.findById(1)).thenReturn(Optional.of(existing));

        ProductDTO productDTO = new ProductDTO();
        productDTO.setName("New name");
        productDTO.setBrand("New brand");
        productDTO.setCategory("New category");
        productDTO.setPrice(2.0);
        productDTO.setDescription("New description");
        productDTO.setImageFileName(new MockMultipartFile("imageFileName", newFileName, "image/jpeg",
                "image-bytes".getBytes(StandardCharsets.UTF_8)));

        BindingResult result = new BeanPropertyBindingResult(productDTO, "productDTO");

        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.copy(any(InputStream.class), any(Path.class), any())).thenReturn(0L);

            String view = productsController.updateProduct(model, 1, productDTO, result);

            assertThat(view).isEqualTo("redirect:/products");

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).save(captor.capture());
            assertThat(captor.getValue().getImageFileName()).isEqualTo(newFileName);
        }
    }

    @Test
    void updateProduct_fileUploadFails_catchesExceptionAndRedirectsToProducts() {
        Product existing = new Product();
        existing.setId(1);
        existing.setImageFileName("old.jpg");

        when(productRepository.findById(1)).thenReturn(Optional.of(existing));

        ProductDTO productDTO = new ProductDTO();
        productDTO.setName("New name");
        productDTO.setBrand("New brand");
        productDTO.setCategory("New category");
        productDTO.setPrice(2.0);
        productDTO.setDescription("New description");
        productDTO.setImageFileName(new MockMultipartFile("imageFileName", "new.jpg", "image/jpeg",
                "image-bytes".getBytes(StandardCharsets.UTF_8)));

        BindingResult result = new BeanPropertyBindingResult(productDTO, "productDTO");

        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.copy(any(InputStream.class), any(Path.class), any()))
                       .thenThrow(new IOException("disk full"));

            String view = productsController.updateProduct(model, 1, productDTO, result);

            assertThat(view).isEqualTo("redirect:/products");
            verify(productRepository, never()).save(any(Product.class));
        }
    }

    @Test
    void updateProduct_resultHasErrors_redirectsToEditProduct() {
        Product existing = new Product();
        existing.setId(1);

        when(productRepository.findById(1)).thenReturn(Optional.of(existing));

        ProductDTO productDTO = new ProductDTO();
        BindingResult result = new BeanPropertyBindingResult(productDTO, "productDTO");
        result.addError(new FieldError("productDTO", "name", "Product name must not be empty"));

        String view = productsController.updateProduct(model, 1, productDTO, result);

        assertThat(view).isEqualTo("redirect:/products/edit/1");
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProduct_unknownProduct_redirectsToProducts() {
        when(productRepository.findById(99)).thenReturn(Optional.empty());

        ProductDTO productDTO = new ProductDTO();
        BindingResult result = new BeanPropertyBindingResult(productDTO, "productDTO");

        String view = productsController.updateProduct(model, 99, productDTO, result);

        assertThat(view).isEqualTo("redirect:/products");
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void deleteProduct_existingProduct_deletesProductAndRedirects() {
        Product product = new Product();
        product.setId(1);
        product.setImageFileName("nonexistent-image.jpg");

        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        String view = productsController.deleteProduct(model, 1);

        assertThat(view).isEqualTo("redirect:/products");
        verify(productRepository).delete(product);
    }

    @Test
    void deleteProduct_unknownProduct_redirectsToProducts() {
        when(productRepository.findById(99)).thenReturn(Optional.empty());

        String view = productsController.deleteProduct(model, 99);

        assertThat(view).isEqualTo("redirect:/products");
        verify(productRepository, never()).delete(any(Product.class));
    }
}
