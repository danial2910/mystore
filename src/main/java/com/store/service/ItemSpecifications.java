package com.store.service;

import org.springframework.data.jpa.domain.Specification;

import com.store.model.Item;
import com.store.model.ItemStatus;
import com.store.model.User;

public final class ItemSpecifications {

    private ItemSpecifications() {
    }

    public static Specification<Item> hasStatus(ItemStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Item> hasCategory(String category) {
        return (root, query, cb) -> (category == null || category.isBlank())
                ? null
                : cb.equal(root.get("category"), category);
    }

    public static Specification<Item> nameContains(String search) {
        return (root, query, cb) -> (search == null || search.isBlank())
                ? null
                : cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%");
    }

    public static Specification<Item> hasOwner(User owner) {
        return (root, query, cb) -> owner == null ? null : cb.equal(root.get("owner"), owner);
    }
}
