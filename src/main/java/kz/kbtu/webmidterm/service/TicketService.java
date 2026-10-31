package kz.kbtu.webmidterm.service;

import kz.kbtu.webmidterm.dto.BoardView;
import kz.kbtu.webmidterm.dto.TicketView;
import kz.kbtu.webmidterm.entity.ServiceType;
import kz.kbtu.webmidterm.entity.Ticket;
import kz.kbtu.webmidterm.enums.TicketStatus;
import kz.kbtu.webmidterm.exception.ConflictException;
import kz.kbtu.webmidterm.exception.NotFoundException;
import kz.kbtu.webmidterm.repository.ServiceTypeRepository;
import kz.kbtu.webmidterm.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final RedisQueue redisQueue;

    public TicketView take(Long serviceId) {
        ServiceType service = serviceTypeRepository.findById(serviceId)
                .orElseThrow(() -> new NotFoundException("Service not found: " + serviceId));
        Ticket ticket = new Ticket();
        ticket.setService(service);
        ticket.setTicketNumber(redisQueue.nextNumber(service.getCode()));
        ticket = ticketRepository.save(ticket);
        redisQueue.add(ticket.getId());
        return toView(ticket);
    }

    @Transactional(readOnly = true)
    public TicketView get(Long id) {
        return toView(find(id));
    }

    @Transactional
    public TicketView cancel(Long id) {
        Ticket ticket = find(id);
        if (ticket.getStatus() != TicketStatus.WAITING || !redisQueue.remove(id)) {
            throw new ConflictException("Ticket cannot be cancelled");
        }
        ticket.setStatus(TicketStatus.CANCELLED);
        ticket.setFinishedAt(LocalDateTime.now());
        return toView(ticket);
    }

    @Transactional
    public TicketView callNext(int windowNumber) {
        Long id;
        while ((id = redisQueue.popNext()) != null) {
            Optional<Ticket> found = ticketRepository.findById(id);
            if (found.isPresent() && found.get().getStatus() == TicketStatus.WAITING) {
                Ticket ticket = found.get();
                ticket.setStatus(TicketStatus.CALLED);
                ticket.setWindowNumber(windowNumber);
                ticket.setCalledAt(LocalDateTime.now());
                return toView(ticket);
            }
        }
        throw new NotFoundException("Queue is empty");
    }

    @Transactional
    public TicketView start(Long id) {
        Ticket ticket = find(id);
        requireStatus(ticket, TicketStatus.CALLED);
        ticket.setStatus(TicketStatus.IN_SERVICE);
        ticket.setStartedAt(LocalDateTime.now());
        return toView(ticket);
    }

    @Transactional
    public TicketView complete(Long id) {
        Ticket ticket = find(id);
        requireStatus(ticket, TicketStatus.IN_SERVICE);
        ticket.setStatus(TicketStatus.DONE);
        ticket.setFinishedAt(LocalDateTime.now());
        return toView(ticket);
    }

    @Transactional
    public TicketView noShow(Long id) {
        Ticket ticket = find(id);
        requireStatus(ticket, TicketStatus.CALLED);
        ticket.setStatus(TicketStatus.NO_SHOW);
        ticket.setFinishedAt(LocalDateTime.now());
        return toView(ticket);
    }

    @Transactional(readOnly = true)
    public BoardView board() {
        List<TicketView> serving = ticketRepository
                .findByStatusInOrderByCalledAtAsc(List.of(TicketStatus.CALLED, TicketStatus.IN_SERVICE))
                .stream()
                .map(this::toView)
                .toList();
        return new BoardView(serving, redisQueue.size());
    }

    private Ticket find(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ticket not found: " + id));
    }

    private void requireStatus(Ticket ticket, TicketStatus expected) {
        if (ticket.getStatus() != expected) {
            throw new ConflictException("Ticket is " + ticket.getStatus() + ", expected " + expected);
        }
    }

    private TicketView toView(Ticket ticket) {
        Integer position = null;
        Integer waitMinutes = null;
        if (ticket.getStatus() == TicketStatus.WAITING) {
            Long rank = redisQueue.position(ticket.getId());
            if (rank != null) {
                position = rank.intValue() + 1;
                waitMinutes = rank.intValue() * ticket.getService().getAvgMinutes();
            }
        }
        return new TicketView(
                ticket.getId(),
                ticket.getTicketNumber(),
                ticket.getService().getName(),
                ticket.getStatus(),
                position,
                waitMinutes,
                ticket.getWindowNumber());
    }
}
