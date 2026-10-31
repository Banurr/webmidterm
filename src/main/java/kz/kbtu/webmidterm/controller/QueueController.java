package kz.kbtu.webmidterm.controller;

import jakarta.validation.Valid;
import kz.kbtu.webmidterm.dto.BoardView;
import kz.kbtu.webmidterm.dto.CallNextRequest;
import kz.kbtu.webmidterm.dto.TicketView;
import kz.kbtu.webmidterm.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class QueueController {

    private final TicketService ticketService;

    @GetMapping("/board")
    public BoardView board() {
        return ticketService.board();
    }

    @PostMapping("/call-next")
    public TicketView callNext(@Valid @RequestBody CallNextRequest request) {
        return ticketService.callNext(request.windowNumber());
    }
}
