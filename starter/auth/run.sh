echo "Making a request to http://localhost:8084/auth/login"

curl -iX POST http://localhost:8084/auth/login --header "Content-Type: application/json" -d "{
    \"email\" : \"test@example.com\",
    \"password\" : \"TestPassword123\"}"

echo "Making a request to http://localhost:8084/auth/signup"

# This is not the correct body
curl -iX POST http://localhost:8084/auth/login --header "Content-Type: application/json" -d "{
    \"email\" : \"test@example.com\",
    \"password\" : \"TestPassword123\"}"
