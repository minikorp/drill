#!/usr/bin/env bash

tags=$(git tag --sort=-version:refname)
latest_tag=$(echo "$tags" | head -1)
new_tag=$(./semver.sh bump "$1" "$latest_tag")
echo "$latest_tag -> $new_tag"
git tag new_tag
