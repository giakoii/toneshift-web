"use client";
import { ScrollReveal } from "./ScrollReveal";
import { Zap, FileText, LayoutTemplate } from "lucide-react";

export function ThreePillars() {
  const pillars = [
    {
      icon: Zap,
      title: "Xử lý tức thì",
      desc: "Không độ trễ, không tải lại trang. Chọn tone mới và nhận kết quả ngay lập tức nhờ engine xử lý tại trình duyệt.",
    },
    {
      icon: FileText,
      title: "Phân tích thông minh",
      desc: "Nhận diện chính xác hợp âm và nốt nhạc mà không làm vỡ format lời bài hát hay ký tự tiếng Việt.",
    },
    {
      icon: LayoutTemplate,
      title: "Hỗ trợ mọi định dạng",
      desc: "Paste từ mọi nguồn, hỗ trợ văn bản thuần, sheet hợp âm, và các định dạng cảm âm phổ biến nhất.",
    },
  ];

  return (
    <section className="py-24 md:py-32" id="features">
      <div className="mx-auto max-w-7xl px-6 md:px-8">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {pillars.map((pillar, i) => (
            <ScrollReveal key={i} delay={i * 0.1}>
              <div className="bg-card border border-border p-8 rounded-3xl h-full flex flex-col hover:border-primary/50 transition-colors">
                <div className="h-12 w-12 rounded-2xl bg-primary/10 flex items-center justify-center mb-6 text-primary">
                  <pillar.icon className="w-6 h-6" />
                </div>
                <h3 className="text-xl font-bold mb-3">{pillar.title}</h3>
                <p className="text-muted-foreground font-light leading-[1.7] flex-1">
                  {pillar.desc}
                </p>
              </div>
            </ScrollReveal>
          ))}
        </div>
      </div>
    </section>
  );
}
