import {
  CHROMATIC_SCALE,
  FLAT_ALIASES,
  VIET_TO_WESTERN,
  WESTERN_TO_VIET,
} from "@/constants/musical-keys";

export interface Token {
  type: "note" | "text" | "separator";
  value: string;
}

// Vietnamese roots must come before [A-G] so "Fa" matches as Vietnamese, not just "F"
const VIET_ROOTS = "Sol|Đô|Rê|Mi|Fa|La|Si";
const ROOT = `(?:${VIET_ROOTS}|[A-G])`;
const QUALITY = "(?:m(?:aj|in)?|dim|aug|sus[24]?|add|[0-9]+)*";
const SLASH = `(?:\\/${ROOT}[#b]?)?`;

// Unicode-aware boundaries prevent matching letters inside Vietnamese words
// e.g. "Có" won't match "C" because "ó" is \p{L}
const CHORD_PATTERN = `(?<!\\p{L})${ROOT}[#b]?${QUALITY}${SLASH}(?!\\p{L})`;

function createNoteRegex(): RegExp {
  return new RegExp(CHORD_PATTERN, "gu");
}

/**
 * Calculate semitone distance between two keys.
 */
function getSemitones(fromKey: string, toKey: string): number {
  const fromNorm = FLAT_ALIASES[fromKey] ?? fromKey;
  const toNorm = FLAT_ALIASES[toKey] ?? toKey;
  const from = CHROMATIC_SCALE.indexOf(fromNorm);
  const to = CHROMATIC_SCALE.indexOf(toNorm);
  if (from === -1 || to === -1) return 0;
  return (to - from + 12) % 12;
}

/**
 * Transpose a single root+accidental pair.
 * Preserves Vietnamese notation when the original root was Vietnamese.
 */
function transposeRootWithAccidental(
  root: string,
  accidental: string,
  semitones: number,
): string {
  const isViet = root in VIET_TO_WESTERN;
  const westernRoot = isViet ? VIET_TO_WESTERN[root] : root;

  // Normalize flats to sharps
  const combined = westernRoot + accidental;
  const normalized = FLAT_ALIASES[combined] ?? combined;

  const index = CHROMATIC_SCALE.indexOf(normalized);
  if (index === -1) return root + accidental;

  const newNote = CHROMATIC_SCALE[(index + semitones + 12) % 12];

  if (isViet) {
    const newWesternRoot = newNote[0];
    const newAcc = newNote.length > 1 ? newNote.slice(1) : "";
    return (WESTERN_TO_VIET[newWesternRoot] ?? newWesternRoot) + newAcc;
  }

  return newNote;
}

/**
 * Transpose all root notes within a chord string.
 * e.g. "Am7/G" → "Em7/D", "Fam7/Đô" → "Đôm7/Sol"
 */
function transposeChord(chord: string, semitones: number): string {
  const rootInChord = new RegExp(`(${VIET_ROOTS}|[A-G])([#b]?)`, "gu");
  return chord.replace(rootInChord, (_match, root: string, accidental: string) =>
    transposeRootWithAccidental(root, accidental, semitones),
  );
}

/**
 * Transpose all chords in a text from one key to another.
 */
export function transposeText(text: string, fromKey: string, toKey: string): string {
  if (fromKey === toKey) return text;
  const semitones = getSemitones(fromKey, toKey);
  const regex = createNoteRegex();
  return text.replace(regex, (match) => transposeChord(match, semitones));
}

/**
 * Parse text into tokens for syntax highlighting.
 */
export function parseTokens(text: string): Token[] {
  const tokens: Token[] = [];
  let lastIndex = 0;
  const regex = createNoteRegex();
  let match: RegExpExecArray | null;

  while ((match = regex.exec(text)) !== null) {
    if (match.index > lastIndex) {
      pushTextTokens(tokens, text.slice(lastIndex, match.index));
    }
    tokens.push({ type: "note", value: match[0] });
    lastIndex = regex.lastIndex;
  }

  if (lastIndex < text.length) {
    pushTextTokens(tokens, text.slice(lastIndex));
  }

  return tokens;
}

/**
 * Split a raw text segment into text and separator tokens.
 */
function pushTextTokens(tokens: Token[], segment: string) {
  const parts = segment.split(/([/\-|])/);
  for (const part of parts) {
    if (!part) continue;
    if (/^[/\-|]$/.test(part)) {
      tokens.push({ type: "separator", value: part });
    } else {
      tokens.push({ type: "text", value: part });
    }
  }
}

/**
 * Transpose and parse in one step (convenience for highlighted output).
 */
export function transposeAndParse(text: string, fromKey: string, toKey: string): Token[] {
  const transposed = transposeText(text, fromKey, toKey);
  return parseTokens(transposed);
}
