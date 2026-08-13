package com.minitrello.application.task.dto;

import java.util.UUID;

public record TagResponse(UUID id, String name, String color) {
}
