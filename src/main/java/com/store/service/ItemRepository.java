package com.store.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.store.model.Item;
import com.store.model.ItemStatus;
import com.store.model.User;

public interface ItemRepository extends JpaRepository<Item, Integer> {

    List<Item> findByStatus(ItemStatus status);

    List<Item> findByOwnerOrderByCreatedAtDesc(User owner);

    Optional<Item> findByIdAndOwner(int id, User owner);
}
