#!/usr/bin/env bash
# Generate/serve Allure reports for ATAS tests.
# Usage:
#   scripts/generate-reports.sh             # run tests if needed, build static report
#   scripts/generate-reports.sh --serve     # run tests if needed, serve report via allure:serve
#   scripts/generate-reports.sh --static    # force static only (no serve)
#   scripts/generate-reports.sh --no-test   # don't run tests; use existing results
# Note: Spring Boot will automatically load .env file via EnvFileLoader

set -euo pipefail

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
MAVEN_WRAPPER="$PROJECT_ROOT/mvnw"

# Ensure we're in the project root
cd "$PROJECT_ROOT"
MODULE="atas-tests"
RESULTS_DIR="$PROJECT_ROOT/$MODULE/target/allure-results"
SITE_DIR="$PROJECT_ROOT/$MODULE/target/site/allure-maven-plugin"
HISTORY_SRC="$SITE_DIR/history"
HISTORY_DST="$RESULTS_DIR/history"

SERVE=false
RUN_TESTS=true
STATIC_ONLY=false

for arg in "${@:-}"; do
  case "$arg" in
    --serve)   SERVE=true ;;
    --static)  STATIC_ONLY=true ;;
    --no-test) RUN_TESTS=false ;;
    *) echo "Unknown arg: $arg" >&2; exit 2 ;;
  esac
done

# Already in PROJECT_ROOT from above, but keep pushd for consistency
pushd "$PROJECT_ROOT" >/dev/null

# 0) Ensure module compiles (fast fail if broken POMs)
"$MAVEN_WRAPPER" -q -pl "$MODULE" -am -DskipTests verify -DskipITs=true >/dev/null

# 1) If previous report exists, copy history so trends persist
if [[ -d "$HISTORY_SRC" ]]; then
  mkdir -p "$RESULTS_DIR"
  rsync -a --delete "$HISTORY_SRC/" "$HISTORY_DST/" || true
fi

# 2) Run tests if requested or if no results yet
if $RUN_TESTS || [[ ! -d "$RESULTS_DIR" ]] || [[ -z "$(ls -A "$RESULTS_DIR" 2>/dev/null || true)" ]]; then
  echo "Running tests to produce Allure results..."
  "$MAVEN_WRAPPER" -q -pl "$MODULE" -am clean test
fi

# 3) Sanity: ensure results exist now
if [[ ! -d "$RESULTS_DIR" ]] || [[ -z "$(ls -A "$RESULTS_DIR" 2>/dev/null || true)" ]]; then
  echo "No Allure results found in $RESULTS_DIR"
  echo "Make sure tests ran and the Allure JUnit 5 adapter is active."
  exit 1
fi

# 4) Generate static HTML
echo "Generating Allure static report..."
"$MAVEN_WRAPPER" -q -pl "$MODULE" allure:report
echo "Static report: $SITE_DIR/index.html"

# 5) Serve if requested (best experience)
if $SERVE && ! $STATIC_ONLY; then
  echo "Serving Allure report (Ctrl+C to stop)..."
  # This runs Allure's embedded HTTP server
  "$MAVEN_WRAPPER" -pl "$MODULE" allure:serve
else
  # Or advise how to serve static files to avoid file:// CORS issues
  echo "Tip: to view the static report without CORS issues:"
  echo "  cd \"$SITE_DIR\" && python3 -m http.server 8088"
  echo "  open http://localhost:8088"
fi

popd >/dev/null
