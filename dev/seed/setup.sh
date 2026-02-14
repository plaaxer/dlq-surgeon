#!/bin/bash
set -e

RABBITMQ_HOST=${RABBITMQ_HOST:-rabbitmq}
RABBITMQ_USER=${RABBITMQ_USER:-user}
RABBITMQ_PASS=${RABBITMQ_PASS:-password}
RABBITMQ_PORT=${RABBITMQ_PORT:-15672}

echo "Downloading rabbitmqadmin..."
curl -s -o rabbitmqadmin http://$RABBITMQ_HOST:$RABBITMQ_PORT/cli/rabbitmqadmin
chmod +x rabbitmqadmin

ADMIN="./rabbitmqadmin --host=$RABBITMQ_HOST --port=$RABBITMQ_PORT --username=$RABBITMQ_USER --password=$RABBITMQ_PASS"

echo "Declaring exchanges..."
$ADMIN declare exchange name=orders type=direct durable=true
$ADMIN declare exchange name=orders.dlx type=direct durable=true

echo "Declaring queues..."
$ADMIN declare queue name=orders.queue durable=true arguments='{"x-dead-letter-exchange":"orders.dlx"}'
$ADMIN declare queue name=orders.dead durable=true

echo "Declaring bindings..."
$ADMIN declare binding source=orders destination=orders.queue routing_key=orders
$ADMIN declare binding source=orders.dlx destination=orders.dead routing_key=orders

echo "Topology ready."