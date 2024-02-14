package org.technologybrewery.orphedomos.util.model;

public class DockerDeployInfo {

    private String dockerImageTag;
    private String dockerDeployImageTag;

    public DockerDeployInfo(String dockerImageTag, String dockerDeployImageTag) {
        this.dockerImageTag = dockerImageTag;
        this.dockerDeployImageTag = dockerDeployImageTag;
    }

    public String getDockerImageTag() {
        return this.dockerImageTag;
    }

    public String getDockerDeployImageTag() {
        return this.dockerDeployImageTag;
    }
    
}
