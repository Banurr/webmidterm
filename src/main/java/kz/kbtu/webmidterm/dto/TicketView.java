package kz.kbtu.webmidterm.dto;

import kz.kbtu.webmidterm.enums.TicketStatus;

public record TicketView(
        Long id,
        String number,
        String service,
        TicketStatus status,
        Integer position,
        Integer waitMinutes,
        Integer windowNumber) {
}
