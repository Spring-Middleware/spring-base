#!/usr/bin/env sh
# Build only the registry Docker image using the runtime Dockerfile (POSIX shell)
# Usage: ./tools/build-registry-image.sh [--jar <path>] [--dockerfile <path>] [--tag <image:tag>] [--no-cache]

set -eu

JAR="parent/registry/boot/target/registry-boot-1.0.0.jar"
DOCKERFILE="parent/registry/boot/Dockerfile.runtime"
TAG="spring-base_registry:local"
NO_CACHE=0

print_help() {
  cat <<EOF
Usage: $0 [--jar <relative-path>] [--dockerfile <relative-path>] [--tag <image:tag>] [--no-cache]

Defaults:
  --jar        $JAR
  --dockerfile $DOCKERFILE
  --tag        $TAG

Example:
  $0 --no-cache --tag "spring-base/registry:dev"
EOF
}

# parse args
while [ $# -gt 0 ]; do
  case "$1" in
    --jar)
      JAR="$2"; shift 2;;
    --dockerfile)
      DOCKERFILE="$2"; shift 2;;
    --tag)
      TAG="$2"; shift 2;;
    --no-cache)
      NO_CACHE=1; shift 1;;
    -h|--help)
      print_help; exit 0;;
    *)
      echo "Unknown arg: $1" >&2; print_help; exit 2;;
  esac
done

# resolve repository root (parent of tools folder)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DOCKERFILE_PATH="$REPO_ROOT/$DOCKERFILE"
JAR_PATH="$REPO_ROOT/$JAR"

printf "Repository root: %s\n" "$REPO_ROOT"
printf "Using Dockerfile: %s\n" "$DOCKERFILE_PATH"
printf "Using JAR: %s\n" "$JAR_PATH"
printf "Target image tag: %s\n" "$TAG"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker CLI not found in PATH. Please install Docker." >&2
  exit 2
fi

if [ ! -f "$DOCKERFILE_PATH" ]; then
  echo "Dockerfile not found: $DOCKERFILE_PATH" >&2
  exit 3
fi

if [ ! -f "$JAR_PATH" ]; then
  echo "Warning: JAR not found at $JAR_PATH" >&2
  echo "You can build it with: mvn -pl parent/registry/boot -am -DskipTests package" >&2
  # continue: user may want to build inside image by changing ARG in Dockerfile
fi

NO_CACHE_ARG=""
if [ "$NO_CACHE" -eq 1 ]; then
  NO_CACHE_ARG="--no-cache"
fi

# build
CMD=(docker build $NO_CACHE_ARG -f "$DOCKERFILE" -t "$TAG" --build-arg "JAR=$JAR" "$REPO_ROOT")

printf "Running: %s\n" "${CMD[*]}"

# Execute the command
# Use eval to preserve quoting semantics
eval "${CMD[*]}"

printf "Docker image built: %s\n" "$TAG"
exit 0

