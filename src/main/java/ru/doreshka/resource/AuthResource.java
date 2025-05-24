package ru.doreshka.resource;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ru.doreshka.dto.auth.RegisterRequest;
import ru.doreshka.dto.auth.RegisterResponse;
import ru.doreshka.exceptions.ConflictException;
import ru.doreshka.service.AuthService;

import java.util.Map;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {
    @Inject
    AuthService authService;

    @POST
    @Path("/register")
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
                )
                .onFailure().recoverWithItem(ex ->
                        Response.status(Response.Status.BAD_REQUEST)
                                .entity(Map.of("error", ex.getMessage()))
                                .build()
                );
    }

    @GET
    @Path("/users")
    public Uni<Response> getUsers(){
        return authService.getUsers()
                .onItem()
                .transform(user -> Response.status(Response.Status.OK).entity(user).build());
    }

}
