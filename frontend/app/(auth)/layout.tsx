import Link from "next/link";
import { Music } from "lucide-react";

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen flex items-center justify-center bg-background px-4">
      <div className="w-full max-w-sm">
        <Link href="/" className="flex items-center justify-center gap-2 mb-8 text-foreground">
          <Music className="w-6 h-6 text-primary" />
          <span className="font-bold text-2xl tracking-tight">ToneShift</span>
        </Link>
        <div className="bg-card border border-border rounded-2xl p-6 shadow-2xl">
          {children}
        </div>
      </div>
    </div>
  );
}
