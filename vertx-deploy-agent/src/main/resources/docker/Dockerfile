FROM vertx/vertx3
MAINTAINER Marcel Soute <msoute@gmail.com>

RUN mkdir /usr/local/vertx/run
RUN mkdir /etc/vertx
RUN mkdir /var/log/vertx
RUN mkdir /var/cache/vertx

RUN wget -P /usr/local/vertx/lib http://central.maven.org/maven2/ch/qos/logback/logback-core/1.2.3/logback-core-1.2.3.jar
RUN wget -P /usr/local/vertx/lib http://central.maven.org/maven2/ch/qos/logback/logback-classic/1.2.3/logback-classic-1.2.3.jar
RUN wget -P /usr/local/vertx/lib http://central.maven.org/maven2/org/slf4j/slf4j-api/1.7.25/slf4j-api-1.7.25.jar

COPY init/defaults /etc/default/vertx
COPY init/debian.vertx.deploy /etc/init.d/vertx-deploy
COPY config/config.json /etc/vertx/config.json
COPY config/service-config.json /etc/vertx/service-config.json
COPY config/logback.xml /usr/local/vertx/conf/logback.xml

EXPOSE 6789 6789

ENTRYPOINT ["sh", "-c"]
CMD ["exec vertx run maven:nl.jpoint.vertx-deploy-tools:vertx-deploy-agent:3.5.0-SNAPSHOT -instances 1 -conf /etc/vertx/config.json -id nl.jpoint.vertx-deploy-tools:vertx-deploy-agent:3.5.0-SNAPSHOT"]



