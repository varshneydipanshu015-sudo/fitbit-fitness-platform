import type { Metadata } from "next";
import ExerciseLibrary from "@/components/ExerciseLibrary";

export const metadata: Metadata = {
  title: "Exercise library | Fitbit",
};

export default function ExerciseLibraryPage() {
  return (
    <section className="exercise-library" aria-labelledby="exercises-heading">
      <ExerciseLibrary />
    </section>
  );
}
