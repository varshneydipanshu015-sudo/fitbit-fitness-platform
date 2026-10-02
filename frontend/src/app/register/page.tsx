import type { Metadata } from "next";
import AuthForm from "@/components/AuthForm";

export const metadata: Metadata = {
  title: "Create account | Fitbit",
};

export default function RegisterPage() {
  return <AuthForm mode="register" />;
}
