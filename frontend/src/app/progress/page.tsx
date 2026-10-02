import type { Metadata } from "next";
import ProgressTracker from "@/components/ProgressTracker";

export const metadata: Metadata = {
  title: "Progress tracker | Fitbit",
};

export default function ProgressPage() {
  return <ProgressTracker />;
}
