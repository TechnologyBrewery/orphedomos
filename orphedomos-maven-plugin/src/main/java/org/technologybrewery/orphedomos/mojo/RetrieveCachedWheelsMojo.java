package org.technologybrewery.orphedomos.mojo;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.technologybrewery.habushu.RetrieveWheelsMojo;

/**
 * Helper mojo that handles the retrieving of wheel artifacts from poetry
 * cache by artifactId and into a given targetDirectory during the 
 * {@link LifecyclePhase#VALIDATE} build phase. 
 *
 * @param wheelDependencies A List of Wheel Dependencies which will identify wheel 
 *                          files by {@WheelDependency.artifactId} in poetry cache and place them into 
 *                          a given {@WheelDependency.targetDirectory}. This logic specifically targets
 *                          wheel artifacts cached by the {@param cacheWheels} parameter and REQUIRES 
 *                          the requested wheel to have first been cached prior to setting this config
 * @throws HabushuException
 */
@Mojo(name = "retrieve-wheels", defaultPhase = LifecyclePhase.VALIDATE)
public class RetrieveCachedWheelsMojo extends RetrieveWheelsMojo {
    
}
