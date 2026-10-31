package kz.kbtu.webmidterm.dto;

import java.util.List;

public record BoardView(List<TicketView> serving, long waiting) {
}
