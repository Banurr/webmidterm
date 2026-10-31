package kz.kbtu.webmidterm.dto;

import jakarta.validation.constraints.NotNull;

public record TakeTicketRequest(@NotNull Long serviceId) {
}
