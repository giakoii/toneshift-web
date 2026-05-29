import type { NoteName, ParsedNote, NoteOctave } from "@/types";

/**
 * All valid note names (longest first so regex greedy matching works correctly).
 */
export const NOTE_NAMES: NoteName[] = [
  "Sol", "Mib", "Reb", "Lab", "Sib", "Fab",
  "Do", "Re", "Mi", "Fa", "La", "Si",
];

/**
 * Regex that matches a note token, including optional octave digit.
 * Handles compound notes like "SolFa", "LaRe3", "ReDo".
 * Case-insensitive match then we restore casing from original.
 */
const NOTE_PATTERN = new RegExp(
  `(${NOTE_NAMES.join("|")})(\\d?)`,
  "gi"
);

/**
 * Tokenise a single line of notation text.
 * Returns an array of segments: each is either a note token or a plain text token.
 */
export function tokenizeLine(line: string): Array<{ text: string; isNote: boolean }> {
  const tokens: Array<{ text: string; isNote: boolean }> = [];
  let lastIndex = 0;
  const regex = new RegExp(NOTE_PATTERN.source, "gi");

  for (const match of line.matchAll(regex)) {
    const start = match.index ?? 0;
    if (start > lastIndex) {
      tokens.push({ text: line.slice(lastIndex, start), isNote: false });
    }
    tokens.push({ text: match[0], isNote: true });
    lastIndex = start + match[0].length;
  }

  if (lastIndex < line.length) {
    tokens.push({ text: line.slice(lastIndex), isNote: false });
  }

  return tokens;
}

/**
 * Parse a raw note string (e.g. "Re3", "sol") into a ParsedNote.
 */
export function parseNote(raw: string): ParsedNote | null {
  const match = raw.match(new RegExp(`^(${NOTE_NAMES.join("|")})(\\d?)$`, "i"));
  if (!match) return null;

  const notePart = match[1];
  const octavePart = match[2];

  // Normalise to title-case: "re" → "Re", "SOL" → "Sol"
  const normalized =
    notePart.charAt(0).toUpperCase() + notePart.slice(1).toLowerCase();

  return {
    raw,
    note: normalized as NoteName,
    octave: octavePart ? (parseInt(octavePart, 10) as NoteOctave) : undefined,
    isUpperCase: notePart === notePart.toUpperCase(),
  };
}
