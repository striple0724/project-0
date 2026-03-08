package com.taxworkbench.api.dto.common;

public record PageInfo(int number, int size, long totalElements, int totalPages) {
}
