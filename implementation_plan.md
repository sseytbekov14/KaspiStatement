# Milestone 5: Category Dictionary & Fuzzy Matching

This milestone introduces rule-based categorization using Apache Commons Text for Jaro-Winkler fuzzy matching. It matches parsed transaction merchants against a predefined mapping table.

## Proposed Changes

### 1. Dependencies (`pom.xml`)
- [MODIFY] `pom.xml`
  - Add `org.apache.commons:commons-text` dependency for Levenshtein/Jaro-Winkler distance logic.

---

### 2. JPA Entities & Enums (`src/main/java/com/sultan/kaspitracker/entity`)
- [NEW] `MappingSource.java`
  - Enum containing: `MANUAL`, `FUZZY_MATCHED`, `AI_FALLBACK`.
- [NEW] `MerchantCategoryMapping.java`
  - Fields: `Long id`, `String merchantPattern` (normalized string), `Category category` (ManyToOne), `MappingSource source` (Enum), `Instant createdAt`.

---

### 3. Flyway Migrations (`src/main/resources/db/migration`)
- [NEW] `V3__seed_more_categories.sql`
  - Adds missing categories: `Cash Withdrawal`, `Utilities`, `Subscriptions`. (Skipping duplicates: Groceries, Transport, Communication, Entertainment, Transfers, Other are already in V2).
- [NEW] `V4__create_merchant_category_mapping.sql`
  - Creates the `merchant_category_mappings` table.
  - Adds 20-30 seed mappings from real Kaspi statement data (e.g., MAGNUM -> Groceries, ONAY -> Transport, YANDEX.GO -> Transport, GOOGLE -> Subscriptions).

---

### 4. Matching Service (`src/main/java/com/sultan/kaspitracker/service`)
- [NEW] `CategoryMatcherService.java`
  - Maintains an internal cache/fetch mechanism for `MerchantCategoryMapping`.
  - Normalizes merchant string (uppercase, trim).
  - Checks for an exact match against the normalized mapping string.
  - If no exact match, performs fuzzy matching via Jaro-Winkler distance on all dictionary entries.
  - Returns `Optional<Category>` if highest score >= `MATCH_THRESHOLD` (e.g. 0.85).
  - Handles edge cases cleanly (null/empty inputs return `Optional.empty()`).

---

### 5. Persistence Service Update (`src/main/java/com/sultan/kaspitracker/service`)
- [MODIFY] `StatementPersistenceService.java`
  - Inject `CategoryMatcherService`.
  - Upon converting `ParsedTransaction` to `Transaction` entity, pass `merchantDetails` to the matcher.
  - If a category is matched, set it on the `Transaction` entity before saving.
  - If no match, leave it as null (will be categorized in a later AI fallback milestone).

---

### 6. Unit Tests (`src/test/java/com/sultan/kaspitracker/service`)
- [NEW] `CategoryMatcherServiceTest.java`
  - Pure unit tests using JUnit 5 (no Spring Context, no Testcontainers).
  - Mocks the `MerchantCategoryMappingRepository`.
  - Verifies exact matching.
  - Verifies fuzzy matching (e.g., "MAGNUM CASH&CARRY" vs "Magnum Cash&Carry").
  - Verifies threshold boundaries (low similarity returns empty).
  - Verifies null/empty string handling.

## Verification Plan

### Automated Tests
- Run `mvn clean test` to ensure `CategoryMatcherServiceTest` passes cleanly without requiring a dockerized environment.

### Manual Verification
- Output statistics on how many of the 131 sample transactions would now be categorized versus how many remain uncategorized given the seeded `V4` rules.
