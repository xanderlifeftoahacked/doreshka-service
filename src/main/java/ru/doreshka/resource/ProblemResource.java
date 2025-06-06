package ru.doreshka.resource;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import ru.doreshka.domain.entity.Problem;
import ru.doreshka.domain.entity.ProblemTest;
import ru.doreshka.dto.problem.AddProblemRequest;
import ru.doreshka.service.ProblemService;
import ru.doreshka.service.TestFileService;

import java.util.List;

@Path("/api/problems")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProblemResource {
    @Inject
    TestFileService testFileService;

    @Inject
    ProblemService problemService;

    @POST
    @Path("/add")
    public Uni<Response> addProblem(@Valid AddProblemRequest request){
        return problemService.createProblem(request).onItem().transform(
                problem -> Response.ok(problem).build()
        );
    }

    @GET
    @Path("/{problemId}")
    public Uni<Response> getProblem(@PathParam("problemId") Long problemId){
        return problemService.getProblem(problemId)
                .onItem().transform(problem -> Response.ok(problem).build());
    }

    @POST
    @Path("/{problemId}/tests")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Uni<Response> addTests(
            @PathParam("problemId") Long problemId,
            @RestForm List<FileUpload> files
    ) {
        return testFileService.processProblemTests(problemId, files)
                .onItem().transform(tests -> Response.ok(tests).build())
                .onFailure().recoverWithItem(throwable ->
                        Response.status(Response.Status.BAD_REQUEST)
                                .entity(throwable.getMessage())
                                .build()
                );
    }

    public static class TestFileUploadForm {
        @RestForm("files")
        public List<FileUpload> file;
    }
}
