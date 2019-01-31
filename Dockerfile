FROM openjdk:11-jre-slim-stretch

RUN addgroup wuekabel && useradd -g wuekabel wuekabel && apt update && apt -y install mysql-client

USER wuekabel:wuekabel

ADD --chown=wuekabel:wuekabel target/universal/wuekabel.tgz /home/wuekabel/

WORKDIR /home/wuekabel/wuekabel

COPY --chown=wuekabel:wuekabel other_resources/docker_entrypoint.sh .

EXPOSE 9000

ENTRYPOINT ./docker_entrypoint.sh