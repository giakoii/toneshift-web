"use client";
import { ScrollReveal } from "./ScrollReveal";

export function Reviews() {
  const reviews = [
    { title: "Cuối cùng cũng có app không bị lỗi phông", desc: "Mình chuyên copy hợp âm tiếng Việt. ToneShift là tool duy nhất không làm loạn lời bài hát khi dịch tone.", author: "Tuấn Phạm - Acoustic Guitarist" },
    { title: "Rất nhanh và chính xác", desc: "UI siêu đẹp và mượt. Khi có khách yêu cầu đổi tone đột xuất, mình chỉ mất 3 giây để lấy hợp âm mới.", author: "Khánh Lê - Pianist" },
    { title: "Tính năng Auto-scroll thật sự hữu ích", desc: "Để iPad lên giá nhạc và cứ thế đàn thôi, không cần phải với tay lướt trang nữa. Quá tuyệt vời.", author: "Minh Anh - Singer" },
  ];

  return (
    <section className="py-24 md:py-32 bg-background border-t border-border">
      <div className="mx-auto max-w-7xl px-6 md:px-8">
        <ScrollReveal>
          <div className="mb-16 md:mb-20 text-center">
            <h2 className="text-[clamp(32px,4vw,56px)] font-bold tracking-[-0.03em]">Được tin dùng bởi nhạc công.</h2>
          </div>
        </ScrollReveal>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {reviews.map((review, i) => (
            <ScrollReveal key={i} delay={i * 0.1}>
              <div className="bg-card border border-border p-8 rounded-3xl h-full flex flex-col hover:border-primary/30 transition-colors">
                <div className="flex gap-1 mb-6 text-primary text-sm">
                  ★★★★★
                </div>
                <h3 className="text-lg font-bold mb-3">"{review.title}"</h3>
                <p className="text-muted-foreground font-light leading-[1.7] flex-1 mb-6">
                  {review.desc}
                </p>
                <div className="text-sm font-bold tracking-wider uppercase text-foreground/50">
                  — {review.author}
                </div>
              </div>
            </ScrollReveal>
          ))}
        </div>
      </div>
    </section>
  );
}
