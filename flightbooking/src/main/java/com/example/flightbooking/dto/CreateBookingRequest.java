package com.example.flightbooking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class CreateBookingRequest {

    @NotBlank
    private String flightId;

    @NotEmpty
    private List<String> seatIds;

    @NotEmpty
    @Valid
    private List<PassengerDto> passengers;

    // Bổ sung danh sách dịch vụ mua thêm (không bắt buộc @NotEmpty)
    private List<AncillaryRequest> ancillaries;

    public static class AncillaryRequest {
        private String ancillaryItemId;
        private Integer quantity;

        public String getAncillaryItemId() { return ancillaryItemId; }
        public void setAncillaryItemId(String ancillaryItemId) { this.ancillaryItemId = ancillaryItemId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    // Getters & Setters
    public String getFlightId() { return flightId; }
    public void setFlightId(String flightId) { this.flightId = flightId; }
    public List<String> getSeatIds() { return seatIds; }
    public void setSeatIds(List<String> seatIds) { this.seatIds = seatIds; }
    public List<PassengerDto> getPassengers() { return passengers; }
    public void setPassengers(List<PassengerDto> passengers) { this.passengers = passengers; }
    public List<AncillaryRequest> getAncillaries() { return ancillaries; }
    public void setAncillaries(List<AncillaryRequest> ancillaries) { this.ancillaries = ancillaries; }
}