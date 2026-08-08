package com.kleos.transfers.common.dto;

import jakarta.validation.constraints.Size;

/**
 * Replace identity media metadata (photo or crest). Null fields clear the stored value.
 */
public record UpdateIdentityMediaRequest(
        @Size(max = 1000) String imageUrl,
        @Size(max = 500) String attribution,
        @Size(max = 80) String license,
        @Size(max = 40) String source
) {
}
