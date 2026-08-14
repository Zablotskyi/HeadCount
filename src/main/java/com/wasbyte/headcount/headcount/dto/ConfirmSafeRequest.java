package com.wasbyte.headcount.headcount.dto;

import jakarta.validation.constraints.Size;

public record ConfirmSafeRequest(@Size(max = 100) String confirmationSource) {
}
