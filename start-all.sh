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

java -jar xslt-processor/target/xslt-processor-$VERSION.jar \
     --server.port=9001 \
     --spring.application.name=order-pricer \
     --spring.cloud.stream.bindings.input.destination=orders.input \
     --spring.cloud.stream.bindings.output.destination=orders.priced \
     --xslt.stylesheet=file:resources/add-price.xsl \
     > logs/order-pricer.log 2>&1 &
echo $! >> logs/pids

java -jar xslt-processor/target/xslt-processor-$VERSION.jar \
     --server.port=9002 \
     --spring.application.name=tax-calculator \
     --spring.cloud.stream.bindings.input.destination=orders.priced \
     --spring.cloud.stream.bindings.output.destination=orders.taxed \
     --xslt.stylesheet=file:resources/calc-tax.xsl \
     > logs/tax-calculator.log 2>&1 &
echo $! >> logs/pids
