# Fitbit fitness platform

A local fitness web app with fitness-user and trainer accounts, exercise browsing,
workout tracking, progress tracking, trainer-created workout plans, and trainer
client rosters.

## Requirements

- Java 21
- Maven
- Node.js and npm
- MySQL 8
- Apache Tomcat 10.1

## One-time database setup

If the database has not been initialized yet, open `database/schema.sql` in
MySQL Workbench (or another MySQL client) and execute it. Run SQL in a MySQL
client, not at a PowerShell prompt. The script creates `fitbit_db` and the
application tables.

Configure these Windows environment variables for the account that starts
Tomcat:

- `FITBIT_DB_URL` — JDBC URL for `fitbit_db` (for example,
  `jdbc:mysql://localhost:3306/fitbit_db`)
- `FITBIT_DB_USER` — the limited MySQL application account
- `FITBIT_DB_PASSWORD` — that account's password

Keep the database password out of source files and chat. Restart Tomcat after
changing Windows environment variables so its Java process receives them.

## Build and deploy the backend

From PowerShell:

```powershell
Set-Location C:\Fitbit\backend
mvn clean test package
Copy-Item .\target\fitbit-backend.war "$env:CATALINA_HOME\webapps\fitbit-app.war" -Force
```

Tomcat deploys the app at `http://localhost:8080/fitbit-app`. Deploy the WAR as
`fitbit-app.war`; do not replace the separate `fitbit-backend` application.
Wait for Tomcat to finish redeploying before testing endpoints.

## Run the frontend

From a second PowerShell window:

```powershell
Set-Location C:\Fitbit\frontend
npm install
npm run dev
```

Open `http://localhost:3000`. The frontend uses
`http://localhost:8080/fitbit-app` as its default backend. To use another
backend URL, set `NEXT_PUBLIC_API_BASE_URL` before starting the frontend.

## Using the app

1. Create an account at `/register`. Choose **Fitness user** for personal
   workouts and progress, or **Trainer** to author plans and manage a roster.
2. Sign in at `/login`; sign-in sends you to the dashboard.
3. Trainers can use **My plans** to create routines from saved exercises and
   **Clients** to add an existing fitness-user account by email.
4. Fitness users can use **Workout plans** to browse plans and start one.
   Started plans appear in workout history. Users can also track personal
   workouts, progress, and recommendations.

The exercise library must contain exercises before trainers can build plans.
The plans and trainer-client association tables are part of
`database/schema.sql`.

## Verification

Run the backend tests and package the WAR:

```powershell
Set-Location C:\Fitbit\backend
mvn clean test package
```

Run frontend lint and a production build:

```powershell
Set-Location C:\Fitbit\frontend
npm run lint
npm run build
```

Useful local smoke checks:

- `http://localhost:8080/fitbit-app/api/health` returns HTTP 200.
- `http://localhost:8080/fitbit-app/api/exercises` returns the exercise list.
- Protected APIs return HTTP 401 when requested without a signed-in session;
  that is expected behavior.

The default CORS configuration is for local development at
`http://localhost:3000`. Production deployment requires configuring the frontend
origin and serving the app over HTTPS.
