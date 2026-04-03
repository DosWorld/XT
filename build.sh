#!/bin/bash

rm xt.zip
mvn clean
mvn package -DskipTests
mkdir target/bin
cp bin/* target/bin
cp target/xt.jar target/bin
cd target/bin
zip ../../xt.zip *
cd ../..

