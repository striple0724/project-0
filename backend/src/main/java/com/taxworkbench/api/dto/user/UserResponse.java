package com.taxworkbench.api.dto.user;

import java.time.LocalDateTime;

public record UserResponse(
        Long seq,
        String id,
        String name,
        String email,
        String mobileNo,
        String useYn,
        String crtBy,
        LocalDateTime crtDt,
        String crtIp,
        String amnBy,
        LocalDateTime amnDt,
        String amnIp
) {
}
