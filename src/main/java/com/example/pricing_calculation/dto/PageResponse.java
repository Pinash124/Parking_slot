package com.example.pricing_calculation.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public class PageResponse<T> {

    private final List<T> content;
    private final int page;
    private final int size;
    private final int totalPages;
    private final long totalElements;
    private final boolean first;
    private final boolean last;

    private PageResponse(Page<T> source) {
        content = source.getContent();
        page = source.getNumber();
        size = source.getSize();
        totalPages = source.getTotalPages();
        totalElements = source.getTotalElements();
        first = source.isFirst();
        last = source.isLast();
    }

    public static <T> PageResponse<T> from(Page<T> source) {
        return new PageResponse<>(source);
    }

    public List<T> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public boolean isFirst() {
        return first;
    }

    public boolean isLast() {
        return last;
    }
}
