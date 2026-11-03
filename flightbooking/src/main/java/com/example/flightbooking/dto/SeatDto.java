package com.example.flightbooking.dto;

import java.math.BigDecimal;

public class SeatDto {
    private String id;
    private String seatNumber;
    private String status;
    private String seatClass;
    private BigDecimal price;
    private String handBaggage;
    private String checkedBaggage;
    private boolean allowDateChange;
    private boolean allowRefund;
    private String fareNote;

    public SeatDto(String id, String seatNumber, String status, String seatClass, BigDecimal price,
                   String handBaggage, String checkedBaggage, boolean allowDateChange, boolean allowRefund, String fareNote) {
        this.id = id;
        this.seatNumber = seatNumber;
        this.status = status;
        this.seatClass = seatClass;
        this.price = price;
        this.handBaggage = handBaggage;
        this.checkedBaggage = checkedBaggage;
        this.allowDateChange = allowDateChange;
        this.allowRefund = allowRefund;
        this.fareNote = fareNote;
    }

    // Getters...
    public String getId() { return id; }
    public String getSeatNumber() { return seatNumber; }
    public String getStatus() { return status; }
    public String getSeatClass() { return seatClass; }
    public BigDecimal getPrice() { return price; }
    public String getHandBaggage() { return handBaggage; }
    public String getCheckedBaggage() { return checkedBaggage; }
    public boolean isAllowDateChange() { return allowDateChange; }
    public boolean isAllowRefund() { return allowRefund; }
    public String getFareNote() { return fareNote; }
}