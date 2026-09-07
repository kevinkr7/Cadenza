import { createFileRoute } from "@tanstack/react-router";
import { Navbar } from "@/components/Navbar";
import { Hero } from "@/components/Hero";
import { Loader } from "@/components/Loader";
import { Footer } from "@/components/Footer";
import { WhatIsCadenza } from "@/sections/WhatIsCadenza";
import { HowItWorks } from "@/sections/HowItWorks";
import { VocalAnalysis } from "@/sections/VocalAnalysis";
import { AICompanion } from "@/sections/AICompanion";
import { ProgressSection } from "@/sections/Progress";
import { PracticeToPerformance } from "@/sections/PracticeToPerformance";
import { FutureVision } from "@/sections/FutureVision";
import { Technology } from "@/sections/Technology";
import { About } from "@/sections/About";
import { DownloadSection } from "@/sections/DownloadSection";

const TITLE = "Cadenza — Your Personal Vocal Coaching Companion";
const DESCRIPTION =
  "Cadenza helps singers understand, practice and improve their voice through intelligent vocal analysis, personalized feedback and progress tracking.";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: TITLE },
      { name: "description", content: DESCRIPTION },
      { property: "og:title", content: TITLE },
      { property: "og:description", content: DESCRIPTION },
      { property: "og:type", content: "website" },
      { property: "og:url", content: "/" },
      { name: "twitter:card", content: "summary_large_image" },
      { name: "twitter:title", content: TITLE },
      { name: "twitter:description", content: DESCRIPTION },
    ],
    links: [{ rel: "canonical", href: "/" }],
    scripts: [
      {
        type: "application/ld+json",
        children: JSON.stringify({
          "@context": "https://schema.org",
          "@type": "SoftwareApplication",
          name: "Cadenza",
          applicationCategory: "MusicApplication",
          operatingSystem: "Android",
          description: DESCRIPTION,
        }),
      },
    ],
  }),
  component: Index,
});

function Index() {
  return (
    <>
      <Loader />
      <Navbar />
      <main>

        <Hero />
        <WhatIsCadenza />
        <HowItWorks />
        <VocalAnalysis />
        <AICompanion />
        <ProgressSection />
        <PracticeToPerformance />
        <FutureVision />
        <Technology />
        <About />
        <DownloadSection />
      </main>
      <Footer />
    </>
  );
}
