ALTER TABLE status ADD CONSTRAINT uk_status_name UNIQUE (name);
ALTER TABLE unittype ADD CONSTRAINT uk_unit_type_name UNIQUE (name);

INSERT INTO status (name)
SELECT 'ACTIVE' WHERE NOT EXISTS (SELECT 1 FROM status WHERE name = 'ACTIVE');
INSERT INTO status (name)
SELECT 'PAUSED' WHERE NOT EXISTS (SELECT 1 FROM status WHERE name = 'PAUSED');
INSERT INTO status (name)
SELECT 'SOLD_OUT' WHERE NOT EXISTS (SELECT 1 FROM status WHERE name = 'SOLD_OUT');
INSERT INTO status (name)
SELECT 'CLOSED' WHERE NOT EXISTS (SELECT 1 FROM status WHERE name = 'CLOSED');

INSERT INTO unittype (name)
SELECT 'KILOGRAMO' WHERE NOT EXISTS (SELECT 1 FROM unittype WHERE name = 'KILOGRAMO');
INSERT INTO unittype (name)
SELECT 'LIBRA' WHERE NOT EXISTS (SELECT 1 FROM unittype WHERE name = 'LIBRA');
INSERT INTO unittype (name)
SELECT 'QUINTAL' WHERE NOT EXISTS (SELECT 1 FROM unittype WHERE name = 'QUINTAL');
INSERT INTO unittype (name)
SELECT 'TONELADA' WHERE NOT EXISTS (SELECT 1 FROM unittype WHERE name = 'TONELADA');
INSERT INTO unittype (name)
SELECT 'UNIDAD' WHERE NOT EXISTS (SELECT 1 FROM unittype WHERE name = 'UNIDAD');

CREATE TABLE publication_category (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT uk_publication_category_code UNIQUE (code)
);

CREATE TABLE publication_product (
    id SERIAL PRIMARY KEY,
    category_id INT NOT NULL REFERENCES publication_category(id),
    code VARCHAR(60) NOT NULL,
    name VARCHAR(120) NOT NULL,
    CONSTRAINT uk_publication_product_code UNIQUE (code)
);

INSERT INTO publication_category (code, name) VALUES
    ('AGRICULTURAL', 'Productos agrícolas'),
    ('RECYCLED_MATERIALS', 'Materiales reciclables');

INSERT INTO publication_product (category_id, code, name) VALUES
    ((SELECT id FROM publication_category WHERE code = 'AGRICULTURAL'), 'CORN', 'Maíz'),
    ((SELECT id FROM publication_category WHERE code = 'AGRICULTURAL'), 'BEANS', 'Frijol'),
    ((SELECT id FROM publication_category WHERE code = 'AGRICULTURAL'), 'COFFEE', 'Café'),
    ((SELECT id FROM publication_category WHERE code = 'AGRICULTURAL'), 'VEGETABLES', 'Hortalizas'),
    ((SELECT id FROM publication_category WHERE code = 'AGRICULTURAL'), 'OTHER_AGRICULTURAL', 'Otro producto agrícola'),
    ((SELECT id FROM publication_category WHERE code = 'RECYCLED_MATERIALS'), 'PET_PLASTIC', 'Plástico PET'),
    ((SELECT id FROM publication_category WHERE code = 'RECYCLED_MATERIALS'), 'CARDBOARD', 'Cartón'),
    ((SELECT id FROM publication_category WHERE code = 'RECYCLED_MATERIALS'), 'ALUMINUM', 'Aluminio'),
    ((SELECT id FROM publication_category WHERE code = 'RECYCLED_MATERIALS'), 'GLASS', 'Vidrio');

ALTER TABLE publication ADD COLUMN category_id INT;
ALTER TABLE publication ADD COLUMN product_id INT;
ALTER TABLE publication ADD COLUMN currency_code VARCHAR(3) DEFAULT 'GTQ' NOT NULL;
ALTER TABLE publication ADD COLUMN date_updated TIMESTAMP DEFAULT NOW();
ALTER TABLE publication ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

UPDATE publication
SET category_id = (SELECT id FROM publication_category WHERE code = 'AGRICULTURAL')
WHERE category_id IS NULL;

UPDATE publication
SET product_id = (SELECT id FROM publication_product WHERE code = 'OTHER_AGRICULTURAL')
WHERE product_id IS NULL;

UPDATE publication
SET status_id = (SELECT id FROM status WHERE name = 'ACTIVE')
WHERE status_id IS NULL;

UPDATE publication SET date_updated = date_created WHERE date_updated IS NULL;

ALTER TABLE publication ALTER COLUMN category_id SET NOT NULL;
ALTER TABLE publication ALTER COLUMN product_id SET NOT NULL;
ALTER TABLE publication ALTER COLUMN status_id SET NOT NULL;
ALTER TABLE publication ALTER COLUMN date_updated SET NOT NULL;

ALTER TABLE publication
    ADD CONSTRAINT fk_publication_category FOREIGN KEY (category_id) REFERENCES publication_category(id);
ALTER TABLE publication
    ADD CONSTRAINT fk_publication_product FOREIGN KEY (product_id) REFERENCES publication_product(id);

CREATE INDEX idx_publication_owner ON publication (user_owner_id);
CREATE INDEX idx_publication_status_created ON publication (status_id, date_created);
CREATE INDEX idx_publication_category_product ON publication (category_id, product_id);
CREATE INDEX idx_publication_price ON publication (price);
CREATE INDEX idx_publication_available_date ON publication (available_date);
