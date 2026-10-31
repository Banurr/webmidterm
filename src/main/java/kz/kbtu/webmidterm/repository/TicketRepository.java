package kz.kbtu.webmidterm.repository;

import kz.kbtu.webmidterm.entity.Ticket;
import kz.kbtu.webmidterm.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByStatusInOrderByCalledAtAsc(Collection<TicketStatus> statuses);
}
