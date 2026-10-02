import type { Metadata } from "next";
import ProfileSettings from "@/components/ProfileSettings";

export const metadata: Metadata = {
  title: "Profile settings | Fitbit",
};

export default function ProfilePage() {
  return <ProfileSettings />;
}
