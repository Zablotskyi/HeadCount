package com.wasbyte.headcount.headcount.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmNeedHelpRequest(
        @Size(max = 100) String confirmationSource,
        @NotBlank @Size(max = 1000) String helpMessage) {
}
