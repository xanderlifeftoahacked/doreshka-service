package ru.doreshka.resource;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import ru.doreshka.domain.entity.Problem;
import ru.doreshka.domain.entity.Submission;
import ru.doreshka.domain.entity.User;
import ru.doreshka.exceptions.DBException;
import ru.doreshka.service.SubmissionService;

import java.io.IOException;
import java.nio.file.Files;

@Path("/sumbissions")
public class SubmissionResource {

    @Inject
    SubmissionService submissionService;

    @GET
    @Path("/my")
    @RolesAllowed({"user"})
    public Uni<Response> getMySubmissions(@Context SecurityContext securityContext) {
        Long id = Long.valueOf(securityContext.getUserPrincipal().getName());

        return User.<User>findById(id)
                .onItem().ifNotNull().transformToUni(user ->
                        Submission.find("user.id = ?1", id).list()
                                .onItem().transform(submissions ->
                                        Response.ok(submissions).build()
                                )
                )
                .onItem().ifNull().continueWith(() ->
                        Response.status(Response.Status.UNAUTHORIZED).build()
                );
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/{problemId}")
    @RolesAllowed({"user"})
    public Uni<Response> submit(@RestForm FileUpload data,
                                @PathParam("problemId") Long problemId,
                                @Context SecurityContext securityContext) {
        Uni<User> curUser = User.findById(Long.valueOf(securityContext.getUserPrincipal().getName()));

        if (curUser == null) {
            return Uni.createFrom().item(Response.status(Response.Status.UNAUTHORIZED).build());
        }

        return curUser.onItem()
                .transformToUni(user -> {
                    byte[] fileContent;

                    try {
                        fileContent = Files.readAllBytes(data.filePath());
                    } catch (IOException e) {
                        return Uni.createFrom().item(
                                Response.serverError()
                                        .entity("Failed to read file")
                                        .build()
                        );
                    }

                    return Problem.<Problem>findById(problemId)
                            .onItem().ifNull().failWith(
                                    new DBException("Problem not found")
                            )
                            .onItem().transformToUni(problem ->
                                    submissionService.createSubmission(problem, user, fileContent))
                            .onItem().transform(submission ->
                                    Response.status(Response.Status.CREATED)
                                            .entity(submission)
                                            .build()
                            ).onFailure().recoverWithItem(Response.ok().build());
                });
    }
}
