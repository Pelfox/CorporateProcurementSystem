CREATE TABLE tags (
                      id    UUID NOT NULL,
                      name  VARCHAR(100) NOT NULL,
                      color VARCHAR(20),
                      CONSTRAINT pk_tags PRIMARY KEY (id),
                      CONSTRAINT uk_tags_name UNIQUE (name)
);

CREATE TABLE order_tags (
                            order_id UUID NOT NULL,
                            tag_id   UUID NOT NULL,
                            CONSTRAINT pk_order_tags PRIMARY KEY (order_id, tag_id),
                            CONSTRAINT fk_order_tags_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
                            CONSTRAINT fk_order_tags_tag   FOREIGN KEY (tag_id)   REFERENCES tags (id)   ON DELETE CASCADE
);

CREATE INDEX idx_order_tags_tag_id ON order_tags (tag_id);