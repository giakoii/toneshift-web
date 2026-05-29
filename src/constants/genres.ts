import type { Genre, GenreSlug } from "@/types";

export const GENRES: Genre[] = [
  { slug: "bolero",    label: "Bolero",    description: "Nhạc bolero & trữ tình", icon: "🎵" },
  { slug: "cai-luong", label: "Cải Lương", description: "Cải lương Nam Bộ",       icon: "🎭" },
  { slug: "sao-truc",  label: "Sáo Trúc",  description: "Nhạc sáo trúc Việt Nam", icon: "🎶" },
  { slug: "nhac-tre",  label: "Nhạc Trẻ",  description: "Nhạc trẻ hiện đại",      icon: "🎤" },
  { slug: "guitar",    label: "Guitar",    description: "Nhạc guitar các loại",   icon: "🎸" },
  { slug: "harmonica", label: "Harmonica", description: "Kèn harmonica",          icon: "🎹" },
  { slug: "dan-ca",    label: "Dân Ca",    description: "Dân ca 3 miền",          icon: "🪗" },
];

export const GENRES_MAP: Record<GenreSlug, Genre> = Object.fromEntries(
  GENRES.map((g) => [g.slug, g])
) as Record<GenreSlug, Genre>;
