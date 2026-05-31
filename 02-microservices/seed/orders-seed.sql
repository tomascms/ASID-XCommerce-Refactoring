-- Seed: 5 encomendas CONFIRMED por utilizador (250 total), 4 itens cada
-- Produz o padrão N+1 no endpoint GET /orders (T2 dos testes H2a)

DO $$
DECLARE
    uname TEXT;
    oid   BIGINT;
    pid   BIGINT;
BEGIN
    FOR uname IN SELECT 'user' || i FROM generate_series(1, 50) AS i LOOP
        FOR oi IN 1..5 LOOP
            INSERT INTO orders (username, status, order_date, total_amount)
            VALUES (uname, 'CONFIRMED', NOW(), 100.00)
            RETURNING id INTO oid;

            FOR pid IN 1..4 LOOP
                INSERT INTO order_items (order_id, product_id, quantity)
                VALUES (oid, pid, 1);
            END LOOP;
        END LOOP;
    END LOOP;
END $$;
