#!/usr/bin/env bash

MOODLE_PORT=8080

docker run -d --name DB -e MYSQL_DATABASE=moodle -e MYSQL_ROOT_PASSWORD=moodle -e MYSQL_USER=moodle -e MYSQL_PASSWORD=moodle mysql:5

docker run -d -P --name moodle --link DB:DB -e MOODLE_URL=http://localhost:${MOODLE_PORT} -p ${MOODLE_PORT}:80 jhardison/moodle