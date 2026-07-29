package io.github.rohits1402.gimmecomments.dto;

import io.github.rohits1402.gimmecomments.model.Gender;

public record UpdateProfileRequest(String name, Gender gender, String birthday) {
}
