"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";

type AuthMode = "login" | "register";
type AccountRole = "USER" | "TRAINER";

type AuthResponse = {
  error?: string;
  fullName?: string;
  email?: string;
};

type AuthFormProps = {
  mode: AuthMode;
};

export default function AuthForm({ mode }: AuthFormProps) {
  const router = useRouter();
  const isRegistration = mode === "register";
  const [accountRole, setAccountRole] = useState<AccountRole>("USER");
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setNotice("");
    setError("");
    setIsSubmitting(true);

    const formData = new FormData(event.currentTarget);
    const requestBody = isRegistration
      ? {
          role: accountRole,
          fullName: formData.get("fullName"),
          email: formData.get("email"),
          password: formData.get("password"),
          ...(accountRole === "USER"
            ? {
                fitnessGoal: formData.get("fitnessGoal"),
                fitnessLevel: formData.get("fitnessLevel"),
              }
            : {
                bio: formData.get("bio"),
                specialization: formData.get("specialization"),
              }),
        }
      : {
          email: formData.get("email"),
          password: formData.get("password"),
        };
    const apiBaseUrl =
      process.env.NEXT_PUBLIC_API_BASE_URL ??
      "http://localhost:8080/fitbit-app";

    let response: Response;
    try {
      response = await fetch(
        `${apiBaseUrl}/api/auth/${isRegistration ? "register" : "login"}`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          credentials: "include",
          body: JSON.stringify(requestBody),
        },
      );
    } catch {
      setError(
        "Could not reach the Fitbit backend. Make sure Tomcat is running and the backend URL is correct.",
      );
      setIsSubmitting(false);
      return;
    }

    let result: AuthResponse;
    try {
      result = (await response.json()) as AuthResponse;
    } catch {
      setError("The backend returned a response that could not be read.");
      setIsSubmitting(false);
      return;
    }

    if (!response.ok) {
      setError(result.error ?? "The account request could not be completed.");
      setIsSubmitting(false);
      return;
    }

    if (!isRegistration) {
      router.replace("/dashboard");
      router.refresh();
      return;
    }

    setNotice("Your account was created. You can now sign in.");
    setIsSubmitting(false);
  }

  return (
    <section className="auth-card" aria-labelledby="auth-heading">
      <p className="eyebrow">{isRegistration ? "START YOUR JOURNEY" : "WELCOME BACK"}</p>
      <h1 id="auth-heading">{isRegistration ? "Create your account" : "Sign in"}</h1>
      <p className="auth-intro">
        {isRegistration
          ? "Set up your profile to get started with a fitness routine that fits you."
          : "Sign in to continue your fitness journey."}
      </p>

      <form className="auth-form" onSubmit={handleSubmit}>
        {isRegistration && (
          <label className="field">
            <span>Account type</span>
            <select
              onChange={(event) => {
                if (
                  event.target.value === "USER" ||
                  event.target.value === "TRAINER"
                ) {
                  setAccountRole(event.target.value);
                }
              }}
              value={accountRole}
            >
              <option value="USER">Fitness user</option>
              <option value="TRAINER">Trainer</option>
            </select>
          </label>
        )}

        {isRegistration && (
          <label className="field">
            <span>Full name</span>
            <input
              autoComplete="name"
              maxLength={100}
              name="fullName"
              placeholder="Your name"
              required
            />
          </label>
        )}

        <label className="field">
          <span>Email address</span>
          <input
            autoComplete="email"
            maxLength={254}
            name="email"
            placeholder="you@example.com"
            required
            type="email"
          />
        </label>

        <label className="field">
          <span>Password</span>
          <input
            autoComplete={isRegistration ? "new-password" : "current-password"}
            maxLength={72}
            minLength={8}
            name="password"
            placeholder="At least 8 characters"
            required
            type="password"
          />
          {isRegistration && accountRole === "USER" && (
            <span className="field-hint">Use at least 8 characters.</span>
          )}
        </label>

        {isRegistration && (
          <div className="field-row">
            <label className="field">
              <span>Fitness goal</span>
              <select defaultValue="" name="fitnessGoal" required>
                <option disabled value="">
                  Select a goal
                </option>
                <option value="WEIGHT_LOSS">Weight loss</option>
                <option value="MUSCLE_GAIN">Muscle gain</option>
                <option value="GENERAL_FITNESS">General fitness</option>
                <option value="STRENGTH">Strength</option>
                <option value="ENDURANCE">Endurance</option>
              </select>
            </label>

            <label className="field">
              <span>Fitness level</span>
              <select defaultValue="" name="fitnessLevel" required>
                <option disabled value="">
                  Select a level
                </option>
                <option value="BEGINNER">Beginner</option>
                <option value="INTERMEDIATE">Intermediate</option>
                <option value="ADVANCED">Advanced</option>
              </select>
            </label>
          </div>
        )}

        {isRegistration && accountRole === "TRAINER" && (
          <>
            <label className="field">
              <span>Specialization (optional)</span>
              <input
                maxLength={120}
                name="specialization"
                placeholder="For example, strength training"
              />
            </label>
            <label className="field">
              <span>Short bio (optional)</span>
              <textarea
                maxLength={5000}
                name="bio"
                placeholder="Share a little about your coaching experience"
                rows={4}
              />
            </label>
          </>
        )}

        <button className="auth-submit" disabled={isSubmitting} type="submit">
          {isSubmitting
            ? "Please wait..."
            : isRegistration
              ? "Create account"
              : "Sign in"}
        </button>
        <p className="form-status" aria-live="polite" role="status">
          {notice}
        </p>
        {error && (
          <p className="form-error" aria-live="assertive" role="alert">
            {error}
          </p>
        )}
      </form>

      <p className="auth-switch">
        {isRegistration ? "Already have an account?" : "New to Fitbit?"}{" "}
        <Link href={isRegistration ? "/login" : "/register"}>
          {isRegistration ? "Sign in" : "Create an account"}
        </Link>
      </p>
    </section>
  );
}
