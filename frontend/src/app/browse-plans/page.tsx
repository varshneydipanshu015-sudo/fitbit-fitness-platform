import type { Metadata } from "next";
import PlanBrowser from "@/components/PlanBrowser";

export const metadata: Metadata = {
  title: "Workout plans | Fitbit",
};

export default function BrowsePlansPage() {
  return <PlanBrowser />;
}
