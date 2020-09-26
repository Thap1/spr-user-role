package com.example.spruserrole.payload;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class ChoiceRequest {
    @NotBlank
    @Size(max = 40)
    private String text;
}
