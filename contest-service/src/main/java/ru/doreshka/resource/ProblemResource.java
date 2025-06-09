package ru.doreshka.resource;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import ru.doreshka.dto.problem.AddProblemRequest;
import ru.doreshka.exceptions.DBException;
import ru.doreshka.service.ProblemService;
import ru.doreshka.service.TestFileService;

import java.util.List;
import java.util.Map;

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
    @RolesAllowed({"admin"})
    public Uni<Response> addProblem(@Valid AddProblemRequest request) {
        return problemService.createProblem(request)
                .onItem().transform(problem -> Response.ok(problem).build())
                .onFailure(DBException.class).recoverWithItem(ex ->
                        Response.status(Response.Status.CONFLICT)
                                .entity(Map.of("error", ex.getMessage()))
                                .build()
                );
    }

    @GET
    @Path("/{problemId}")
    @RolesAllowed({"admin"})
    public Uni<Response> getProblem(@PathParam("problemId") Long problemId) {
        return problemService.getProblem(problemId)
                .onItem().transform(problem -> Response.ok(problem).build())
                .onFailure(DBException.class).recoverWithItem(ex ->
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity(Map.of("error", ex.getMessage()))
                                .build()
                );
    }

    @POST
    @Path("/{problemId}/tests")
    @RolesAllowed({"admin"})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Uni<Response> addTests(
            @PathParam("problemId") Long problemId,
            @RestForm List<FileUpload> files
    ) {
        return testFileService.processProblemTests(problemId, files)
                .onItem().transform(tests -> Response.ok(tests).build());
    }

    public static class TestFileUploadForm {
        @RestForm("files")
        public List<FileUpload> file;
    }
}
