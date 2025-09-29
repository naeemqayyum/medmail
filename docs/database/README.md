
### Windows (PowerShell)
```powershell
New-Item -ItemType Directory -Force -Path docs/database | Out-Null
@'
# Flyway: how to apply migrations (MySQL)

**Migrations live at:** `database/src/main/resources/db/migration`  
They’re packaged in the **database** module JAR and auto-applied by Spring Boot on startup.

## 0) Prereqs
- Java 21, Maven, MySQL 8.x
- DB user with DDL rights (CREATE/ALTER/INDEX)

Set env vars (or use your own):
$env:DB_HOST="localhost"
$env:DB_PORT="3306"
$env:DB_NAME="medmail"
$env:DB_USER="root"
$env:DB_PASS=""

## 1) First-time setup
mvn -DskipTests clean install

## 2) Run the app (auto-migrates)
mvn -pl admin-portal spring-boot:run

## 3) Verify
USE medmail;
SELECT version, description, success, installed_on
FROM flyway_schema_history
ORDER BY installed_rank;

## 4) New migration
Create `database/src/main/resources/db/migration/V003__short_description.sql`,
then: `mvn -DskipTests clean install` and restart the app.

## 5) If a migration fails (repair)
mvn -pl admin-portal flyway:repair `
  -Dflyway.url="jdbc:mysql://${env:DB_HOST}:${env:DB_PORT}/${env:DB_NAME}?useSSL=false&serverTimezone=UTC" `
  -Dflyway.user="${env:DB_USER}" -Dflyway.password="${env:DB_PASS}"

## 6) Tips
- Don’t edit applied versions; add new ones.
- MySQL has no `CREATE INDEX IF NOT EXISTS`.
- `baseline-on-migrate` and `clean-disabled` are enabled.
'@ | Set-Content -Path docs/database/README.md -Encoding UTF8
