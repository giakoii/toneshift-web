// ─── Note System ────────────────────────────────────────────────────────────

export type NoteName =
  | "Do"
  | "Re"
  | "Mi"
  | "Fa"
  | "Sol"
  | "La"
  | "Si"
  | "Mib"
  | "Reb"
  | "Lab"
  | "Sib"
  | "Fab";

export type NoteOctave = 1 | 2 | 3 | 4 | 5;

export interface ParsedNote {
  raw: string;        // original string from input e.g. "Re3"
  note: NoteName;     // normalized note name
  octave?: NoteOctave;
  isUpperCase: boolean;
}

// ─── Tone / Key System ───────────────────────────────────────────────────────

export type ToneKey =
  | "C" | "D" | "E" | "F" | "G" | "A" | "B"
  | "C#" | "D#" | "F#" | "G#" | "A#"
  | "Db" | "Eb" | "Gb" | "Ab" | "Bb";

export interface Tone {
  key: ToneKey;
  label: string;       // e.g. "Đô (C)"
  shortLabel: string;  // e.g. "C"
  semitone: number;    // 0–11 (C=0)
}

// ─── Converter ───────────────────────────────────────────────────────────────

export interface ConvertInput {
  text: string;
  fromTone: ToneKey;
  toTone: ToneKey;
}

export interface ConvertedToken {
  original: string;
  converted: string;
  isNote: boolean;
  position: number;
}

export interface ConvertResult {
  input: string;
  output: string;
  tokens: ConvertedToken[];
  fromTone: ToneKey;
  toTone: ToneKey;
  detectedTone?: ToneKey;
}

// ─── Genre ───────────────────────────────────────────────────────────────────

export type GenreSlug =
  | "bolero"
  | "cai-luong"
  | "sao-truc"
  | "nhac-tre"
  | "guitar"
  | "harmonica"
  | "dan-ca";

export interface Genre {
  slug: GenreSlug;
  label: string;
  description: string;
  icon?: string;
}

// ─── Song / Library ──────────────────────────────────────────────────────────

export type SongVisibility = "public" | "private";

export interface Song {
  id: string;
  title: string;
  artist?: string;
  genre: GenreSlug;
  originalTone: ToneKey;
  notation: string;
  lyrics?: string;
  visibility: SongVisibility;
  authorId?: string;
  authorName?: string;
  likes: number;
  views: number;
  createdAt: string; // ISO string
  updatedAt: string;
  tags?: string[];
}

export interface SongPreview
  extends Pick<Song, "id" | "title" | "artist" | "genre" | "originalTone" | "likes" | "views" | "createdAt"> {
  preview: string; // first ~80 chars of notation
}
