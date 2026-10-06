package com.openspace.skillplanner.api.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateTaskRequest(@NotNull Boolean completed) {
}
