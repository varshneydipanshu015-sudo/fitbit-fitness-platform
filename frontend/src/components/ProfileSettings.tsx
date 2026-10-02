"use client";

import Link from "next/link";
import { useEffect, useState, type FormEvent } from "react";

type FitnessGoal =
  | "WEIGHT_LOSS"
  | "MUSCLE_GAIN"
  | "GENERAL_FITNESS"
  | "STRENGTH"
  | "ENDURANCE";

type FitnessLevel = "BEGINNER" | "INTERMEDIATE" | "ADVANCED";

type Profile = {
  role: "USER";
  fullName: string;
  email: string;
  fitnessGoal: FitnessGoal;
  fitnessLevel: FitnessLevel;
};

type TrainerProfile = {
  role: "TRAINER";
  fullName: string;
  email: string;
  bio: string | null;
  specialization: string | null;
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

function isProfile(value: unknown): value is Profile {
  if (typeof value !== "object" || value === null) {
    return false;
  }

  const profile = value as Record<string, unknown>;
  return (
    profile.role === "USER" &&
    typeof profile.fullName === "string" &&
    typeof profile.email === "string" &&
    typeof profile.fitnessGoal === "string" &&
    Object.hasOwn(goalLabels, profile.fitnessGoal) &&
    typeof profile.fitnessLevel === "string" &&
    Object.hasOwn(levelLabels, profile.fitnessLevel)
  );
}

function isTrainerProfile(value: unknown): value is TrainerProfile {
  if (typeof value !== "object" || value === null) {
    return false;
  }
  const profile = value as Record<string, unknown>;
  return (
    profile.role === "TRAINER" &&
    typeof profile.fullName === "string" &&
    typeof profile.email === "string" &&
    (typeof profile.bio === "string" || profile.bio === null) &&
    (typeof profile.specialization === "string" ||
      profile.specialization === null)
  );
}

async function readError(response: Response) {
  try {
    const result = (await response.json()) as ApiError;
    return result.error ?? "Profile settings could not be loaded.";
  } catch {
    return "The backend returned a response that could not be read.";
  }
}

export default function ProfileSettings() {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [trainerProfile, setTrainerProfile] = useState<TrainerProfile | null>(null);
  const [fitnessGoal, setFitnessGoal] = useState<FitnessGoal>("GENERAL_FITNESS");
  const [fitnessLevel, setFitnessLevel] = useState<FitnessLevel>("BEGINNER");
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [isSignedOut, setIsSignedOut] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  useEffect(() => {
    const controller = new AbortController();

    async function loadProfile() {
      let response: Response;
      try {
        response = await fetch(`${apiBaseUrl}/api/auth/me`, {
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
        if (isTrainerProfile(result)) {
          setTrainerProfile(result);
        } else if (isProfile(result)) {
          setProfile(result);
          setFitnessGoal(result.fitnessGoal);
          setFitnessLevel(result.fitnessLevel);
        } else {
          throw new Error("Unexpected profile response.");
        }
      } catch {
        setError("The backend returned profile information in an unexpected format.");
      }
      setIsLoading(false);
    }

    void loadProfile();
    return () => controller.abort();
  }, []);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setNotice("");
    setIsSaving(true);

    let response: Response;
    try {
      response = await fetch(`${apiBaseUrl}/api/auth/me`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ fitnessGoal, fitnessLevel }),
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

    try {
      const result: unknown = await response.json();
      if (!isProfile(result)) {
        throw new Error("Unexpected profile response.");
      }
      setProfile(result);
      setFitnessGoal(result.fitnessGoal);
      setFitnessLevel(result.fitnessLevel);
      setNotice("Your fitness preferences were updated.");
    } catch {
      setError("The backend returned profile information in an unexpected format.");
    }
    setIsSaving(false);
  }

  return (
    <section className="profile-page" aria-labelledby="profile-heading">
      <div className="library-heading">
        <div>
          <p className="eyebrow">YOUR ACCOUNT</p>
          <h1 id="profile-heading">Profile settings</h1>
          <p className="dashboard-intro">
            Manage your account details and fitness preferences.
          </p>
        </div>
      </div>

      {isLoading && (
        <p className="library-message" role="status">
          Loading your profile...
        </p>
      )}

      {isSignedOut && (
        <div className="library-message">
          <p>Sign in to view and update your profile settings.</p>
          <Link className="secondary-link" href="/login">
            Sign in
          </Link>
        </div>
      )}

      {!isLoading && !isSignedOut && profile && (
        <>
          <div className="profile-summary">
            <p className="eyebrow">ACCOUNT DETAILS</p>
            <h2>{profile.fullName}</h2>
            <p>{profile.email}</p>
          </div>

          <form className="profile-form" onSubmit={handleSubmit}>
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
                    setFitnessLevel(value);
                  }
                }}
                value={fitnessLevel}
              >
                {Object.entries(levelLabels).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </label>
            <button className="auth-submit" disabled={isSaving} type="submit">
              {isSaving ? "Saving..." : "Save preferences"}
            </button>
          </form>
        </>
      )}

      {!isLoading && !isSignedOut && trainerProfile && (
        <div className="profile-summary">
          <p className="eyebrow">TRAINER ACCOUNT</p>
          <h2>{trainerProfile.fullName}</h2>
          <p>{trainerProfile.email}</p>
          {trainerProfile.specialization && (
            <p>Specialization: {trainerProfile.specialization}</p>
          )}
          {trainerProfile.bio && <p>{trainerProfile.bio}</p>}
        </div>
      )}

      {error && (
        <p className="library-message library-error" role="alert">
          {error}
        </p>
      )}
      {notice && (
        <p className="library-message" role="status">
          {notice}
        </p>
      )}
    </section>
  );
}
