package ru.doreshka.exception_mappers;

import io.quarkus.logging.Log;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import ru.doreshka.exceptions.DBException;

import java.util.Map;

@Provider
public class DBExceptionMapper implements ExceptionMapper<DBException> {
    @Override
    public Response toResponse(DBException ex) {
        Log.error(ex.getMessage());
        return Response.status(Response.Status.FORBIDDEN)
                .entity(Map.of("error", ex.getMessage()))
                .build();
    }
}
