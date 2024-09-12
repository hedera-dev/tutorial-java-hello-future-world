#!/bin/bash

DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# install main dependencies
echo "Installing additional dependencies…"

cd ${DIR}/../transfer
gradle install

cd ${DIR}/../hts
gradle install

cd ${DIR}/../hcs
gradle install
