Feature: Deploy an entire project's Docker artifacts to an alternate repository

    Scenario: Get Docker Deploy Information from a project using property dockerImageName
        Given a project test-project using Orphedomos and using dockerImageName as the image name
        When the project Docker artifact deployment is triggered 
        Then Docker deploy information is obtained from the project

    Scenario: Get Docker Deploy Information from a project using the artifact ID
        Given a project test-project using Orphedomos and using artifactID as the image name
        When the project Docker artifact deployment is triggered 
        Then Docker deploy information is obtained from the project