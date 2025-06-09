package ru.doreshka.resource;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import ru.doreshka.domain.entity.User;
import ru.doreshka.domain.repository.ProblemRepository;
import ru.doreshka.domain.repository.UserRepository;
import ru.doreshka.domain.repository.ContestRepository;
import ru.doreshka.domain.repository.ContestProblemRepository;
import ru.doreshka.domain.entity.UserContestAccess;
import ru.doreshka.dto.problem.AddProblemRequest;
import ru.doreshka.service.ProblemService;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class ProblemResourceTest {

    @Inject
    ProblemService problemService;

    @Inject
    UserRepository userRepository;
    
    @Inject
    ProblemRepository problemRepository;

    @Inject
    ContestRepository contestRepository;

    @Inject
    ContestProblemRepository contestProblemRepository;

    @BeforeEach
    @RunOnVertxContext  
    public void clearDatabase(UniAsserter asserter) {
        asserter.execute(() -> contestProblemRepository.clear());
        asserter.execute(() -> Panache.withTransaction(() -> UserContestAccess.deleteAll()));
        asserter.execute(() -> userRepository.clear());
        asserter.execute(() -> contestRepository.clear());
        asserter.execute(() -> problemRepository.clear());
    }

    private Uni<User> createUser(Long id, String username) {
        return Panache.withTransaction(() -> {
            User user = new User();
            user.id = id;
            user.setUsername(username);
            user.setPasswordHash("password");
            return userRepository.persistAndFlush(user);
        });
    }

    @Test
    @TestSecurity(user = "1", roles = {"admin"})
    @RunOnVertxContext
    public void testAddProblemSuccess(UniAsserter asserter) {
        asserter.execute(() -> {
            AddProblemRequest request = new AddProblemRequest();
            request.setTitle("Test Problem");
            request.setDescription("Problem description");
            request.setTimeLimit(1000);
            request.setMemoryLimit(256);

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/problems/add")
                    .then()
                    .statusCode(Response.Status.OK.getStatusCode());
        });
    }

    @Test
    @TestSecurity(user = "2", roles = {"user"})
    @RunOnVertxContext
    public void testAddProblemForbidden(UniAsserter asserter) {
        asserter.execute(() -> {
            AddProblemRequest request = new AddProblemRequest();
            request.setTitle("Test Problem");
            request.setDescription("Problem description");
            request.setTimeLimit(1000);
            request.setMemoryLimit(256);

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/problems/add")
                    .then()
                    .statusCode(Response.Status.FORBIDDEN.getStatusCode());
        });
    }

    @Test
    @TestSecurity(user = "5", roles = {"user"})
    @RunOnVertxContext
    public void testGetProblemForbidden(UniAsserter asserter) {
        asserter.execute(() -> {
            given()
                    .when()
                    .get("/api/problems/1")
                    .then()
                    .statusCode(Response.Status.FORBIDDEN.getStatusCode());
        });
    }

    @Test
    @TestSecurity(user = "6", roles = {"admin"})
    @RunOnVertxContext
    public void testAddProblemWithInvalidData(UniAsserter asserter) {
        asserter.execute(() -> {
            AddProblemRequest request = new AddProblemRequest();
            request.setTitle("");
            request.setDescription("Test description");
            request.setTimeLimit(1000);
            request.setMemoryLimit(256);

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/problems/add")
                    .then()
                    .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
        });
    }

    @Test
    @TestSecurity(user = "7", roles = {"admin"})
    @RunOnVertxContext
    public void testAddProblemWithNegativeLimits(UniAsserter asserter) {
        asserter.execute(() -> {
            AddProblemRequest request = new AddProblemRequest();
            request.setTitle("Test Problem");
            request.setDescription("Test description");
            request.setTimeLimit(-1000);
            request.setMemoryLimit(256);

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/problems/add")
                    .then()
                    .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
        });
    }

    @Test
    @TestSecurity(user = "10", roles = {"admin"})
    @RunOnVertxContext
    public void testAddProblemWithZeroLimits(UniAsserter asserter) {
        asserter.execute(() -> {
            AddProblemRequest request = new AddProblemRequest();
            request.setTitle("Zero Limits Problem");
            request.setDescription("Problem with zero limits");
            request.setTimeLimit(0);
            request.setMemoryLimit(0);

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/problems/add")
                    .then()
                    .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
        });
    }

    @Test
    @TestSecurity(user = "11", roles = {"admin"})
    @RunOnVertxContext
    public void testAddProblemWithLongDescription(UniAsserter asserter) {
        asserter.execute(() -> {
            AddProblemRequest request = new AddProblemRequest();
            request.setTitle("Long Description Problem");
            request.setDescription("A".repeat(200));
            request.setTimeLimit(1500);
            request.setMemoryLimit(1024);

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/problems/add")
                    .then()
                    .statusCode(Response.Status.OK.getStatusCode())
                    .body("title", equalTo("Long Description Problem"))
                    .body("description", equalTo("A".repeat(200)))
                    .body("timeLimit", equalTo(1500))
                    .body("memoryLimit", equalTo(1024));
        });
    }

    @Test
    @TestSecurity(user = "9", roles = {"admin"})
    @RunOnVertxContext
    public void testGetNonExistentProblem(UniAsserter asserter) {
        asserter.execute(() -> {
            given()
                    .when()
                    .get("/api/problems/999")
                    .then()
                    .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                    .body("error", notNullValue());
        });
    }

} 