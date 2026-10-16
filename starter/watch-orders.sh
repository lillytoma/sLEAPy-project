#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
KAFKA_CONTAINER="${KAFKA_CONTAINER:-sleapy-kafka}"
BOOTSTRAP_SERVER="${BOOTSTRAP_SERVER:-localhost:9092}"
TOPIC="${TOPIC:-order_placement}"

cd "$ROOT_DIR"

echo "Starting Kafka and Zookeeper..."
docker-compose up -d zookeeper kafka

echo "Waiting for Kafka at ${BOOTSTRAP_SERVER}..."
for attempt in {1..30}; do
  if docker exec "$KAFKA_CONTAINER" kafka-broker-api-versions \
      --bootstrap-server "$BOOTSTRAP_SERVER" >/dev/null 2>&1; then
    break
  fi

  if [[ "$attempt" -eq 30 ]]; then
    echo "Kafka did not become ready within 30 seconds." >&2
    exit 1
  fi
  sleep 1
done

docker exec "$KAFKA_CONTAINER" kafka-topics \
  --bootstrap-server "$BOOTSTRAP_SERVER" \
  --create \
  --if-not-exists \
  --topic "$TOPIC" \
  --partitions 3 \
  --replication-factor 1 >/dev/null

echo
echo "${TOPIC} topic layout:"
docker exec "$KAFKA_CONTAINER" kafka-topics \
  --bootstrap-server "$BOOTSTRAP_SERVER" \
  --describe \
  --topic "$TOPIC"

echo
echo "Streaming new ${TOPIC} messages. Press Ctrl+C to stop."
echo
docker exec -it "$KAFKA_CONTAINER" kafka-console-consumer \
  --bootstrap-server "$BOOTSTRAP_SERVER" \
  --topic "$TOPIC" \
  --property print.timestamp=true \
  --property print.partition=true \
  --property print.offset=true \
  --property print.key=true
