#!/bin/sh
GRADLE_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
GRADLE_URL="https://services.gradle.org/distributions/gradle-8.4-bin.zip"
GRADLE_DIR="$GRADLE_HOME/wrapper/dists/gradle-8.4-bin"

if [ ! -d "$GRADLE_DIR" ]; then
    mkdir -p "$GRADLE_DIR"
    curl -L "$GRADLE_URL" -o "$GRADLE_DIR/gradle.zip"
    unzip -q "$GRADLE_DIR/gradle.zip" -d "$GRADLE_DIR"
fi

exec "$GRADLE_DIR/gradle-8.4/bin/gradle" "$@"
