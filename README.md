# quarkus-lra

- https://github.com/quarkusio/quarkusio.github.io/blob/d2cb1bc3c2b8f78c3b1a02fda1f6a44e6a836fca/_posts/2021-08-23-using-lra.adoc
- https://download.eclipse.org/microprofile/microprofile-lra-1.0-M1/microprofile-lra-spec.html

Bootstrap a lra-coordinator
```bash
mkdir ~/git/quarkus-lra && cd quarkus-lra

mvn io.quarkus:quarkus-maven-plugin:2.2.0.Final:create \
      -DprojectGroupId=org.acme \
      -DprojectArtifactId=narayana-lra-coordinator \
      -Dextensions="resteasy-jackson,rest-client"

cd narayana-lra-coordinator/
rm -rf src
```

Add dependency to pom.xml
```bash
    <dependency>
      <groupId>org.jboss.narayana.rts</groupId>
      <artifactId>lra-coordinator-jar</artifactId>
      <version>5.12.0.Final</version>
    </dependency>
```

Build and run it
```bash
mvn clean package
java -Dquarkus.http.port=50000 -jar target/quarkus-app/quarkus-run.jar &
```

Test
```bash
curl -s http://localhost:50000/lra-coordinator | jq .
```

Bootstrap our lra project
```bash
quarkus create app --maven --java --no-wrapper -x quarkus-narayana-lra org.acme:lra-quickstart:1.0.0-SNAPSHOT
```

Add this class
```java
package org.acme;

import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_CONTEXT_HEADER;

@Path("/hello")
public class GreetingResource {


    @GET
    @LRA // Step 2b: The method should run within an LRA
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId /* Step 2c the context is useful for associating compensation logic */) {
        System.out.printf("hello with context %s%n", lraId);
        return "Hello RESTEasy";
    }

    // Step 2d: There must be a method to compensate for the action if it's cancelled
    @PUT
    @Path("compensate")
    @Compensate
    public Response compensateWork(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        System.out.printf("compensating %s%n", lraId);
        return Response.ok(lraId.toASCIIString()).build();
    }

    // Step 2e: An optional callback notifying that the LRA is closing
    @PUT
    @Path("complete")
    @Complete
    public Response completeWork(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        System.out.printf("completing %s%n", lraId);
        return Response.ok(lraId.toASCIIString()).build();
    }

    @GET
    @Path("/start")
    @LRA(end = false) // Step 3a: The method should run within an LRA
    @Produces(MediaType.TEXT_PLAIN)
    public String start(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        System.out.printf("hello with context %s%n", lraId);
        return lraId.toASCIIString();
    }

    @GET
    @Path("/end")
    @LRA(value = LRA.Type.MANDATORY) // Step 3a: The method MUST be invoked with an LRA
    @Produces(MediaType.TEXT_PLAIN)
    public String end(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return lraId.toASCIIString();
    }
}
```

Test
```bash
LRA_URL=$(curl -s http://localhost:8080/hello/start)
curl --header "Long-Running-Action: $LRA_URL" http://localhost:8080/hello
```

Run two different instances
```bash
java -Dquarkus.http.port=8080 -jar target/quarkus-app/quarkus-run.jar &
java -Dquarkus.http.port=8081 -jar target/quarkus-app/quarkus-run.jar &
```

Start LRA in one process, end in the other
```
LRA_URL=$(curl -s http://localhost:8080/hello/start)
curl --header "Long-Running-Action: $LRA_URL" http://localhost:8081/hello
```

Natively
```bash
mvn package -DskipTests -Pnative -Dquarkus.native.additional-build-args="-J-Dorg.graalvm.version=21.0.0.0-Final -J-Xmx4G"

./target/lra-quickstart-1.0.0-SNAPSHOT-runner -Dquarkus.lra.coordinator-url=http://localhost:50000/lra-coordinator -Dquarkus.http.port=8080 &
./target/lra-quickstart-1.0.0-SNAPSHOT-runner -Dquarkus.lra.coordinator-url=http://localhost:50000/lra-coordinator -Dquarkus.http.port=8081 &
```

Test recovery
```bash
LRA_URL=$(curl -s http://localhost:8080/hello/start)
kill 110270 # first one
curl -s --header "Long-Running-Action: $LRA_URL" http://localhost:8081/hello
# completing still, can recover
curl -s http://localhost:50000/lra-coordinator | jq .
curl http://localhost:50000/lra-coordinator/recovery
# will only complete when restart this
./target/lra-quickstart-1.0.0-SNAPSHOT-runner -Dquarkus.lra.coordinator-url=http://localhost:50000/lra-coordinator -Dquarkus.http.port=8080 &
```

Embedding LRA
```bash
    <dependency>
      <groupId>org.jboss.narayana.rts</groupId>
      <artifactId>lra-coordinator-jar</artifactId>
      <version>5.12.0.Final</version>
    </dependency>
```

`application.properties`
```
quarkus.resteasy.ignore-application-classes=true
quarkus.http.port=8080
quarkus.lra.coordinator-url=http://localhost:8080/lra-coordinator
```

Configure storage
```bash
java -DObjectStoreEnvironmentBean.objectStoreDir=target/lra-logs -jar target/quarkus-app/quarkus-run.jar &
```
