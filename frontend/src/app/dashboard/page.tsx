import type { Metadata } from "next";
import DashboardOverview from "@/components/DashboardOverview";

export const metadata: Metadata = {
  title: "Dashboard | Fitbit",
};

export default function DashboardPage() {
  return <DashboardOverview />;
}
