# dev

Local development environment for testing DLQ-Surgeon.

Spins up a RabbitMQ instance pre-populated with 3 dead-lettered messages with intentionally broken payloads.

## Usage

```bash
cd dev
docker compose up
```

Once running, the RabbitMQ management UI is available at `http://localhost:15672` (user: `user`, password: `password`).
The 3 broken messages will be sitting in `orders.dead`, ready to be repaired with DLQ-Surgeon.

## Seed module

```bash
cd dev/seed
uv sync
```