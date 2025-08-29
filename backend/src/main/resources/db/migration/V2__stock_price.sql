CREATE TABLE stock_price (
    id SERIAL PRIMARY KEY,
    exchange VARCHAR(20) NOT NULL,
    trading_symbol VARCHAR(50) NOT NULL,
    isin VARCHAR(20) NOT NULL,
    last_price NUMERIC(15,2) NOT NULL,
    day_change NUMERIC(15,2) NOT NULL,
    day_change_percentage NUMERIC(7,4) NOT NULL
);

CREATE UNIQUE INDEX idx_stock_unique ON stock_price (isin, exchange);