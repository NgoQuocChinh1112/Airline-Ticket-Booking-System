package com.example.flightbooking.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "flight_seats", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"flight_id", "seat_number"})
})
public class FlightSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @Column(name = "seat_number", nullable = false)
    private String seatNumber; // VD: 12A

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status = SeatStatus.AVAILABLE;

    // userId đang giữ ghế tạm thời (lưu dạng String để không phụ thuộc cứng vào User entity ở lớp này)
    @Column(name = "held_by")
    private String heldBy;

    @Column(name = "held_until")
    private Instant heldUntil;

    @Column(name = "seat_class", nullable = false, length = 32)
    private String seatClass = "ECONOMY";

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price = new BigDecimal("1200000");

    @Version
    private Long version;

    public FlightSeat() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Flight getFlight() { return flight; }
    public void setFlight(Flight flight) { this.flight = flight; }

    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }

    public SeatStatus getStatus() { return status; }
    public void setStatus(SeatStatus status) { this.status = status; }

    public String getHeldBy() { return heldBy; }
    public void setHeldBy(String heldBy) { this.heldBy = heldBy; }

    public Instant getHeldUntil() { return heldUntil; }
    public void setHeldUntil(Instant heldUntil) { this.heldUntil = heldUntil; }

    public String getSeatClass() { return seatClass; }
    public void setSeatClass(String seatClass) { this.seatClass = seatClass; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
