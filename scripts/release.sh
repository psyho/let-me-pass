#!/bin/bash

set -e

rm -rf target/release
mkdir -p target/release
cp -r target/unpacked/*.{png,ttf,js,css,json} resources/_locales target/release
(cd target && zip -r ../release.zip release)
