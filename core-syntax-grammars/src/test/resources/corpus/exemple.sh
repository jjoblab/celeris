#!/bin/sh
# Corpus Shell — construction d'un éditeur embarquable

set -eu

PROJET="celeris"
VERSION=2
DESTINATION="${1:-build/${PROJET}}"
JDK="${JAVA_HOME:-/usr/lib/jvm/java-21}"

log() {
    echo "[${PROJET}] $1"
}

nettoyer() {
    rm -rf "${DESTINATION}" && log "nettoyage de ${DESTINATION}"
}

compiler() {
    local cible="$1"
    shift
    log "compilation de ${cible} avec ${JDK}"
    ./gradlew ":${cible}:assemble" "$@"
}

empaqueter() {
    if [ -d "${DESTINATION}/sorties" ]; then
        tar -czf "${PROJET}-${VERSION}.tar.gz" "${DESTINATION}/sorties"
        log "paquet ${PROJET}-${VERSION}.tar.gz pret"
    else
        log "rien a empaqueter" >&2
        exit 1
    fi
}

nettoyer
compiler core-text --no-daemon
compiler demo --no-daemon
empaqueter
log "termine en ${SECONDS}s"
