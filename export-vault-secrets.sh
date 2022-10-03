#!/usr/bin/env sh

if test -f /var/run/secrets/nais.io/srvdokdistdittnav/username;
then
    echo "Setting SERVICEUSER_USERNAME"
    export SERVICEUSER_USERNAME=$(cat /var/run/secrets/nais.io/srvdokdistdittnav/username)
fi

if test -f /var/run/secrets/nais.io/srvdokdistdittnav/password;
then
    echo "Setting SERVICEUSER_PASSWORD"
    export SERVICEUSER_PASSWORD=$(cat /var/run/secrets/nais.io/srvdokdistdittnav/password)
fi

if test -f /var/run/secrets/nais.io/vault/certificate_keystore
then
    echo "Setting SRVDOKDISTDITTNAVCERT_KEYSTORE"
    CERT_PATH='/var/run/secrets/nais.io/vault/certificate_keystore-extracted'
    openssl base64 -d -A -in /var/run/secrets/nais.io/vault/certificate_keystore -out $CERT_PATH
    export SRVDOKDISTDITTNAVCERT_KEYSTORE=$CERT_PATH
fi

if test -f /var/run/secrets/nais.io/vault/certificate_keystore_password
then
    echo "Setting SRVDOKDISTDITTNAVCERT_PASSWORD"
    export SRVDOKDISTDITTNAVCERT_PASSWORD=$(cat /var/run/secrets/nais.io/vault/certificate_keystore_password)
fi

