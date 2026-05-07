#!/bin/bash

# Check if jq is installed
if ! command -v jq &> /dev/null; then
    echo "jq is required but not installed. Please install it."
    exit 1
fi

echo "Creating a market order..."
ORDER_RESPONSE=$(curl -s -X POST http://localhost:8084/api/market-orders \
  -H "Content-Type: application/json" \
  -d '{
    "productType": "SEDAN",
    "quantity": 1,
    "bomVersion": "v1.0.0",
    "customerName": "Test Customer",
    "requiredDeliveryDate": "2026-05-15T00:00:00",
    "priority": 2
  }')

ORDER_ID=$(echo $ORDER_RESPONSE | jq -r '.orderId')
if [ "$ORDER_ID" == "null" ] || [ -z "$ORDER_ID" ]; then
    echo "Failed to create order. Response: $ORDER_RESPONSE"
    exit 1
fi

echo "Order Created with ID: $ORDER_ID"

echo "Simulating status transitions via WebSocket trigger endpoint..."
STATUSES=("PROCESSING" "ASSEMBLED" "SHIPPED" "COMPLETED")

for STATUS in "${STATUSES[@]}"; do
  echo "Updating status to $STATUS..."
  curl -s -X POST "http://localhost:8084/api/orders/ws/$ORDER_ID/status?status=$STATUS&message=Simulated%20update%20to%20$STATUS"
  echo ""
  sleep 2
done

echo "Test sequence complete. If the frontend is connected to ws://localhost:8084/ws/orders, it should have received these updates."
