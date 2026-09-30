CREATE TABLE reservations (
    id UUID PRIMARY KEY,
    publication_id UUID NOT NULL REFERENCES publication(id),
    buyer_id UUID NOT NULL REFERENCES users(id),
    provider_id UUID NOT NULL REFERENCES users(id),
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(12,2) NOT NULL CHECK (unit_price > 0),
    currency_code VARCHAR(3) NOT NULL,
    expected_available_date DATE,
    status VARCHAR(40) NOT NULL,
    stock_released BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_reservation_parties CHECK (buyer_id <> provider_id)
);

CREATE TABLE reservation_history (
    id BIGSERIAL PRIMARY KEY,
    reservation_id UUID NOT NULL REFERENCES reservations(id),
    from_status VARCHAR(40),
    to_status VARCHAR(40) NOT NULL,
    actor_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE favorites (
    user_id UUID NOT NULL REFERENCES users(id),
    publication_id UUID NOT NULL REFERENCES publication(id),
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (user_id, publication_id)
);

CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    publication_id UUID NOT NULL REFERENCES publication(id),
    buyer_id UUID NOT NULL REFERENCES users(id),
    provider_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_conversation_context UNIQUE (publication_id, buyer_id, provider_id),
    CONSTRAINT ck_conversation_parties CHECK (buyer_id <> provider_id)
);

CREATE TABLE conversation_messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations(id),
    sender_id UUID NOT NULL REFERENCES users(id),
    content VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE supplier_reviews (
    id UUID PRIMARY KEY,
    reviewer_id UUID NOT NULL REFERENCES users(id),
    supplier_id UUID NOT NULL REFERENCES users(id),
    conversation_id UUID NOT NULL REFERENCES conversations(id),
    purchased BOOLEAN NOT NULL,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_supplier_review_conversation UNIQUE (conversation_id),
    CONSTRAINT ck_supplier_review_parties CHECK (reviewer_id <> supplier_id)
);

CREATE TABLE product_reviews (
    id UUID PRIMARY KEY,
    reviewer_id UUID NOT NULL REFERENCES users(id),
    product_id INT NOT NULL REFERENCES publication_product(id),
    reservation_id UUID NOT NULL REFERENCES reservations(id),
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_product_review_reservation UNIQUE (reservation_id)
);

CREATE INDEX idx_reservations_buyer_created ON reservations (buyer_id, created_at);
CREATE INDEX idx_reservations_provider_created ON reservations (provider_id, created_at);
CREATE INDEX idx_reservations_publication ON reservations (publication_id);
CREATE INDEX idx_reservation_history_reservation ON reservation_history (reservation_id, created_at);
CREATE INDEX idx_favorites_user_created ON favorites (user_id, created_at);
CREATE INDEX idx_conversations_buyer ON conversations (buyer_id, updated_at);
CREATE INDEX idx_conversations_provider ON conversations (provider_id, updated_at);
CREATE INDEX idx_messages_conversation_created ON conversation_messages (conversation_id, created_at);
CREATE INDEX idx_supplier_reviews_supplier ON supplier_reviews (supplier_id, created_at);
CREATE INDEX idx_product_reviews_product ON product_reviews (product_id, created_at);
