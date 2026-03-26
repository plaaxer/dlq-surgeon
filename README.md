# DLQ-Surgeon

A CLI tool for repairing and re-injecting messages from RabbitMQ Dead Letter Queues. Fetch a message, edit the payload in your `$EDITOR`, optionally validate it against a JSON Schema, then re-publish it to the original exchange. The source message is deleted only after a publisher confirm is received - if anything fails, nothing is touched.

Distributed as a single native binary (no JRE required). Also available as a fat JAR.

---

## The Problem

RabbitMQ's Dead Letter Exchanges do their job — but most messages in DLQs fail for recoverable reasons:

- A field was renamed in a deploy (`currency` → `currency_code`)
- A frontend bug sent `shippingAddress` as a string instead of an object
- A new required field (`metadata.version`) appeared after a schema migration
- A transient Redis blip dead-lettered 400 perfectly valid inventory updates

The usual approach is to copy the payload from the Management UI, edit it locally, manually reconstruct the original exchange and routing key, and republish with curl. One typo can mean a lost message or duplicate processing.

Stateless. No internal database. Messages are held in RAM only - never written to disk except for the temp file your editor opens, which is cleaned up on exit.

---

## Quick Start

Download the binary for your platform from the [releases page](../../releases) and make it executable:

```bash
chmod +x dlq-surgeon
```

**Usage:**

```
dlq-surgeon [--profile <name>] <command> [<queue>] [--host <host>] [--user <user>] [--password <password>] [options]
```

Against a local RabbitMQ with default credentials (`guest`/`guest` on `localhost`), no flags are needed:

```bash
./dlq-surgeon list
./dlq-surgeon peek orders.dlq
./dlq-surgeon fix orders.dlq
```

Against a remote host:

```bash
# See all queues (message counts, DLX flag)
./dlq-surgeon list --host rabbitmq.prod.internal --user admin --password s3cr3t

# Inspect messages without touching anything
./dlq-surgeon peek orders.dlq --host rabbitmq.prod.internal --user admin --password s3cr3t

# Repair and re-inject, with schema validation
./dlq-surgeon fix orders.dlq \
  --host rabbitmq.prod.internal \
  --user admin \
  --password s3cr3t \
  --schema ./schemas/order-created.json
```

`fix` will:
1. Fetch messages from the DLQ (held in memory only)
2. Show an interactive picker — select the message to repair
3. Open the payload in your `$EDITOR`
4. Validate the edited payload against the JSON Schema (if `--schema` is given)
5. Show the full repair plan and ask for confirmation
6. Publish to the original exchange + routing key (read from `x-death` headers)
7. Wait for a publisher confirm from the broker
8. **Only then** delete the message from the DLQ

---

## Build from Source

```bash
# Fat JAR (requires Java 21+, no GraalVM needed)
mvn package
java -jar target/dlq-surgeon-fat.jar --help

# Native binary (requires GraalVM 21 with native-image)
mvn -Pnative package
./target/dlq-surgeon --help
```

---

## Connection Options

Resolution order (highest to lowest priority): CLI flag → config file → env var → built-in default.

| Flag | Env var | Default |
|---|---|---|
| `--profile` | — | `default` |
| `--host` | `RABBITMQ_HOST` | `localhost` |
| `--management-port` | `RABBITMQ_MANAGEMENT_PORT` | `15672` |
| `--amqp-port` | `RABBITMQ_AMQP_PORT` | `5672` |
| `--vhost` | `RABBITMQ_VHOST` | `/` |
| `--user` | `RABBITMQ_USER` | `guest` |
| `--password` | `RABBITMQ_PASSWORD` | `guest` |
| `--read-only` | — | `false` |

### Config file

Create `~/.dlq-surgeon/config.toml` with named profiles:

```toml
[default]
host     = "localhost"
user     = "guest"
password = "guest"

[prod]
host     = "rabbitmq.prod.internal"
user     = "admin"
password = "s3cr3t"
vhost    = "orders"

[staging]
host     = "rabbitmq.staging.internal"
user     = "admin"
password = "stagingsecret"
```

Then just:

```bash
dlq-surgeon fix orders.dlq                    # uses [default]
dlq-surgeon --profile prod fix orders.dlq     # uses [prod]
```

Any flag passed explicitly on the CLI overrides the config file value.

---

## Re-injection Target: How x-death Is Used

When a message is dead-lettered, RabbitMQ **prepends** an entry to the `x-death` header array — so **index 0 is always the most recent death**. Each entry records:

- `exchange` — the source exchange the message was originally published to (not the DLX)
- `routing-keys` — the routing keys used at that time
- `queue` — the queue where it died
- `reason` — `rejected`, `expired`, `maxlen`, or `delivery-limit`
- `count` — how many times it died in this queue/exchange combination

`fix` reads `x-death[0]` to determine where to re-inject. For a message that died twice in two different queues, the most recent queue's exchange and routing key are used. Use `--target-exchange` and `--target-routing-key` to override if needed.

---

## JSON Schema Validation

Pass `--schema` with a path to a JSON Schema file (Draft-04 through 2020-12):

```bash
dlq-surgeon fix orders.dead --schema ./schemas/order-created.json
```

If the edited payload fails validation, you'll be shown the errors and offered a chance to re-open the editor before the repair is abandoned or retried.

---

## Safety Notes

- **`--read-only`** disables all write operations. `list` and `peek` always work; `fix` exits immediately.
- **`--yes` / `-y`** skips confirmation prompts — useful in automation but use carefully.
- **`--strip-death-headers`** removes `x-death` and `x-first-death-*` headers before re-injection.
  Without this flag, the repaired message carries its full death history (the default, safer behavior).
- The delete step uses `basic.get` + `basic.ack` (not the bulk Management API delete) to ensure
  exactly the correct message is removed, even if new messages arrived in the DLQ during editing.