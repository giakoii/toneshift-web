import type { Song, SongPreview, SongFilter, PaginatedResponse } from "@/types";
import { MOCK_SONGS, MOCK_SONG_PREVIEWS } from "@/mocks";

/**
 * Song service – currently backed by mock data.
 * When the API is ready, swap out the implementations below.
 *
 * All methods return Promises to simulate async network calls,
 * making the API shape identical to the real implementation.
 */

export const songService = {
  /** Get paginated list of song previews */
  async getAll(filter?: SongFilter): Promise<PaginatedResponse<SongPreview>> {
    await delay(200); // simulate network latency

    let results = [...MOCK_SONG_PREVIEWS];

    if (filter?.genre) {
      results = results.filter((s) => s.genre === filter.genre);
    }
    if (filter?.query) {
      const q = filter.query.toLowerCase();
      results = results.filter(
        (s) =>
          s.title.toLowerCase().includes(q) ||
          s.artist?.toLowerCase().includes(q)
      );
    }
    if (filter?.sortBy === "popular") {
      results.sort((a, b) => b.likes - a.likes);
    }

    const page = filter?.page ?? 1;
    const pageSize = filter?.pageSize ?? 12;
    const start = (page - 1) * pageSize;

    return {
      data: results.slice(start, start + pageSize),
      total: results.length,
      page,
      pageSize,
      hasNext: start + pageSize < results.length,
    };
  },

  /** Get a single song by id */
  async getById(id: string): Promise<Song | null> {
    await delay(150);
    return MOCK_SONGS.find((s) => s.id === id) ?? null;
  },

  /** Create a new song (noop in mock) */
  async create(song: Omit<Song, "id" | "createdAt" | "updatedAt" | "likes" | "views">): Promise<Song> {
    await delay(300);
    const now = new Date().toISOString();
    return {
      ...song,
      id: crypto.randomUUID(),
      likes: 0,
      views: 0,
      createdAt: now,
      updatedAt: now,
    };
  },
};

// ─── Helper ──────────────────────────────────────────────────────────────────
function delay(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
