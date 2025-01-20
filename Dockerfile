FROM gcr.io/distroless/java21-debian12:nonroot

LABEL org.opencontainers.image.source=https://github.com/navikt/ft-tilgang
# Healtcheck lokalt/test
COPY --from=busybox:stable-musl /bin/wget /usr/bin/wget

# Working dir for RUN, CMD, ENTRYPOINT, COPY and ADD (required because of nonroot user cannot run commands in root)
WORKDIR /app

ENV LC_ALL="nb_NO.UTF-8"
ENV LANG="nb_NO.UTF-8"
ENV TZ="Europe/Oslo"
ENV JAVA_OPTS="-XX:+PrintCommandLineFlags \
    -XX:ActiveProcessorCount=2 \
    -XX:MaxRAMPercentage=75 \
    -XX:UseSVE=0 \
    -Duser.timezone=Europe/Oslo \
    -Djava.security.egd=file:/dev/urandom \
    -Dlogback.configurationFile=conf/logback.xml"

COPY target/classes/logback*.xml conf/
COPY target/lib/*.jar lib/
COPY target/app.jar .

CMD ["app.jar"]
