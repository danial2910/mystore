package com.store.service;

import com.store.model.ServiceRecord;
import com.store.model.ServiceRecordStatus;
import com.store.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceRecordRepository extends JpaRepository<ServiceRecord, Integer> {
    List<ServiceRecord> findByOwnerOrderByStartTimeDesc(User owner);
    Optional<ServiceRecord> findByItemIdAndStatus(int itemId, ServiceRecordStatus status);
}
