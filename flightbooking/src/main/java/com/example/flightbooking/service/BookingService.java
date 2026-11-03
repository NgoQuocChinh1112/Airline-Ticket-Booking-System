package com.example.flightbooking.service;

import com.example.flightbooking.dto.CreateBookingRequest;
import com.example.flightbooking.dto.HoldSeatRequest;
import com.example.flightbooking.dto.PassengerDto;
import com.example.flightbooking.entity.*;
import com.example.flightbooking.exception.ApiException;
import com.example.flightbooking.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BookingService {

    private final FlightRepository flightRepository;
    private final FlightSeatRepository flightSeatRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final RedisLockService redisLockService;
    private final PricingService pricingService;
    private final long holdSeconds;

    public BookingService(FlightRepository flightRepository,
                          FlightSeatRepository flightSeatRepository,
                          BookingRepository bookingRepository,
                          UserRepository userRepository,
                          RedisLockService redisLockService,
                          PricingService pricingService,
                          @Value("${app.seat-hold-seconds}") long holdSeconds) {
        this.flightRepository = flightRepository;
        this.flightSeatRepository = flightSeatRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.redisLockService = redisLockService;
        this.pricingService = pricingService;
        this.holdSeconds = holdSeconds;
    }

    /** BƯỚC 1: Giữ ghế tạm thời */
    @Transactional
    public FlightSeat holdSeat(String userId, HoldSeatRequest req) {
        UUID flightId = UUID.fromString(req.getFlightId());
        UUID seatId = UUID.fromString(req.getSeatId());
        boolean locked = redisLockService.acquireSeatLock(req.getFlightId(), req.getSeatId(), userId, holdSeconds);
        if (!locked) {
            throw ApiException.badRequest("Ghế này đang được người khác chọn hoặc xử lý");
        }
        try {
            FlightSeat seat = flightSeatRepository.findByIdForUpdate(seatId)
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy ghế"));

            Instant now = Instant.now();
            if (seat.getStatus() == SeatStatus.BOOKED) {
                throw ApiException.badRequest("Ghế đã được đặt chính thức");
            }

            if (seat.getStatus() == SeatStatus.HELD && seat.getHeldUntil() != null && seat.getHeldUntil().isAfter(now)) {
                if (!userId.equals(seat.getHeldBy())) {
                    throw ApiException.badRequest("Ghế đang được giữ bởi người dùng khác");
                }
            }

            seat.setStatus(SeatStatus.HELD);
            seat.setHeldBy(userId);
            seat.setHeldUntil(now.plusSeconds(holdSeconds));

            return flightSeatRepository.save(seat);
        } catch (RuntimeException ex) {
            redisLockService.releaseSeatLock(req.getFlightId(), req.getSeatId(), userId);
            throw ex;
        }
    }

    /** BƯỚC 2: Tạo Booking PENDING */
    @Transactional
    public Booking createBooking(String userId, CreateBookingRequest req) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy người dùng"));

        Flight flight = flightRepository.findById(UUID.fromString(req.getFlightId()))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy chuyến bay"));

        List<UUID> seatUuids = req.getSeatIds().stream().map(UUID::fromString).toList();
        List<FlightSeat> seats = flightSeatRepository.findByIdIn(seatUuids);

        if (seats.size() != req.getSeatIds().size()) {
            throw ApiException.badRequest("Danh sách ghế không hợp lệ");
        }

        // 1. Kiểm tra thời hạn giữ chỗ
        Instant now = Instant.now();
        for (FlightSeat seat : seats) {
            if (seat.getStatus() != SeatStatus.HELD
                    || !userId.equals(seat.getHeldBy())
                    || seat.getHeldUntil() == null
                    || seat.getHeldUntil().isBefore(now)) {
                throw ApiException.badRequest("Ghế " + seat.getSeatNumber() + " đã hết thời gian giữ chỗ hoặc không thuộc về bạn");
            }
        }

        // 2. Tính tổng tiền vé (Giá từng ghế x Hệ số Dynamic Pricing)
        BigDecimal multiplier = pricingService.calculateMultiplier(flight.getId());
        BigDecimal totalSeatPrice = BigDecimal.ZERO;
        for (FlightSeat seat : seats) {
            BigDecimal actualSeatPrice = seat.getPrice().multiply(multiplier);
            totalSeatPrice = totalSeatPrice.add(actualSeatPrice);
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setFlight(flight);
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalPrice(totalSeatPrice);

        // 3. Gán ghế & Hành khách
        Map<String, PassengerDto> seatIdToPassenger = mapSeatToPassenger(req);
        for (FlightSeat seat : seats) {
            BookingSeat bs = new BookingSeat();
            bs.setBooking(booking);
            bs.setFlightSeat(seat);
            booking.getSeats().add(bs);

            PassengerDto pDto = seatIdToPassenger.get(seat.getId().toString());
            Passenger passenger = new Passenger();
            passenger.setBooking(booking);
            passenger.setFullName(pDto != null ? pDto.getFullName() : "Hành Khách");
            passenger.setPassportNumber(pDto != null ? pDto.getPassportNumber() : "");
            passenger.setDob(pDto != null && pDto.getDob() != null ? LocalDate.parse(pDto.getDob()) : LocalDate.now());
            booking.getPassengers().add(passenger);
        }

        // 4. Giảm số ghế trống khả dụng
        if (flight.getAvailableSeats() != null) {
            flight.setAvailableSeats(Math.max(0, flight.getAvailableSeats() - seats.size()));
            flightRepository.save(flight);
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

    /** BƯỚC 3: Xác nhận thanh toán */
    @Transactional
    public Booking confirmBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy booking"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw ApiException.badRequest("Booking đang ở trạng thái " + booking.getStatus() + ", không thể xác nhận");
        }

        Instant now = Instant.now();

        for (BookingSeat bookingSeat : booking.getSeats()) {
            FlightSeat seat = flightSeatRepository
                    .findByIdForUpdate(bookingSeat.getFlightSeat().getId())
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy ghế"));

            boolean seatIsStillHeldByCustomer =
                    seat.getStatus() == SeatStatus.HELD
                            && booking.getUser().getId().toString().equals(seat.getHeldBy())
                            && seat.getHeldUntil() != null
                            && seat.getHeldUntil().isAfter(now);

            if (!seatIsStillHeldByCustomer) {
                throw ApiException.badRequest("Ghế " + seat.getSeatNumber() + " đã hết thời gian giữ chỗ");
            }

            seat.setStatus(SeatStatus.BOOKED);
            seat.setHeldBy(null);
            seat.setHeldUntil(null);
            flightSeatRepository.save(seat);

            redisLockService.releaseSeatLock(
                    booking.getFlight().getId().toString(),
                    seat.getId().toString(),
                    booking.getUser().getId().toString()
            );
        }

        booking.setStatus(BookingStatus.CONFIRMED);

        return bookingRepository.save(booking);
    }

    /** Hủy Booking */
    @Transactional
    public void cancelBooking(UUID bookingId, String userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy booking"));

        if (!booking.getUser().getId().toString().equals(userId)) {
            throw ApiException.unauthorized("Bạn không có quyền hủy booking này");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw ApiException.badRequest("Chỉ có thể hủy booking đang chờ thanh toán");
        }

        booking.setStatus(BookingStatus.CANCELLED);

        for (BookingSeat bs : booking.getSeats()) {
            FlightSeat seat = bs.getFlightSeat();
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setHeldBy(null);
            seat.setHeldUntil(null);
            flightSeatRepository.save(seat);

            redisLockService.releaseSeatLock(
                    booking.getFlight().getId().toString(),
                    seat.getId().toString(),
                    booking.getUser().getId().toString()
            );
        }

        // Khôi phục lại số ghế trống cho Flight
        Flight flight = booking.getFlight();
        if (flight != null && flight.getAvailableSeats() != null) {
            flight.setAvailableSeats(flight.getAvailableSeats() + booking.getSeats().size());
            flightRepository.save(flight);
        }

        bookingRepository.save(booking);
    }

    @Transactional
    public void releaseSeatHold(String userId, UUID flightId, UUID seatId) {
        FlightSeat seat = flightSeatRepository.findById(seatId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy ghế"));

        if (seat.getStatus() == SeatStatus.HELD && userId.equals(seat.getHeldBy())) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setHeldBy(null);
            seat.setHeldUntil(null);
            flightSeatRepository.save(seat);

            redisLockService.releaseSeatLock(flightId.toString(), seatId.toString(), userId);
        }
    }
}