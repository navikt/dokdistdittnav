FROM navikt/java:17

COPY app/target/app.jar /app/app.jar
COPY export-vault-secrets.sh /init-scripts/50-export-vault-secrets.sh

ENV JAVA_OPTS="-Xmx1024m \
               -Djavax.net.ssl.trustStoreType=jks \
               -Djavax.net.ssl.keyStore=${SRVDOKDISTDITTNAVCERT_KEYSTORE} \
               -Djavax.net.ssl.keyStoreType=jks \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=nais"

