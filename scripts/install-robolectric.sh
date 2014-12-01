#!/bin/bash

set -e

PROJECT=$(cd $(dirname "$0")/..; pwd)

cd "$PROJECT"; mvn clean

cd "$PROJECT"/robolectric-annotations; mvn clean install
cd "$PROJECT"/robolectric-utils; mvn clean install
cd "$PROJECT"/robolectric-resources; mvn clean install
cd "$PROJECT"/robolectric-processor; mvn clean install
cd "$PROJECT"/robolectric-fakehttp; mvn clean install

cd "$PROJECT"/robolectric-shadows; mvn clean velocity:velocity javadoc:javadoc source:jar install -Pandroid-15 -DskipTests
cd "$PROJECT"/robolectric-shadows; mvn clean velocity:velocity javadoc:javadoc source:jar install -Pandroid-16 -DskipTests
cd "$PROJECT"/robolectric-shadows; mvn clean velocity:velocity javadoc:javadoc source:jar install -Pandroid-17 -DskipTests
cd "$PROJECT"/robolectric-shadows; mvn clean velocity:velocity javadoc:javadoc source:jar install -Pandroid-18 -DskipTests
cd "$PROJECT"/robolectric-shadows; mvn clean velocity:velocity javadoc:javadoc source:jar install -Pandroid-19 -DskipTests
cd "$PROJECT"/robolectric-shadows; mvn clean velocity:velocity javadoc:javadoc source:jar install -Pandroid-21 -DskipTests

cd "$PROJECT"; mvn javadoc:javadoc source:jar install
