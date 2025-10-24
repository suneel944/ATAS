#!/usr/bin/env bash
#
# Simple wrapper to build and run all tests in the monorepo.  It
# invokes Maven on the root project, ensuring that the framework is
# compiled and the test module is executed.  Any additional Maven
# arguments may be passed to this script.

set -euo pipefail

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
pushd "$PROJECT_ROOT" > /dev/null

echo "Running ATAS tests..."
mvn -q -pl atas-tests -am clean test "$@"

echo "Tests completed."
popd > /dev/null