FROM vertx/vertx3
MAINTAINER Marcel Soute <msoute@gmail.com>

RUN addgroup vertx
RUN useradd -d /home/vertx -g vertx -m -s /bin/bash vertx

RUN mkdir /usr/local/vertx/run
RUN mkdir /etc/vertx
RUN mkdir /var/log/vertx
RUN mkdir /var/cache/vertx

RUN wget -P /usr/local/vertx/lib http://central.maven.org/maven2/ch/qos/logback/logback-core/1.1.7/logback-core-1.1.7.jar
RUN wget -P /usr/local/vertx/lib http://central.maven.org/maven2/ch/qos/logback/logback-classic/1.1.7/logback-classic-1.1.7.jar
RUN wget -P /usr/local/vertx/lib http://central.maven.org/maven2/org/slf4j/slf4j-api/1.7.21/slf4j-api-1.7.21-javadoc.jar


COPY init/defaults /etc/default/vertx
COPY init/debian.vertx.deploy /etc/init.d/vertx-deploy
COPY config/config.json /etc/vertx/config.json
COPY config/service-config.json /etc/vertx/service-config.json
COPY config/logback.xml /usr/local/vertx/conf/logback.xml

RUN chown -R vertx:vertx /usr/local/vertx
RUN chown -R vertx:vertx /etc/vertx
RUN chown -R vertx:vertx /var/log/vertx
RUN chown -R vertx:vertx /var/cache/vertx

#USER vertx

EXPOSE 6789 6789

ENTRYPOINT vertx run -Dvertx.maven.remoteRepos=https://oss.sonatype.org/content/repositories/snapshots/ -Dvertx.maven.remoteSnapshotPolicy=always maven:nl.jpoint.vertx-deploy-tools:vertx-deploy:3.1.0-SNAPSHOT -instances 1 -conf /etc/vertx/config.json -id nl.jpoint.vertx-deploy-tools:vertx-deploy:3.1.0-SNAPSHOT



