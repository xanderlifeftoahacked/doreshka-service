package ru.doreshka.resource;

import io.quarkus.test.TestReactiveTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.doreshka.domain.repository.UserRepository;
import ru.doreshka.dto.auth.LoginRequest;
import ru.doreshka.dto.auth.RegisterRequest;
import ru.doreshka.service.AuthService;
import io.quarkus.test.security.TestSecurity;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestSecurity(user = "testuser", roles = {"admin", "user"})
public class AuthTest {

    @Inject
    AuthService authService;

    @Inject
    UserRepository userRepository;

    @BeforeEach
    @RunOnVertxContext
    public void clearDatabase(UniAsserter asserter) {
        asserter.execute(() -> userRepository.clear());
    }

    @Test
    @RunOnVertxContext
    public void testLoginSuccess(UniAsserter asserter) {
        asserter.execute(() ->
                authService.registerUser(new RegisterRequest("testuser", "password"))
        );

        asserter.execute(() -> {
            LoginRequest request = new LoginRequest("testuser", "password");

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/auth/login")
                    .then()
                    .statusCode(Response.Status.OK.getStatusCode())
                    .body("token", notNullValue());
        });
    }

    @Test
    @RunOnVertxContext
    public void testLoginWrongPassword(UniAsserter asserter) {
        asserter.execute(() ->
                authService.registerUser(new RegisterRequest("testuser", "password"))
        );

        asserter.execute(() -> {
            LoginRequest request = new LoginRequest("testuser", "wrong-password");

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/auth/login")
                    .then()
                    .statusCode(Response.Status.FORBIDDEN.getStatusCode())
                    .body("error", equalTo("Invalid password"));
        });
    }

    @Test
    @RunOnVertxContext
    public void testLoginUserNotFound(UniAsserter asserter) {
        asserter.execute(() -> {
            LoginRequest request = new LoginRequest("unknown", "pass");

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/auth/login")
                    .then()
                    .statusCode(Response.Status.FORBIDDEN.getStatusCode())
                    .body("error", equalTo("user doesn't exist"));
        });
    }

    @Test
    @RunOnVertxContext
    public void testRegisterSuccess(UniAsserter asserter) {
        asserter.execute(() -> {
            RegisterRequest request = new RegisterRequest("newuser", "password");

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/auth/register")
                    .then()
                    .statusCode(Response.Status.CREATED.getStatusCode())
                    .body("username", equalTo("newuser"))
                    .body("id", notNullValue());
        });
    }

    @Test
    @RunOnVertxContext
    public void testRegisterConflict(UniAsserter asserter) {
        asserter.execute(() ->
                authService.registerUser(new RegisterRequest("existing", "password"))
        );

        asserter.execute(() -> {
            RegisterRequest request = new RegisterRequest("existing", "pass");

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/auth/register")
                    .then()
                    .statusCode(Response.Status.CONFLICT.getStatusCode())
                    .body("error", equalTo("User already exists"));
        });
    }

    @Test
    @RunOnVertxContext
    public void testGetUsersSuccess(UniAsserter asserter) {
        asserter.execute(() ->
                authService.registerUser(new RegisterRequest("user1", "pass1"))
        );
        asserter.execute(() ->
                authService.registerUser(new RegisterRequest("user2", "pass2"))
        );

        asserter.execute(() -> {
            given()
                    .when()
                    .get("/api/auth/users")
                    .then()
                    .statusCode(Response.Status.OK.getStatusCode())
                    .body("size()", is(2),
                            "[0].username", equalTo("user1"),
                            "[1].username", equalTo("user2"));
        });
    }

    @Test
    @RunOnVertxContext
    public void testGetUsersEmpty(UniAsserter asserter) {
        asserter.execute(() -> {
            given()
                    .when()
                    .get("/api/auth/users")
                    .then()
                    .statusCode(Response.Status.OK.getStatusCode())
                    .body("", empty());
        });
    }
}