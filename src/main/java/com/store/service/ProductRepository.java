package com.store.service;

import org.springframework.data.jpa.repository.JpaRepository;

import com.store.model.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {

}
