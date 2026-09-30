"use client";
import { ScrollReveal } from "./ScrollReveal";
import {
  Zap,
  Languages,
  MonitorPlay,
  ListMusic,
  Link2,
  MessageSquare,
} from "lucide-react";

export function ThreePillars() {
  const pillars = [
    {
      icon: Zap,
      title: "Chuyển tone tức thì",
      desc: "Chọn tone mới là có kết quả ngay, đủ 12 tone và cả ký hiệu Đô Rê Mi, không cần tải lại trang.",
    },
    {
      icon: Languages,
      title: "Giữ nguyên lời tiếng Việt",
      desc: "Chỉ đổi hợp âm, không thay đổi lời ca.",
    },
    {
      icon: MonitorPlay,
      title: "Chế độ biểu diễn",
      desc: "Tự động cuộn theo tốc độ bạn chọn và giữ màn hình luôn sáng, để hai tay rảnh cho cây đàn.",
    },
    {
      icon: ListMusic,
      title: "Setlist cho buổi diễn",
      desc: "Gom các bài đã chuyển tone thành một danh sách, sắp xếp thứ tự và vuốt để sang bài tiếp theo trên sân khấu.",
    },
    {
      icon: Link2,
      title: "Chia sẻ bằng một đường link",
      desc: "Gửi cho ban nhạc một link ngắn, mọi người xem đúng tone của mình, có thể in hoặc lưu thành PDF.",
    },
    {
      icon: MessageSquare,
      title: "Cộng đồng cảm âm",
      desc: "Khám phá bản cảm âm của người chơi khác, đánh giá sao và bình luận để cùng chỉnh cho chuẩn.",
    },
  ];

  return (
    <section className="py-24 md:py-32" id="features">
      <div className="mx-auto max-w-7xl px-6 md:px-8">
        <ScrollReveal>
          <div className="mb-16 md:mb-20 text-center">
            <h2 className="text-[clamp(32px,4vw,56px)] font-bold tracking-[-0.03em]">
              Mọi thứ nhạc công cần, trong một nơi.
            </h2>
          </div>
        </ScrollReveal>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {pillars.map((pillar, i) => (
            <ScrollReveal key={i} delay={(i % 3) * 0.1}>
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
