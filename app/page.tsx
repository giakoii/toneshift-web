"use client";

import HeroSection from "@/components/sections/HeroSection";
import ConverterShowcase from "@/components/sections/ConverterShowcase";
import KeyFeatures from "@/components/sections/KeyFeatures";
import CommunityShowcase from "@/components/sections/CommunityShowcase";
import { AuroraBackground } from "@/components/effects/AuroraBackground";
import { MouseSpotlight } from "@/components/effects/MouseSpotlight";

export default function Home() {
  return (
    <>
      <AuroraBackground />
      <MouseSpotlight />
      <HeroSection />
      <ConverterShowcase />
      <KeyFeatures />
      <CommunityShowcase />
    </>
  );
}