CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(40) NOT NULL UNIQUE,
    slug VARCHAR(50) NOT NULL UNIQUE,
    display_order INTEGER NOT NULL
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(80) NOT NULL UNIQUE,
    price INTEGER NOT NULL CHECK (price >= 0),
    original_price INTEGER CHECK (original_price >= 0),
    stock INTEGER NOT NULL CHECK (stock >= 0),
    summary VARCHAR(240),
    description TEXT,
    image_url VARCHAR(500),
    origin VARCHAR(100),
    manufacturer VARCHAR(120),
    weight VARCHAR(120),
    shelf_life VARCHAR(160),
    storage_method VARCHAR(200),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    category_id BIGINT NOT NULL REFERENCES categories(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_product_active_category ON products(active,category_id);

CREATE TABLE customer_orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(24) NOT NULL UNIQUE,
    customer_name VARCHAR(40) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(120) NOT NULL,
    postal_code VARCHAR(10) NOT NULL,
    address VARCHAR(200) NOT NULL,
    address_detail VARCHAR(200),
    delivery_memo VARCHAR(200),
    subtotal INTEGER NOT NULL,
    shipping_fee INTEGER NOT NULL,
    total_amount INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_order_created ON customer_orders(created_at);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES customer_orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(120) NOT NULL,
    unit_price INTEGER NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    line_total INTEGER NOT NULL
);

INSERT INTO categories(name,slug,display_order) VALUES
('짝태','jjagtae',1),('먹태','meoktae',2),('장아찌','jangajji',3),
('미역','miyeok',4),('오징어','squid',5),('가공식품','processed',6);
