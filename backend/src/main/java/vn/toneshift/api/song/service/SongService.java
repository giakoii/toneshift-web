package vn.toneshift.api.song.service;

import vn.toneshift.api.common.dto.PageResponse;
import vn.toneshift.api.song.dto.SongRequest;
import vn.toneshift.api.song.dto.SongResponse;
import vn.toneshift.api.song.dto.SongSummary;

import java.util.UUID;

/**
 * The signed-in user's own library of saved chord sheets.
 *
 * <p>Every method takes the caller's id and enforces ownership itself: a song belonging to someone
 * else is reported as missing, never as forbidden.
 */
public interface SongService {

    /**
     * Saves a new private song for the caller.
     *
     * @param userId  the id of the signed-in user
     * @param request the song to save
     * @return the saved song
     */
    SongResponse create(UUID userId, SongRequest request);

    /**
     * Lists the caller's songs.
     *
     * @param userId the id of the signed-in user
     * @param q      a search term matched against title and artist, may be null or blank
     * @param sort   {@code updated} for most recently changed first, or {@code title}
     * @param page   the 1-based page number
     * @param limit  the page size
     * @return the matching page of songs
     */
    PageResponse<SongSummary> listMine(UUID userId, String q, String sort, int page, int limit);

    /**
     * Returns one of the caller's songs with its full chord sheet.
     *
     * @param userId the id of the signed-in user
     * @param id     the song id
     * @return the song
     */
    SongResponse getMine(UUID userId, UUID id);

    /**
     * Replaces the contents of one of the caller's songs.
     *
     * @param userId  the id of the signed-in user
     * @param id      the song id
     * @param request the new values, optionally carrying the version last seen by the client
     * @return the updated song
     */
    SongResponse update(UUID userId, UUID id, SongRequest request);

    /**
     * Soft-deletes one of the caller's songs.
     *
     * @param userId the id of the signed-in user
     * @param id     the song id
     */
    void delete(UUID userId, UUID id);
}
