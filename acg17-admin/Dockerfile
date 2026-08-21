# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:25-jdk-noble AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY acg17-admin/pom.xml acg17-admin/pom.xml
COPY acg17-admin/src/ acg17-admin/src/

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -pl acg17-admin -am clean package -Dmaven.test.skip=true

FROM eclipse-temurin:25-jre-noble AS runtime

ARG APP_UID=10001
ARG APP_GID=10001

RUN apt-get update \
    && apt-get install --yes --no-install-recommends ca-certificates libvips42t64 \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid "${APP_GID}" acg17 \
    && useradd --uid "${APP_UID}" --gid acg17 --home-dir /app \
        --no-create-home --shell /usr/sbin/nologin acg17 \
    && install -d -o acg17 -g acg17 /app /data/acg17

WORKDIR /app

COPY --from=build --chown=acg17:acg17 \
    /workspace/acg17-admin/target/acg17-admin-*.jar /app/acg17-admin.jar

ENV LANG=C.UTF-8

USER acg17:acg17

EXPOSE 18002

ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "/app/acg17-admin.jar"]
