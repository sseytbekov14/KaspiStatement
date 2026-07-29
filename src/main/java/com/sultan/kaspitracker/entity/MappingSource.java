package com.sultan.kaspitracker.entity;

/**
 * Indicates how a merchant was mapped to a category.
 */
public enum MappingSource {
    MANUAL,
    FUZZY_MATCHED,
    AI_FALLBACK
}
