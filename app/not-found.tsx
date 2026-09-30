import Link from "next/link";
import { Navbar } from "@/components/hush/Navbar";

export default function NotFound() {
  return (
    <>
      <Navbar />
      <main className="min-h-screen flex flex-col items-center justify-center px-6 text-center">
        <p className="text-primary font-mono font-bold tracking-widest mb-4">404</p>
        <h1 className="text-[clamp(32px,4vw,56px)] font-bold tracking-[-0.03em] mb-4">
          Không tìm thấy trang này
        </h1>
        <p className="text-lg text-muted-foreground font-light mb-10 max-w-md">
          Đường dẫn có thể đã sai hoặc trang đã được chuyển đi.
        </p>
        <div className="flex flex-col sm:flex-row gap-4">
          <Link
            href="/converter"
            className="inline-flex items-center justify-center h-12 px-8 rounded-full bg-primary text-primary-foreground font-semibold hover:brightness-110 transition-all"
          >
            Đến công cụ chuyển tone
          </Link>
          <Link
            href="/"
            className="inline-flex items-center justify-center h-12 px-8 rounded-full bg-card border border-border text-foreground font-semibold hover:bg-card/80 transition-colors"
          >
            Về trang chủ
          </Link>
        </div>
      </main>
    </>
  );
}
