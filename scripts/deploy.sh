#!/usr/bin/env bash
version=1.4.19
mvn package shade:shade -DskipTests
cd target
rm -rf java
rm solar-moon-common.zip
mkdir java
mkdir java/lib
cp solar-moon-common-$version.jar java/lib
zip -r solar-moon-common.zip java
aws s3 cp solar-moon-common.zip s3://solarmoonanalytics/lambda/
aws lambda publish-layer-version --layer-name solar-moon-common --compatible-architectures arm64 --compatible-runtimes java17 --description $version --region us-west-2 --content S3Bucket=solarmoonanalytics,S3Key=lambda/solar-moon-common.zip --output text