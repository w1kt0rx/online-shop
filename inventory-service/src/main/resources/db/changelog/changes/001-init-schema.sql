--liquibase formatted sql

--changeset shop:inventory-001
CREATE TABLE stock_items (
    product_id BIGINT PRIMARY KEY,   -- id z product-service (bez FK - inna baza/serwis)
    quantity   INT    NOT NULL CHECK (quantity >= 0),
    version    BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE reservations (
    id         BIGSERIAL PRIMARY KEY,
    order_id   BIGINT      NOT NULL,
    product_id BIGINT      NOT NULL,
    quantity   INT         NOT NULL CHECK (quantity > 0),
    status     VARCHAR(20) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT now()
);
CREATE INDEX idx_reservations_order ON reservations (order_id);
--rollback DROP TABLE reservations; DROP TABLE stock_items;
