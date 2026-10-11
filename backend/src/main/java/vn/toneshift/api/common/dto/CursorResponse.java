package vn.toneshift.api.common.dto;

import java.util.List;

/**
 * Cursor-based pagination response; {@code nextCursor} is null when there is no more data.
 *
 * @param <T>        type of the items in the page
 * @param data       items of the current page
 * @param nextCursor opaque cursor for the next page, or null if there is none
 * @param hasNext    whether more data exists after this page
 * @param total      total number of matching items
 */
public record CursorResponse<T>(List<T> data, String nextCursor, boolean hasNext, long total) {
}
