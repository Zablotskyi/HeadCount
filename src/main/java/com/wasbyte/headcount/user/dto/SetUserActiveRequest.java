package com.wasbyte.headcount.user.dto;

import jakarta.validation.constraints.NotNull;

public record SetUserActiveRequest(@NotNull Boolean active) {
}
