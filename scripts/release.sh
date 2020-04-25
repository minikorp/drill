#!/usr/bin/env bash

require_clean_work_tree() {
  # Update the index
  git update-index -q --ignore-submodules --refresh
  err=0

  # Disallow unstaged changes in the working tree
  if ! git diff-files --quiet --ignore-submodules --; then
    echo >&2 "cannot $1: you have unstaged changes."
    git diff-files --name-status -r --ignore-submodules -- >&2
    err=1
  fi

  # Disallow uncommitted changes in the index
  if ! git diff-index --cached --quiet HEAD --ignore-submodules --; then
    echo >&2 "cannot $1: your index contains uncommitted changes."
    git diff-index --cached --name-status -r --ignore-submodules HEAD -- >&2
    err=1
  fi

  if [[ ${err} == 1 ]]; then
    echo >&2 "Please commit or stash them."
    exit 1
  fi
}

cd "$(dirname "$0")" || exit
cd .. # Go root project directory

# Ensure tags are up to date
require_clean_work_tree "Generate release"
git pull
git pull --tags
require_clean_work_tree "Generate release"

# Ensure test pass
./gradlew clean build test || exit 1

# bump version and push
./bump-tag.sh minor
git push --tags
git push