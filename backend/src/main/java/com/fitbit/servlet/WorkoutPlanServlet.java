package com.fitbit.servlet;

import com.fitbit.dao.WorkoutPlanDAO;
import com.fitbit.model.Exercise;
import com.fitbit.model.FitnessGoal;
import com.fitbit.model.PlanExercise;
import com.fitbit.model.WorkoutPlan;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Allows trainers to manage their plans and signed-in users to browse all plans. */
@WebServlet("/api/plans")
public class WorkoutPlanServlet extends HttpServlet {
    private static final String FRONTEND_ORIGIN = "http://localhost:3000";
    // Keep nullable description and exercise fields present for the frontend response validators.
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private final WorkoutPlanDAO workoutPlanDAO = new WorkoutPlanDAO();

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        prepareResponse(response);
        if (!configureCors(request, response)) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "Origin is not allowed.");
            return;
        }
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        prepareResponse(response);
        if (!configureCors(request, response)) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "Origin is not allowed.");
            return;
        }
        HttpSession session = request.getSession(false);
        if (session == null
                || !(session.getAttribute("userId") instanceof Integer userId)
                || userId < 1
                || !("USER".equals(session.getAttribute("userRole"))
                    || "TRAINER".equals(session.getAttribute("userRole")))) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Please sign in to browse workout plans.");
            return;
        }
        try {
            // Trainers see only their own plans; fitness users can browse the full catalog.
            response.setStatus(HttpServletResponse.SC_OK);
            GSON.toJson(
                    "TRAINER".equals(session.getAttribute("userRole"))
                            ? workoutPlanDAO.findByTrainerId(userId)
                            : workoutPlanDAO.findAll(),
                    response.getWriter()
            );
        } catch (SQLException exception) {
            getServletContext().log("Workout plan lookup failed.", exception);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Workout plans are temporarily unavailable.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        prepareResponse(response);
        if (!configureCors(request, response)) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "Origin is not allowed.");
            return;
        }
        Integer trainerId = getTrainerId(request, response);
        if (trainerId == null) {
            return;
        }

        try {
            CreatePlanRequest createRequest =
                    GSON.fromJson(request.getReader(), CreatePlanRequest.class);
            if (createRequest == null) {
                writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Request body is required.");
                return;
            }
            WorkoutPlan plan = toWorkoutPlan(createRequest, trainerId);
            int planId = workoutPlanDAO.create(plan);
            response.setStatus(HttpServletResponse.SC_CREATED);
            GSON.toJson(Map.of("planId", planId), response.getWriter());
        } catch (JsonParseException | IllegalStateException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Request body must be valid JSON.");
        } catch (IllegalArgumentException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (SQLException exception) {
            if ("23000".equals(exception.getSQLState())) {
                writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "One or more selected exercises could not be found.");
                return;
            }
            getServletContext().log("Workout plan creation failed.", exception);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Workout plan could not be saved.");
        }
    }

    private WorkoutPlan toWorkoutPlan(CreatePlanRequest request, int trainerId) {
        if (request.exercises == null || request.exercises.isEmpty()) {
            throw new IllegalArgumentException("Select at least one exercise.");
        }
        FitnessGoal goal;
        try {
            if (request.fitnessGoal == null || request.fitnessGoal.isBlank()) {
                throw new IllegalArgumentException("Fitness goal is required.");
            }
            goal = FitnessGoal.valueOf(
                    request.fitnessGoal.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Select a valid fitness goal.");
        }
        WorkoutPlan plan = new WorkoutPlan(
                0,
                trainerId,
                request.planName,
                request.description == null || request.description.isBlank()
                        ? null
                        : request.description.trim(),
                goal
        );
        // The list position is the canonical order, avoiding caller-supplied gaps or duplicates.
        for (int index = 0; index < request.exercises.size(); index++) {
            PlanExerciseRequest item = request.exercises.get(index);
            if (item == null || item.exerciseId == null || item.setsCount == null
                    || item.repsCount == null || item.restSeconds == null) {
                throw new IllegalArgumentException(
                        "Each exercise needs sets, reps, and rest values."
                );
            }
            plan.addExercise(new PlanExercise(
                    new Exercise(
                            item.exerciseId,
                            "",
                            null,
                            null,
                            null,
                            ""
                    ),
                    index + 1,
                    item.setsCount,
                    item.repsCount,
                    item.restSeconds
            ));
        }
        return plan;
    }

    private Integer getTrainerId(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null
                || !"TRAINER".equals(session.getAttribute("userRole"))
                || !(session.getAttribute("userId") instanceof Integer trainerId)
                || trainerId < 1) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Please sign in with a trainer account.");
            return null;
        }
        return trainerId;
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

    private void prepareResponse(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
    }

    private void writeError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        GSON.toJson(Map.of("error", message), response.getWriter());
    }

    private static class CreatePlanRequest {
        private String planName;
        private String description;
        private String fitnessGoal;
        private List<PlanExerciseRequest> exercises;
    }

    private static class PlanExerciseRequest {
        private Integer exerciseId;
        private Integer setsCount;
        private Integer repsCount;
        private Integer restSeconds;
    }
}
