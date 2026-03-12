# DLQ-Surgeon

> A sole-purpose sidecar scalpel for RabbitMQ Dead Letter Queues.
> Fetch → edit → validate → re-inject. Stateless. No data loss.

Distributed as a **GraalVM Native Image** — a single executable binary compiled ahead-of-time directly to machine code. No JRE required, no JVM warmup, ~50 ms startup. Drop it on a server and run it. Also available as a fat JAR for platforms without a matching binary.

---

## The Problem

RabbitMQ's Dead Letter Exchanges do their job perfectly — but most messages in DLQs fail for *recoverable* reasons:

- A field was renamed in a deploy (`currency` → `currency_code`)
- A frontend bug sent `shippingAddress` as a string instead of an object
- A new required field (`metadata.version`) appeared after a schema migration
- A transient Redis blip dead-lettered 400 perfectly valid inventory updates

**Current reality:** developers copy the payload from the Management UI, edit it in VS Code, manually reconstruct the original exchange and routing key, and republish with curl — at 2 a.m. in production. One typo = lost message or duplicate processing.

DLQ-Surgeon solves this in 30 seconds with zero risk of data loss.

---

## Design Philosophy

| Principle | What it means |
|---|---|
| **Stateless** | No internal database. Pure RabbitMQ Management HTTP + AMQP client. |
| **In-memory-only** | Messages are held in RAM during the edit session. Never written to disk except for the temp file your editor opens (cleaned up on exit). |
| **Confirm-before-delete** | The original message is deleted from the DLQ *only after* a publisher confirm is received for the repaired message. If publish fails, nothing changes. |
| **Zero-bloat binary** | Single GraalVM Native Image binary. No JRE required. ~40 MB. <50 ms startup. |
| **Focused scope** | This tool does one thing. It is not a queue manager. |

---

## Quick Start

### Prerequisites

```bash
# For running the fat JAR (development)
java 21+

# For building the native binary (production)
sdk install java 21.0.4-graal   # via SDKMAN
gu install native-image
```

### Build

```bash
# Fat JAR (fast iteration, no GraalVM needed)
mvn package
java -jar target/dlq-surgeon-fat.jar --help

# Native binary (production, requires GraalVM)
mvn -Pnative package
./target/dlq-surgeon --help
```

### Core Workflow

```bash
# 1. List all queues and their dead-letter counts
dlq-surgeon list

# 2. Inspect messages without touching anything
dlq-surgeon peek orders.dead --count 20

# 3. Repair and re-inject
dlq-surgeon fix orders.dead \
  --schema ./schemas/order-created.json \
  --host rabbitmq.prod.internal \
  --user admin
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

Any flag still passed explicitly on the CLI overrides the config file value.

---

## Project Structure

```
src/main/java/dev/plaaxer/dlqsurgeon/
│
├── Main.java                        Entry point → delegates to DlqSurgeon
├── DlqSurgeon.java                  Root Picocli command, wires subcommands
│
├── cli/
│   ├── ConnectOptions.java          Reusable @Mixin: connection flags for all commands
│   ├── ListCommand.java             `dlq-surgeon list`  — read-only queue listing
│   ├── PeekCommand.java             `dlq-surgeon peek`  — read-only message inspection
│   └── FixCommand.java              `dlq-surgeon fix`   — the surgical repair workflow
│
├── client/
│   ├── ApiHttpClient.java           Thin HTTP client wrapper (auth, error handling)
│   ├── ManagementClient.java        RabbitMQ Management HTTP API (message fetch, queue info)
│   └── AmqpPublisher.java           AMQP re-publish with publisher confirms + DLQ ack-delete
│
├── model/
│   ├── RabbitMessage.java           In-memory snapshot of a fetched DLQ message
│   ├── XDeathEntry.java             Parsed x-death header entry
│   ├── QueueInfo.java               Queue metadata (name, message count, DLX flag)
│   └── RepairPlan.java              Immutable plan: target exchange/key + AMQP properties, shown before confirm
│
├── surgeon/
│   ├── MessageFetcher.java          Orchestrates fetching via ManagementClient
│   ├── PayloadEditor.java           Writes payload to temp file, opens $EDITOR, reads result
│   ├── SchemaValidator.java         JSON Schema validation before re-injection
│   └── Reinjector.java              Publish → confirm → delete (the safety-critical step)
│
└── tui/
    ├── Console.java                 Coloured output helpers (info/success/warn/error)
    └── MessagePicker.java           Interactive numbered list + search for message selection
```

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

## Adding JSON Schema Validation

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

---

## Contributing

The codebase is intentionally minimal. Before adding a feature, ask: *does this belong in a focused DLQ repair tool, or in a full queue manager?* If the latter, it probably doesn't belong here.
