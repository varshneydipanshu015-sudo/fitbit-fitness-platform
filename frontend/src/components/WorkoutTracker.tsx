"use client";

import Link from "next/link";
import { useEffect, useState, type FormEvent } from "react";

type Workout = {
  workoutId: number;
  planId: number | null;
  startedAt: string;
  completedAt: string | null;
  status: "IN_PROGRESS" | "COMPLETED";
  notes: string | null;
};

type ApiError = {
  error?: string;
};

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/fitbit-app";

async function readError(response: Response) {
  try {
    const result = (await response.json()) as ApiError;
    return result.error ?? "The workout request could not be completed.";
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

/** Records personal workout sessions and supports completing in-progress entries. */
export default function WorkoutTracker() {
  const [workouts, setWorkouts] = useState<Workout[]>([]);
  const [notes, setNotes] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [isSignedOut, setIsSignedOut] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [reloadCount, setReloadCount] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    async function loadWorkouts() {
      let response: Response;
      try {
        response = await fetch(`${apiBaseUrl}/api/workouts`, {
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

      if (response.status === 401) {
        setIsSignedOut(true);
        setIsLoading(false);
        return;
      }
      if (!response.ok) {
        setError(await readError(response));
        setIsLoading(false);
        return;
      }

      try {
        const result: unknown = await response.json();
        if (!Array.isArray(result)) {
          throw new Error("Expected a list of workout sessions.");
        }
        setWorkouts(result as Workout[]);
      } catch {
        setError("The backend returned workout history in an unexpected format.");
      }
      setIsLoading(false);
    }

    void loadWorkouts();
    return () => controller.abort();
  }, [reloadCount]);

  async function saveWorkout(path: string, body: object, successMessage: string) {
    setError("");
    setNotice("");
    setIsSaving(true);

    let response: Response;
    try {
      response = await fetch(`${apiBaseUrl}${path}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify(body),
      });
    } catch {
      setError("Could not reach the Fitbit backend. Check that Tomcat is running.");
      setIsSaving(false);
      return;
    }

    if (response.status === 401) {
      setIsSignedOut(true);
      setIsSaving(false);
      return;
    }
    if (!response.ok) {
      setError(await readError(response));
      setIsSaving(false);
      return;
    }

    setNotice(successMessage);
    if (path === "/api/workouts") {
      setNotes("");
    }
    setIsSaving(false);
    setReloadCount((count) => count + 1);
  }

  function handleStartWorkout(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void saveWorkout(
      "/api/workouts",
      { notes: notes.trim() || null },
      "Workout started.",
    );
  }

  return (
    <section className="workout-page" aria-labelledby="workout-page-heading">
      <div className="library-heading">
        <div>
          <p className="eyebrow">YOUR ACTIVITY</p>
          <h1 id="workout-page-heading">Workout tracker</h1>
          <p className="dashboard-intro">
            Start a session when you begin exercising, then mark it complete to
            keep your personal workout history.
          </p>
        </div>
      </div>

      <form className="workout-start-form" onSubmit={handleStartWorkout}>
        <label className="field">
          <span>Session note (optional)</span>
          <textarea
            maxLength={1000}
            name="notes"
            placeholder="Add a focus or note for this workout"
            rows={3}
            value={notes}
            onChange={(event) => setNotes(event.target.value)}
          />
          <span className="field-hint">Up to 1000 characters.</span>
        </label>
        <button
          className="auth-submit"
          aria-label="Start workout"
          disabled={isSaving || isLoading || isSignedOut}
          type="submit"
        >
          {isSaving ? "Saving..." : "Start a workout"}
        </button>
      </form>

      {notice && (
        <p className="library-message" role="status">
          {notice}
        </p>
      )}
      {error && (
        <p className="library-message library-error" role="alert">
          {error}
        </p>
      )}

      {isSignedOut && (
        <div className="library-message">
          <p>Please sign in with your user account to track workouts.</p>
          <Link className="secondary-link" href="/login">
            Sign in
          </Link>
        </div>
      )}

      {!isSignedOut && (
        <section className="workout-history" aria-labelledby="workout-history-heading">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">YOUR HISTORY</p>
              <h2 id="workout-history-heading">Workout sessions</h2>
            </div>
          </div>

          {isLoading && (
            <p className="library-message" role="status">
              Loading your workout history...
            </p>
          )}

          {!isLoading && !error && workouts.length === 0 && (
            <p className="library-message">
              No workouts logged yet. Start your first session when you’re ready.
            </p>
          )}

          {!isLoading && workouts.length > 0 && (
            <div className="workout-list">
              {workouts.map((workout) => (
                <article className="workout-card" key={workout.workoutId}>
                  <div>
                    <p className="workout-card-status">
                      {workout.status === "IN_PROGRESS"
                        ? "In progress"
                        : "Completed"}
                    </p>
                    <h3>
                      {workout.planId
                        ? `Workout plan #${workout.planId}`
                        : `Workout #${workout.workoutId}`}
                    </h3>
                    <p className="workout-card-date">
                      Started {formatDate(workout.startedAt)}
                    </p>
                    {workout.completedAt && (
                      <p className="workout-card-date">
                        Completed {formatDate(workout.completedAt)}
                      </p>
                    )}
                    {workout.notes && <p>{workout.notes}</p>}
                  </div>
                  {workout.status === "IN_PROGRESS" && (
                    <button
                      className="secondary-link workout-complete"
                      disabled={isSaving}
                      onClick={() =>
                        void saveWorkout(
                          "/api/workouts/complete",
                          { workoutId: workout.workoutId },
                          "Workout completed.",
                        )
                      }
                      type="button"
                    >
                      {isSaving ? "Saving..." : "Complete workout"}
                    </button>
                  )}
                </article>
              ))}
            </div>
          )}
        </section>
      )}
    </section>
  );
}
