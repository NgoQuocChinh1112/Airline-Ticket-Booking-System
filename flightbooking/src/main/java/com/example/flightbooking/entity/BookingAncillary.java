package com.example.flightbooking.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity // lịch sử sử dụng dịch vụ
@Table(name = "booking_ancillaries")
public class BookingAncillary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ancillary_item_id", nullable = false)
    private AncillaryItem ancillaryItem;

    @Column(nullable = false)
    private Integer quantity; // Số lượng mua (VD: 2 kiện hành lý)

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price; // Lưu lại giá tại thời điểm mua để tránh việc sau này hãng đổi giá làm sai lệch bill cũ

    public BookingAncillary() {}

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public AncillaryItem getAncillaryItem() { return ancillaryItem; }
    public void setAncillaryItem(AncillaryItem ancillaryItem) { this.ancillaryItem = ancillaryItem; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}