# Fix authentication issues by creating a proper user

Write-Host "=== Fixing Auth Issues ===" -ForegroundColor Cyan

# BCrypt hash of '123456' generated with $2a$10$ prefix
# This is a valid bcrypt hash for password '123456'
$bcryptHash = '$2a$10$ZL6bkCaMCT8XL0uBPyF5IuB6s58CrTxZZtG.W4lB6z1mq7K1pF6Rq'

Write-Host "Removing old admin user..."
docker compose exec -T db psql -U postgres -d xcommerce_auth -c "DELETE FROM users_micro WHERE username = 'admin';"

Write-Host "Creating new admin user with correct password hash..."
docker compose exec -T db psql -U postgres -d xcommerce_auth -c "
INSERT INTO users_micro (username, email, password_hash, enabled, active, role)
VALUES ('admin', 'admin@xcommerce.com', '$bcryptHash', true, true, 'ADMIN');
"

Write-Host "Verifying user creation..."
docker compose exec -T db psql -U postgres -d xcommerce_auth -c "SELECT username, email, active, role FROM users_micro WHERE username = 'admin';"

Write-Host "" -ForegroundColor Green
Write-Host "✅ Auth user created!" -ForegroundColor Green
Write-Host "Test with: curl -X POST http://localhost:8081/auth/login -H 'Content-Type: application/json' -d '{\"username\":\"admin\",\"password\":\"123456\"}'" -ForegroundColor Yellow
