"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";

type Profile = {
  fullName: string;
  role: "USER" | "TRAINER";
};

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/fitbit-app";

export default function SiteHeader() {
  const pathname = usePathname();
  const router = useRouter();
  const [profile, setProfile] = useState<Profile | null>(null);
  const [isCheckingSession, setIsCheckingSession] = useState(true);
  const [isSigningOut, setIsSigningOut] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();

    async function loadProfile() {
      setIsCheckingSession(true);
      setError("");
      try {
        const response = await fetch(`${apiBaseUrl}/api/auth/me`, {
          credentials: "include",
          signal: controller.signal,
        });
        if (!response.ok) {
          setProfile(null);
          if (response.status !== 401) {
            setError("Account status is temporarily unavailable.");
          }
          return;
        }

        const result: unknown = await response.json();
        if (
          typeof result === "object" &&
          result !== null &&
          "fullName" in result &&
          typeof result.fullName === "string" &&
          "role" in result &&
          (result.role === "USER" || result.role === "TRAINER")
        ) {
          setProfile({ fullName: result.fullName, role: result.role });
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
  }, [pathname]);

  async function handleSignOut() {
    setError("");
    setIsSigningOut(true);
    try {
      const response = await fetch(`${apiBaseUrl}/api/auth/logout`, {
        method: "POST",
        credentials: "include",
      });
      if (!response.ok) {
        setError("Sign out could not be completed. Please try again.");
        setIsSigningOut(false);
        return;
      }
      setProfile(null);
      router.replace("/login");
      router.refresh();
    } catch {
      setError("Could not reach the Fitbit backend to sign out.");
      setIsSigningOut(false);
    }
  }

  return (
    <header className="site-header">
      <Link className="brand" href="/" aria-label="Fitbit home">
        <span className="brand-mark" aria-hidden="true">
          F
        </span>
        <span>fitbit</span>
      </Link>
      <nav className="site-nav" aria-label="Main navigation">
        {!isCheckingSession && profile ? (
          <>
            <Link href="/dashboard">Dashboard</Link>
            {profile.role === "TRAINER" && (
              <>
                <Link href="/plans">My plans</Link>
                <Link href="/clients">Clients</Link>
              </>
            )}
            {profile.role === "USER" && (
              <Link href="/browse-plans">Workout plans</Link>
            )}
            <Link href="/exercises">Exercises</Link>
            {profile.role === "USER" && (
              <>
                <Link href="/workouts">Workouts</Link>
                <Link href="/progress">Progress</Link>
              </>
            )}
            <Link href="/profile">Profile</Link>
            <span className="site-nav-user">{profile.fullName}</span>
            <button
              className="site-nav-signout"
              disabled={isSigningOut}
              onClick={() => void handleSignOut()}
              type="button"
            >
              {isSigningOut ? "Signing out..." : "Sign out"}
            </button>
          </>
        ) : !isCheckingSession ? (
          <>
            <Link href="/login">Sign in</Link>
            <Link className="site-nav-register" href="/register">
              Create account
            </Link>
          </>
        ) : (
          <span className="header-note">ONLINE FITNESS TRAINING</span>
        )}
      </nav>
      {error && (
        <p className="site-header-error" role="alert">
          {error}
        </p>
      )}
    </header>
  );
}
