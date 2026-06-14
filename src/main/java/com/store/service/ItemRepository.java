package com.store.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.store.model.Item;
import com.store.model.ItemStatus;
import com.store.model.User;

public interface ItemRepository extends JpaRepository<Item, Integer>, JpaSpecificationExecutor<Item> {

    List<Item> findByStatus(ItemStatus status);

    long countByStatus(ItemStatus status);

    List<Item> findByOwnerOrderByCreatedAtDesc(User owner);

    Optional<Item> findByIdAndOwner(int id, User owner);

    List<Item> findByRequestedStorageEndTimeIsNotNull();

    long countByRequestedStorageEndTimeIsNotNull();

    @Query("SELECT DISTINCT i.category FROM Item i WHERE i.status = :status ORDER BY i.category")
    List<String> findDistinctCategoriesByStatus(@Param("status") ItemStatus status);
}
