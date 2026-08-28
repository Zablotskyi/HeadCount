package com.wasbyte.headcount.headcount.dto;

public record HeadcountParticipantDetailsResponse(
        Long participantId,
        Long userId,
        String firstName,
        String lastName,
        String position,
        String email,
        String mobileNumber,
        String country,
        String city,
        String office,
        String address) {
}
