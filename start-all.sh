#!/bin/bash

VERSION=1.0.0.BUILD-SNAPSHOT

if [ ! -d logs ]; then
  mkdir logs
fi

java -jar config-server/target/config-server-$VERSION.jar > logs/config-server.log 2>&1 &
echo $! > logs/pids

java -jar eureka-server/target/eureka-server-$VERSION.jar > logs/eureka-server.log 2>&1 &
echo $! >> logs/pids

java -jar gateway/target/gateway-$VERSION.jar > logs/gateway.log 2>&1 &
echo $! >> logs/pids

java -jar message-dispatcher/target/message-dispatcher-$VERSION.jar > logs/message-dispatcher.log 2>&1 &
echo $! >> logs/pids

java -jar order-status/target/order-status-$VERSION.jar > logs/order-status.log 2>&1 &
echo $! >> logs/pids

java -jar order-phase2/target/order-phase2-$VERSION.jar > logs/order-phase2.log 2>&1 &
echo $! >> logs/pids

java -jar order-processor/target/order-processor-$VERSION.jar > logs/order-processor.log 2>&1 &
echo $! >> logs/pids
