"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

type PlanExercise = {
  exercise: {
    exerciseId: number;
    exerciseName: string;
    targetMuscle?: string | null;
    equipment: string;
  };
  exerciseOrder: number;
  setsCount: number;
  repsCount: number;
  restSeconds: number;
};

type WorkoutPlan = {
  planId: number;
  trainerName: string;
  planName: string;
  description?: string | null;
  fitnessGoal: string;
  exercises: PlanExercise[];
};

type ApiError = {
  error?: string;
};

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/fitbit-app";

const goalLabels: Record<string, string> = {
  WEIGHT_LOSS: "Weight loss",
  MUSCLE_GAIN: "Muscle gain",
  GENERAL_FITNESS: "General fitness",
  STRENGTH: "Strength",
  ENDURANCE: "Endurance",
};

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function isPlanExercise(value: unknown): value is PlanExercise {
  if (!isRecord(value) || !isRecord(value.exercise)) {
    return false;
  }
  return (
    typeof value.exerciseOrder === "number" &&
    typeof value.setsCount === "number" &&
    typeof value.repsCount === "number" &&
    typeof value.restSeconds === "number" &&
    typeof value.exercise.exerciseId === "number" &&
    typeof value.exercise.exerciseName === "string" &&
    (typeof value.exercise.targetMuscle === "string" ||
      value.exercise.targetMuscle === null ||
      value.exercise.targetMuscle === undefined) &&
    typeof value.exercise.equipment === "string"
  );
}

function isWorkoutPlan(value: unknown): value is WorkoutPlan {
  return (
    isRecord(value) &&
    typeof value.planId === "number" &&
    typeof value.trainerName === "string" &&
    typeof value.planName === "string" &&
    (typeof value.description === "string" ||
      value.description === null ||
      value.description === undefined) &&
    typeof value.fitnessGoal === "string" &&
    Object.hasOwn(goalLabels, value.fitnessGoal) &&
    Array.isArray(value.exercises) &&
    value.exercises.every(isPlanExercise)
  );
}

function isWorkoutPlanArray(value: unknown): value is WorkoutPlan[] {
  return Array.isArray(value) && value.every(isWorkoutPlan);
}

async function readError(response: Response) {
  try {
    const result = (await response.json()) as ApiError;
    return result.error ?? "Workout plans could not be loaded.";
  } catch {
    return "The backend returned a response that could not be read.";
  }
}

/** Lets fitness users inspect trainer plans and create linked workout sessions. */
export default function PlanBrowser() {
  const router = useRouter();
  const [plans, setPlans] = useState<WorkoutPlan[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSignedOut, setIsSignedOut] = useState(false);
  const [isNotUser, setIsNotUser] = useState(false);
  const [startingPlanId, setStartingPlanId] = useState<number | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();

    async function loadPlans() {
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
          setError(await readError(profileResponse));
          setIsLoading(false);
          return;
        }
        const profile: unknown = await profileResponse.json();
        if (
          !isRecord(profile) ||
          profile.role !== "USER"
        ) {
          setIsNotUser(true);
          setIsLoading(false);
          return;
        }

        const response = await fetch(`${apiBaseUrl}/api/plans`, {
          credentials: "include",
          signal: controller.signal,
        });
        if (!response.ok) {
          setError(await readError(response));
          setIsLoading(false);
          return;
        }
        const result: unknown = await response.json();
        if (!isWorkoutPlanArray(result)) {
          throw new Error("The backend returned workout plans in an unexpected format.");
        }
        setPlans(result);
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
            loadError.message.includes("unexpected format")
            ? loadError.message
            : "Could not reach the Fitbit backend. Check that Tomcat is running.",
        );
        setIsLoading(false);
      }
    }

    void loadPlans();
    return () => controller.abort();
  }, []);

  async function startPlan(plan: WorkoutPlan) {
    setError("");
    setStartingPlanId(plan.planId);
    try {
      // Starting a plan uses the existing workout endpoint so history stays unified.
      const response = await fetch(`${apiBaseUrl}/api/workouts`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ planId: plan.planId }),
      });
      if (response.status === 401) {
        setIsSignedOut(true);
        setStartingPlanId(null);
        return;
      }
      if (!response.ok) {
        setError(await readError(response));
        setStartingPlanId(null);
        return;
      }
      router.push("/workouts");
    } catch {
      setError("Could not reach the Fitbit backend. Check that Tomcat is running.");
      setStartingPlanId(null);
    }
  }

  return (
    <section className="plan-browser" aria-labelledby="plan-browser-heading">
      <div className="library-heading">
        <div>
          <p className="eyebrow">TRAINER-CREATED ROUTINES</p>
          <h1 id="plan-browser-heading">Workout plans</h1>
          <p className="dashboard-intro">
            Explore routines created by trainers and start one to add it to your
            workout history.
          </p>
        </div>
      </div>

      {isLoading && (
        <p className="library-message" role="status">Loading workout plans...</p>
      )}
      {isSignedOut && (
        <div className="library-message">
          <p>Sign in with a fitness user account to browse and start plans.</p>
          <Link className="secondary-link" href="/login">Sign in</Link>
        </div>
      )}
      {isNotUser && (
        <div className="library-message">
          <p>Plan browsing is available to fitness user accounts.</p>
          <Link className="secondary-link" href="/dashboard">Back to dashboard</Link>
        </div>
      )}
      {error && <p className="library-message library-error" role="alert">{error}</p>}

      {!isLoading && !isSignedOut && !isNotUser && plans.length === 0 && (
        <p className="library-message">
          No trainer plans are available yet. Check back after trainers publish
          their first routines.
        </p>
      )}

      {!isLoading && !isSignedOut && !isNotUser && plans.length > 0 && (
        <div className="trainer-plan-list">
          {plans.map((plan) => (
            <article className="trainer-plan-card" key={plan.planId}>
              <p className="eyebrow">
                {goalLabels[plan.fitnessGoal]} · By {plan.trainerName}
              </p>
              <h2>{plan.planName}</h2>
              {plan.description && <p>{plan.description}</p>}
              <ol>
                {plan.exercises.map((item) => (
                  <li key={item.exerciseOrder}>
                    <strong>{item.exercise.exerciseName}</strong> —{" "}
                    {item.setsCount} sets × {item.repsCount} reps,{" "}
                    {item.restSeconds}s rest
                    {item.exercise.targetMuscle &&
                      ` · ${item.exercise.targetMuscle}`}
                  </li>
                ))}
              </ol>
              <button
                className="primary-link plan-start-button"
                disabled={startingPlanId !== null}
                onClick={() => void startPlan(plan)}
                type="button"
              >
                {startingPlanId === plan.planId
                  ? "Starting workout..."
                  : "Start this plan"}
              </button>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
