import { Navbar } from "@/components/hush/Navbar";
import { HeroSection } from "@/components/hush/HeroSection";
import { SocialProof } from "@/components/hush/SocialProof";
import { ThreePillars } from "@/components/hush/ThreePillars";
import { CinematicConverter } from "@/components/hush/CinematicConverter";
import { Workflow } from "@/components/hush/Workflow";
import { Reviews } from "@/components/hush/Reviews";
import { FinalCTA } from "@/components/hush/FinalCTA";
import { Footer } from "@/components/hush/Footer";

export default function Home() {
  return (
    <>
      <Navbar />
      <main>
        <HeroSection />
        <SocialProof />
        <ThreePillars />
        <CinematicConverter />
        <Workflow />
        <Reviews />
        <FinalCTA />
      </main>
      <Footer />
    </>
  );
}