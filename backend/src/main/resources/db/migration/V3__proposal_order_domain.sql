CREATE TABLE proposals (
    id BIGINT NOT NULL AUTO_INCREMENT,
    job_id BIGINT NOT NULL,
    freelancer_id BIGINT NOT NULL,
    cover_letter TEXT NOT NULL,
    bid_amount DECIMAL(12,2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    delivery_days INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    ai_generated TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_proposal_job_freelancer (job_id, freelancer_id),
    INDEX idx_proposals_freelancer (freelancer_id, status),
    INDEX idx_proposals_job_status (job_id, status),
    CONSTRAINT fk_proposals_job FOREIGN KEY (job_id) REFERENCES jobs (id),
    CONSTRAINT fk_proposals_freelancer FOREIGN KEY (freelancer_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_number VARCHAR(32) NOT NULL,
    job_id BIGINT NOT NULL,
    proposal_id BIGINT NOT NULL,
    client_id BIGINT NOT NULL,
    freelancer_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    platform_fee DECIMAL(12,2) NOT NULL,
    freelancer_payout DECIMAL(12,2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(32) NOT NULL DEFAULT 'AWAITING_PAYMENT',
    delivery_note TEXT NULL,
    delivered_at TIMESTAMP(6) NULL,
    completed_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_number (order_number),
    UNIQUE KEY uk_orders_proposal (proposal_id),
    INDEX idx_orders_client (client_id, status),
    INDEX idx_orders_freelancer (freelancer_id, status),
    INDEX idx_orders_job (job_id),
    CONSTRAINT fk_orders_job FOREIGN KEY (job_id) REFERENCES jobs (id),
    CONSTRAINT fk_orders_proposal FOREIGN KEY (proposal_id) REFERENCES proposals (id),
    CONSTRAINT fk_orders_client FOREIGN KEY (client_id) REFERENCES users (id),
    CONSTRAINT fk_orders_freelancer FOREIGN KEY (freelancer_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE jobs
    ADD COLUMN hired_proposal_id BIGINT NULL AFTER status,
    ADD CONSTRAINT fk_jobs_hired_proposal FOREIGN KEY (hired_proposal_id) REFERENCES proposals (id);
