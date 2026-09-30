import { Navbar } from "@/components/hush/Navbar";
import { HeroSection } from "@/components/hush/HeroSection";
import { ThreePillars } from "@/components/hush/ThreePillars";
import { CinematicConverter } from "@/components/hush/CinematicConverter";
import { Workflow } from "@/components/hush/Workflow";
import { FinalCTA } from "@/components/hush/FinalCTA";
import { Footer } from "@/components/hush/Footer";

export default function Home() {
  return (
    <>
      <Navbar />
      <main>
        <HeroSection />
        <ThreePillars />
        <CinematicConverter />
        <Workflow />
        <FinalCTA />
      </main>
      <Footer />
    </>
  );
}