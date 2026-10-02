"use client";

import { useEffect, useMemo, useState } from "react";

type Exercise = {
  exerciseId: number;
  exerciseName: string;
  description: string | null;
  targetMuscle: string | null;
  difficulty: "BEGINNER" | "INTERMEDIATE" | "ADVANCED";
  equipment: string;
};

type ExerciseApiError = {
  error?: string;
};

const difficultyLabels: Record<Exercise["difficulty"], string> = {
  BEGINNER: "Beginner",
  INTERMEDIATE: "Intermediate",
  ADVANCED: "Advanced",
};

/** Fetches the exercise catalog and filters it locally for a responsive search. */
export default function ExerciseLibrary() {
  const [exercises, setExercises] = useState<Exercise[]>([]);
  const [query, setQuery] = useState("");
  const [difficulty, setDifficulty] = useState("ALL");
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [retryCount, setRetryCount] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    async function loadExercises() {
      const apiBaseUrl =
        process.env.NEXT_PUBLIC_API_BASE_URL ??
        "http://localhost:8080/fitbit-app";

      let response: Response;
      try {
        response = await fetch(`${apiBaseUrl}/api/exercises`, {
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
        setError(
          "Could not reach the Fitbit backend. Make sure Tomcat is running and the backend URL is correct.",
        );
        setIsLoading(false);
        return;
      }

      if (!response.ok) {
        let responseError: ExerciseApiError = {};
        try {
          responseError = (await response.json()) as ExerciseApiError;
        } catch {
          setError("The backend returned a response that could not be read.");
          setIsLoading(false);
          return;
        }
        setError(responseError.error ?? "The exercise library could not be loaded.");
        setIsLoading(false);
        return;
      }

      let result: Exercise[];
      try {
        result = (await response.json()) as Exercise[];
        if (!Array.isArray(result)) {
          throw new Error("Expected the exercise API to return a list.");
        }
      } catch {
        setError("The backend returned exercise data in an unexpected format.");
        setIsLoading(false);
        return;
      }

      setExercises(result);
      setIsLoading(false);
    }

    void loadExercises();
    return () => controller.abort();
  }, [retryCount]);

  const filteredExercises = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();
    return exercises.filter((exercise) => {
      const matchesDifficulty =
        difficulty === "ALL" || exercise.difficulty === difficulty;
      const searchableText = [
        exercise.exerciseName,
        exercise.description ?? "",
        exercise.targetMuscle ?? "",
        exercise.equipment,
      ]
        .join(" ")
        .toLowerCase();
      return matchesDifficulty && searchableText.includes(normalizedQuery);
    });
  }, [difficulty, exercises, query]);

  return (
    <>
      <div className="library-heading">
        <div>
          <p className="eyebrow">MOVE WITH PURPOSE</p>
          <h1 id="exercises-heading">Exercise library</h1>
          <p className="dashboard-intro">
            Explore movements and find exercises that fit your routine.
          </p>
        </div>
        {!isLoading && !error && (
          <p className="library-count">
            {filteredExercises.length}{" "}
            {filteredExercises.length === 1 ? "exercise" : "exercises"}
          </p>
        )}
      </div>

      <div className="library-filters" aria-label="Filter exercises">
        <label className="field">
          <span>Search</span>
          <input
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Name, muscle, or equipment"
            type="search"
            value={query}
          />
        </label>
        <label className="field">
          <span>Difficulty</span>
          <select
            onChange={(event) => setDifficulty(event.target.value)}
            value={difficulty}
          >
            <option value="ALL">All levels</option>
            <option value="BEGINNER">Beginner</option>
            <option value="INTERMEDIATE">Intermediate</option>
            <option value="ADVANCED">Advanced</option>
          </select>
        </label>
      </div>

      {isLoading && (
        <p className="library-message" role="status">
          Loading exercises...
        </p>
      )}

      {!isLoading && error && (
        <div className="library-message library-error" role="alert">
          <p>{error}</p>
          <button
            className="secondary-link retry-button"
            onClick={() => {
              setError("");
              setIsLoading(true);
              setRetryCount((count) => count + 1);
            }}
            type="button"
          >
            Try again
          </button>
        </div>
      )}

      {!isLoading && !error && exercises.length === 0 && (
        <p className="library-message">
          No exercises are in the database yet. Add exercises to MySQL to
          populate the library.
        </p>
      )}

      {!isLoading && !error && exercises.length > 0 && filteredExercises.length === 0 && (
        <p className="library-message">
          No exercises match these filters. Try a different search or difficulty.
        </p>
      )}

      {!isLoading && !error && filteredExercises.length > 0 && (
        <div className="exercise-grid">
          {filteredExercises.map((exercise) => (
            <article className="exercise-card" key={exercise.exerciseId}>
              <div className="exercise-card-heading">
                <span className="exercise-icon" aria-hidden="true">
                  {exercise.exerciseName.slice(0, 1).toUpperCase()}
                </span>
                <span className="difficulty-badge">
                  {difficultyLabels[exercise.difficulty]}
                </span>
              </div>
              <h2>{exercise.exerciseName}</h2>
              <p className="exercise-description">
                {exercise.description || "No description has been added yet."}
              </p>
              <dl className="exercise-details">
                <div>
                  <dt>Target</dt>
                  <dd>{exercise.targetMuscle || "Not specified"}</dd>
                </div>
                <div>
                  <dt>Equipment</dt>
                  <dd>{exercise.equipment}</dd>
                </div>
              </dl>
            </article>
          ))}
        </div>
      )}
    </>
  );
}
