package com.example.flightbooking.service;

import com.example.flightbooking.entity.FlightSeat;
import com.example.flightbooking.entity.SeatStatus;
import com.example.flightbooking.repository.FlightSeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Ở bản Node.js, việc tự động nhả ghế dùng BullMQ (delayed job, nhả đúng thời điểm cho từng ghế).
 * Ở bản Java này, để đơn giản hoá cho sườn học tập, ta dùng cách phổ biến không kém: một job
 * chạy định kỳ (polling) quét các ghế đã hết hạn "held" và trả về "available".
 * Nếu muốn chính xác đến từng giây như BullMQ, có thể thay bằng Spring's TaskScheduler
 * kết hợp ScheduledFuture cho từng ghế, hoặc dùng Redis keyspace notification.
 */
@Component
public class SeatReleaseScheduler {

    private static final Logger log = LoggerFactory.getLogger(SeatReleaseScheduler.class);

    private final FlightSeatRepository flightSeatRepository;
    private final RedisLockService redisLockService;

    public SeatReleaseScheduler(FlightSeatRepository flightSeatRepository, RedisLockService redisLockService) {
        this.flightSeatRepository = flightSeatRepository;
        this.redisLockService = redisLockService;
    }

    // Quét mỗi 30 giây một lần
    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void releaseExpiredSeats() {
        List<FlightSeat> expiredSeats = flightSeatRepository.findByStatusAndHeldUntilBefore(SeatStatus.HELD, Instant.now());

        for (FlightSeat seat : expiredSeats) {
            String ownerId = seat.getHeldBy();
            String flightId = seat.getFlight().getId().toString();

            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setHeldBy(null);
            seat.setHeldUntil(null);
            flightSeatRepository.save(seat);

            if (ownerId != null) {
                redisLockService.releaseSeatLock(flightId, seat.getId().toString(), ownerId);
            }

            log.info("Đã tự động nhả ghế {} (flight {}) vì hết hạn giữ chỗ", seat.getSeatNumber(), flightId);
        }
    }
}
