"use client";

import Link from "next/link";
import { useEffect, useState, type FormEvent } from "react";

type ProgressEntry = {
  progressId: number;
  recordedOn: string;
  weightKg: number;
  note: string | null;
};

type ApiError = {
  error?: string;
};

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/fitbit-app";

async function readError(response: Response) {
  try {
    const result = (await response.json()) as ApiError;
    return result.error ?? "The progress request could not be completed.";
  } catch {
    return "The backend returned a response that could not be read.";
  }
}

function formatDate(value: string) {
  return new Date(`${value}T00:00:00`).toLocaleDateString(undefined, {
    dateStyle: "long",
  });
}

function formatShortDate(value: string) {
  return new Date(`${value}T00:00:00`).toLocaleDateString(undefined, {
    month: "short",
    day: "numeric",
  });
}

/** Saves progress entries and draws a small SVG trend chart without chart dependencies. */
export default function ProgressTracker() {
  const [entries, setEntries] = useState<ProgressEntry[]>([]);
  const [weight, setWeight] = useState("");
  const [note, setNote] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [isSignedOut, setIsSignedOut] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [reloadCount, setReloadCount] = useState(0);
  // Reverse the latest ten records so the chart reads left-to-right in date order.
  const chartEntries = entries.slice(0, 10).reverse();
  const chartWeights = chartEntries.map((entry) => entry.weightKg);
  const minimumWeight = Math.min(...chartWeights);
  const maximumWeight = Math.max(...chartWeights);
  const weightRange = maximumWeight - minimumWeight;
  const chartPadding = weightRange === 0 ? 1 : weightRange * 0.1;
  const chartMinimum = minimumWeight - chartPadding;
  const chartMaximum = maximumWeight + chartPadding;
  const chartWidth = 640;
  const chartHeight = 240;
  const chartLeft = 58;
  const chartRight = chartWidth - 20;
  const chartTop = 20;
  const chartBottom = chartHeight - 42;
  // Convert weight and entry position into SVG coordinates inside the chart margins.
  const chartPoints = chartEntries.map((entry, index) => {
    const x =
      chartEntries.length === 1
        ? (chartLeft + chartRight) / 2
        : chartLeft +
          (index / (chartEntries.length - 1)) * (chartRight - chartLeft);
    const y =
      chartBottom -
      ((entry.weightKg - chartMinimum) / (chartMaximum - chartMinimum)) *
        (chartBottom - chartTop);
    return { ...entry, x, y };
  });
  const chartLine = chartPoints
    .map((point) => `${point.x},${point.y}`)
    .join(" ");

  useEffect(() => {
    const controller = new AbortController();

    async function loadEntries() {
      let response: Response;
      try {
        response = await fetch(`${apiBaseUrl}/api/progress`, {
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
          throw new Error("Expected a list of progress entries.");
        }
        setEntries(result as ProgressEntry[]);
      } catch {
        setError("The backend returned progress data in an unexpected format.");
      }
      setIsLoading(false);
    }

    void loadEntries();
    return () => controller.abort();
  }, [reloadCount]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setNotice("");
    setIsSaving(true);

    let response: Response;
    try {
      response = await fetch(`${apiBaseUrl}/api/progress`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({
          weightKg: Number(weight),
          note: note.trim() || null,
        }),
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

    setNotice("Your progress entry was saved.");
    setWeight("");
    setNote("");
    setIsSaving(false);
    setIsLoading(true);
    setReloadCount((count) => count + 1);
  }

  return (
    <section className="progress-page" aria-labelledby="progress-heading">
      <div className="library-heading">
        <div>
          <p className="eyebrow">NOTICE YOUR PROGRESS</p>
          <h1 id="progress-heading">Progress tracker</h1>
          <p className="dashboard-intro">
            Log your weight when you choose to and keep a private history in
            your account. Progress is personal; there’s no required schedule.
          </p>
        </div>
      </div>

      {!isSignedOut && (
        <form className="progress-form" onSubmit={handleSubmit}>
          <label className="field">
            <span>Weight (kg)</span>
            <input
              max="999.99"
              min="0.01"
              name="weightKg"
              required
              step="0.01"
              type="number"
              value={weight}
              onChange={(event) => setWeight(event.target.value)}
            />
          </label>
          <label className="field">
            <span>Note (optional)</span>
            <input
              maxLength={255}
              name="note"
              placeholder="A short note about today"
              type="text"
              value={note}
              onChange={(event) => setNote(event.target.value)}
            />
          </label>
          <button className="auth-submit" disabled={isSaving} type="submit">
            {isSaving ? "Saving..." : "Save progress"}
          </button>
        </form>
      )}

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
          <p>Sign in to save and view your personal progress history.</p>
          <Link className="secondary-link" href="/login">
            Sign in
          </Link>
        </div>
      )}

      {!isSignedOut && (
        <section className="progress-history" aria-labelledby="progress-history-heading">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">YOUR RECORDS</p>
              <h2 id="progress-history-heading">Weight history</h2>
            </div>
          </div>
          {isLoading && (
            <p className="library-message" role="status">
              Loading your progress...
            </p>
          )}
          {!isLoading && !error && entries.length === 0 && (
            <p className="library-message">
              No progress entries yet. Add one whenever you want to start
              keeping a record.
            </p>
          )}
          {!isLoading && entries.length > 0 && (
            <>
              {chartEntries.length > 1 ? (
                <div className="progress-chart-card">
                  <div className="progress-chart-heading">
                    <h3>Recent weight trend</h3>
                    <p>
                      Showing your last {chartEntries.length} entries. The line
                      connects entries in date order.
                    </p>
                  </div>
                  <svg
                    aria-label={`Weight trend from ${chartEntries[0].weightKg.toFixed(2)} kilograms on ${formatDate(chartEntries[0].recordedOn)} to ${chartEntries[chartEntries.length - 1].weightKg.toFixed(2)} kilograms on ${formatDate(chartEntries[chartEntries.length - 1].recordedOn)}.`}
                    className="progress-chart"
                    role="img"
                    viewBox={`0 0 ${chartWidth} ${chartHeight}`}
                  >
                    <line
                      className="progress-chart-gridline"
                      x1={chartLeft}
                      x2={chartRight}
                      y1={chartTop}
                      y2={chartTop}
                    />
                    <line
                      className="progress-chart-gridline"
                      x1={chartLeft}
                      x2={chartRight}
                      y1={(chartTop + chartBottom) / 2}
                      y2={(chartTop + chartBottom) / 2}
                    />
                    <line
                      className="progress-chart-axis"
                      x1={chartLeft}
                      x2={chartRight}
                      y1={chartBottom}
                      y2={chartBottom}
                    />
                    <text className="progress-chart-label" x="4" y={chartTop + 4}>
                      {maximumWeight.toFixed(2)}
                    </text>
                    <text
                      className="progress-chart-label"
                      x="4"
                      y={chartBottom + 4}
                    >
                      {minimumWeight.toFixed(2)}
                    </text>
                    <polyline
                      className="progress-chart-line"
                      points={chartLine}
                    />
                    {chartPoints.map((point) => (
                      <circle
                        className="progress-chart-point"
                        cx={point.x}
                        cy={point.y}
                        key={point.progressId}
                        r="5"
                      >
                        <title>
                          {point.weightKg.toFixed(2)} kg on{" "}
                          {formatDate(point.recordedOn)}
                        </title>
                      </circle>
                    ))}
                    <text
                      className="progress-chart-date"
                      textAnchor="start"
                      x={chartLeft}
                      y={chartHeight - 8}
                    >
                      {formatShortDate(chartEntries[0].recordedOn)}
                    </text>
                    <text
                      className="progress-chart-date"
                      textAnchor="end"
                      x={chartRight}
                      y={chartHeight - 8}
                    >
                      {formatShortDate(
                        chartEntries[chartEntries.length - 1].recordedOn,
                      )}
                    </text>
                  </svg>
                  <p className="progress-chart-unit">Weight in kilograms</p>
                </div>
              ) : (
                <p className="library-message">
                  Add another entry to see your weight trend over time.
                </p>
              )}
              <ol className="progress-list">
                {entries.map((entry) => (
                  <li className="progress-card" key={entry.progressId}>
                    <div>
                      <p className="progress-weight">
                        {entry.weightKg.toFixed(2)} kg
                      </p>
                      <p className="progress-date">
                        {formatDate(entry.recordedOn)}
                      </p>
                    </div>
                    {entry.note && (
                      <p className="progress-note">{entry.note}</p>
                    )}
                  </li>
                ))}
              </ol>
            </>
          )}
        </section>
      )}
    </section>
  );
}
