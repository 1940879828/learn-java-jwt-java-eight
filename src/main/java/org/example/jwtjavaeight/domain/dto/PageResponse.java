package org.example.jwtjavaeight.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class PageResponse<T> {
    // Getters and Setters
    private List<T> items;
    private Integer page;
    private Integer size;
    private Long total;
    private Integer totalPages;

    public static <T> PageResponse<T> of(List<T> items, int page, int size, long total) {
        PageResponse<T> response = new PageResponse<>();
        response.setItems(items);
        response.setPage(page);
        response.setSize(size);
        response.setTotal(total);
        response.setTotalPages((int) Math.ceil((double) total / size));
        return response;
    }

}
