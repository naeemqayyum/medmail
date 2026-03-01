# AGENTS.md

## Cursor Cloud specific instructions

### Project overview
Medmail is a Java 21 / Spring Boot 3.3.4 multi-module Maven project (no Maven wrapper). The main runnable module is `admin-portal`, which serves a Thymeleaf-based web UI on port 8080 for uploading, previewing, and committing medical JSON records into MySQL.

### Prerequisites
- **Java 21** (pre-installed on the VM)
- **Maven 3.8+** (installed via `sudo apt-get install -y maven`)
- **MySQL 8.x** (installed via `sudo apt-get install -y mysql-server`)

### Starting MySQL
MySQL must be started manually in this environment (systemd is not available):
```bash
sudo mkdir -p /var/run/mysqld && sudo chown mysql:mysql /var/run/mysqld
sudo mysqld --user=mysql --datadir=/var/lib/mysql &>/tmp/mysql.log &
```
Wait ~5 seconds for it to be ready, then verify with `sudo mysql -u root -e "SELECT 1;"`.

### Build & Run
See `docs/database/README.md` for the canonical build/run commands. In short:
```bash
mvn -DskipTests clean install
mvn -pl admin-portal spring-boot:run
```
The app auto-creates the `medmail` database and applies Flyway migrations on startup (`createDatabaseIfNotExist=true` in the JDBC URL). Default connection uses `root` with no password on `localhost:3306`.

### Key gotchas
- There is **no Maven wrapper** (`mvnw`) in the repo; system Maven must be installed.
- The `fix_modules.sh` script creates the `commons`, `core`, and `api` modules with stub source files. These modules already exist in the repo, so the script is only needed if they're deleted.
- `jpa.hibernate.ddl-auto` is `validate`, so Flyway migrations **must** run before Hibernate validates the schema. This happens automatically on app startup.
- No test source files currently exist, so `mvn test` passes trivially (BUILD SUCCESS with "No tests to run").
- Integration tests use Testcontainers (MySQL), which requires Docker. Docker is not needed for running the app itself.
- The `api` module is a skeleton with no real endpoints; focus development on `admin-portal`.
