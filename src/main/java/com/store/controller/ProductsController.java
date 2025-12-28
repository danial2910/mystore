package com.store.controller;

import java.io.InputStream;
import java.nio.file.*;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.store.service.ProductRepository;

import jakarta.validation.Valid;

import com.store.model.Product;
import com.store.model.ProductDTO;



@Controller
@RequestMapping("/products")
public class ProductsController {

    @Autowired
    private ProductRepository productRepository;

    @GetMapping({"", "/"})
    public String showProductList (Model model){
        List<Product> products = productRepository.findAll();
        model.addAttribute("products", products);
        return "products/index";
    }

    @GetMapping("/create")
    public String showPCreateproduct (Model model){
        ProductDTO productDTO = new ProductDTO();
        model.addAttribute(	"productDTO", productDTO);
        return "products/CreateProduct";
    }

    @PostMapping("/create")
    public String createProduct(@Valid @ModelAttribute ProductDTO productDTO, BindingResult result) {

        if(productDTO.getImageFileName().isEmpty()){
            result.addError(new FieldError("productDTO","imageFileName", "Image File is required"));
        }

        if(result.hasErrors()){
            return "products/CreateProduct";
        }

        MultipartFile image = productDTO.getImageFileName();
        Date createdAt = new Date();
        String storageFileName = image.getOriginalFilename();

        try{
            String uploadDir = "public/images/";
            Path uploadPath = Paths.get(uploadDir);

            if(!Files.exists(uploadPath)){
                Files.createDirectories(uploadPath);
            }

            try(InputStream inputStream = image.getInputStream()){
                Files.copy(inputStream, Paths.get(uploadDir + storageFileName), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ex){
            System.out.println("Exception: " + ex.getMessage());
        }

        Product product = new Product();
        product.setName(productDTO.getName());
        product.setBrand(productDTO.getBrand());    
        product.setCategory(productDTO.getCategory());
        product.setPrice(productDTO.getPrice());
        product.setDescription(productDTO.getDescription());
        product.setImageFileName(storageFileName);
        product.setCreatedAt(createdAt); 

        productRepository.save(product);

        return "redirect:/products";
    }

    @GetMapping("/edit/{id}")
    public String showEditPage(Model model, @PathVariable int id){
        try{
            Product product = productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid product Id:" + id));
            ProductDTO productDTO = new ProductDTO();
            productDTO.setName(product.getName());
            productDTO.setBrand(product.getBrand());
            productDTO.setCategory(product.getCategory());
            productDTO.setPrice(product.getPrice());
            productDTO.setDescription(product.getDescription());

            model.addAttribute("productDTO", productDTO);
        }
        catch(Exception ex){
            System.out.println("Exception message:" + ex.getMessage());
            return "redirect:/products:";
        }
        return "products/EditProduct";
    }

    @PostMapping("/edit/{id}")
    public String updateProduct(Model model, @PathVariable int id, @Valid @ModelAttribute ProductDTO productDTO, BindingResult result){
        try{
            Product product = productRepository.findById(id).get();
            model.addAttribute("product", product);

            if(result.hasErrors()){
                return "redirect:/EditProduct";
            }

            if(!productDTO.getImageFileName().isEmpty()){
                String uploadDir = "public/images/";
                Path oldImagePath = Paths.get(uploadDir + product.getImageFileName());

                try{
                    Files.delete(oldImagePath);
                }
                catch(Exception ex){
                    System.out.println("Exception message:" + ex.getMessage());
                }

                MultipartFile image = productDTO.getImageFileName();
                String storageFileName = image.getOriginalFilename();

                try(InputStream inputStream = image.getInputStream()){
                    Files.copy(inputStream, Paths.get(uploadDir + storageFileName),StandardCopyOption.REPLACE_EXISTING);
                }

                product.setImageFileName(storageFileName);
            }

            product.setName(productDTO.getName());
            product.setBrand(productDTO.getBrand());
            product.setCategory(productDTO.getCategory());
            product.setPrice(productDTO.getPrice());
            product.setDescription(productDTO.getDescription());

            productRepository.save(product);
        }
        catch(Exception ex){
            System.out.println("Exception message:" + ex.getMessage());
            return "redirect:/products";
        }
        return "redirect:/products";
    }

    @GetMapping("/delete/{id}")
    public String deleteProduct(Model model, @PathVariable int id){
        try{
            Product product = productRepository.findById(id).get();

            Path imageFilePath = Paths.get("public/images/" + product.getImageFileName());

            try{
                Files.delete(imageFilePath);
            }
            catch(Exception ex){
                System.out.println("Exception message:" + ex.getMessage());
            }

            productRepository.delete(product);

        }
        catch(Exception ex){
            System.out.println(" Exception Message:" + ex.getMessage());
            return "redirect:/products";
        }
        return "redirect:/products";
    }

 }

