package com.fitbit.servlet;

import com.fitbit.dao.WorkoutSessionDAO;
import com.fitbit.model.WorkoutSession;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/** Starts and completes sessions while keeping every operation scoped to its owner. */
@WebServlet(urlPatterns = {"/api/workouts", "/api/workouts/complete"})
public class WorkoutSessionServlet extends HttpServlet {
    private static final String FRONTEND_ORIGIN = "http://localhost:3000";
    private static final int MAX_NOTES_LENGTH = 1000;
    private static final Gson GSON = new Gson();
    private final WorkoutSessionDAO workoutSessionDAO = new WorkoutSessionDAO();

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        if (!configureCors(request, response)) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "Origin is not allowed.");
            return;
        }
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        prepareResponse(request, response);
        if (!configureCors(request, response)) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "Origin is not allowed.");
            return;
        }

        Integer userId = getAuthenticatedUserId(request, response);
        if (userId == null) {
            return;
        }

        try {
            List<WorkoutResponse> workouts = workoutSessionDAO.findByUserId(userId)
                    .stream()
                    .map(WorkoutResponse::from)
                    .toList();
            response.setStatus(HttpServletResponse.SC_OK);
            GSON.toJson(workouts, response.getWriter());
        } catch (SQLException exception) {
            logDatabaseError(exception);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Workout history is temporarily unavailable.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        prepareResponse(request, response);
        if (!configureCors(request, response)) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "Origin is not allowed.");
            return;
        }

        Integer userId = getAuthenticatedUserId(request, response);
        if (userId == null) {
            return;
        }

        try {
            // Use the endpoint path to share common session handling for start and complete.
            if (request.getServletPath().endsWith("/complete")) {
                completeWorkout(request, response, userId);
            } else {
                startWorkout(request, response, userId);
            }
        } catch (JsonParseException | IllegalStateException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Request body must be valid JSON.");
        } catch (IllegalArgumentException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (SQLException exception) {
            if ("23000".equals(exception.getSQLState())) {
                writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "The selected workout plan does not exist.");
                return;
            }
            logDatabaseError(exception);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Workout service is temporarily unavailable.");
        }
    }

    private void startWorkout(
            HttpServletRequest request,
            HttpServletResponse response,
            int userId
    ) throws IOException, SQLException {
        StartWorkoutRequest startRequest =
                GSON.fromJson(request.getReader(), StartWorkoutRequest.class);
        if (startRequest == null) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Request body is required.");
            return;
        }
        if (startRequest.notes != null && startRequest.notes.length() > MAX_NOTES_LENGTH) {
            throw new IllegalArgumentException("Workout notes must be 1000 characters or fewer.");
        }

        int workoutId = workoutSessionDAO.startWorkout(
                userId,
                startRequest.planId,
                startRequest.notes
        );
        response.setStatus(HttpServletResponse.SC_CREATED);
        GSON.toJson(Map.of("workoutId", workoutId, "status", "IN_PROGRESS"),
                response.getWriter());
    }

    private void completeWorkout(
            HttpServletRequest request,
            HttpServletResponse response,
            int userId
    ) throws IOException, SQLException {
        CompleteWorkoutRequest completeRequest =
                GSON.fromJson(request.getReader(), CompleteWorkoutRequest.class);
        if (completeRequest == null || completeRequest.workoutId == null) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Workout ID is required.");
            return;
        }

        boolean completed = workoutSessionDAO.completeWorkout(
                completeRequest.workoutId,
                userId
        );
        if (!completed) {
            writeError(response, HttpServletResponse.SC_NOT_FOUND,
                    "An in-progress workout for this user was not found.");
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);
        GSON.toJson(Map.of("workoutId", completeRequest.workoutId, "status", "COMPLETED"),
                response.getWriter());
    }

    private Integer getAuthenticatedUserId(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null
                || !"USER".equals(session.getAttribute("userRole"))
                || !(session.getAttribute("userId") instanceof Integer userId)
                || userId < 1) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Please sign in with a user account.");
            return null;
        }
        return userId;
    }

    private void prepareResponse(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
    }

    private boolean configureCors(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Vary", "Origin");
        String origin = request.getHeader("Origin");
        if (origin == null) {
            return true;
        }
        if (!FRONTEND_ORIGIN.equals(origin)) {
            return false;
        }

        response.setHeader("Access-Control-Allow-Origin", FRONTEND_ORIGIN);
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        return true;
    }

    private void logDatabaseError(SQLException exception) {
        getServletContext().log("Workout database operation failed.", exception);
    }

    private void writeError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        GSON.toJson(Map.of("error", message), response.getWriter());
    }

    private static class StartWorkoutRequest {
        private Integer planId;
        private String notes;
    }

    private static class CompleteWorkoutRequest {
        private Integer workoutId;
    }

    private record WorkoutResponse(
            int workoutId,
            Integer planId,
            String startedAt,
            String completedAt,
            String status,
            String notes
    ) {
        private static WorkoutResponse from(WorkoutSession workout) {
            return new WorkoutResponse(
                    workout.getWorkoutId(),
                    workout.getPlanId(),
                    workout.getStartedAt().toString(),
                    workout.getCompletedAt() == null
                            ? null
                            : workout.getCompletedAt().toString(),
                    workout.getStatus().name(),
                    workout.getNotes()
            );
        }
    }
}
