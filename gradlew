#!/bin/sh
# Gradle wrapper kept in source control so a clean checkout can build without a
# machine-specific Gradle installation.
set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec java ${JAVA_OPTS:-} ${GRADLE_OPTS:-} \
  -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
  org.gradle.wrapper.GradleWrapperMain "$@"
ïž