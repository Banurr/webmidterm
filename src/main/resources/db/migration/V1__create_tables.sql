CREATE TABLE services (
                          id BIGSERIAL PRIMARY KEY,
                          code VARCHAR(1) NOT NULL UNIQUE,
                          name VARCHAR(150) NOT NULL,
                          avg_minutes INT NOT NULL CHECK (avg_minutes > 0)
);

CREATE TABLE tickets (
                         id BIGSERIAL PRIMARY KEY,
                         ticket_number VARCHAR(10) NOT NULL,
                         service_id BIGINT NOT NULL REFERENCES services (id),
                         status VARCHAR(20) NOT NULL CHECK (status IN ('WAITING', 'CALLED', 'IN_SERVICE', 'DONE', 'CANCELLED', 'NO_SHOW')),
                         window_number INT,
                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         called_at TIMESTAMP,
                         started_at TIMESTAMP,
                         finished_at TIMESTAMP
);

CREATE INDEX idx_tickets_status ON tickets (status);
CREATE INDEX idx_tickets_service ON tickets (service_id);