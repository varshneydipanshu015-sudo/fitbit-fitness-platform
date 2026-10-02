package com.fitbit.servlet;

import com.fitbit.exception.UserValidationException;
import com.fitbit.model.FitnessGoal;
import com.fitbit.model.FitnessLevel;
import com.fitbit.model.FitnessUser;
import com.fitbit.model.Trainer;
import com.fitbit.model.User;
import com.fitbit.service.AuthService;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Handles account registration, sign-in, session profile lookup, and logout. */
@WebServlet(urlPatterns = {
        "/api/auth/register",
        "/api/auth/login",
        "/api/auth/me",
        "/api/auth/logout"
})
public class AuthServlet extends HttpServlet {
    private static final String FRONTEND_ORIGIN = "http://localhost:3000";
    private static final Gson GSON = new Gson();
    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        if (!configureCors(request, response)) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "Origin is not allowed.");
            return;
        }

        HttpSession session = request.getSession(false);
        if (session == null
                || !(session.getAttribute("userId") instanceof Integer userId)
                || userId < 1) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Please sign in to view your profile.");
            return;
        }

        try {
            // Resolve the stored role-specific account before trusting its session identity.
            String role = (String) session.getAttribute("userRole");
            Optional<? extends User> user = "TRAINER".equals(role)
                    ? authService.findTrainerById(userId)
                    : "USER".equals(role)
                            ? authService.findFitnessUserById(userId)
                            : Optional.empty();
            if (user.isEmpty()) {
                session.invalidate();
                writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Please sign in to view your profile.");
                return;
            }
            response.setStatus(HttpServletResponse.SC_OK);
            GSON.toJson(userResponse(user.get()), response.getWriter());
        } catch (SQLException exception) {
            getServletContext().log("Profile database operation failed.", exception);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Profile information is temporarily unavailable.");
        }
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (!configureCors(request, response)) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "Origin is not allowed.");
            return;
        }
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        if (!configureCors(request, response)) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "Origin is not allowed.");
            return;
        }

        try {
            if (request.getServletPath().endsWith("/logout")) {
                logout(request, response);
            } else if (request.getServletPath().endsWith("/register")) {
                register(request, response);
            } else if (request.getServletPath().endsWith("/login")) {
                login(request, response);
            } else {
                writeError(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found.");
            }
        } catch (JsonParseException | IllegalStateException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Request body must be valid JSON.");
        } catch (UserValidationException | IllegalArgumentException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (SQLException exception) {
            if (exception.getErrorCode() == 1062) {
                writeError(response, HttpServletResponse.SC_CONFLICT,
                        "An account with this email already exists.");
                return;
            }
            getServletContext().log("Authentication database operation failed.", exception);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Account service is temporarily unavailable.");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        if (!configureCors(request, response)) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "Origin is not allowed.");
            return;
        }
        if (!request.getServletPath().endsWith("/me")) {
            writeError(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found.");
            return;
        }

        HttpSession session = request.getSession(false);
        if (session == null
                || !"USER".equals(session.getAttribute("userRole"))
                || !(session.getAttribute("userId") instanceof Integer userId)
                || userId < 1) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Please sign in with a user account.");
            return;
        }

        // Fitness preferences belong only to fitness-user accounts, never trainers.
        try {
            ProfileUpdateRequest update = GSON.fromJson(
                    request.getReader(),
                    ProfileUpdateRequest.class
            );
            if (update == null) {
                writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Request body is required.");
                return;
            }
            FitnessGoal goal = parseGoal(update.fitnessGoal);
            FitnessLevel level = parseLevel(update.fitnessLevel);
            if (!authService.updateFitnessPreferences(userId, goal, level)) {
                session.invalidate();
                writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Please sign in with a user account.");
                return;
            }

            Optional<FitnessUser> user = authService.findFitnessUserById(userId);
            if (user.isEmpty()) {
                session.invalidate();
                writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Please sign in with a user account.");
                return;
            }
            response.setStatus(HttpServletResponse.SC_OK);
            GSON.toJson(userResponse(user.get()), response.getWriter());
        } catch (JsonParseException | IllegalStateException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Request body must be valid JSON.");
        } catch (UserValidationException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (SQLException exception) {
            getServletContext().log("Profile update database operation failed.", exception);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Profile preferences could not be saved.");
        }
    }

    private void logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    private void register(HttpServletRequest request, HttpServletResponse response)
            throws IOException, UserValidationException, SQLException {
        RegisterRequest registration = GSON.fromJson(request.getReader(), RegisterRequest.class);
        if (registration == null) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Request body is required.");
            return;
        }

        // Only user accounts require fitness goal and level fields.
        User user;
        if (registration.role == null || "USER".equalsIgnoreCase(registration.role.trim())) {
            FitnessGoal goal = parseGoal(registration.fitnessGoal);
            FitnessLevel level = parseLevel(registration.fitnessLevel);
            user = authService.register(
                    registration.fullName,
                    registration.email,
                    registration.password,
                    goal,
                    level
            );
        } else if ("TRAINER".equalsIgnoreCase(registration.role.trim())) {
            user = authService.registerTrainer(
                    registration.fullName,
                    registration.email,
                    registration.password,
                    registration.bio,
                    registration.specialization
            );
        } else {
            throw new UserValidationException("Select a valid account type.");
        }
        response.setStatus(HttpServletResponse.SC_CREATED);
        GSON.toJson(userResponse(user), response.getWriter());
    }

    private void login(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException {
        LoginRequest credentials = GSON.fromJson(request.getReader(), LoginRequest.class);
        if (credentials == null || credentials.email == null || credentials.password == null) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Email and password are required.");
            return;
        }

        var authenticatedUser = authService.authenticate(credentials.email, credentials.password);
        if (authenticatedUser.isEmpty()) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Email or password is incorrect.");
            return;
        }

        HttpSession session = request.getSession(true);
        request.changeSessionId();
        // Store only the account identity and role; profile data is fetched separately.
        User user = authenticatedUser.get();
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("userRole", user.getRole());

        response.setStatus(HttpServletResponse.SC_OK);
        GSON.toJson(userResponse(user), response.getWriter());
    }

    private FitnessGoal parseGoal(String value) throws UserValidationException {
        if (value == null || value.isBlank()) {
            throw new UserValidationException("Fitness goal is required.");
        }
        try {
            return FitnessGoal.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new UserValidationException("Select a valid fitness goal.");
        }
    }

    private FitnessLevel parseLevel(String value) throws UserValidationException {
        if (value == null || value.isBlank()) {
            throw new UserValidationException("Fitness level is required.");
        }
        try {
            return FitnessLevel.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new UserValidationException("Select a valid fitness level.");
        }
    }

    private Map<String, Object> userResponse(User user) {
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("userId", user.getUserId());
        result.put("fullName", user.getFullName());
        result.put("email", user.getEmail());
        result.put("role", user.getRole());
        if (user instanceof FitnessUser fitnessUser) {
            result.put("fitnessGoal", fitnessUser.getFitnessGoal().name());
            result.put("fitnessLevel", fitnessUser.getFitnessLevel().name());
        } else if (user instanceof Trainer trainer) {
            result.put("bio", trainer.getBio());
            result.put("specialization", trainer.getSpecialization());
        }
        return result;
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
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        return true;
    }

    private void writeError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        GSON.toJson(Map.of("error", message), response.getWriter());
    }

    private static class RegisterRequest {
        private String role;
        private String fullName;
        private String email;
        private String password;
        private String fitnessGoal;
        private String fitnessLevel;
        private String bio;
        private String specialization;
    }

    private static class LoginRequest {
        private String email;
        private String password;
    }

    private static class ProfileUpdateRequest {
        private String fitnessGoal;
        private String fitnessLevel;
    }
}
