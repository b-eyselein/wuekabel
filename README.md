# Wuekabel

VokabelApp

## Deployment with Docker

* Prerequisites: Docker (`docker.io`) and Docker-compose (`docker-compose`) installed and configured

* Copy the files `docker-compose.yaml` and `conf/create_all.sql` on your server (mind the subfolder!)

* Run `docker-compose up -d`

* The server waits for a successful ping from the db and starts up

* After startup the server is reachable on `localhost:9090`