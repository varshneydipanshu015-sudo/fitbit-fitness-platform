import type { Metadata } from "next";
import WorkoutTracker from "@/components/WorkoutTracker";

export const metadata: Metadata = {
  title: "Workout tracker | Fitbit",
};

export default function WorkoutsPage() {
  return <WorkoutTracker />;
}
