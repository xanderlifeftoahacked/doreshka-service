package ru.doreshka.resource;

import io.quarkus.security.PermissionsAllowed;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ru.doreshka.domain.entity.User;
import ru.doreshka.dto.auth.LoginRequest;
import ru.doreshka.dto.auth.LoginResponse;
import ru.doreshka.dto.auth.RegisterRequest;
import ru.doreshka.dto.auth.RegisterResponse;
import ru.doreshka.exceptions.ConflictException;
import ru.doreshka.exceptions.LoginException;
import ru.doreshka.exceptions.WrongPasswordException;
import ru.doreshka.service.AuthService;

import java.time.Duration;
import java.util.Map;
import jakarta.annotation.security.RolesAllowed;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {
    @Inject
    AuthService authService;

    @POST
    @Path("/login")
    public Uni<Response> loginUser(@Valid LoginRequest request) {
        return authService.loginUser(request)
                .onItem().transform(token ->
                        Response.status(Response.Status.OK)
                                .entity(new LoginResponse(token))
                                .build()
                )
                .onFailure(LoginException.class).recoverWithItem(ex ->
                                Response.status(Response.Status.FORBIDDEN)
                                        .entity(Map.of("error", ex.getMessage()))
                                        .build()
               );
    }

    @POST
    @Path("/register")
    @RolesAllowed("admin")
    public Uni<Response> registerUser(@Valid RegisterRequest request) {
        return authService.registerUser(request)
                .onItem().transform(user ->
                        Response.status(Response.Status.CREATED)
                                .entity(RegisterResponse.fromUser(user))
                                .build()
                )
                .onFailure(ConflictException.class).recoverWithItem(ex ->
                        Response.status(Response.Status.CONFLICT)
                                .entity(Map.of("error", ex.getMessage()))
                                .build()
                );
    }

    @GET
    @Path("/users")
    @RolesAllowed("admin")
    public Uni<Response> getUsers(){
        return authService.getUsers()
                .onItem()
                .transform(user -> Response.status(Response.Status.OK).entity(user).build());
    }

}
