"use client";

import { useState, useEffect, useCallback } from "react";
import type { SongPreview, SongFilter } from "@/types";
import { songService } from "@/services";

interface UseSongsReturn {
  songs: SongPreview[];
  total: number;
  loading: boolean;
  error: string | null;
  filter: SongFilter;
  setFilter: (filter: Partial<SongFilter>) => void;
  loadMore: () => void;
  hasMore: boolean;
  refresh: () => void;
}

export function useSongs(initialFilter: SongFilter = {}): UseSongsReturn {
  const [songs, setSongs] = useState<SongPreview[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilterState] = useState<SongFilter>({ page: 1, pageSize: 12, ...initialFilter });
  const [hasMore, setHasMore] = useState(false);

  const fetchSongs = useCallback(async (f: SongFilter, append = false) => {
    setLoading(true);
    setError(null);
    try {
      const res = await songService.getAll(f);
      setSongs((prev) => append ? [...prev, ...res.data] : res.data);
      setTotal(res.total);
      setHasMore(res.hasNext);
    } catch (err) {
      setError("Không thể tải danh sách bài nhạc. Vui lòng thử lại.");
    } finally {
      setLoading(false);
    }
  }, []);

  // Fetch when filter changes (but not page – that's handled by loadMore)
  useEffect(() => {
    const f = { ...filter, page: 1 };
    setFilterState(f);
    fetchSongs(f, false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filter.genre, filter.query, filter.sortBy]);

  const setFilter = useCallback((partial: Partial<SongFilter>) => {
    setFilterState((prev) => ({ ...prev, ...partial }));
  }, []);

  const loadMore = useCallback(() => {
    if (loading || !hasMore) return;
    const nextFilter = { ...filter, page: (filter.page ?? 1) + 1 };
    setFilterState(nextFilter);
    fetchSongs(nextFilter, true);
  }, [filter, hasMore, loading, fetchSongs]);

  const refresh = useCallback(() => {
    fetchSongs({ ...filter, page: 1 }, false);
  }, [filter, fetchSongs]);

  return { songs, total, loading, error, filter, setFilter, loadMore, hasMore, refresh };
}
