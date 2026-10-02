"use client";

import Link from "next/link";
import { useEffect, useState, type FormEvent } from "react";

type Client = {
  userId: number;
  fullName: string;
  email: string;
  assignedAt: string;
};

type ApiError = {
  error?: string;
};

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/fitbit-app";

function isClientArray(value: unknown): value is Client[] {
  return (
    Array.isArray(value) &&
    value.every(
      (client) =>
        typeof client === "object" &&
        client !== null &&
        typeof client.userId === "number" &&
        typeof client.fullName === "string" &&
        typeof client.email === "string" &&
        typeof client.assignedAt === "string",
    )
  );
}

async function readError(response: Response, fallback: string) {
  try {
    const result = (await response.json()) as ApiError;
    return result.error ?? fallback;
  } catch {
    return "The backend returned a response that could not be read.";
  }
}

export default function TrainerClients() {
  const [clients, setClients] = useState<Client[]>([]);
  const [email, setEmail] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [isSignedOut, setIsSignedOut] = useState(false);
  const [isNotTrainer, setIsNotTrainer] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  useEffect(() => {
    const controller = new AbortController();

    async function loadClients() {
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

        const response = await fetch(`${apiBaseUrl}/api/trainer/clients`, {
          credentials: "include",
          signal: controller.signal,
        });
        if (response.status === 401) {
          setIsSignedOut(true);
          setIsLoading(false);
          return;
        }
        if (!response.ok) {
          setError(await readError(response, "Your client list could not be loaded."));
          setIsLoading(false);
          return;
        }
        const result: unknown = await response.json();
        if (!isClientArray(result)) {
          setError("The backend returned client data in an unexpected format.");
          setIsLoading(false);
          return;
        }
        setClients(result);
        setIsLoading(false);
      } catch (loadError) {
        if (
          loadError instanceof DOMException &&
          loadError.name === "AbortError"
        ) {
          return;
        }
        setError("Could not reach the Fitbit backend. Check that Tomcat is running.");
        setIsLoading(false);
      }
    }

    void loadClients();
    return () => controller.abort();
  }, []);

  async function handleAssignClient(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setNotice("");
    setIsSaving(true);

    try {
      const response = await fetch(`${apiBaseUrl}/api/trainer/clients`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ email: email.trim() }),
      });
      if (response.status === 401) {
        setIsSignedOut(true);
        setIsSaving(false);
        return;
      }
      if (!response.ok) {
        setError(await readError(response, "This client could not be assigned."));
        setIsSaving(false);
        return;
      }
      const clientsResponse = await fetch(`${apiBaseUrl}/api/trainer/clients`, {
        credentials: "include",
      });
      if (!clientsResponse.ok) {
        setError(await readError(
          clientsResponse,
          "The client was assigned, but the list could not be refreshed.",
        ));
        setIsSaving(false);
        return;
      }
      const result: unknown = await clientsResponse.json();
      if (!isClientArray(result)) {
        setError("The backend returned client data in an unexpected format.");
        setIsSaving(false);
        return;
      }
      setClients(result);
      setEmail("");
      setNotice("Client added to your roster.");
    } catch {
      setError("Could not reach the Fitbit backend. Check that Tomcat is running.");
    }
    setIsSaving(false);
  }

  return (
    <section className="trainer-clients" aria-labelledby="trainer-clients-heading">
      <div className="library-heading">
        <div>
          <p className="eyebrow">TRAINER TOOLS</p>
          <h1 id="trainer-clients-heading">Client roster</h1>
          <p className="dashboard-intro">
            Add an existing fitness-user account by email to keep track of your
            coaching clients.
          </p>
        </div>
        <Link className="secondary-link" href="/dashboard">
          Back to dashboard
        </Link>
      </div>

      {isLoading && (
        <p className="library-message" role="status">Loading your client roster...</p>
      )}
      {isSignedOut && (
        <div className="library-message">
          <p>Sign in with a trainer account to manage clients.</p>
          <Link className="secondary-link" href="/login">Sign in</Link>
        </div>
      )}
      {isNotTrainer && (
        <div className="library-message">
          <p>Client management is available to trainer accounts.</p>
          <Link className="secondary-link" href="/dashboard">Back to dashboard</Link>
        </div>
      )}

      {!isLoading && !isSignedOut && !isNotTrainer && (
        <>
          <form className="trainer-client-form" onSubmit={handleAssignClient}>
            <label className="field">
              <span>Fitness user’s email</span>
              <input
                autoComplete="email"
                onChange={(event) => setEmail(event.target.value)}
                placeholder="client@example.com"
                required
                type="email"
                value={email}
              />
            </label>
            <button className="auth-submit" disabled={isSaving} type="submit">
              {isSaving ? "Adding client..." : "Add client"}
            </button>
          </form>
          {notice && <p className="library-message" role="status">{notice}</p>}
          {error && <p className="library-message library-error" role="alert">{error}</p>}

          <section className="trainer-client-roster" aria-labelledby="roster-heading">
            <div className="panel-heading">
              <div>
                <p className="eyebrow">YOUR COACHING</p>
                <h2 id="roster-heading">Assigned clients</h2>
              </div>
              <p className="library-count">
                {clients.length} {clients.length === 1 ? "client" : "clients"}
              </p>
            </div>
            {clients.length === 0 ? (
              <p className="library-message">
                Your roster is empty. Add a fitness user using the email linked
                to their account.
              </p>
            ) : (
              <div className="trainer-client-list">
                {clients.map((client) => (
                  <article className="trainer-client-card" key={client.userId}>
                    <div>
                      <h3>{client.fullName}</h3>
                      <p>{client.email}</p>
                    </div>
                    <p className="trainer-client-date">
                      Added {new Date(client.assignedAt).toLocaleDateString()}
                    </p>
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
