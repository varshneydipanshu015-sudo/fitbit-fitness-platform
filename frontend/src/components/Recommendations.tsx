"use client";

import { useEffect, useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";

type FitnessGoal =
  | "WEIGHT_LOSS"
  | "MUSCLE_GAIN"
  | "GENERAL_FITNESS"
  | "STRENGTH"
  | "ENDURANCE";

type FitnessLevel = "BEGINNER" | "INTERMEDIATE" | "ADVANCED";

type RecommendationResponse = {
  goal?: FitnessGoal;
  level?: FitnessLevel;
  exercises?: string[];
  error?: string;
};

type SavedPreferences = {
  fitnessGoal?: string;
  fitnessLevel?: string;
};

type ApiError = {
  error?: string;
};

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/fitbit-app";

const goalLabels: Record<FitnessGoal, string> = {
  WEIGHT_LOSS: "Weight loss",
  MUSCLE_GAIN: "Muscle gain",
  GENERAL_FITNESS: "General fitness",
  STRENGTH: "Strength",
  ENDURANCE: "Endurance",
};

const levelLabels: Record<FitnessLevel, string> = {
  BEGINNER: "Beginner",
  INTERMEDIATE: "Intermediate",
  ADVANCED: "Advanced",
};

function isFitnessGoal(value: string): value is FitnessGoal {
  return Object.hasOwn(goalLabels, value);
}

function isFitnessLevel(value: string): value is FitnessLevel {
  return Object.hasOwn(levelLabels, value);
}

/** Loads saved preferences, requests suggestions, and optionally starts a session. */
export default function Recommendations() {
  const router = useRouter();
  const [goal, setGoal] = useState<FitnessGoal>("GENERAL_FITNESS");
  const [level, setLevel] = useState<FitnessLevel>("BEGINNER");
  const [recommendations, setRecommendations] = useState<string[]>([]);
  const [error, setError] = useState("");
  const [preferenceNotice, setPreferenceNotice] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [isLoadingPreferences, setIsLoadingPreferences] = useState(true);
  const [isStartingWorkout, setIsStartingWorkout] = useState(false);
  const [needsSignIn, setNeedsSignIn] = useState(false);

  useEffect(() => {
    const controller = new AbortController();

    async function loadSavedPreferences() {
      try {
        const response = await fetch(`${apiBaseUrl}/api/auth/me`, {
          credentials: "include",
          signal: controller.signal,
        });

        if (response.status === 401) {
          return;
        }
        if (!response.ok) {
          setPreferenceNotice(
            "Could not load saved profile preferences. Choose your goal and level below.",
          );
          return;
        }

        const result: unknown = await response.json();
        if (
          typeof result !== "object" ||
          result === null ||
          !("fitnessGoal" in result) ||
          !("fitnessLevel" in result)
        ) {
          setPreferenceNotice(
            "Saved preferences were unavailable. Choose your goal and level below.",
          );
          return;
        }

        const preferences = result as SavedPreferences;
        if (
          preferences.fitnessGoal &&
          isFitnessGoal(preferences.fitnessGoal) &&
          preferences.fitnessLevel &&
          isFitnessLevel(preferences.fitnessLevel)
        ) {
          setGoal(preferences.fitnessGoal);
          setLevel(preferences.fitnessLevel);
        } else {
          setPreferenceNotice(
            "Saved preferences were unavailable. Choose your goal and level below.",
          );
        }
      } catch (fetchError) {
        if (
          fetchError instanceof DOMException &&
          fetchError.name === "AbortError"
        ) {
          return;
        }
        setPreferenceNotice(
          "Could not load saved profile preferences. Choose your goal and level below.",
        );
      } finally {
        setIsLoadingPreferences(false);
      }
    }

    void loadSavedPreferences();
    return () => controller.abort();
  }, []);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setNeedsSignIn(false);
    setRecommendations([]);
    setIsLoading(true);

    const url = new URL(`${apiBaseUrl}/api/recommendations`);
    url.searchParams.set("goal", goal);
    url.searchParams.set("level", level);

    let response: Response;
    try {
      response = await fetch(url);
    } catch {
      setError("Could not reach the Fitbit backend. Check that Tomcat is running.");
      setIsLoading(false);
      return;
    }

    let result: RecommendationResponse;
    try {
      result = (await response.json()) as RecommendationResponse;
    } catch {
      setError("The backend returned a response that could not be read.");
      setIsLoading(false);
      return;
    }

    if (!response.ok) {
      setError(result.error ?? "Recommendations could not be loaded.");
      setIsLoading(false);
      return;
    }
    if (!Array.isArray(result.exercises)) {
      setError("The backend returned recommendations in an unexpected format.");
      setIsLoading(false);
      return;
    }

    setRecommendations(result.exercises);
    setIsLoading(false);
  }

  async function handleStartWorkout() {
    setError("");
    setNeedsSignIn(false);
    setIsStartingWorkout(true);

    let response: Response;
    try {
      response = await fetch(`${apiBaseUrl}/api/workouts`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({
          notes: `Recommended for ${goalLabels[goal]} (${levelLabels[level]}): ${recommendations.join(", ")}`,
        }),
      });
    } catch {
      setError("Could not reach the Fitbit backend. Check that Tomcat is running.");
      setIsStartingWorkout(false);
      return;
    }

    if (response.status === 401) {
      setNeedsSignIn(true);
      setIsStartingWorkout(false);
      return;
    }
    if (!response.ok) {
      let message = "The recommended workout could not be started.";
      try {
        const result = (await response.json()) as ApiError;
        message = result.error ?? message;
      } catch {
        message = "The backend returned a response that could not be read.";
      }
      setError(message);
      setIsStartingWorkout(false);
      return;
    }

    router.push("/workouts");
    router.refresh();
  }

  return (
    <section className="recommendation-page" aria-labelledby="recommendation-heading">
      <div className="library-heading">
        <div>
          <p className="eyebrow">BUILT AROUND YOUR GOAL</p>
          <h1 id="recommendation-heading">Workout recommendations</h1>
          <p className="dashboard-intro">
            Choose a goal and fitness level to get a simple, rule-based exercise
            suggestion from the Java backend.
          </p>
        </div>
      </div>

      <form className="recommendation-form" onSubmit={handleSubmit}>
        <label className="field">
          <span>Fitness goal</span>
          <select
            onChange={(event) => {
              const value = event.target.value;
              if (isFitnessGoal(value)) {
                setGoal(value);
              }
            }}
            value={goal}
          >
            {Object.entries(goalLabels).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </label>
        <label className="field">
          <span>Fitness level</span>
          <select
            onChange={(event) => {
              const value = event.target.value;
              if (isFitnessLevel(value)) {
                setLevel(value);
              }
            }}
            value={level}
          >
            {Object.entries(levelLabels).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </label>
        <button
          className="auth-submit"
          disabled={isLoading || isLoadingPreferences}
          type="submit"
        >
          {isLoadingPreferences
            ? "Loading preferences..."
            : isLoading
              ? "Finding exercises..."
              : "Get recommendations"}
        </button>
      </form>

      {preferenceNotice && (
        <p className="library-message" role="status">
          {preferenceNotice}
        </p>
      )}

      {error && (
        <p className="library-message library-error" role="alert">
          {error}
        </p>
      )}
      {needsSignIn && (
        <div className="library-message">
          <p>Sign in to save this recommendation as a workout session.</p>
          <Link className="secondary-link" href="/login">
            Sign in
          </Link>
        </div>
      )}

      {recommendations.length > 0 && (
        <section className="recommendation-results" aria-live="polite">
          <p className="eyebrow">
            {goalLabels[goal].toUpperCase()} · {levelLabels[level].toUpperCase()}
          </p>
          <h2>Your suggested exercises</h2>
          <ol className="recommendation-list">
            {recommendations.map((exercise, index) => (
              <li key={`${exercise}-${index}`}>
                <span className="recommendation-number">
                  {String(index + 1).padStart(2, "0")}
                </span>
                <span>{exercise}</span>
              </li>
            ))}
          </ol>
          <p className="recommendation-note">
            These are general suggestions, not medical advice. Choose movements
            that are appropriate for you and stop if you feel pain.
          </p>
          <button
            className="auth-submit recommendation-start"
            disabled={isStartingWorkout}
            onClick={() => void handleStartWorkout()}
            type="button"
          >
            {isStartingWorkout ? "Starting workout..." : "Start this workout"}
          </button>
        </section>
      )}
    </section>
  );
}
