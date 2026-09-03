--clients
create table clients (
	client_id BIGSERIAL PRIMARY KEY,
	
	username VARCHAR(50) UNIQUE NOT NULL,
	email VARCHAR(255) UNIQUE NOT NULL,
	password_hash TEXT NOT NULL,
	
	first_name VARCHAR(100) NOT NULL,
	last_name VARCHAR(100) NOT NULL,
	
	address VARCHAR(255) NOT NULL,
	phone VARCHAR(20) NOT NULL,
	
	account_role VARCHAR(20) NOT NULL
		CHECK (account_role IN ('CLIENT', 'ADMIN', 'ANALYST')),

	created_at TIMESTAMP NOT NULL,
	last_logged_in TIMESTAMP
);



--orders
CREATE TABLE orders (
	order_id BIGserial PRIMARY KEY,

	client_id BIGINT NOT NULL,
	instrument_id BIGINT NOT NULL,

	order_type VARCHAR(20) NOT NULL
		CHECK (order_type IN ('MARKET', 'LIMIT')),

	transaction_type VARCHAR(10) NOT NULL
		CHECK (transaction_type In ('BUY', 'SELL')),

	quantity NUMERIC(18,0) NOT NULL
		CHECK (quantity > 0),

	requested_price NUMERIC(18,8)
		CHECK (requested_price >= 0),

	status VARCHAR(20) NOT NULL
		CHECK (
			status in (
					'SUBMITTED',
					'ACCEPTED',
					'REJECTED',
					'CANCELED'
			)
		),

	submitted_at TIMESTAMP NOT NULL,
	accepted_at TIMESTAMP,
	filled_at TIMESTAMP,

	rejected_reason TEXT,

	CONSTRAINT fk_orders_clients
		FOREIGN KEY (client_id)
		REFERENCES clients(client_id),

	CONSTRAINT fk_orders_instruments
		FOREIGN KEY (client_id)
		REFERENCES clients(client_id),

	CONSTRAINT chk_orders_dates
		CHECK (
			accepted_at IS NULL
			OR accepted_at >= submitted_at
		),

	CONSTRAINT chk_filled_dates
		CHECK (
			filled_at IS NULL
			OR filled_at >= submitted_at
		)
);



--instruments
CREATE TABLE instruments (
	instrument_id BIGSERIAL PRIMARY KEY,

	symbol VARCHAR(20) UNIQUE NOT NULL,
	instrument_name VARCHAR(255) NOT NULL,

	instrument_type VARCHAR(20) NOT NULL
		CHECK (
			instrument_type IN (
				'EQUITY',
				'CRYPTO',
				'FOREX'
			)
		),
	exchange VARCHAR(50),
	currency VARCHAR(10),
	
	is_active BOOLEAN DEFAULT TRUE,
	
	created_at TIMESTAMP NOT NULL
);
 