-- ReLoop schema (MySQL 8). Money = DECIMAL(12,2). Enums stored as VARCHAR.
CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(160) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  full_name VARCHAR(120) NOT NULL,
  phone VARCHAR(20),
  role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE=InnoDB;

CREATE TABLE addresses (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  label VARCHAR(40) NOT NULL,
  line1 VARCHAR(160) NOT NULL,
  line2 VARCHAR(160),
  city VARCHAR(80) NOT NULL,
  state VARCHAR(80) NOT NULL,
  postal_code VARCHAR(10) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  default_address BOOLEAN NOT NULL DEFAULT FALSE,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT fk_addr_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_addr_user (user_id)
) ENGINE=InnoDB;

CREATE TABLE partner_profiles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  business_name VARCHAR(140) NOT NULL,
  city VARCHAR(80) NOT NULL,
  approved BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_partner_user UNIQUE (user_id),
  CONSTRAINT fk_partner_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE categories (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(80) NOT NULL,
  slug VARCHAR(80) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT uq_cat_slug UNIQUE (slug)
) ENGINE=InnoDB;

CREATE TABLE products (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  category_id BIGINT NOT NULL,
  name VARCHAR(160) NOT NULL,
  brand VARCHAR(80),
  description TEXT,
  condition_type VARCHAR(20) NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  original_price DECIMAL(12,2),
  image_url VARCHAR(500),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  sold_count INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_prod_cat FOREIGN KEY (category_id) REFERENCES categories(id),
  CONSTRAINT chk_prod_price CHECK (price > 0),
  INDEX idx_prod_cat (category_id),
  INDEX idx_prod_cond (condition_type),
  INDEX idx_prod_price (price),
  FULLTEXT INDEX ft_prod_name (name, brand)
) ENGINE=InnoDB;

CREATE TABLE product_images (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  url VARCHAR(500) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_pimg_prod FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE inventory (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  quantity INT NOT NULL DEFAULT 0,
  CONSTRAINT uq_inv_prod UNIQUE (product_id),
  CONSTRAINT chk_inv_qty CHECK (quantity >= 0),
  CONSTRAINT fk_inv_prod FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE carts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  CONSTRAINT uq_cart_user UNIQUE (user_id),
  CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE cart_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  cart_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  CONSTRAINT uq_cart_prod UNIQUE (cart_id, product_id),
  CONSTRAINT chk_ci_qty CHECK (quantity > 0),
  CONSTRAINT fk_ci_cart FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
  CONSTRAINT fk_ci_prod FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB;

CREATE TABLE wishlist_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_wish UNIQUE (user_id, product_id),
  CONSTRAINT fk_wish_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_wish_prod FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB;

CREATE TABLE orders (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  order_number VARCHAR(30) NOT NULL,
  order_type VARCHAR(10) NOT NULL DEFAULT 'STORE',
  status VARCHAR(20) NOT NULL,
  payment_method VARCHAR(12) NOT NULL,
  payment_status VARCHAR(12) NOT NULL,
  subtotal DECIMAL(12,2) NOT NULL,
  total DECIMAL(12,2) NOT NULL,
  ship_name VARCHAR(120) NOT NULL,
  ship_address VARCHAR(500) NOT NULL,
  ship_phone VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_order_number UNIQUE (order_number),
  CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_order_user (user_id),
  INDEX idx_order_created (created_at)
) ENGINE=InnoDB;

CREATE TABLE resale_listings (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  owned_product_id BIGINT NOT NULL,
  seller_id BIGINT NOT NULL,
  title VARCHAR(160) NOT NULL,
  description TEXT,
  condition_type VARCHAR(20) NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  image_url VARCHAR(500),
  status VARCHAR(12) NOT NULL DEFAULT 'DRAFT',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_rl_price CHECK (price > 0),
  CONSTRAINT fk_rl_seller FOREIGN KEY (seller_id) REFERENCES users(id),
  INDEX idx_rl_status (status),
  INDEX idx_rl_owned (owned_product_id)
) ENGINE=InnoDB;

CREATE TABLE order_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  product_id BIGINT,
  listing_id BIGINT,
  product_name VARCHAR(160) NOT NULL,
  unit_price DECIMAL(12,2) NOT NULL,
  quantity INT NOT NULL,
  CONSTRAINT fk_oi_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
  CONSTRAINT fk_oi_prod FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT fk_oi_listing FOREIGN KEY (listing_id) REFERENCES resale_listings(id),
  INDEX idx_oi_order (order_id)
) ENGINE=InnoDB;

CREATE TABLE payments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  provider VARCHAR(12) NOT NULL,
  provider_order_id VARCHAR(80),
  provider_payment_id VARCHAR(80),
  status VARCHAR(12) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_pay_order FOREIGN KEY (order_id) REFERENCES orders(id),
  INDEX idx_pay_order (order_id)
) ENGINE=InnoDB;

CREATE TABLE reviews (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  order_item_id BIGINT NOT NULL,
  rating INT NOT NULL,
  comment VARCHAR(2000),
  hidden BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_review_item UNIQUE (order_item_id),
  CONSTRAINT chk_rating CHECK (rating BETWEEN 1 AND 5),
  CONSTRAINT fk_rev_prod FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT fk_rev_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_rev_item FOREIGN KEY (order_item_id) REFERENCES order_items(id),
  INDEX idx_rev_prod (product_id)
) ENGINE=InnoDB;

CREATE TABLE owned_products (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  owner_id BIGINT NOT NULL,
  name VARCHAR(160) NOT NULL,
  category VARCHAR(80) NOT NULL,
  brand VARCHAR(80),
  model VARCHAR(80),
  serial_number VARCHAR(80),
  purchase_date DATE NOT NULL,
  receipt_file VARCHAR(200),
  status VARCHAR(12) NOT NULL DEFAULT 'ACTIVE',
  public_token VARCHAR(40) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_owned_token UNIQUE (public_token),
  CONSTRAINT fk_owned_owner FOREIGN KEY (owner_id) REFERENCES users(id),
  INDEX idx_owned_owner (owner_id)
) ENGINE=InnoDB;

ALTER TABLE resale_listings ADD CONSTRAINT fk_rl_owned FOREIGN KEY (owned_product_id) REFERENCES owned_products(id);

CREATE TABLE warranties (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  owned_product_id BIGINT NOT NULL,
  provider VARCHAR(120),
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  notes VARCHAR(500),
  CONSTRAINT chk_warranty_dates CHECK (end_date >= start_date),
  CONSTRAINT fk_war_owned FOREIGN KEY (owned_product_id) REFERENCES owned_products(id) ON DELETE CASCADE,
  INDEX idx_war_owned (owned_product_id)
) ENGINE=InnoDB;

-- Append-only: the application exposes no update/delete path for this table.
CREATE TABLE lifecycle_events (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  owned_product_id BIGINT NOT NULL,
  event_type VARCHAR(30) NOT NULL,
  verification VARCHAR(20) NOT NULL,
  actor_id BIGINT,
  actor_label VARCHAR(160) NOT NULL,
  details VARCHAR(1000),
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT fk_le_owned FOREIGN KEY (owned_product_id) REFERENCES owned_products(id),
  INDEX idx_le_owned (owned_product_id, created_at)
) ENGINE=InnoDB;

CREATE TABLE repair_requests (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  owned_product_id BIGINT NOT NULL,
  customer_id BIGINT NOT NULL,
  partner_id BIGINT NOT NULL,
  problem VARCHAR(2000) NOT NULL,
  status VARCHAR(12) NOT NULL,
  final_cost DECIMAL(12,2),
  work_notes VARCHAR(2000),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_rr_owned FOREIGN KEY (owned_product_id) REFERENCES owned_products(id),
  CONSTRAINT fk_rr_customer FOREIGN KEY (customer_id) REFERENCES users(id),
  CONSTRAINT fk_rr_partner FOREIGN KEY (partner_id) REFERENCES partner_profiles(id),
  INDEX idx_rr_customer (customer_id),
  INDEX idx_rr_partner (partner_id, status)
) ENGINE=InnoDB;

CREATE TABLE repair_images (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  repair_request_id BIGINT NOT NULL,
  url VARCHAR(500) NOT NULL,
  CONSTRAINT fk_ri_rr FOREIGN KEY (repair_request_id) REFERENCES repair_requests(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE repair_quotations (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  repair_request_id BIGINT NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  estimated_days INT NOT NULL,
  notes VARCHAR(1000),
  status VARCHAR(12) NOT NULL DEFAULT 'PENDING',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_rq_rr FOREIGN KEY (repair_request_id) REFERENCES repair_requests(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE repair_status_history (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  repair_request_id BIGINT NOT NULL,
  from_status VARCHAR(12),
  to_status VARCHAR(12) NOT NULL,
  actor_id BIGINT NOT NULL,
  note VARCHAR(500),
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT fk_rsh_rr FOREIGN KEY (repair_request_id) REFERENCES repair_requests(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE ownership_transfers (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  owned_product_id BIGINT NOT NULL,
  from_user_id BIGINT NOT NULL,
  to_user_id BIGINT NOT NULL,
  listing_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_transfer_order UNIQUE (order_id),
  CONSTRAINT fk_ot_owned FOREIGN KEY (owned_product_id) REFERENCES owned_products(id),
  CONSTRAINT fk_ot_from FOREIGN KEY (from_user_id) REFERENCES users(id),
  CONSTRAINT fk_ot_to FOREIGN KEY (to_user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE recycling_records (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  owned_product_id BIGINT NOT NULL,
  recycled_on DATE NOT NULL,
  partner_name VARCHAR(140),
  evidence_url VARCHAR(500),
  status VARCHAR(16) NOT NULL DEFAULT 'SELF_REPORTED',
  recorded_by BIGINT NOT NULL,
  CONSTRAINT fk_rec_owned FOREIGN KEY (owned_product_id) REFERENCES owned_products(id)
) ENGINE=InnoDB;

CREATE TABLE listing_reports (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  listing_id BIGINT NOT NULL,
  reporter_id BIGINT NOT NULL,
  reason VARCHAR(500) NOT NULL,
  status VARCHAR(10) NOT NULL DEFAULT 'OPEN',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_lr_listing FOREIGN KEY (listing_id) REFERENCES resale_listings(id),
  CONSTRAINT fk_lr_user FOREIGN KEY (reporter_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE audit_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  actor_id BIGINT,
  action VARCHAR(60) NOT NULL,
  entity_type VARCHAR(40) NOT NULL,
  entity_id BIGINT,
  details VARCHAR(500),
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  INDEX idx_audit_created (created_at)
) ENGINE=InnoDB;
