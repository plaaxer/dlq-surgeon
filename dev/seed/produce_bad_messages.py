import pika
import json
import os
import time

RABBITMQ_HOST = os.getenv("RABBITMQ_HOST", "rabbitmq")
RABBITMQ_USER = os.getenv("RABBITMQ_USER", "user")
RABBITMQ_PASS = os.getenv("RABBITMQ_PASS", "password")

connection = None
for i in range(10):
    try:
        connection = pika.BlockingConnection(
            pika.ConnectionParameters(
                host=RABBITMQ_HOST,
                credentials=pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASS)
            )
        )
        print("Connected to RabbitMQ.")
        break
    except pika.exceptions.AMQPConnectionError:
        print(f"Waiting for RabbitMQ... ({i+1}/10)")
        time.sleep(3)

if connection is None:
    print("Could not connect to RabbitMQ after 10 attempts. Exiting.")
    exit(1)

channel = connection.channel()

EXCHANGE = "orders"
ROUTING_KEY = "orders"

broken_messages = [
    {
        "description": "currency instead of currency_code",
        "payload": {
            "orderId": "order-001",
            "currency": "USD",
            "shippingAddress": {
                "street": "123 Main St",
                "city": "New York",
                "country": "US"
            },
            "metadata": {"version": 1}
        }
    },
    {
        "description": "shippingAddress as string instead of object",
        "payload": {
            "orderId": "order-002",
            "currency_code": "USD",
            "shippingAddress": "123 Main St, New York, US",
            "metadata": {"version": 1}
        }
    },
    {
        "description": "missing metadata.version",
        "payload": {
            "orderId": "order-003",
            "currency_code": "USD",
            "shippingAddress": {
                "street": "123 Main St",
                "city": "New York",
                "country": "US"
            },
            "metadata": {}
        }
    }
]

for msg in broken_messages:
    channel.basic_publish(
        exchange=EXCHANGE,
        routing_key=ROUTING_KEY,
        body=json.dumps(msg["payload"]),
        properties=pika.BasicProperties(
            delivery_mode=2,
            content_type="application/json",
            headers={"x-description": msg["description"]}
        )
    )
    print(f"Published: {msg['description']}")

print("Consuming and nacking to trigger dead-lettering...")

for _ in range(len(broken_messages)):
    method, properties, body = channel.basic_get(queue="orders.queue", auto_ack=False)
    if method:
        channel.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
        print(f"Nacked: {json.loads(body)['orderId']}")

connection.close()
print("Done. Check orders.dead in the management UI.")