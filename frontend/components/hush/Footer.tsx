import Link from "next/link";
import { Music } from "lucide-react";

export function Footer() {
  return (
    <footer className="bg-background border-t border-border py-12 md:py-16">
      <div className="mx-auto max-w-7xl px-6 md:px-8">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-8 md:gap-12 mb-16">
          <div className="col-span-2 md:col-span-1">
            <div className="flex items-center gap-2 text-foreground mb-6">
              <Music className="w-5 h-5 text-primary" />
              <span className="font-bold tracking-tight text-xl">ToneShift</span>
            </div>
            <p className="text-muted-foreground text-sm max-w-xs">
              Chuyển tone bài hát, hợp âm, cảm âm nhanh chóng và chuẩn xác. 
            </p>
          </div>
          
          <div>
            <h4 className="font-bold text-foreground mb-4">Sản phẩm</h4>
            <ul className="space-y-3 text-sm text-muted-foreground">
              <li><Link href="/#features" className="hover:text-foreground transition-colors">Tính năng</Link></li>
              <li><Link href="/converter" className="hover:text-foreground transition-colors">Công cụ chuyển đổi</Link></li>
            </ul>
          </div>

          <div>
            <h4 className="font-bold text-foreground mb-4">Hỗ trợ</h4>
            <ul className="space-y-3 text-sm text-muted-foreground">
              <li><Link href="/#workflow" className="hover:text-foreground transition-colors">Hướng dẫn</Link></li>
            </ul>
          </div>
        </div>
        
        <div className="flex flex-col md:flex-row justify-between items-center gap-4 pt-8 border-t border-border text-sm text-muted-foreground/60">
          <p>© 2026 ToneShift. Bảo lưu mọi quyền.</p>
          <p>Mã nguồn mở, xây dựng cho cộng đồng âm nhạc.</p>
        </div>
      </div>
    </footer>
  );
}
