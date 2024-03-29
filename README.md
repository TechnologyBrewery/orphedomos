## Orphedomos Overview
[![License](https://img.shields.io/github/license/mashape/apistatus.svg)](https://opensource.org/licenses/mit)
[![Maven Central](https://img.shields.io/maven-central/v/org.technologybrewery.orphedomos/orphedomos-parent.svg)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.technologybrewery.orphedomos%22%20AND%20a%3A%22orphedomos-parent%22)
[![Status](https://img.shields.io/badge/Status-Incubating-yellow)](https://img.shields.io/badge/Status-Incubating-yellow)

Orphedomos is a plugin for orchestration and execution of docker builds as part of a maven build process.
Named for Orpheus, a great composer from Greek myth, and oikodómos, the Greek term for 'builder', Orphedomos
takes the process of building your project's docker images and elegantly composes it into your existing Maven 
workflows.  

The biggest difference between Orphedomos and other docker maven plugins is that Orphedomos intentionally leverages
the docker CLI for operations.  This avoids architecture-specific compatibility issues that are prevalent in certain
options, as well as producing highly explainable, user-replicable behaviors.  We support BuildKit as a first class
citizen, without mandating its usage.  Notably, these approaches are not universally desirable.  Orphedomos is 
meant to fill an existing gap in available functionality and design patterns, but does not claim to replace existing
options that provide alternative implementation choices.

## Project Status
Orphedomos is currently incubating.  As explained above, there are several similar projects in the community, but 
none currently meet the architecture-nuetral needs and support BuildKit.  We'd be happy to migrate to one of these 
more mature efforts if/when they support BuildKit or equivalent functionality in the future.  But until then, Orphedomos
can help fill this void.

## Usage

Orphedomos is integrated as a custom packaging type, which can be specified in a module's pom file.  This module makes the assertion that the creation of a Docker image should be a standalone process, with any prerequisites and dependencies
captured appropriately and represented within their own modules.  Below is a minimal example POM
that leverages Orphedomos.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
    
  <artifactId>orphedomos-demo</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <packaging>orphedomos</packaging>

  <name>Orphedomos Demo</name>

  <build>
    <plugins>
      <plugin>
        <groupId>org.technologybrewery.orphedomos</groupId>
        <artifactId>orphedomos-maven-plugin</artifactId>
        <version>LATEST VERSION HERE</version>
        <extensions>true</extensions>
      </plugin>
    </plugins>
  </build>
</project>
``` 

### Provided Goals

* `verify-docker-environment`.  Default Phase: `validate`.
* `build-docker-image`.  Default Phase: `package`.
* `push-docker-image`.  Default Phase: `deploy`.
* `multiplatform-build-deploy`.  Default Phase: `None`

### Artifacts

Three artifacts will be processed as part of the `install` and `deploy` phases.

* The Docker Image will be published by Orphedomos to a docker image repository.
* The Dockerfile and POM file will be published to the Maven artifact repository, allowing for safe dependency
specification and reactor ordering between `orphedomos`-packaged modules.  This can be configured
through the [`maven-install-plugin`](https://maven.apache.org/plugins/maven-install-plugin/install-mojo.html) and
[`maven-deploy-plugin`](https://maven.apache.org/plugins/maven-deploy-plugin/deploy-mojo.html).
    * WARNING: The `multiplatform-build-deploy` goal _does not_ include any POM publications.  We encourage inclusion of
  the default lifecycle with `skip` set to `true` so that the POM can be published normally and the environment can be
  verified.

### Command Line Utilities

Orphedomos also provides auxiliary command-line executable goals which may be helpful
for testing or CI processes.

#### Multi Platform Builds

This command leverages buildkit's multi-platform build capability to execute builds for multiple architectures 
simultaneously.  Due to a current buildkit limitation, this functionality is only usable when publishing to the
docker repository immediately.  It cannot be used to load images to your local image registry, and it demands that
your docker credentials be configured or supplied appropriately.

Example Syntax: 

*CLI:* 
```
mvn orphedomos:multiplatform-build-deploy -Dorphedomos.build.architecture=linux/arm64,linux/amd64
```

*POM:* 
```
<targetArchitectures>
  <targetArchitectures>linux/arm64</targetArchitectures>
  <targetArchitectures>linux/amd64</targetArchitectures>
</targetArchitectures>
```
#### Push Docker Image

This command leverages the Docker daemon to push a Docker image to a repository. In order to use this command, a Docker images must have already been generated by Orphedomos. 

Example Syntax: 

*CLI:* 
```
mvn orphedomos:push-docker-image -Dorphedomos.repository.url=<url/to/docker/repository> -Dorphedomos.repository.id=<repo-id> -Dorphedomos.credential.usePlainTextPassword=true
```
### Configuration Options

#### Docker Context
*Description:* Specifies the root build context directory

*CLI Option:* `orphedomos.docker.context`

*POM Option:* `<dockerContext>`

*Default:* `${project.basedir}`

#### Image Name
*Description:* Sets the version as part of the image tag.  The resulting image name and tag will be of the
form `{imageName}:{imageVersion}{tagSuffix}`.

*CLI Option:* `orphedomos.image.name`

*POM Option:* `<imageName>`

*Default:* `${project.artifactId}`

#### Image Version
*Description:* Sets the version as part of the image tag.  The resulting image name and tag will be of the 
form `{imageName}:{imageVersion}{tagSuffix}`.

*CLI Option:* `orphedomos.image.version`

*POM Option:* `<imageVersion>`

*Default:* `${project.version}`

#### Aliases
*Description:* Specifies additional tags to apply to the image.  Occurs as a supplemental action in both `build-docker-image` and `push-docker-image` phases.

*CLI Option:* `orphedomos.image.aliases`

*POM Option:* `<aliases>`

#### Plain-Text Password Opt-In

*Description:* Option to accept a plain text password.  Highly discouraged unless paired with something like Jenkins Credentials
where the password is stored in an encrypted way, but simply provided as plain-text.

*CLI Option:* `orphedomos.credential.usePlainTextPassword`

*POM Option:* `<usePlainTextPassword>`

*Default:* `false`

#### Repository ID

*Description:* Server ID holding the credentials for docker login.  Must match to a server ID in your maven settings.xml file.  Docker login will be 
attempted for any applicable goal for which this parameter is defined.  For instance, to login only during the `deploy`
phase, define this parameter specifically within the `default-push-docker-image` execution's configuration.

*CLI Option:* `orphedomos.repository.id`

*POM Option:* `<repoId>`

*Default:* `""`

#### Repository URL

*Description:* Remote repository URL for docker login and image deployment. 

(Note: The URL should not contain the transport protocol ex. http, https)

*CLI Option:* `orphedomos.repository.url`

*POM Option:* `<repoUrl>`

*Default:* `""`

#### Use BuildKit

*CLI Option:* `orphedomos.buildkit.enable`

*POM Option:* `<useBuildKit>`

*Default:* `true`

#### Dockerfile Path

*Description:* Specifies the dockerfile path relative to the build context root directory.

*CLI Option:* `orphedomos.dockerfile.path`

*POM Option:* `<dockerfilePath>`

*Default:* `"/src/main/resources/docker/Dockerfile"`

#### Target Architecture

Specifies target architectures when using the multiplatform build CLI goal.

*CLI Option:* `orphedomos.build.architecture`

*POM Option:* `<targetArchitectures><targetArchitecture> </targetArchitecture></targetArchitectures>`

*Default:* `[]`

#### Docker Build Arguments

*Description:* Specifies arguments to be passed to the dockerfile during the build process.

*CLI Option:* `orphedomos.build.args`

*POM Option:* `<buildArgs>`

*Example:*

```xml
<configuration>
    <buildArgs>
        <arg1>value1</arg1>
        <someArg>value2</someArg>
    </buildArgs>
</configuration>
```

*Default:* None

#### wheelDependencies ####

*Description:*  Optional set of wheel dependencies to retrieve from poetry cache. This allows previously cached external 
wheel dependencies to be copied into a given target directory if it exists in poetry cache. This logic 
depends on wheels to have first been cached by [`cacheWheels`](https://github.com/TechnologyBrewery/habushu?tab=readme-ov-file#install) habushu-maven-plugin configuration and executes
during the VALIDATE maven phase. Warnings will be logged if the specified wheel isn't found. 

*POM Option:* `<wheelDependencies>`

*Example:*

```xml
<configuration>
    <wheelDependencies>
        <wheelDependency>
            <artifactId>{artifact-id}</artifactId>
            <targetDirectory>{wheelOutputDirectory}</targetDirectory>
        </wheelDependency>
    </wheelDependencies>
</configuration>
```
*Default:* None

#### Docker Build Options

*Description:* Specifies additional [options](https://docs.docker.com/engine/reference/commandline/image_build/#options) to be passed to the docker CLI executor during the build process. Build options should not contain any whitespace. If whitespace is required, the option should be split into separate `<buildOption>` parameters.

*CLI Option:* `orphedomos.build.options`

*POM Option:* `<buildOptions><buildOption> </buildOption></buildOptions>`

*Example:*

```xml
<configuration>
    <buildOptions>
        <buildOption>--no-cache</buildOption>
        <buildOption>--label</buildOption>
        <buildOption>test-label=test-value</buildOption>
        <buildOption>--progress</buildOption>
        <buildOption>auto</buildOption>
        <buildOption>--output=type=docker</buildOption>
    </buildOptions>
</configuration>
```

*Default:* []


#### Tag Suffix
*Description:* Appends a suffix to the end of the tag specification.  Useful for specifying a target architecture or dev
snapshot.  The resulting image name and tag will be of the form `{imageName}:{imageVersion}{tagSuffix}`.

*CLI Option:* `orphedomos.image.tagsuffix`

*POM Option:* `<tagSuffix>`

*Default:* `""`

#### Skip All Plugin Goals

*Description:* Skips the execution of all plugin goals, effectively resulting in a no-op

*CLI Option:* `orphedomos.skip`

*POM Option:* `<skip>`

*Default:* `false`

## Building Orphedomos

If you are working on Orphedomos, please be aware of some nuances in working with a plugin that defines a custom Maven 
build lifecycle and packaging. `orphedomos-test` is used to immediately test the`orphedomos-maven-plugin` and associated 
`orphedomos` lifecycle. If the `orphedomos-maven-plugin` has not been previously built or there are un-built changes to 
the `orphedomos` lifecycle, developers must manually build the `orphedomos-maven-plugin` and then execute **another, separate** 
build of `orphedomos-test` to use the updated `orphedomos-maven-plugin` and `orphedomos` lifecycle. Developers are 
**not** able to build updates to the `orphedomos` lifecycle and test their application in `orphedomos-test` within the 
same build. That said, if developers do not update the`orphedomos` lifecycle and simply make updates to existing 
`Mojo`s defined in the `orphedomos-maven-plugin`, a single build may be used to build `orphedomos-maven-plugin` and 
apply the updates to `orphedomos-test`. To assist, there are two  profiles available in the build:

* `mvn clean install -Pbootstrap`: Builds the `orphedomos-maven-plugin` such that the custom `orphedomos` lifecycle may 
* be utilized within subsequent builds.
* `mvn clean install -Pdefault`: (ACTIVE BY DEFAULT - `-Pdefault` does not need to be specified) builds all modules. 
* Developers may use this profile to build and apply changes to existing `orphedomos-maven-plugin` `Mojo` classes


## Release Instructions
Given the nature of releasing a Maven plugin with test modules, please use the `release-orphedomos.sh` script in the
poroject root.  For example:
```shell
release-orphedomos.sh <RELEASE VERSION - e.g., 0.11.0> <NEW DEVELOPEMENT VERSION - e.g., 0.12.0-SNAPSHOT>
```