package com.fitbit.servlet;

import com.fitbit.dao.ProgressDAO;
import com.fitbit.model.ProgressEntry;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@WebServlet("/api/progress")
public class ProgressServlet extends HttpServlet {
    private static final String FRONTEND_ORIGIN = "http://localhost:3000";
    private static final Gson GSON = new Gson();
    private final ProgressDAO progressDAO = new ProgressDAO();

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

        Integer userId = getAuthenticatedUserId(request, response);
        if (userId == null) {
            return;
        }

        try {
            List<ProgressResponse> entries = progressDAO.findByUserId(userId)
                    .stream()
                    .map(ProgressResponse::from)
                    .toList();
            response.setStatus(HttpServletResponse.SC_OK);
            GSON.toJson(entries, response.getWriter());
        } catch (SQLException exception) {
            logDatabaseError(exception);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Progress history is temporarily unavailable.");
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

        Integer userId = getAuthenticatedUserId(request, response);
        if (userId == null) {
            return;
        }

        try {
            ProgressRequest progressRequest =
                    GSON.fromJson(request.getReader(), ProgressRequest.class);
            if (progressRequest == null) {
                writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Request body is required.");
                return;
            }
            int progressId = progressDAO.create(
                    userId,
                    progressRequest.weightKg,
                    progressRequest.note
            );
            response.setStatus(HttpServletResponse.SC_CREATED);
            GSON.toJson(Map.of("progressId", progressId), response.getWriter());
        } catch (JsonParseException | IllegalStateException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Request body must be valid JSON.");
        } catch (IllegalArgumentException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (SQLException exception) {
            logDatabaseError(exception);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Progress entry could not be saved.");
        }
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

    private void prepareResponse(HttpServletResponse response) {
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
        getServletContext().log("Progress database operation failed.", exception);
    }

    private void writeError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        GSON.toJson(Map.of("error", message), response.getWriter());
    }

    private static class ProgressRequest {
        private BigDecimal weightKg;
        private String note;
    }

    private record ProgressResponse(
            int progressId,
            String recordedOn,
            BigDecimal weightKg,
            String note
    ) {
        private static ProgressResponse from(ProgressEntry entry) {
            return new ProgressResponse(
                    entry.getProgressId(),
                    entry.getRecordedOn().toString(),
                    entry.getWeightKg(),
                    entry.getNote()
            );
        }
    }
}
