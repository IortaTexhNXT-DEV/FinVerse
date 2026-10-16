#!/usr/bin/env bash
# Starts the backend jar against a throwaway PostgreSQL database for the User Access load test.
# Local runs only (seed profile, local keys, the limits of sign-in and integration raised so that
# one load generator is not throttled; the rate-limit tests cover the limits themselves).
#
#   perf/uam/local/run-backend.sh            # uses database uam_perf on localhost:5432
#
# Needs: backend/target/brokerverse-backend.jar (mvn -DskipTests package), Python 3 with PyJWT and
# cryptography, and the database (createdb -O brokerverse uam_perf).
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../../.." && pwd)"
WORK="${PERF_WORK:-$HERE/work}"
mkdir -p "$WORK"
python3 "$HERE/integration_token.py" "$WORK" --hours 8 > "$WORK/scim.token"
(cd "$WORK" && python3 -m http.server 18099 --bind 127.0.0.1 > "$WORK/jwks.log" 2>&1 &)

export SPRING_PROFILES_ACTIVE=seed
export BROKERVERSE_PORT="${BROKERVERSE_PORT:-18080}"
export BROKERVERSE_DB_URL="${BROKERVERSE_DB_URL:-jdbc:postgresql://localhost:5432/uam_perf}"
export BROKERVERSE_DB_USER="${BROKERVERSE_DB_USER:-brokerverse}"
export BROKERVERSE_DB_PASSWORD="${BROKERVERSE_DB_PASSWORD:-brokerverse}"
export BROKERVERSE_JWT_SECRET="perf-local-secret-$(head -c 24 /dev/urandom | base64)"
export BROKERVERSE_MFA_ENCRYPTION_KEY="$(head -c 32 /dev/urandom | base64)"
export BROKERVERSE_CONFIG_PROMOTION_SIGNING_KEY="perf-local-signing-$(head -c 24 /dev/urandom | base64)"
export BROKERVERSE_MIGRATION_MASKING_KEY="perf-local-masking-$(head -c 24 /dev/urandom | base64)"
export BROKERVERSE_REDIS_ENABLED=false
export BROKERVERSE_KAFKA_ENABLED=false
export BROKERVERSE_SEED_PASSWORD="${PERF_PASSWORD:-Perf#Local2026}"
export BROKERVERSE_LOGIN_RATE_LIMIT=100000
export BROKERVERSE_INTEGRATION_RATE_LIMIT=100000
export BROKERVERSE_INTEGRATION_JWK_SET_URI=http://127.0.0.1:18099/jwks.json
export BROKERVERSE_INTEGRATION_ISSUER=https://gateway.perf.local
export BROKERVERSE_INTEGRATION_AUDIENCES=bibs-perf
exec java ${JAVA_OPTS:--Xmx1g} -jar "$ROOT/backend/target/brokerverse-backend.jar"
