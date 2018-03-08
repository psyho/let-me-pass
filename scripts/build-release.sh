#!/bin/bash

set -e

lein clean
lein with-profile +release chromebuild once
cp -r resources/_locales target/unpacked-release