package com.core.data;

public class Pagination {

    private Integer pageNumber;
    private Integer pageSize;

    private Pagination(Integer pageNumber, Integer pageSize) {

        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }

    public static Pagination from(Integer pageNumber, Integer pageSize) {

        Pagination pagination = null;
        if (pageNumber != null && pageSize != null) {
            pagination = new Pagination(pageNumber, pageSize);
        }
        return pagination;
    }

    public Integer getPageNumber() {

        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {

        this.pageNumber = pageNumber;
    }

    public Integer getPageSize() {

        return pageSize;
    }

    public void setPageSize(Integer pageSize) {

        this.pageSize = pageSize;
    }
}
