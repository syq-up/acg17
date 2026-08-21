# syntax=docker/dockerfile:1.7

FROM mysql:8.4

COPY --chmod=0444 database/acg17.sql /docker-entrypoint-initdb.d/001-acg17.sql

