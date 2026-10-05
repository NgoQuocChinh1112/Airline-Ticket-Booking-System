package com.example.flightbooking.repository;

import com.example.flightbooking.entity.FlightSeat;
import com.example.flightbooking.entity.SeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlightSeatRepository extends JpaRepository<FlightSeat, UUID> {

    List<FlightSeat> findByFlightIdOrderBySeatNumberAsc(UUID flightId);

    List<FlightSeat> findByIdIn(List<UUID> ids);

    /**
     * PESSIMISTIC_WRITE = tương đương "SELECT ... FOR UPDATE" trong SQL thuần.
     * Transaction thứ 2 gọi hàm này trên cùng 1 dòng sẽ phải CHỜ transaction thứ 1
     * commit/rollback xong mới đọc được -> đây là Lớp 2 chống race condition.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from FlightSeat s where s.id = :id")
    Optional<FlightSeat> findByIdForUpdate(@Param("id") UUID id);

    // Dùng cho job tự động nhả ghế hết hạn giữ chỗ (thay thế cho BullMQ ở bản Node.js)
    List<FlightSeat> findByStatusAndHeldUntilBefore(SeatStatus status, Instant time);
}
