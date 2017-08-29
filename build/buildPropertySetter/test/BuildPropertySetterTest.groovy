import spock.lang.Specification
import static org.jfrog.artifactory.client.ArtifactoryClient.create
import org.jfrog.artifactory.client.model.repository.settings.impl.MavenRepositorySettingsImpl
import groovy.json.JsonSlurper
import org.jfrog.artifactory.client.ArtifactoryRequest
import org.jfrog.artifactory.client.impl.ArtifactoryRequestImpl
import groovy.json.*

class BuildPropertySetterTest extends Specification {
    def 'buildPropertySetterTest'() {

        setup:
        def baseurl = 'http://localhost:8088/artifactory'
        def artifactory = create(baseurl, 'admin', 'password')


        def builder = artifactory.repositories().builders()
        def local = builder.localRepositoryBuilder().key('libs-snapshot-local')
            .repositorySettings(new MavenRepositorySettingsImpl()).build()
            artifactory.repositories().create(0, local)



        def pom = new File('./src/test/groovy/BuildPropertySetterTest/pom.xml')
        def pom2 = new File('./src/test/groovy/BuildPropertySetterTest/pom2.xml')
        def path = "group/artifact/1.0-SNAPSHOT/artifact-1.0-20170828.232033-1.pom"
        def path2 = "group/artifact/2.0-SNAPSHOT/artifact-1.0-20170828.232043-2.pom"
        

        when:
        artifactory.repository("libs-snapshot-local")
            .upload(path,pom)
            .withProperty("build.name","unit-test")
            .withProperty("build.number","20")
            .doUpload(); 

        artifactory.repository("libs-snapshot-local")
            .upload(path2,pom2)
            .withProperty("build.name","unit-test")
            .withProperty("build.number","21")
            .doUpload(); 


        def file = new File('./src/test/groovy/BuildPropertySetterTest/build.json')
        def file2 = new File('./src/test/groovy/BuildPropertySetterTest/build2.json')

        ArtifactoryRequest uploadBuild = new ArtifactoryRequestImpl().apiUrl("api/build")
          .method(ArtifactoryRequest.Method.PUT)
          .requestType(ArtifactoryRequest.ContentType.JSON)
          .requestBody(new JsonSlurper().parse(file))
        artifactory.restCall(uploadBuild)

        ArtifactoryRequest uploadBuild2 = new ArtifactoryRequestImpl().apiUrl("api/build")
          .method(ArtifactoryRequest.Method.PUT)
          .requestType(ArtifactoryRequest.ContentType.JSON)
          .requestBody(new JsonSlurper().parse(file2))
        artifactory.restCall(uploadBuild2)

     
        then:
        def check = artifactory.searches()
            .repositories("libs-snapshot-local")  
            .itemsByProperty()
            .property("latest","true")
            .property("build.name","unit-test")
            .property("build.number","21")
            .doSearch(); 

        cleanup:
        artifactory.repository("libs-snapshot-local").delete()
        ArtifactoryRequest delete = new ArtifactoryRequestImpl().apiUrl("api/build/unit-test")
        .setQueryParams(deleteAll: 1)
        .method(ArtifactoryRequest.Method.DELETE)
        artifactory.restCall(delete)

    }
}
