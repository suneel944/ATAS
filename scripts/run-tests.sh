#!/usr/bin/env bash
#
# Simple wrapper to build and run all tests in the monorepo.  It
# invokes Maven on the root project, ensuring that the framework is
# compiled and the test module is executed.  Any additional Maven
# arguments may be passed to this script.
# Note: Spring Boot will automatically load .env file via EnvFileLoader

set -euo pipefail

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
MAVEN_WRAPPER="$PROJECT_ROOT/mvnw"

# Ensure we're in the project root (for .env file access)
cd "$PROJECT_ROOT"
pushd "$PROJECT_ROOT" > /dev/null

echo "Running ATAS tests..."
"$MAVEN_WRAPPER" -q -pl atas-tests -am clean test "$@"

echo "Tests completed."
popd > /dev/null