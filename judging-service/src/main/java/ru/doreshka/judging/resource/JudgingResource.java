package ru.doreshka.judging.resource;

import io.quarkus.logging.Log;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import ru.doreshka.judging.dto.JudgeRequest;
import ru.doreshka.judging.entity.Problem;
import ru.doreshka.judging.entity.Submission;
import ru.doreshka.judging.entity.Verdict;
import ru.doreshka.judging.service.JudgingService;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

@Path("/api/judge")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Judging Service", description = "Solution judging and submission management")
public class JudgingResource {

    @Inject
    JudgingService judgingService;

    @POST
    @Path("/submit/{contestId}/{problemId}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed({"user"})
    @Transactional
    public Response submitSolution(@PathParam("contestId") Long contestId,
                                   @PathParam("problemId") Long problemId,
                                   @RestForm("codeFile") FileUpload upload,
                                   @Context SecurityContext securityContext) {
        try {
            Long userId = Long.valueOf(securityContext.getUserPrincipal().getName());
            Log.infof("Received solution submission for problem %d in contest %d from user %d", problemId, contestId, userId);

            Problem problem = Problem.findById(problemId);
            if (problem == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Problem not found"))
                        .build();
            }

            Submission submission = new Submission();
            submission.problemId = problemId;
            submission.contestId = contestId;
            submission.userId = userId;
            submission.timeLimit = problem.getTimeLimit();
            submission.memoryLimit = problem.getMemoryLimit();
            submission.verdict = Verdict.Pending;

            submission.persist();

            String sourceCode = Files.readString(upload.uploadedFile());

            String sourcePath = judgingService.saveSolutionFile(submission, sourceCode);
            submission.sourcePath = sourcePath;
            submission.persist();

            judgingService.judgeAsync(new JudgeRequest(
                    submission.id,
                    problemId,
                    sourcePath,
                    problem.getTimeLimit(),
                    problem.getMemoryLimit()
            ));

            return Response.ok(Map.of(
                    "submissionId", submission.id,
                    "verdict", "Pending",
                    "message", "Solution submitted successfully. Check status using /api/judge/submission/" + submission.id
            )).build();

        } catch (IOException e) {
            Log.errorf(e, "Failed to read uploaded file");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Failed to read uploaded file: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to process submission");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/submissions/my")
    @RolesAllowed({"user"})
    public Response getMySubmissions(@Context SecurityContext securityContext) {
        try {
            Long userId = Long.valueOf(securityContext.getUserPrincipal().getName());
            List<Submission> submissions = Submission.find("userId", userId).list();
            return Response.ok(submissions).build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to get submissions for authenticated user");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/submissions/my/contest/{contestId}")
    @RolesAllowed({"user"})
    public Response getMySubmissionsForContest(@PathParam("contestId") Long contestId,
                                               @Context SecurityContext securityContext) {
        try {
            Long userId = Long.valueOf(securityContext.getUserPrincipal().getName());
            List<Submission> submissions = Submission.find("userId = ?1 and contestId = ?2", userId, contestId).list();
            return Response.ok(submissions).build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to get submissions for authenticated user in contest %d", contestId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/submissions/{userId}")
    @RolesAllowed({"admin"})
    public Response getUserSubmissions(@PathParam("userId") Long userId) {
        try {
            List<Submission> submissions = Submission.find("userId", userId).list();
            return Response.ok(submissions).build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to get submissions for user %d", userId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/submission/{submissionId}")
    @RolesAllowed({"user"})
    public Response getSubmission(@PathParam("submissionId") Long submissionId,
                                  @Context SecurityContext securityContext) {
        try {
            Long userId = Long.valueOf(securityContext.getUserPrincipal().getName());
            Submission submission = Submission.findById(submissionId);

            if (submission == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Submission not found"))
                        .build();
            }

            if (!submission.userId.equals(userId) && !securityContext.isUserInRole("admin")) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(Map.of("error", "Access denied"))
                        .build();
            }

            return Response.ok(submission).build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to get submission %d", submissionId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/ping")
    public Response health() {
        return Response.ok(Map.of("status", "OK", "service", "judging-service")).build();
    }
} 