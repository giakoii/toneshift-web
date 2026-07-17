import { CHROMATIC_SCALE, FLAT_ALIASES } from "@/constants/musical-keys";

export interface Token {
  type: "note" | "text" | "separator";
  value: string;
}

// Regex: root note + optional suffix (quality, extensions, slash bass)
const NOTE_REGEX = /\b([A-G][#b]?)(m|maj|min|dim|aug|sus|add|[0-9])*(\/[A-G][#b]?)?\b/g;

/**
 * Normalize a root note to its sharp equivalent.
 * e.g. "Db" → "C#", "C" → "C"
 */
function normalizeNote(note: string): string {
  return FLAT_ALIASES[note] ?? note;
}

/**
 * Transpose a single root note by a number of semitones.
 */
function transposeRoot(root: string, semitones: number): string {
  const normalized = normalizeNote(root);
  const index = CHROMATIC_SCALE.indexOf(normalized);
  if (index === -1) return root;
  return CHROMATIC_SCALE[(index + semitones + 12) % 12];
}

/**
 * Calculate semitone distance between two keys.
 */
function getSemitones(fromKey: string, toKey: string): number {
  const from = CHROMATIC_SCALE.indexOf(normalizeNote(fromKey));
  const to = CHROMATIC_SCALE.indexOf(normalizeNote(toKey));
  if (from === -1 || to === -1) return 0;
  return (to - from + 12) % 12;
}

/**
 * Transpose a full chord string (e.g. "Am7/G" → "Em7/D").
 */
function transposeChord(chord: string, semitones: number): string {
  // Handle slash chords: transpose both the main root and the bass note
  return chord.replace(/[A-G][#b]?/g, (match) => transposeRoot(match, semitones));
}

/**
 * Parse text into tokens for syntax highlighting.
 */
export function parseTokens(text: string): Token[] {
  const tokens: Token[] = [];
  let lastIndex = 0;

  // Reset regex state
  NOTE_REGEX.lastIndex = 0;
  let match: RegExpExecArray | null;

  while ((match = NOTE_REGEX.exec(text)) !== null) {
    // Text before this match
    if (match.index > lastIndex) {
      const between = text.slice(lastIndex, match.index);
      pushTextTokens(tokens, between);
    }

    tokens.push({ type: "note", value: match[0] });
    lastIndex = NOTE_REGEX.lastIndex;
  }

  // Remaining text after last match
  if (lastIndex < text.length) {
    pushTextTokens(tokens, text.slice(lastIndex));
  }

  return tokens;
}

/**
 * Split a raw text segment into text and separator tokens.
 */
function pushTextTokens(tokens: Token[], segment: string) {
  // Split on separators (/, -, |) while keeping them
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
 * Transpose all chords in a text from one key to another.
 */
export function transposeText(text: string, fromKey: string, toKey: string): string {
  if (fromKey === toKey) return text;

  const semitones = getSemitones(fromKey, toKey);
  NOTE_REGEX.lastIndex = 0;

  return text.replace(NOTE_REGEX, (match) => transposeChord(match, semitones));
}

/**
 * Parse tokens from already-transposed text (for highlighted output).
 */
export function transposeAndParse(text: string, fromKey: string, toKey: string): Token[] {
  const transposed = transposeText(text, fromKey, toKey);
  return parseTokens(transposed);
}
