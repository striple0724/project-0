package com.taxworkbench.api.dto.common;

import java.util.List;

public record PagedResponse<T>(List<T> data, PageInfo page) {
}
