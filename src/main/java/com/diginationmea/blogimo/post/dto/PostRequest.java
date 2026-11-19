package com.diginationmea.blogimo.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank String content,
        @NotNull Long categoryId,
        @NotNull Long authorId) {
}
