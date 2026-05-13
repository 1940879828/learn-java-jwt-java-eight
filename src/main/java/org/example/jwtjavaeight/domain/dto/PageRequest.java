package org.example.jwtjavaeight.domain.dto;

import java.util.Set;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

public abstract class PageRequest {
    @Min(1)
    private int page = 1;

    @Min(1)
    @Max(100)
    private int size = 20;

    private String sort = "id";
    private String order = "asc";

    protected abstract Set<String> allowedSortColumns();

    public String getSafeSort() {
        return allowedSortColumns().contains(sort) ? sort : "id";
    }

    public String getSafeOrder() {
        return "desc".equalsIgnoreCase(order) ? "desc" : "asc";
    }

    public int getOffset() {
        return (page - 1) * size;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }
}
