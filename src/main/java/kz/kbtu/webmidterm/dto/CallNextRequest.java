package kz.kbtu.webmidterm.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CallNextRequest(@NotNull @Positive Integer windowNumber) {
}