"use client";

import Link from "next/link";
import { useEffect, useState, type FormEvent } from "react";

type FitnessGoal =
  | "WEIGHT_LOSS"
  | "MUSCLE_GAIN"
  | "GENERAL_FITNESS"
  | "STRENGTH"
  | "ENDURANCE";

type Exercise = {
  exerciseId: number;
  exerciseName: string;
  difficulty: "BEGINNER" | "INTERMEDIATE" | "ADVANCED";
};

type PlanExercise = {
  exercise: Exercise;
  exerciseOrder: number;
  setsCount: number;
  repsCount: number;
  restSeconds: number;
};

type WorkoutPlan = {
  planId: number;
  planName: string;
  description?: string | null;
  fitnessGoal: FitnessGoal;
  exercises: PlanExercise[];
};

type PlanExerciseDraft = {
  exerciseId: number;
  setsCount: number;
  repsCount: number;
  restSeconds: number;
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

function isFitnessGoal(value: unknown): value is FitnessGoal {
  return typeof value === "string" && Object.hasOwn(goalLabels, value);
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function isExercise(value: unknown): value is Exercise {
  if (!isRecord(value)) {
    return false;
  }
  return (
    typeof value.exerciseId === "number" &&
    typeof value.exerciseName === "string" &&
    (value.difficulty === "BEGINNER" ||
      value.difficulty === "INTERMEDIATE" ||
      value.difficulty === "ADVANCED")
  );
}

function isExerciseArray(value: unknown): value is Exercise[] {
  return Array.isArray(value) && value.every(isExercise);
}

function isPlanExercise(value: unknown): value is PlanExercise {
  if (!isRecord(value)) {
    return false;
  }
  return (
    typeof value.exerciseOrder === "number" &&
    typeof value.setsCount === "number" &&
    typeof value.repsCount === "number" &&
    typeof value.restSeconds === "number" &&
    isExercise(value.exercise)
  );
}

function isWorkoutPlan(value: unknown): value is WorkoutPlan {
  if (!isRecord(value)) {
    return false;
  }
  return (
    typeof value.planId === "number" &&
    typeof value.planName === "string" &&
    (typeof value.description === "string" ||
      value.description === null ||
      value.description === undefined) &&
    isFitnessGoal(value.fitnessGoal) &&
    Array.isArray(value.exercises) &&
    value.exercises.every(isPlanExercise)
  );
}

function isWorkoutPlanArray(value: unknown): value is WorkoutPlan[] {
  return Array.isArray(value) && value.every(isWorkoutPlan);
}

async function readError(response: Response, fallback: string) {
  try {
    const result = (await response.json()) as ApiError;
    return result.error ?? fallback;
  } catch {
    return "The backend returned a response that could not be read.";
  }
}

/** Builds and displays reusable workout plans owned by the signed-in trainer. */
export default function TrainerPlans() {
  const [exercises, setExercises] = useState<Exercise[]>([]);
  const [plans, setPlans] = useState<WorkoutPlan[]>([]);
  const [draft, setDraft] = useState<PlanExerciseDraft[]>([]);
  const [exerciseToAdd, setExerciseToAdd] = useState("");
  const [planName, setPlanName] = useState("");
  const [description, setDescription] = useState("");
  const [fitnessGoal, setFitnessGoal] = useState<FitnessGoal>("GENERAL_FITNESS");
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [isSignedOut, setIsSignedOut] = useState(false);
  const [isNotTrainer, setIsNotTrainer] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  useEffect(() => {
    const controller = new AbortController();

    async function loadTrainerPlans() {
      try {
        const profileResponse = await fetch(`${apiBaseUrl}/api/auth/me`, {
          credentials: "include",
          signal: controller.signal,
        });
        if (profileResponse.status === 401) {
          setIsSignedOut(true);
          setIsLoading(false);
          return;
        }
        if (!profileResponse.ok) {
          setError(await readError(profileResponse, "Account information could not be loaded."));
          setIsLoading(false);
          return;
        }
        const profile: unknown = await profileResponse.json();
        if (
          typeof profile !== "object" ||
          profile === null ||
          !("role" in profile) ||
          profile.role !== "TRAINER"
        ) {
          setIsNotTrainer(true);
          setIsLoading(false);
          return;
        }

        const [exerciseResponse, plansResponse] = await Promise.all([
          fetch(`${apiBaseUrl}/api/exercises`, {
            credentials: "include",
            signal: controller.signal,
          }),
          fetch(`${apiBaseUrl}/api/plans`, {
            credentials: "include",
            signal: controller.signal,
          }),
        ]);
        const failedResponse = [exerciseResponse, plansResponse].find(
          (response) => !response.ok,
        );
        if (failedResponse) {
          setError(await readError(failedResponse, "Trainer plans could not be loaded."));
          setIsLoading(false);
          return;
        }
        const [exerciseResult, planResult]: unknown[] = await Promise.all([
          exerciseResponse.json(),
          plansResponse.json(),
        ]);
        if (!isExerciseArray(exerciseResult) || !isWorkoutPlanArray(planResult)) {
          throw new Error("Unexpected trainer plan response format.");
        }
        setExercises(exerciseResult);
        setPlans(planResult);
        setIsLoading(false);
      } catch (loadError) {
        if (
          loadError instanceof DOMException &&
          loadError.name === "AbortError"
        ) {
          return;
        }
        setError(
          loadError instanceof Error &&
            loadError.message === "Unexpected trainer plan response format."
            ? "The backend returned trainer plan data in an unexpected format."
            : "Could not reach the Fitbit backend. Check that Tomcat is running.",
        );
        setIsLoading(false);
      }
    }

    void loadTrainerPlans();
    return () => controller.abort();
  }, []);

  function addExercise() {
    const exerciseId = Number(exerciseToAdd);
    if (!exerciseId || draft.some((item) => item.exerciseId === exerciseId)) {
      return;
    }
    setDraft((items) => [
      ...items,
      { exerciseId, setsCount: 3, repsCount: 10, restSeconds: 60 },
    ]);
    setExerciseToAdd("");
    setError("");
  }

  function updateDraft(
    exerciseId: number,
    field: keyof Omit<PlanExerciseDraft, "exerciseId">,
    value: number,
  ) {
    setDraft((items) =>
      items.map((item) =>
        item.exerciseId === exerciseId ? { ...item, [field]: value } : item,
      ),
    );
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setNotice("");
    setIsSaving(true);
    let planCreated = false;
    try {
      const response = await fetch(`${apiBaseUrl}/api/plans`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({
          planName,
          description,
          fitnessGoal,
          exercises: draft,
        }),
      });
      if (!response.ok) {
        setError(await readError(response, "Workout plan could not be saved."));
        setIsSaving(false);
        return;
      }
      // Record success before refreshing; a refresh error must not imply the save failed.
      planCreated = true;
      setPlanName("");
      setDescription("");
      setFitnessGoal("GENERAL_FITNESS");
      setDraft([]);
      setNotice("Workout plan created.");

      const plansResponse = await fetch(`${apiBaseUrl}/api/plans`, {
        credentials: "include",
      });
      if (!plansResponse.ok) {
        setError(await readError(
          plansResponse,
          "The plan was created, but the list could not be refreshed. Reload this page to see it.",
        ));
        setIsSaving(false);
        return;
      }
      const result: unknown = await plansResponse.json();
      if (!isWorkoutPlanArray(result)) {
        setError(
          "The plan was created, but the plan list returned unexpected data. Reload this page to see it.",
        );
        setIsSaving(false);
        return;
      }
      setPlans(result);
    } catch {
      setError(planCreated
        ? "The plan was created, but the plan list could not be refreshed. Reload this page to see it."
        : "Could not reach the Fitbit backend. Check that Tomcat is running.");
    }
    setIsSaving(false);
  }

  return (
    <section className="trainer-plans" aria-labelledby="trainer-plans-heading">
      <div className="library-heading">
        <div>
          <p className="eyebrow">TRAINER TOOLS</p>
          <h1 id="trainer-plans-heading">Workout plans</h1>
          <p className="dashboard-intro">
            Build reusable routines by selecting exercises and setting their
            sets, reps, and rest time.
          </p>
        </div>
        <Link className="secondary-link" href="/dashboard">
          Back to dashboard
        </Link>
      </div>

      {isLoading && <p className="library-message" role="status">Loading trainer tools...</p>}
      {isSignedOut && (
        <div className="library-message">
          <p>Sign in with a trainer account to create workout plans.</p>
          <Link className="secondary-link" href="/login">Sign in</Link>
        </div>
      )}
      {isNotTrainer && (
        <div className="library-message">
          <p>Workout-plan creation is available to trainer accounts.</p>
          <Link className="secondary-link" href="/dashboard">Back to dashboard</Link>
        </div>
      )}
      {error && <p className="library-message library-error" role="alert">{error}</p>}

      {!isLoading && !isSignedOut && !isNotTrainer && (
        <>
          <form className="trainer-plan-form" onSubmit={handleSubmit}>
            <label className="field">
              <span>Plan name</span>
              <input
                maxLength={120}
                onChange={(event) => setPlanName(event.target.value)}
                required
                value={planName}
              />
            </label>
            <label className="field">
              <span>Fitness goal</span>
              <select
                onChange={(event) => {
                  const value = event.target.value;
                  if (isFitnessGoal(value)) {
                    setFitnessGoal(value);
                  }
                }}
                value={fitnessGoal}
              >
                {Object.entries(goalLabels).map(([value, label]) => (
                  <option key={value} value={value}>{label}</option>
                ))}
              </select>
            </label>
            <label className="field trainer-plan-description">
              <span>Description (optional)</span>
              <textarea
                maxLength={5000}
                onChange={(event) => setDescription(event.target.value)}
                rows={3}
                value={description}
              />
            </label>

            <div className="trainer-exercise-picker">
              <label className="field">
                <span>Add an exercise</span>
                <select
                  onChange={(event) => setExerciseToAdd(event.target.value)}
                  value={exerciseToAdd}
                >
                  <option value="">Choose an exercise</option>
                  {exercises
                    .filter((exercise) => !draft.some(
                      (item) => item.exerciseId === exercise.exerciseId,
                    ))
                    .map((exercise) => (
                      <option key={exercise.exerciseId} value={exercise.exerciseId}>
                        {exercise.exerciseName}
                      </option>
                    ))}
                </select>
              </label>
              <button
                className="secondary-link"
                disabled={!exerciseToAdd || draft.length >= 30}
                onClick={addExercise}
                type="button"
              >
                Add exercise
              </button>
            </div>

            {draft.length === 0 ? (
              <p className="library-message">Add at least one exercise to your plan.</p>
            ) : (
              <div className="trainer-plan-exercise-list">
                {draft.map((item, index) => {
                  const exercise = exercises.find(
                    (candidate) => candidate.exerciseId === item.exerciseId,
                  );
                  return (
                    <div className="trainer-plan-exercise" key={item.exerciseId}>
                      <div className="trainer-plan-exercise-title">
                        <span>{index + 1}.</span>
                        <strong>{exercise?.exerciseName}</strong>
                        <button
                          className="remove-plan-exercise"
                          onClick={() => setDraft((items) =>
                            items.filter((candidate) =>
                              candidate.exerciseId !== item.exerciseId,
                            ),
                          )}
                          type="button"
                        >
                          Remove
                        </button>
                      </div>
                      <label className="field">
                        <span>Sets</span>
                        <input
                          max={255}
                          min={1}
                          onChange={(event) => updateDraft(
                            item.exerciseId, "setsCount", Number(event.target.value),
                          )}
                          required
                          type="number"
                          value={item.setsCount}
                        />
                      </label>
                      <label className="field">
                        <span>Reps</span>
                        <input
                          max={65535}
                          min={1}
                          onChange={(event) => updateDraft(
                            item.exerciseId, "repsCount", Number(event.target.value),
                          )}
                          required
                          type="number"
                          value={item.repsCount}
                        />
                      </label>
                      <label className="field">
                        <span>Rest (seconds)</span>
                        <input
                          max={65535}
                          min={0}
                          onChange={(event) => updateDraft(
                            item.exerciseId, "restSeconds", Number(event.target.value),
                          )}
                          required
                          type="number"
                          value={item.restSeconds}
                        />
                      </label>
                    </div>
                  );
                })}
              </div>
            )}

            <button
              className="auth-submit"
              disabled={isSaving || draft.length === 0 || exercises.length === 0}
              type="submit"
            >
              {isSaving ? "Saving plan..." : "Create workout plan"}
            </button>
            {notice && <p className="library-message" role="status">{notice}</p>}
          </form>

          <section className="trainer-plan-history" aria-labelledby="created-plans-heading">
            <h2 id="created-plans-heading">Your plans</h2>
            {plans.length === 0 ? (
              <p className="library-message">You haven’t created a plan yet.</p>
            ) : (
              <div className="trainer-plan-list">
                {plans.map((plan) => (
                  <article className="trainer-plan-card" key={plan.planId}>
                    <p className="eyebrow">{goalLabels[plan.fitnessGoal]}</p>
                    <h3>{plan.planName}</h3>
                    {plan.description && <p>{plan.description}</p>}
                    <ol>
                      {plan.exercises.map((item) => (
                        <li key={item.exerciseOrder}>
                          {item.exercise.exerciseName} — {item.setsCount} sets ×{" "}
                          {item.repsCount} reps, {item.restSeconds}s rest
                        </li>
                      ))}
                    </ol>
                  </article>
                ))}
              </div>
            )}
          </section>
        </>
      )}
    </section>
  );
}
