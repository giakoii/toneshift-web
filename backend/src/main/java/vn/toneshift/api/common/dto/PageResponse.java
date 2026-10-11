package vn.toneshift.api.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Page-based pagination response with a 1-based page number.
 *
 * @param <T>        type of the items in the page
 * @param data       items of the current page
 * @param page       current page number, starting from 1
 * @param limit      maximum number of items per page
 * @param total      total number of items across all pages
 * @param totalPages total number of pages
 * @param hasNext    whether a next page exists
 */
public record PageResponse<T>(List<T> data, int page, int limit, long total, int totalPages, boolean hasNext) {

    /**
     * Converts a Spring Data page (0-based) into a response with a 1-based page number.
     *
     * @param page the Spring Data page
     * @param <T>  type of the items in the page
     * @return the page response
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
