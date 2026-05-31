-- Seed: 50 utilizadores de teste (user1..user50)
-- BCrypt de 'password': $2a$10$Sbh0IfZdkaW4qaiHgtgP5Ofe2VNcazCGf9DxnGqOAExsjWp/h8kRG

INSERT INTO users (username, email, password_hash, first_name, last_name, address, role, active)
SELECT
    'user' || i,
    'user' || i || '@test.com',
    '$2a$10$Sbh0IfZdkaW4qaiHgtgP5Ofe2VNcazCGf9DxnGqOAExsjWp/h8kRG',
    'Nome' || i,
    'Apelido' || i,
    'Rua Teste ' || i,
    'CUSTOMER',
    true
FROM generate_series(1, 50) AS i
ON CONFLICT (username) DO NOTHING;
