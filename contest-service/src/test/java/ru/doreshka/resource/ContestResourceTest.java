package ru.doreshka.resource;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.doreshka.domain.entity.User;
import ru.doreshka.domain.entity.UserContestAccess;
import ru.doreshka.domain.repository.ContestProblemRepository;
import ru.doreshka.domain.repository.ContestRepository;
import ru.doreshka.domain.repository.ProblemRepository;
import ru.doreshka.domain.repository.UserRepository;
import ru.doreshka.dto.contest.AddContestRequest;
import ru.doreshka.dto.contest.AddProblemToContestRequest;
import ru.doreshka.dto.problem.AddProblemRequest;
import ru.doreshka.service.ContestService;
import ru.doreshka.service.ProblemService;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class ContestResourceTest {

    private final AtomicLong currentContestId = new AtomicLong();
    private final AtomicLong currentProblemId = new AtomicLong();
    private final AtomicLong currentUserId = new AtomicLong();
    @Inject
    ContestService contestService;
    @Inject
    ProblemService problemService;
    @Inject
    UserRepository userRepository;
    @Inject
    ContestRepository contestRepository;
    @Inject
    ProblemRepository problemRepository;
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
            user.setUsername(username);
            user.setPasswordHash("password");
            return userRepository.persistAndFlush(user);
        });
    }

    private Uni<User> createUserAndSetId(Long id, String username) {
        return createUser(id, username).onItem().invoke(user -> currentUserId.set(user.id));
    }

    @Test
    @TestSecurity(user = "1", roles = {"admin"})
    @RunOnVertxContext
    public void testCreateContestSuccess(UniAsserter asserter) {
        asserter.execute(() -> {
            AddContestRequest request = new AddContestRequest(
                    "New Contest",
                    "Contest description",
                    LocalDateTime.now().plusHours(1),
                    LocalDateTime.now().plusDays(1)
            );

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/contest/new")
                    .then()
                    .statusCode(Response.Status.OK.getStatusCode());
        });
    }

    @Test
    @TestSecurity(user = "2", roles = {"user"})
    @RunOnVertxContext
    public void testCreateContestForbidden(UniAsserter asserter) {
        asserter.execute(() -> {
            AddContestRequest request = new AddContestRequest(
                    "Test Contest",
                    "Description",
                    LocalDateTime.now().plusHours(1),
                    LocalDateTime.now().plusDays(1)
            );

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/contest/new")
                    .then()
                    .statusCode(Response.Status.FORBIDDEN.getStatusCode());
        });
    }

    @Test
    @TestSecurity(user = "3", roles = {"admin"})
    @RunOnVertxContext
    public void testGetAllContests(UniAsserter asserter) {
        asserter.execute(() -> {
            given()
                    .when()
                    .get("/api/contest/all")
                    .then()
                    .statusCode(Response.Status.OK.getStatusCode())
                    .body("$", hasSize(greaterThanOrEqualTo(0)));
        });
    }

    @Test
    @TestSecurity(user = "4", roles = {"user"})
    @RunOnVertxContext
    public void testGetAllContestsForbidden(UniAsserter asserter) {
        asserter.execute(() -> {
            given()
                    .when()
                    .get("/api/contest/all")
                    .then()
                    .statusCode(Response.Status.FORBIDDEN.getStatusCode());
        });
    }

    @Test
    @TestSecurity(user = "7", roles = {"user"})
    @RunOnVertxContext
    public void testGetMyContestsEmpty(UniAsserter asserter) {
        asserter.execute(() -> {
            given()
                    .when()
                    .get("/api/contest/my")
                    .then()
                    .statusCode(Response.Status.OK.getStatusCode())
                    .body("$", hasSize(0));
        });
    }

    @Test
    @TestSecurity(user = "9", roles = {"admin"})
    @RunOnVertxContext
    public void testAddProblemToContestSuccess(UniAsserter asserter) {
        asserter.execute(() -> {
            AddProblemRequest problemRequest = new AddProblemRequest();
            problemRequest.setTitle("Contest Problem");
            problemRequest.setDescription("Problem for contest");
            problemRequest.setTimeLimit(1000);
            problemRequest.setMemoryLimit(256);
            return problemService.createProblem(problemRequest)
                    .onItem().invoke(problem -> currentProblemId.set(problem.id));
        });

        asserter.execute(() -> {
            AddContestRequest contestRequest = new AddContestRequest(
                    "Contest for Problem",
                    "Description",
                    LocalDateTime.now().plusHours(1),
                    LocalDateTime.now().plusDays(1)
            );
            return contestService.createContest(contestRequest)
                    .onItem().invoke(contest -> currentContestId.set(contest.id));
        });

        asserter.execute(() -> {
            AddProblemToContestRequest request = new AddProblemToContestRequest(
                    currentProblemId.get(),
                    "A"
            );

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/contest/" + currentContestId.get() + "/problems")
                    .then()
                    .statusCode(Response.Status.CREATED.getStatusCode())
                    .body("shortName", equalTo("A"));
        });
    }

    @Test
    @TestSecurity(user = "10", roles = {"admin"})
    @RunOnVertxContext
    public void testAddNonExistentProblemToContest(UniAsserter asserter) {
        asserter.execute(() -> {
            AddContestRequest contestRequest = new AddContestRequest(
                    "Contest for Test",
                    "Description",
                    LocalDateTime.now().plusHours(1),
                    LocalDateTime.now().plusDays(1)
            );
            return contestService.createContest(contestRequest)
                    .onItem().invoke(contest -> currentContestId.set(contest.id));
        });

        asserter.execute(() -> {
            AddProblemToContestRequest request = new AddProblemToContestRequest(
                    999L,
                    "A"
            );

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/contest/" + currentContestId.get() + "/problems")
                    .then()
                    .statusCode(Response.Status.NOT_FOUND.getStatusCode())
                    .body("error", notNullValue());
        });
    }

    @Test
    @TestSecurity(user = "12", roles = {"user"})
    @RunOnVertxContext
    public void testGetContestProblemsWithoutAccess(UniAsserter asserter) {
        asserter.execute(() -> {
            AddContestRequest contestRequest = new AddContestRequest(
                    "No Access Contest",
                    "Contest without access",
                    LocalDateTime.now().plusHours(1),
                    LocalDateTime.now().plusDays(1)
            );
            return contestService.createContest(contestRequest)
                    .onItem().invoke(contest -> currentContestId.set(contest.id));
        });

        asserter.execute(() -> {
            given()
                    .when()
                    .get("/api/contest/" + currentContestId.get() + "/problems")
                    .then()
                    .statusCode(Response.Status.FORBIDDEN.getStatusCode())
                    .body("error", equalTo("no access to contest"));
        });
    }

    @Test
    @TestSecurity(user = "13", roles = {"admin"})
    @RunOnVertxContext
    public void testGetContestProblemsAdmin(UniAsserter asserter) {
        asserter.execute(() -> {
            AddProblemRequest problemRequest = new AddProblemRequest();
            problemRequest.setTitle("Admin Problem");
            problemRequest.setDescription("Problem for admin test");
            problemRequest.setTimeLimit(3000);
            problemRequest.setMemoryLimit(1024);
            return problemService.createProblem(problemRequest)
                    .onItem().invoke(problem -> currentProblemId.set(problem.id));
        });

        asserter.execute(() -> {
            AddContestRequest contestRequest = new AddContestRequest(
                    "Admin Contest",
                    "Contest for admin",
                    LocalDateTime.now().plusHours(1),
                    LocalDateTime.now().plusDays(1)
            );
            return contestService.createContest(contestRequest)
                    .onItem().invoke(contest -> currentContestId.set(contest.id));
        });

        asserter.execute(() ->
                contestService.addProblemToContest(currentContestId.get(),
                        new AddProblemToContestRequest(currentProblemId.get(), "C"))
        );

        asserter.execute(() -> {
            given()
                    .when()
                    .get("/api/contest/" + currentContestId.get() + "/problems_admin")
                    .then()
                    .statusCode(Response.Status.OK.getStatusCode())
                    .body("$", hasSize(1))
                    .body("[0].title", equalTo("Admin Problem"));
        });
    }

    @Test
    @TestSecurity(user = "14", roles = {"admin"})
    @RunOnVertxContext
    public void testCreateContestWithValidDates(UniAsserter asserter) {
        asserter.execute(() -> {
            AddContestRequest request = new AddContestRequest(
                    "Valid Date Contest",
                    "Contest with valid dates",
                    LocalDateTime.now().plusMinutes(30),
                    LocalDateTime.now().plusHours(2)
            );

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/contest/new")
                    .then()
                    .statusCode(Response.Status.OK.getStatusCode());
        });
    }

} 