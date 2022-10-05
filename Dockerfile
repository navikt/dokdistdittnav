FROM navikt/java:17

COPY app/target/app.jar /app/app.jar
COPY export-vault-secrets.sh /init-scripts/50-export-vault-secrets.sh
COPY export-java-opts.sh /init-scripts/99-export-java-opts.sh
