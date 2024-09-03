[![CodeQL](https://github.com/bigboxer23/solar-moon-common/actions/workflows/codeql.yml/badge.svg)](https://github.com/bigboxer23/solar-moon-common/actions/workflows/codeql.yml)

# solar-moon-common

This repository contains common code used by solar moon projects, excluding payment.

It is designed to be leveraged by wrapper webservices which are implemented as lambdas ([solar-moon-ingest-lambda](https://github.com/bigboxer23/solar-moon-ingest-lambda))
or via standalone microservices in a springboot container ([solar-moon-ingest](https://github.com/bigboxer23/solar-moon-ingest)).

## Building
See the [solar-moon-server-config](https://github.com/bigboxer23/solar-moon-server-config/tree/main/scripts/lambda) repository. 
Descriptions of what's included in each directory below:

#### config
properties (and test props) each lambda needs to properly run
#### functions
Scripts in this directory are used for building newer versions of each function from subproject source code (common, ingest).
Small amount of source code also exists here for node native lambda functions used
#### layers
Scripts in this directory are used for building a newer version of each layer from source code in the subprojects (common, ingest)
#### top level scripts
These scripts take individual actions done above and roll them into one handy command.
1. `updateAll.sh` Update all layers, functions
2. `updateAllFunctionLayers.sh` Trigger an update of all functions to use the latest version of each layer
3. `updateAllFunctions.sh` Build all lambda functions, push the new version of each to lambda


