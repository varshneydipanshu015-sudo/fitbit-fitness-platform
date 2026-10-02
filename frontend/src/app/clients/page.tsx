import type { Metadata } from "next";
import TrainerClients from "@/components/TrainerClients";

export const metadata: Metadata = {
  title: "Client roster | Fitbit",
};

export default function ClientsPage() {
  return <TrainerClients />;
}
