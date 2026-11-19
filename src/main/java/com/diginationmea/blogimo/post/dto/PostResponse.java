package com.diginationmea.blogimo.post.dto;

import java.time.Instant;

public record PostResponse(
        Long id,
        String title,
        String content,
        Long categoryId,
        Long authorId,
        Instant createdAt,
        Instant updatedAt) {
}
