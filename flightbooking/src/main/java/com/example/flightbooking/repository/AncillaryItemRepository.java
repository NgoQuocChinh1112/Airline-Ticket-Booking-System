package com.example.flightbooking.repository;

import com.example.flightbooking.entity.AncillaryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AncillaryItemRepository extends JpaRepository<AncillaryItem, UUID> {
    boolean existsByCode(String code);
}