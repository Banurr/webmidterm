package kz.kbtu.webmidterm.controller;

import jakarta.validation.Valid;
import kz.kbtu.webmidterm.dto.TakeTicketRequest;
import kz.kbtu.webmidterm.dto.TicketView;
import kz.kbtu.webmidterm.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketView take(@Valid @RequestBody TakeTicketRequest request) {
        return ticketService.take(request.serviceId());
    }

    @GetMapping("/{id}")
    public TicketView get(@PathVariable Long id) {
        return ticketService.get(id);
    }

    @DeleteMapping("/{id}")
    public TicketView cancel(@PathVariable Long id) {
        return ticketService.cancel(id);
    }

    @PostMapping("/{id}/start")
    public TicketView start(@PathVariable Long id) {
        return ticketService.start(id);
    }

    @PostMapping("/{id}/complete")
    public TicketView complete(@PathVariable Long id) {
        return ticketService.complete(id);
    }

    @PostMapping("/{id}/no-show")
    public TicketView noShow(@PathVariable Long id) {
        return ticketService.noShow(id);
    }
}
