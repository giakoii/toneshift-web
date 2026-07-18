import { Metadata } from "next";
import ConverterPanel from "@/components/converter/ConverterPanel";

export const metadata: Metadata = {
  title: "Chuyển đổi cảm âm | ToneShift",
  description:
    "Chuyển đổi cảm âm bài hát nhanh chóng, hỗ trợ 12 tone chuẩn.",
};

export default function ConverterPage() {
  return (
    <div className="py-8 sm:py-12">
      <ConverterPanel />
    </div>
  );
}
