package org.technologybrewery.orphedomos.mojo;

import org.technologybrewery.orphedomos.util.exec.DockerDeployExecutor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Mojo(name = "push-all-docker-images", aggregator = true)
public class DockerDeployAllMojo extends AbstractDockerMojo {

    private static final Logger logger = LoggerFactory.getLogger(DockerDeployAllMojo.class);

    @Override
    public void doExecute() throws MojoExecutionException {
        login();
        List<MavenProject> projects = mavenSession.getProjects();
        for(MavenProject project : projects) {
            getDockerDeployInfo(project, repoUrl).ifPresent((dockerDeployInfo) -> {
                DockerDeployExecutor executor = new DockerDeployExecutor(project.getBasedir());
                try {
                    executor.deploy(dockerDeployInfo, aliases, dryRun);
                } catch(Exception e) {
                    logger.warn(e.getMessage());
                }
            });
        }
        logout();
    }

}
