#!/usr/bin/env sh

JAVA_OPTS="${JAVA_OPTS} -Xmx1024m \
               -Djavax.net.ssl.trustStoreType=jks \
               -Djavax.net.ssl.keyStore=${NAV_TRUSTSTORE_PATH} \
               -Djavax.net.ssl.keyStorePassword=${NAV_TRUSTSTORE_PASSWORD} \
               -Djavax.net.ssl.keyStoreType=jks \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=nais"

export JAVA_OPTS
