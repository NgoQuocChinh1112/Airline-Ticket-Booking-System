package com.example.flightbooking.controller;

import com.example.flightbooking.entity.AncillaryItem;
import com.example.flightbooking.repository.AncillaryItemRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/ancillaries")
public class AdminAirlineController {

    private final AncillaryItemRepository ancillaryItemRepository;

    public AdminAirlineController(AncillaryItemRepository ancillaryItemRepository) {
        this.ancillaryItemRepository = ancillaryItemRepository;
    }

    // Lấy danh sách tất cả các dịch vụ đi kèm
    @GetMapping
    public List<AncillaryItem> getAllAncillaries() {
        return ancillaryItemRepository.findAll();
    }

    // Thêm mới một dịch vụ đi kèm (VD: Hành lý, Suất ăn)
    @PostMapping
    public ResponseEntity<?> createAncillary(@RequestBody AncillaryItem item) {
        if (ancillaryItemRepository.existsByCode(item.getCode())) {
            return ResponseEntity.badRequest().body("Mã code dịch vụ đã tồn tại!");
        }
        AncillaryItem savedItem = ancillaryItemRepository.save(item);
        return ResponseEntity.ok(savedItem);
    }
}