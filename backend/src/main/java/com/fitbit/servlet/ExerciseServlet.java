package com.fitbit.servlet;

import com.fitbit.dao.ExerciseDAO;
import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/api/exercises")
public class ExerciseServlet extends HttpServlet {
    private static final String FRONTEND_ORIGIN = "http://localhost:3000";
    private static final Gson GSON = new Gson();
    private final ExerciseDAO exerciseDAO = new ExerciseDAO();

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
        response.setHeader("Cache-Control", "no-store");
        if (!configureCors(request, response)) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "Origin is not allowed.");
            return;
        }

        try {
            response.setStatus(HttpServletResponse.SC_OK);
            GSON.toJson(exerciseDAO.findAll(), response.getWriter());
        } catch (SQLException exception) {
            getServletContext().log("Exercise library database operation failed.", exception);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Exercise library is temporarily unavailable.");
        }
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
        response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        return true;
    }

    private void prepareResponse(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
    }

    private void writeError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        GSON.toJson(java.util.Map.of("error", message), response.getWriter());
    }
}
