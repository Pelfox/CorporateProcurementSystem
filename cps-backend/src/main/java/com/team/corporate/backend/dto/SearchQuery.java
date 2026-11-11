package com.team.corporate.backend.dto;

import com.team.corporate.backend.validation.TextValue;

public record SearchQuery(@TextValue String q) {
}
