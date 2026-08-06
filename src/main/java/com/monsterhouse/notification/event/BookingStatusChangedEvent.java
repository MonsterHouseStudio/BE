package com.monsterhouse.notification.event;

import com.monsterhouse.booking.entity.BookingStatus;

public record BookingStatusChangedEvent (
        Long bookingId,
        BookingStatus previousStatus,
        BookingStatus currentStatus
){
    public boolean isCreation(){
        return previousStatus == null;
    }
}
