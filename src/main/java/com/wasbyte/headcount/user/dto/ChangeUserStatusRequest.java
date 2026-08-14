package com.wasbyte.headcount.user.dto;

import com.wasbyte.headcount.user.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeUserStatusRequest(@NotNull UserStatus status) {
}
