package com.example.flightbooking.service;

import com.example.flightbooking.dto.CreateBookingRequest;
import com.example.flightbooking.dto.HoldSeatRequest;
import com.example.flightbooking.dto.PassengerDto;
import com.example.flightbooking.entity.*;
import com.example.flightbooking.exception.ApiException;
import com.example.flightbooking.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final FlightRepository flightRepository;
    private final FlightSeatRepository flightSeatRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final RedisLockService redisLockService;
    private final long holdSeconds;

    public BookingService(FlightRepository flightRepository, FlightSeatRepository flightSeatRepository,
                           BookingRepository bookingRepository, UserRepository userRepository,
                           RedisLockService redisLockService,
                           @Value("${app.seat-hold-seconds}") long holdSeconds) {
        this.flightRepository = flightRepository;
        this.flightSeatRepository = flightSeatRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.redisLockService = redisLockService;
        this.holdSeconds = holdSeconds;
    }

    /**
     * BƯỚC 1 - Giữ ghế tạm thời.
     * Lớp 1: Redis lock (nhanh, chặn ngay từ đầu).
     * Lớp 2: PESSIMISTIC_WRITE lock trong transaction (tương đương SELECT ... FOR UPDATE),
     *        double-check trạng thái ghế ngay trước khi ghi.
     */
    @Transactional
    public FlightSeat holdSeat(String userId, HoldSeatRequest req) {
        UUID flightId = UUID.fromString(req.getFlightId());
        UUID seatId = UUID.fromString(req.getSeatId());

        // --- Lớp 1: Redis distributed lock ---
        boolean acquired = redisLockService.acquireSeatLock(req.getFlightId(), req.getSeatId(), userId, holdSeconds);
        if (!acquired) {
            throw ApiException.conflict("Ghế này đang được người khác giữ, vui lòng chọn ghế khác");
        }

        try {
            // --- Lớp 2: PESSIMISTIC_WRITE (SELECT ... FOR UPDATE) + double check ---
            FlightSeat seat = flightSeatRepository.findByIdForUpdate(seatId)
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy ghế"));

            if (!seat.getFlight().getId().equals(flightId)) {
                throw ApiException.badRequest("Ghế không thuộc chuyến bay này");
            }

            Instant now = Instant.now();
            boolean isFreeToTake = seat.getStatus() == SeatStatus.AVAILABLE
                    || (seat.getStatus() == SeatStatus.HELD && seat.getHeldUntil() != null && seat.getHeldUntil().isBefore(now));

            if (!isFreeToTake) {
                throw ApiException.conflict("Ghế đã được người khác giữ hoặc đặt");
            }

            seat.setStatus(SeatStatus.HELD);
            seat.setHeldBy(userId);
            seat.setHeldUntil(now.plusSeconds(holdSeconds));
            flightSeatRepository.save(seat);

            return seat;
        } catch (RuntimeException ex) {
            // Nếu có lỗi ở tầng DB, phải nhả Redis lock ngay để không "khóa chết" ghế
            redisLockService.releaseSeatLock(req.getFlightId(), req.getSeatId(), userId);
            throw ex;
        }
    }

    /**
     * BƯỚC 2 - Tạo booking "pending" sau khi user điền thông tin hành khách.
     * Yêu cầu tất cả ghế đang được đúng userId này giữ.
     */
    @Transactional
    public Booking createBooking(String userId, CreateBookingRequest req) {
        if (req.getSeatIds().size() != req.getPassengers().size()) {
            throw ApiException.badRequest("Số lượng ghế và số lượng hành khách phải bằng nhau");
        }

        UUID flightId = UUID.fromString(req.getFlightId());
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy chuyến bay"));

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy user"));

        List<UUID> seatUuids = req.getSeatIds().stream().map(UUID::fromString).collect(Collectors.toList());
        List<FlightSeat> seats = flightSeatRepository.findByIdIn(seatUuids);

        if (seats.size() != seatUuids.size()) {
            throw ApiException.notFound("Một số ghế không tồn tại");
        }

        for (FlightSeat seat : seats) {
            if (seat.getStatus() != SeatStatus.HELD || !userId.equals(seat.getHeldBy())) {
                throw ApiException.conflict(
                        "Ghế " + seat.getSeatNumber() + " không còn được bạn giữ (có thể đã hết hạn), vui lòng chọn lại");
            }
        }

        BigDecimal totalPrice = flight.getPrice().multiply(BigDecimal.valueOf(seats.size()));

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setFlight(flight);
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalPrice(totalPrice);

        // Map seatId -> passengerDto tương ứng theo thứ tự trong mảng
        Map<String, PassengerDto> seatIdToPassenger = mapSeatToPassenger(req);

        for (FlightSeat seat : seats) {
            BookingSeat bs = new BookingSeat();
            bs.setBooking(booking);
            bs.setFlightSeat(seat);
            booking.getSeats().add(bs);

            PassengerDto pDto = seatIdToPassenger.get(seat.getId().toString());
            Passenger passenger = new Passenger();
            passenger.setBooking(booking);
            passenger.setFullName(pDto.getFullName());
            passenger.setPassportNumber(pDto.getPassportNumber());
            passenger.setDob(LocalDate.parse(pDto.getDob()));
            booking.getPassengers().add(passenger);
        }

        return bookingRepository.save(booking);
    }

    private Map<String, PassengerDto> mapSeatToPassenger(CreateBookingRequest req) {
        Map<String, PassengerDto> map = new java.util.HashMap<>();
        for (int i = 0; i < req.getSeatIds().size(); i++) {
            map.put(req.getSeatIds().get(i), req.getPassengers().get(i));
        }
        return map;
    }

    /**
     * BƯỚC 3 - Xác nhận thanh toán thành công (gọi từ payment webhook trong thực tế).
     * Đây là điểm ghi nhận cuối: booking -> CONFIRMED, ghế -> BOOKED.
     * Lớp 3 (unique constraint booking_seats.flight_seat_id) tự chặn nếu có gì lọt qua 2 lớp trên.
     */
    @Transactional
    public Booking confirmBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy booking"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw ApiException.badRequest("Booking đang ở trạng thái " + booking.getStatus() + ", không thể xác nhận");
        }

        booking.setStatus(BookingStatus.CONFIRMED);

        for (BookingSeat bs : booking.getSeats()) {
            FlightSeat seat = bs.getFlightSeat();
            seat.setStatus(SeatStatus.BOOKED);
            seat.setHeldBy(null);
            seat.setHeldUntil(null);
            flightSeatRepository.save(seat);

            redisLockService.releaseSeatLock(
                    booking.getFlight().getId().toString(), seat.getId().toString(), booking.getUser().getId().toString());
        }

        return bookingRepository.save(booking);
    }

    /** Hủy booking (thanh toán thất bại/user tự hủy) -> nhả ghế về AVAILABLE ngay. */
    @Transactional
    public void cancelBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy booking"));

        booking.setStatus(BookingStatus.CANCELLED);

        for (BookingSeat bs : booking.getSeats()) {
            FlightSeat seat = bs.getFlightSeat();
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setHeldBy(null);
            seat.setHeldUntil(null);
            flightSeatRepository.save(seat);

            redisLockService.releaseSeatLock(
                    booking.getFlight().getId().toString(), seat.getId().toString(), booking.getUser().getId().toString());
        }

        bookingRepository.save(booking);
    }

    // Trong BookingService.java (hoặc SeatService.java)
    @Transactional
    public void releaseSeatHold(String userId, UUID flightId, UUID seatId) {
        // 1. Kiểm tra thông tin ghế
        FlightSeat flightSeat = flightSeatRepository.findById(seatId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ghế không tồn tại"));

        if (!flightSeat.getFlight().getId().equals(flightId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ghế không thuộc chuyến bay này");
        }

        if (flightSeat.getStatus() == SeatStatus.HELD) {
            if (flightSeat.getHeldBy() != null && !flightSeat.getHeldBy().equals(userId)) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền hủy giữ ghế này");
            }
            flightSeat.setStatus(SeatStatus.AVAILABLE);
            flightSeat.setHeldBy(null);
            flightSeat.setHeldUntil(null);
            flightSeatRepository.save(flightSeat);
        }

        // 3. Giải phóng Redis Lock (nếu đang dùng Redis để lock key giữ ghế)
        String lockKey = "seat_hold:" + flightId + ":" + seatId;
        redisLockService.unlock(lockKey);
    }
}
