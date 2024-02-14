package org.technologybrewery.orphedomos.mojo;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import java.util.Properties;

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.technologybrewery.orphedomos.util.model.DockerDeployInfo;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class DockerDeploySteps {

    private MavenProject project;
    private Optional<DockerDeployInfo> deployInfoOpt;
    private String repoUrl = "localhost:8080";

    @Given("a project {word} using Orphedomos and using {word} as the image name")
    public void a_project_test_project_using_orphedomos(String projectName, String nameSource) throws Exception {
        boolean isDockerImageName = false;
        if(nameSource.equals("dockerImageName")) {
            isDockerImageName = true;
        }
        project = createOrphedomosProject(projectName, isDockerImageName);
    }

    @When("the project Docker artifact deployment is triggered")
    public void the_project_docker_artifact_deployment_is_triggered() throws Exception {
        deployInfoOpt = AbstractDockerMojo.getDockerDeployInfo(project, repoUrl);
    }

    @Then("Docker deploy information is obtained from the project")
    public void docker_deploy_information_is_obtained_from_the_project() {
        DockerDeployInfo deployInfo = deployInfoOpt.get();
        assertNotNull(deployInfo);
        Properties properties = project.getProperties();
        String dockerImageName = properties.getProperty("dockerImageName");
        String imageName = dockerImageName;
        if(dockerImageName == null) {
            imageName = project.getArtifactId();
        }
        String imageTag = AbstractDockerMojo.getImageTag(imageName, project.getVersion());
        assertTrue(deployInfo.getDockerImageTag().equals(imageTag));
        String repoImageTag = AbstractDockerMojo.prependRegistry(repoUrl, imageTag);
        assertTrue(deployInfo.getDockerDeployImageTag().equals(repoImageTag));
    }

    private MavenProject createOrphedomosProject(String projectName, boolean usingDockerImageName) throws Exception {
        Model pomModel = new Model();
        pomModel.setGroupId("com.example");
        pomModel.setArtifactId(projectName);
        pomModel.setPackaging("orphedomos");
        pomModel.setVersion("1.0.0");
        pomModel.setModelVersion("4.0.0");
        Properties properties = new Properties();
        if(usingDockerImageName) {
            properties.setProperty("dockerImageName", String.format("boozallen/%s", projectName));
        }
        pomModel.setProperties(properties);
        return new MavenProject(pomModel);
    }
}
