# Database

The schema is owned by **Flyway** and lives in `backend/src/main/resources/db/migration`:

| File | Purpose |
|------|---------|
| `V1__schema.sql` | All tables, FKs, CHECK constraints and indexes (MySQL 8) |
| `V2__seed_catalog.sql` | 7 categories, 17 demo products and their inventory |

Flyway runs automatically when the backend starts, so there is nothing to import by hand.
Demo **users** and sample lifecycle/listing data are created by `BootstrapRunner` (Java, so passwords are
BCrypt-hashed) when `SEED_DEMO_DATA=true`.

To create the database manually: `CREATE DATABASE reloop CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`
