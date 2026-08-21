#!/usr/bin/env bash
set -Eeuo pipefail

repository_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
image_tag="${IMAGE_TAG:-0.1.0}"
target_platform="${TARGET_PLATFORM:-linux/arm64}"
platform_slug="${target_platform//\//-}"
output_file="${OUTPUT_FILE:-${repository_root}/deploy/out/acg17-images-${image_tag}-${platform_slug}.tar}"

if ! command -v docker >/dev/null 2>&1; then
    printf 'Docker is required to build the image bundle.\n' >&2
    exit 1
fi

if ! docker buildx version >/dev/null 2>&1; then
    printf 'Docker Buildx is required to build the image bundle.\n' >&2
    exit 1
fi

mkdir -p "$(dirname -- "${output_file}")"

printf 'Building ACG17 images with tag %s for %s...\n' "${image_tag}" "${target_platform}"

docker buildx build \
    --platform "${target_platform}" \
    --provenance=false \
    --file "${repository_root}/acg17-admin/Dockerfile" \
    --tag "acg17-backend:${image_tag}" \
    --load \
    "${repository_root}"

docker buildx build \
    --platform "${target_platform}" \
    --provenance=false \
    --file "${repository_root}/acg17-ui/Dockerfile" \
    --tag "acg17-frontend:${image_tag}" \
    --load \
    "${repository_root}"

docker buildx build \
    --platform "${target_platform}" \
    --provenance=false \
    --file "${repository_root}/database/Dockerfile" \
    --tag "acg17-mysql:${image_tag}" \
    --load \
    "${repository_root}"

printf 'Saving images to %s...\n' "${output_file}"

docker save \
    --output "${output_file}" \
    "acg17-backend:${image_tag}" \
    "acg17-frontend:${image_tag}" \
    "acg17-mysql:${image_tag}"

printf 'Image bundle created: %s\n' "${output_file}"
