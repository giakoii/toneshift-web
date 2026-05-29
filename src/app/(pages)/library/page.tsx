import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Thư Viện Cảm Âm – ToneShift",
  description: "Khám phá hàng nghìn bài cảm âm được đóng góp bởi cộng đồng. Tìm kiếm theo thể loại, tone nhạc.",
};

export default function LibraryPage() {
  return (
    <div className="min-h-screen flex items-center justify-center text-zinc-500">
      {/* TODO: implement LibraryView */}
      <p>Library Page – coming soon</p>
    </div>
  );
}
