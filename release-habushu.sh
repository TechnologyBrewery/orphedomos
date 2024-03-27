#!/bin/bash

# This script requires two input parameter, 1)target release version 2)next development version.
# To run the script, enter a command like this: ./release-orphedomos.sh <target-release-version> <next-development-version>

# After the script is executed, navigate to github to create a PR to merge your release branch
# and validate that the released version of orphedomos-maven-plugin may be found in Maven Central

echo "////////////////////////////////////////////"
echo "//"
echo "// Use this script to run a release for Orphedomos"
echo "//"
echo "////////////////////////////////////////////"

echo "//////////// Check out a release branch ////////////"
git checkout -b $1-release

if [[ "$?" -ne 0 ]] ; then
  echo 'Process failed! Unable to check out release branch!'; exit 1
fi

echo "/////////// Check there are no uncommitted local changes ///////////"
mvn scm:check-local-modification

if [[ "$?" -ne 0 ]] ; then
  echo 'Process failed! Uncommitted local changes detected!'; exit 1
fi

echo "/////////// Update POM versions to the target release version ///////////"
mvn versions:set -DnewVersion=$1 -DgenerateBackupPoms=false

if [[ "$?" -ne 0 ]] ; then
  echo 'Process failed! Unable to update POM versions to the target release version'; exit 1
fi

echo "///////////  Rebuild Orphedomos modules ///////////"
mvn clean install -Pbootstrap -Dmaven.build.cache.enabled=false && mvn clean install -Dmaven.build.cache.enabled=false

if [[ "$?" -ne 0 ]] ; then
  echo 'Rebuilding Orphedomos failed!'; exit 1
fi

echo "///////////  Commit all changes that reflect the target release version and create a tag ///////////"
mvn scm:checkin -Dmessage=":bookmark: Prepare release orphedomos-$1"
mvn scm:tag -Dtag=orphedomos-$1

if [[ "$?" -ne 0 ]] ; then
  echo 'Process failed! Unable to commit changes and create a tag!'; exit 1
fi

echo "/////////// Deploy Orphedomos to Maven Central ///////////"
mvn clean deploy -P ossrh-release -Dmaven.build.cache.enabled=false

if [[ "$?" -ne 0 ]] ; then
  echo 'Process failed! Unable to deploy Orphedomos to Maven Central!'; exit 1
fi

echo "/////////// Create snapshot version of Orphedomos ///////////"
mvn clean install -Pbootstrap

echo "/////////// Update POM versions to the next development version ///////////"
mvn versions:set -DnewVersion=$2 -DgenerateBackupPoms=false

if [[ "$?" -ne 0 ]] ; then
  echo 'Process failed! Unable to update POM versions to the next development version!'; exit 1
fi

echo "/////////// Rebuild Orphedomos test modules ///////////"
mvn clean install

if [[ "$?" -ne 0 ]] ; then
  echo 'Rebuilding Orphedomos failed!'; exit 1
fi

echo "/////////// Commit all changes that reflect the next development iteration///////////"
mvn scm:checkin -Dmessage=":arrow_up: Prepare for next development iteration"

if [[ "$?" -ne 0 ]] ; then
  echo 'Process failed! Unable to commit changes for the next development iteration!'; exit 1
fi