FROM ghcr.io/navikt/fp-baseimages/distroless:21
LABEL org.opencontainers.image.source=https://github.com/navikt/ft-tilgang

COPY target/classes/logback*.xml conf/
COPY target/lib/*.jar lib/
COPY target/app.jar .

CMD ["app.jar"]
