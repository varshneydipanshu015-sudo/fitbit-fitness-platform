import type { Metadata } from "next";
import Recommendations from "@/components/Recommendations";

export const metadata: Metadata = {
  title: "Workout recommendations | Fitbit",
};

export default function RecommendationsPage() {
  return <Recommendations />;
}
