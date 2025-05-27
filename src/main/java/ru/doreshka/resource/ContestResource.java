package ru.doreshka.resource;


import io.quarkus.logging.Log;
import io.quarkus.security.Authenticated;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ru.doreshka.domain.entity.User;
import ru.doreshka.dto.auth.LoginRequest;
import ru.doreshka.dto.auth.LoginResponse;
import ru.doreshka.dto.auth.RegisterRequest;
import ru.doreshka.dto.auth.RegisterResponse;
import ru.doreshka.dto.contest.AddContestRequest;
import ru.doreshka.exceptions.ConflictException;
import ru.doreshka.exceptions.DBException;
import ru.doreshka.exceptions.LoginException;
import ru.doreshka.exceptions.WrongPasswordException;
import ru.doreshka.service.AuthService;

import java.time.Duration;
import java.util.Map;
import jakarta.annotation.security.RolesAllowed;
import ru.doreshka.service.ContestService;

@Path("/api/contest")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContestResource {
    @Inject
    ContestService contestService;

    @POST
    @Path("/new")
    @RolesAllowed({"admin"})
    public Uni<Response> addContest(AddContestRequest request) {
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
    public Uni<Response> getMyContests(@Context SecurityIdentity securityIdentity){
        return contestService.getAviableContests(Long.valueOf(securityIdentity.getPrincipal().getName()))
                .onItem().transform(contests ->
                        Response.ok(contests).build());
    }
}
