#!/bin/bash

set -e

lein clean
lein chromebuild once
cp -r resources/_locales target/unpacked