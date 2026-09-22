package com.sonharf.game

import io.ktor.http.ContentType

/**
 * Ktor version compatibility for profile-photo uploads.
 * Keeps the existing upload contract explicitly on image/webp without changing storage/auth behavior.
 */
internal val ContentType.Image.WebP: ContentType
    get() = ContentType("image", "webp")
