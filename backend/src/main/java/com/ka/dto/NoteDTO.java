package com.ka.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class NoteDTO {
    @NotBlank
    private String title;
    private String contentMd;
    private Long categoryId;
    private String difficultyLevel;
    private List<Long> tagIds;
}
