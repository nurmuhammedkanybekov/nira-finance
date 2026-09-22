-- Manually-updated market price per share (no live market data provider wired up).
-- NULL until the user sets it; the app falls back to cost basis for valuation until then.
ALTER TABLE holding ADD COLUMN current_price NUMERIC(14,4);
