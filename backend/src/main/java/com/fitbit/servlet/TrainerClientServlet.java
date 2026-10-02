package com.fitbit.servlet;

import com.fitbit.dao.TrainerClientDAO;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/** Lists and assigns clients using only the trainer identity in the session. */
@WebServlet("/api/trainer/clients")
public class TrainerClientServlet extends HttpServlet {
    private static final String FRONTEND_ORIGIN = "http://localhost:3000";
    private static final Gson GSON = new Gson();
    private final TrainerClientDAO trainerClientDAO = new TrainerClientDAO();

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
        Integer trainerId = getTrainerId(request, response);
        if (trainerId == null) {
            return;
        }
        try {
            response.setStatus(HttpServletResponse.SC_OK);
            GSON.toJson(trainerClientDAO.findClientsByTrainerId(trainerId), response.getWriter());
        } catch (SQLException exception) {
            getServletContext().log("Trainer client lookup failed.", exception);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Trainer clients are temporarily unavailable.");
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
            AssignClientRequest assignRequest =
                    GSON.fromJson(request.getReader(), AssignClientRequest.class);
            if (assignRequest == null) {
                writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Request body is required.");
                return;
            }
            boolean assigned = trainerClientDAO.assignClient(
                    trainerId,
                    assignRequest.email
            );
            if (!assigned) {
                writeError(response, HttpServletResponse.SC_NOT_FOUND,
                        "No fitness user was found with that email.");
                return;
            }
            response.setStatus(HttpServletResponse.SC_CREATED);
            GSON.toJson(Map.of("message", "Client assigned."), response.getWriter());
        } catch (JsonParseException | IllegalStateException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Request body must be valid JSON.");
        } catch (IllegalArgumentException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (SQLException exception) {
            if ("23000".equals(exception.getSQLState())) {
                writeError(response, HttpServletResponse.SC_CONFLICT,
                        "This client is already assigned to your account.");
                return;
            }
            getServletContext().log("Trainer client assignment failed.", exception);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Client could not be assigned.");
        }
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

    private static class AssignClientRequest {
        private String email;
    }
}
