"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

type Workout = {
  workoutId: number;
  startedAt: string;
  completedAt: string | null;
  status: "IN_PROGRESS" | "COMPLETED";
  notes: string | null;
};

type ProgressEntry = {
  progressId: number;
  recordedOn: string;
  weightKg: number;
  note: string | null;
};

type Profile = {
  role: "USER";
  fullName: string;
  fitnessGoal:
    | "WEIGHT_LOSS"
    | "MUSCLE_GAIN"
    | "GENERAL_FITNESS"
    | "STRENGTH"
    | "ENDURANCE";
  fitnessLevel: "BEGINNER" | "INTERMEDIATE" | "ADVANCED";
};

type TrainerProfile = {
  role: "TRAINER";
  fullName: string;
};

type ApiError = {
  error?: string;
};

const goalLabels: Record<Profile["fitnessGoal"], string> = {
  WEIGHT_LOSS: "Weight loss",
  MUSCLE_GAIN: "Muscle gain",
  GENERAL_FITNESS: "General fitness",
  STRENGTH: "Strength",
  ENDURANCE: "Endurance",
};

const levelLabels: Record<Profile["fitnessLevel"], string> = {
  BEGINNER: "Beginner",
  INTERMEDIATE: "Intermediate",
  ADVANCED: "Advanced",
};

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/fitbit-app";

function isWorkoutArray(value: unknown): value is Workout[] {
  return (
    Array.isArray(value) &&
    value.every(
      (item) =>
        typeof item === "object" &&
        item !== null &&
        typeof item.workoutId === "number" &&
        typeof item.startedAt === "string" &&
        (item.status === "IN_PROGRESS" || item.status === "COMPLETED"),
    )
  );
}

function isProgressArray(value: unknown): value is ProgressEntry[] {
  return (
    Array.isArray(value) &&
    value.every(
      (item) =>
        typeof item === "object" &&
        item !== null &&
        typeof item.progressId === "number" &&
        typeof item.recordedOn === "string" &&
        typeof item.weightKg === "number",
    )
  );
}

function isProfile(value: unknown): value is Profile {
  if (typeof value !== "object" || value === null) {
    return false;
  }

  const profile = value as Record<string, unknown>;
  return (
    profile.role === "USER" &&
    typeof profile.fullName === "string" &&
    typeof profile.fitnessGoal === "string" &&
    Object.hasOwn(goalLabels, profile.fitnessGoal) &&
    typeof profile.fitnessLevel === "string" &&
    Object.hasOwn(levelLabels, profile.fitnessLevel)
  );
}

async function responseError(response: Response) {
  try {
    const result = (await response.json()) as ApiError;
    return result.error ?? "Dashboard information could not be loaded.";
  } catch {
    return "The backend returned a response that could not be read.";
  }
}

function formatDate(value: string) {
  return new Date(value).toLocaleString(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  });
}

function workoutsInLastSevenDays(workouts: Workout[]) {
  const now = Date.now();
  const sevenDaysAgo = now - 7 * 24 * 60 * 60 * 1000;
  return workouts.filter((workout) => {
    const startedAt = new Date(workout.startedAt).getTime();
    return startedAt >= sevenDaysAgo && startedAt <= now;
  }).length;
}

export default function DashboardOverview() {
  const [workouts, setWorkouts] = useState<Workout[]>([]);
  const [progress, setProgress] = useState<ProgressEntry[]>([]);
  const [profile, setProfile] = useState<Profile | null>(null);
  const [trainerProfile, setTrainerProfile] = useState<TrainerProfile | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSignedOut, setIsSignedOut] = useState(false);
  const [error, setError] = useState("");
  const [reloadCount, setReloadCount] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    async function loadDashboard() {
      setIsLoading(true);
      setError("");

      let profileResponse: Response;
      try {
        profileResponse = await fetch(`${apiBaseUrl}/api/auth/me`, {
          credentials: "include",
          signal: controller.signal,
        });
      } catch (fetchError) {
        if (
          fetchError instanceof DOMException &&
          fetchError.name === "AbortError"
        ) {
          return;
        }
        setError("Could not reach the Fitbit backend. Check that Tomcat is running.");
        setIsLoading(false);
        return;
      }

      if (profileResponse.status === 401) {
        setIsSignedOut(true);
        setIsLoading(false);
        return;
      }
      if (!profileResponse.ok) {
        setError(await responseError(profileResponse));
        setIsLoading(false);
        return;
      }

      let profileResult: unknown;
      try {
        profileResult = await profileResponse.json();
      } catch {
        setError("The backend returned profile information in an unexpected format.");
        setIsLoading(false);
        return;
      }

      if (
        typeof profileResult === "object" &&
        profileResult !== null &&
        "role" in profileResult &&
        profileResult.role === "TRAINER" &&
        "fullName" in profileResult &&
        typeof profileResult.fullName === "string"
      ) {
        setTrainerProfile({ role: "TRAINER", fullName: profileResult.fullName });
        setIsLoading(false);
        return;
      }

      let responses: Response[];
      try {
        responses = await Promise.all([
          fetch(`${apiBaseUrl}/api/workouts`, {
            credentials: "include",
            signal: controller.signal,
          }),
          fetch(`${apiBaseUrl}/api/progress`, {
            credentials: "include",
            signal: controller.signal,
          }),
        ]);
      } catch (fetchError) {
        if (
          fetchError instanceof DOMException &&
          fetchError.name === "AbortError"
        ) {
          return;
        }
        setError("Could not reach the Fitbit backend. Check that Tomcat is running.");
        setIsLoading(false);
        return;
      }

      if (responses.some((response) => response.status === 401)) {
        setIsSignedOut(true);
        setIsLoading(false);
        return;
      }
      const failedResponse = responses.find((response) => !response.ok);
      if (failedResponse) {
        setError(await responseError(failedResponse));
        setIsLoading(false);
        return;
      }

      try {
        const [workoutResult, progressResult]: unknown[] = await Promise.all(
          responses.map((response) => response.json()),
        );
        if (
          !isWorkoutArray(workoutResult) ||
          !isProgressArray(progressResult) ||
          !isProfile(profileResult)
        ) {
          throw new Error("Unexpected dashboard response format.");
        }
        setWorkouts(workoutResult);
        setProgress(progressResult);
        setProfile(profileResult);
      } catch {
        setError("The backend returned dashboard data in an unexpected format.");
      }
      setIsLoading(false);
    }

    void loadDashboard();
    return () => controller.abort();
  }, [reloadCount]);

  const latestProgress = progress[0];
  const recentWorkouts = workouts.slice(0, 4);

  if (isSignedOut) {
    return (
      <section className="dashboard" aria-labelledby="dashboard-heading">
        <div className="dashboard-heading">
          <div>
            <p className="eyebrow">YOUR FITNESS SPACE</p>
            <h1 id="dashboard-heading">
              {profile ? `${profile.fullName}’s dashboard` : "Your dashboard"}
            </h1>
            <p className="dashboard-intro">
              Sign in to see your personal workout and progress summaries.
            </p>
          </div>
        </div>
        <div className="library-message">
          <Link className="secondary-link" href="/login">
            Sign in to your account
          </Link>
        </div>
      </section>
    );
  }

  if (trainerProfile) {
    return (
      <section className="dashboard" aria-labelledby="dashboard-heading">
        <div className="dashboard-heading">
          <div>
            <p className="eyebrow">TRAINER SPACE</p>
            <h1 id="dashboard-heading">Welcome, {trainerProfile.fullName}</h1>
            <p className="dashboard-intro">
              Create and manage reusable workout plans for your coaching.
            </p>
          </div>
        </div>
        <div className="dashboard-panel">
          <h2 className="trainer-dashboard-heading">Trainer tools</h2>
          <p className="overview-detail">
            Create reusable routines with exercises from the exercise library.
            Keep your coaching roster up to date by adding fitness users.
          </p>
          <Link className="primary-link" href="/plans">
            Create workout plans
          </Link>
          <Link className="secondary-link" href="/clients">
            Manage clients
          </Link>
          <Link className="secondary-link" href="/profile">
            Profile settings
          </Link>
        </div>
      </section>
    );
  }

  return (
    <section className="dashboard" aria-labelledby="dashboard-heading">
      <div className="dashboard-heading">
        <div>
          <p className="eyebrow">YOUR FITNESS SPACE</p>
          <h1 id="dashboard-heading">Your dashboard</h1>
          <p className="dashboard-intro">
            A clear view of your workouts and personal progress.
          </p>
        </div>
        <div className="dashboard-actions">
          <Link className="primary-link" href="/workouts">
            Log a workout
          </Link>
          <Link className="secondary-link" href="/exercises">
            Browse exercises
          </Link>
          <Link className="secondary-link" href="/browse-plans">
            Browse workout plans
          </Link>
          <Link className="secondary-link" href="/recommendations">
            Get recommendations
          </Link>
          <Link className="secondary-link" href="/progress">
            Track progress
          </Link>
          <Link className="secondary-link" href="/profile">
            Profile
          </Link>
        </div>
      </div>

      {error && (
        <div className="library-message library-error" role="alert">
          <p>{error}</p>
          <button
            className="secondary-link retry-button"
            onClick={() => setReloadCount((count) => count + 1)}
            type="button"
          >
            Try again
          </button>
        </div>
      )}

      <div className="overview-grid" aria-label="Fitness overview">
        <article className="overview-card">
          <h2>Workouts in the last 7 days</h2>
          <p className="overview-value">
            {isLoading || error ? "—" : workoutsInLastSevenDays(workouts)}
          </p>
          <p className="overview-detail">Based on your recorded sessions.</p>
        </article>
        <article className="overview-card">
          <h2>Current goal</h2>
          <p className="overview-value">
            {isLoading || error
              ? "—"
              : profile
                ? goalLabels[profile.fitnessGoal]
                : "Not set"}
          </p>
          <p className="overview-detail">
            {profile && !isLoading && !error
              ? `${levelLabels[profile.fitnessLevel]} level`
              : "Your goal will appear after account setup."}
          </p>
        </article>
        <article className="overview-card">
          <h2>Latest weight</h2>
          <p className="overview-value">
            {isLoading || error
              ? "—"
              : latestProgress
                ? `${latestProgress.weightKg.toFixed(2)} kg`
                : "Not set"}
          </p>
          <p className="overview-detail">
            {latestProgress && !isLoading && !error
              ? `Recorded ${latestProgress.recordedOn}.`
              : "Log an optional update in your progress tracker."}
          </p>
        </article>
      </div>

      <div className="dashboard-content-grid">
        <section className="dashboard-panel" aria-labelledby="workout-heading">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">YOUR ACTIVITY</p>
              <h2 id="workout-heading">Recent workouts</h2>
            </div>
            <Link className="panel-link" href="/workouts">
              View all
            </Link>
          </div>
          {isLoading && (
            <p className="library-message" role="status">
              Loading your activity...
            </p>
          )}
          {!isLoading && !error && recentWorkouts.length === 0 && (
            <div className="empty-state">
              <span className="empty-state-icon" aria-hidden="true">
                +
              </span>
              <h3>No workouts logged yet</h3>
              <p>Start a session when you’re ready to begin.</p>
              <Link className="secondary-link" href="/workouts">
                Log a workout
              </Link>
            </div>
          )}
          {!isLoading && !error && recentWorkouts.length > 0 && (
            <div className="dashboard-workout-list">
              {recentWorkouts.map((workout) => (
                <article className="dashboard-workout" key={workout.workoutId}>
                  <div>
                    <h3>Workout #{workout.workoutId}</h3>
                    <p>{formatDate(workout.startedAt)}</p>
                  </div>
                  <span className="dashboard-workout-status">
                    {workout.status === "COMPLETED" ? "Completed" : "In progress"}
                  </span>
                </article>
              ))}
            </div>
          )}
        </section>

        <aside className="dashboard-panel next-steps" aria-labelledby="next-steps-heading">
          <p className="eyebrow">KEEP GOING</p>
          <h2 id="next-steps-heading">Build your routine</h2>
          <p>
            Browse exercises, choose a goal for recommendations, or record a
            progress update whenever it feels useful to you.
          </p>
          <Link className="secondary-link" href="/progress">
            Track progress
          </Link>
        </aside>
      </div>
    </section>
  );
}
