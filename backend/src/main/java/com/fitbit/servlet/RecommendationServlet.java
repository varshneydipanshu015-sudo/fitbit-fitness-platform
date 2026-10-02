package com.fitbit.servlet;

import com.fitbit.model.FitnessGoal;
import com.fitbit.model.FitnessLevel;
import com.fitbit.service.BasicRecommendationService;
import com.fitbit.service.RecommendationService;
import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Validates recommendation inputs and returns the selected exercises as JSON. */
@WebServlet("/api/recommendations")
public class RecommendationServlet extends HttpServlet {
    private static final String FRONTEND_ORIGIN = "http://localhost:3000";
    private static final Gson GSON = new Gson();
    private final RecommendationService recommendationService =
            new BasicRecommendationService();

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
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        if (!configureCors(request, response)) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "Origin is not allowed.");
            return;
        }

        try {
            FitnessGoal goal = parseGoal(request.getParameter("goal"));
            FitnessLevel level = parseLevel(request.getParameter("level"));

            Map<String, Object> result = Map.of(
                    "goal", goal.name(),
                    "level", level.name(),
                    "exercises", recommendationService.recommend(goal, level)
            );
            response.setStatus(HttpServletResponse.SC_OK);
            GSON.toJson(result, response.getWriter());
        } catch (IllegalArgumentException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        }
    }

    private FitnessGoal parseGoal(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("The goal parameter is required.");
        }

        try {
            return FitnessGoal.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid goal. Allowed values: " + allowedValues(FitnessGoal.values())
            );
        }
    }

    private FitnessLevel parseLevel(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("The level parameter is required.");
        }

        try {
            return FitnessLevel.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid level. Allowed values: " + allowedValues(FitnessLevel.values())
            );
        }
    }

    private String allowedValues(Enum<?>[] values) {
        return Arrays.stream(values)
                .map(Enum::name)
                .collect(Collectors.joining(", "));
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
        response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        return true;
    }

    private void writeError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        GSON.toJson(Map.of("error", message), response.getWriter());
    }
}
