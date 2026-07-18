"use client";

import { motion, useInView, useReducedMotion } from "framer-motion";
import { useRef } from "react";
import { Music2 } from "lucide-react";
import Link from "next/link";

interface SongCard {
  title: string;
  artist: string;
  fromKey: string;
  toKey: string;
  featured?: boolean;
}

const TRENDING_SONGS: SongCard[] = [
  {
    title: "Dạ Cổ Hoài Lang",
    artist: "Cao Văn Lầu",
    fromKey: "Am",
    toKey: "Em",
    featured: true,
  },
  {
    title: "Sóng Gió",
    artist: "Jack & K-ICM",
    fromKey: "Dm",
    toKey: "Am",
  },
  {
    title: "Nơi Này Có Anh",
    artist: "Sơn Tùng M-TP",
    fromKey: "G",
    toKey: "C",
  },
  {
    title: "Chạy Ngay Đi",
    artist: "Sơn Tùng M-TP",
    fromKey: "Bm",
    toKey: "Em",
  },
  {
    title: "Hoa Nở Không Màu",
    artist: "Hoài Lâm",
    fromKey: "Am",
    toKey: "Dm",
  },
];

function SongCardComponent({
  song,
  index,
  inView,
  reduce,
}: {
  song: SongCard;
  index: number;
  inView: boolean;
  reduce: boolean | null;
}) {
  return (
    <motion.div
      initial={reduce ? false : { opacity: 0, y: 24 }}
      animate={inView ? { opacity: 1, y: 0 } : {}}
      transition={{
        duration: 0.5,
        delay: reduce ? 0 : index * 0.08,
        ease: [0.16, 1, 0.3, 1],
      }}
      whileHover={reduce ? {} : { y: -4 }}
      className={`group relative overflow-hidden rounded-2xl border bg-white/[0.02] p-6 transition-colors duration-300 hover:border-amber-500/25 ${
        song.featured
          ? "border-amber-500/15 bg-amber-500/[0.03]"
          : "border-white/[0.06]"
      }`}
    >
      {/* Hover glow */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-0 bg-gradient-to-b from-amber-500/[0.04] to-transparent opacity-0 transition-opacity duration-300 group-hover:opacity-100"
      />

      <div className="relative">
        <div className="mb-4 flex items-start gap-3">
          <div
            className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-xl text-amber-400 ${
              song.featured ? "bg-amber-500/15" : "bg-amber-500/10"
            }`}
          >
            <Music2 className="h-5 w-5" />
          </div>
          <div>
            <h3 className="text-sm font-semibold text-white">
              {song.title}
            </h3>
            <p className="mt-0.5 text-xs text-white/35">{song.artist}</p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <span className="rounded-lg bg-white/[0.06] px-2.5 py-1 text-xs font-semibold text-white/50">
            {song.fromKey}
          </span>
          <span className="text-[10px] text-white/20">→</span>
          <span className="rounded-lg bg-amber-500/10 px-2.5 py-1 text-xs font-semibold text-amber-400/70">
            {song.toKey}
          </span>
        </div>
      </div>
    </motion.div>
  );
}

export default function CommunityShowcase() {
  const ref = useRef<HTMLDivElement>(null);
  const inView = useInView(ref, { once: true, margin: "-60px" });
  const reduce = useReducedMotion();

  // Split: featured (first) + rest
  const featured = TRENDING_SONGS[0];
  const rest = TRENDING_SONGS.slice(1);

  return (
    <section ref={ref} className="relative px-6 py-28 sm:py-36 lg:py-44">
      {/* Divider */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-white/[0.06] to-transparent"
      />

      <div className="mx-auto max-w-5xl">
        {/* Section header - no eyebrow */}
        <div className="mb-12 text-center">
          <h2 className="text-2xl font-bold text-white sm:text-3xl">
            Bài hát thịnh hành
          </h2>
          <p className="mx-auto mt-3 max-w-lg text-sm leading-relaxed text-white/40">
            Khám phá những bài hát đang được cộng đồng chuyển tone nhiều nhất.
          </p>
        </div>

        {/* Asymmetric grid: featured left (tall), 2x2 right */}
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-[1.2fr_1fr]">
          {/* Featured card */}
          <SongCardComponent
            song={featured}
            index={0}
            inView={inView}
            reduce={reduce}
          />

          {/* 2x2 grid for remaining */}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            {rest.map((song, i) => (
              <SongCardComponent
                key={song.title}
                song={song}
                index={i + 1}
                inView={inView}
                reduce={reduce}
              />
            ))}
          </div>
        </div>

        {/* Bottom CTA link */}
        <motion.div
          initial={reduce ? false : { opacity: 0 }}
          animate={inView ? { opacity: 1 } : {}}
          transition={{ duration: 0.5, delay: reduce ? 0 : 0.6 }}
          className="mt-8 text-center"
        >
          <Link
            href="/converter"
            className="text-sm font-medium text-amber-400/70 hover:text-amber-400 transition-colors duration-200"
          >
            Xem thêm bài hát →
          </Link>
        </motion.div>
      </div>
    </section>
  );
}
