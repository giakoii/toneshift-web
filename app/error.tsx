"use client";

import Link from "next/link";

export default function Error({
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <main className="min-h-screen flex flex-col items-center justify-center px-6 text-center">
      <h1 className="text-[clamp(32px,4vw,56px)] font-bold tracking-[-0.03em] mb-4">
        Có lỗi xảy ra
      </h1>
      <p className="text-lg text-muted-foreground font-light mb-10 max-w-md">
        Rất tiếc, ToneShift gặp sự cố ngoài ý muốn. Bạn thử tải lại nhé.
      </p>
      <div className="flex flex-col sm:flex-row gap-4">
        <button
          onClick={reset}
          className="inline-flex items-center justify-center h-12 px-8 rounded-full bg-primary text-primary-foreground font-semibold hover:brightness-110 transition-all"
        >
          Thử lại
        </button>
        <Link
          href="/"
          className="inline-flex items-center justify-center h-12 px-8 rounded-full bg-card border border-border text-foreground font-semibold hover:bg-card/80 transition-colors"
        >
          Về trang chủ
        </Link>
      </div>
    </main>
  );
}
