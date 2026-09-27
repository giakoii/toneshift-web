import { Metadata } from "next";
import { Navbar } from "@/components/hush/Navbar";
import { Footer } from "@/components/hush/Footer";
import { AuroraBackground } from "@/components/hush/AuroraBackground";
import { StarField } from "@/components/hush/StarField";
import ConverterPanel from "@/components/converter/ConverterPanel";

export const metadata: Metadata = {
  title: "Chuyển đổi cảm âm | ToneShift",
  description:
    "Chuyển đổi cảm âm bài hát nhanh chóng, hỗ trợ 12 tone chuẩn.",
};

export default function ConverterPage() {
  return (
    <>
      <Navbar />
      <div className="fixed inset-0 pointer-events-none z-[-1]">
        <AuroraBackground />
        <StarField />
      </div>
      <main className="pt-24 pb-8 sm:pt-28 sm:pb-12 min-h-screen relative z-10 flex flex-col justify-center">
        <ConverterPanel />
      </main>
      <Footer />
    </>
  );
}
