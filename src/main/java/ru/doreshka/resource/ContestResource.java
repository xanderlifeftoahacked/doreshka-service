package ru.doreshka.resource;


import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ru.doreshka.dto.contest.AddContestRequest;
import ru.doreshka.dto.contest.AddProblemToContestRequest;
import ru.doreshka.exceptions.DBException;
import ru.doreshka.exceptions.LoginException;
import ru.doreshka.service.ContestService;
import ru.doreshka.service.ProblemService;

import java.util.Map;

@Path("/api/contest")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContestResource {
    @Inject
    ContestService contestService;

    @Inject
    ProblemService problemService;

    @POST
    @Path("/new")
    @RolesAllowed({"admin"})
    public Uni<Response> addContest(@Valid AddContestRequest request) {
        return contestService.createContest(request)
                .onItem().transform(contest ->
                        Response.status(Response.Status.OK)
                                .entity(contest)
                                .build()
                )
                .onFailure(LoginException.class).recoverWithItem(ex ->
                        Response.status(Response.Status.FORBIDDEN)
                                .entity(Map.of("error", ex.getMessage()))
                                .build()
                );
    }

    @GET
    @Path("/all")
    @RolesAllowed({"admin"})
    public Uni<Response> getAll() {
        return contestService.getContests()
                .onItem().transform(contest ->
                        Response.status(Response.Status.OK)
                                .entity(contest)
                                .build()
                )
                .onFailure(LoginException.class).recoverWithItem(ex ->
                        Response.status(Response.Status.FORBIDDEN)
                                .entity(Map.of("error", ex.getMessage()))
                                .build()
                );
    }

    @POST
    @Path("/grantaccess/{user_id:\\d+}/{contest_id:\\d+}")
    @RolesAllowed({"admin"})
    public Uni<Response> grantAccess(
            @PathParam("user_id") Long userId,
            @PathParam("contest_id") Long contestId) {
        return contestService.grantAccess(userId, contestId)
                .onItem().transform(access ->
                        Response.ok(access).build()
                )
                .onFailure(DBException.class).recoverWithItem(ex ->
                        Response.status(Response.Status.FORBIDDEN)
                                .entity(Map.of("error", ex.getMessage()))
                                .build()
                );
    }

    @GET
    @Path("/my")
    @RolesAllowed({"user"})
    public Uni<Response> getMyContests(@Context SecurityIdentity securityIdentity) {
        return contestService.getAviableContests(Long.valueOf(securityIdentity.getPrincipal().getName()))
                .onItem().transform(contests ->
                        Response.ok(contests).build());
    }

    @GET
    @Path("/{contestId}/problems_admin")
    @RolesAllowed({"admin"})
    public Uni<Response> getProblemsAdmin(
            @PathParam("contestId") Long contestId) {

        return contestService.getProblems(contestId)
                .onItem().transform(problems ->
                        Response.status(Response.Status.OK).entity(problems).build()
                );

    }

    @GET
    @Path("/{contestId}/problems")
    @RolesAllowed({"user"})
    public Uni<Response> getProblems(@Context SecurityIdentity securityIdentity,
                                     @PathParam("contestId") Long contestId) {
        Long userId = Long.valueOf(securityIdentity.getPrincipal().getName());

        return contestService.checkUserAccessToContest(userId, contestId)
                .onItem().transformToUni(access -> {
                    if (access == null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.FORBIDDEN)
                                        .entity(Map.of("error", "no access to contest"))
                                        .build()
                        );
                    }
                    return contestService.getProblems(contestId)
                            .onItem().transform(problems ->
                                    Response.status(Response.Status.OK).entity(problems).build()
                            );
                })
                .onFailure().recoverWithItem(failure ->
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity(Map.of("error", "Internal server error: " + failure.getMessage()))
                                .build()
                );
    }

    @POST
    @Path("/{contestId}/problems")
    public Uni<Response> addProblemToContest(
            @PathParam("contestId") Long contestId,
            @Valid AddProblemToContestRequest request) {

        return contestService.addProblemToContest(contestId, request)
                .onItem().transform(item ->
                        Response.status(Response.Status.CREATED)
                                .entity(item)
                                .build()
                )
                .onFailure(IllegalArgumentException.class).recoverWithItem(e ->
                        Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of("error", e.getMessage()))
                                .build()
                )
                .onFailure(IllegalStateException.class).recoverWithItem(e ->
                        Response.status(Response.Status.CONFLICT)
                                .entity(Map.of("error", e.getMessage()))
                                .build()
                )
                .onFailure().recoverWithItem(e ->
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity(Map.of("error", e.getMessage()))
                                .build()
                );
    }
}
