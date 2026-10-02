"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

type Profile = {
  fullName: string;
};

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/fitbit-app";

/** Personalizes the landing page when a valid session is present. */
export default function HomeWelcome() {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [isCheckingSession, setIsCheckingSession] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();

    async function loadProfile() {
      try {
        const response = await fetch(`${apiBaseUrl}/api/auth/me`, {
          credentials: "include",
          signal: controller.signal,
        });

        if (response.status === 401) {
          return;
        }
        if (!response.ok) {
          setError("Account status is temporarily unavailable.");
          return;
        }

        const result: unknown = await response.json();
        if (
          typeof result === "object" &&
          result !== null &&
          "fullName" in result &&
          typeof result.fullName === "string"
        ) {
          setProfile({ fullName: result.fullName });
        } else {
          setError("The backend returned unexpected account information.");
        }
      } catch (fetchError) {
        if (
          fetchError instanceof DOMException &&
          fetchError.name === "AbortError"
        ) {
          return;
        }
        setError("Could not check your account status.");
      } finally {
        setIsCheckingSession(false);
      }
    }

    void loadProfile();
    return () => controller.abort();
  }, []);

  return (
    <section className="welcome-card" aria-labelledby="welcome-heading">
      <p className="eyebrow">A STRONGER ROUTINE STARTS HERE</p>
      <h1 id="welcome-heading">
        {profile
          ? `Welcome back, ${profile.fullName}.`
          : "Your fitness journey, made personal."}
      </h1>
      <p className="welcome-copy">
        Explore exercises, get workout suggestions for your fitness goal, log
        sessions, and keep track of your personal progress.
      </p>
      <p className="setup-note">
        {profile
          ? "Your account is ready. Review your activity and fitness summary on your dashboard."
          : "Create an account to save your fitness preferences, build a workout history, and see your dashboard in one place."}
      </p>
      <div className="welcome-actions">
        {profile ? (
          <Link className="primary-link" href="/dashboard">
            Go to dashboard
          </Link>
        ) : (
          <>
            {!isCheckingSession && (
              <>
                <Link className="primary-link" href="/register">
                  Create your account
                </Link>
                <Link className="secondary-link" href="/login">
                  Sign in
                </Link>
              </>
            )}
          </>
        )}
        <Link className="secondary-link" href="/exercises">
          Browse exercises
        </Link>
        <Link className="secondary-link" href="/recommendations">
          Get recommendations
        </Link>
      </div>
      {error && (
        <p className="library-message library-error" role="alert">
          {error}
        </p>
      )}
    </section>
  );
}
