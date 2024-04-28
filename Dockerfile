FROM ghcr.io/navikt/fp-baseimages/java:22

LABEL org.opencontainers.image.source=https://github.com/navikt/fp-tilgang
ENV TZ=Europe/Oslo

RUN mkdir lib
RUN mkdir conf

COPY target/classes/logback*.xml conf/
COPY target/lib/*.jar lib/
COPY target/app.jar .

ENV TZ=Europe/Oslo
ENV JAVA_OPTS="-Djava.security.egd=file:/dev/urandom \
    -Dlogback.configurationFile=conf/logback.xml"
