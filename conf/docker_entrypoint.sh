#!/usr/bin/env bash

# TODO: wait for mysql db...
while ! mysqladmin ping -h wuekabel_db --silent; do
    echo "Could not ping mysql server, waiting..."
    sleep 1
done

bin/wuekabel -Dplay.http.secret.key=asodivzßs9vz -Dconfig.resource=prod_docker.conf \
    -Dplay.evolutions.db.default.autoApply=true