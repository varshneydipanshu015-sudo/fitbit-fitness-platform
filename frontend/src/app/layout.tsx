import type { Metadata } from "next";
import SiteHeader from "@/components/SiteHeader";
import "./globals.css";

export const metadata: Metadata = {
  title: "Fitbit | Online Fitness Training",
  description: "A simple space to plan workouts and track your fitness journey.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en">
      <body>
        <div className="site-shell">
          <SiteHeader />
          <main className="site-main">{children}</main>
          <footer className="site-footer">
            <span>Build healthy habits, one workout at a time.</span>
            <span>Fitbit academic project</span>
          </footer>
        </div>
      </body>
    </html>
  );
}
