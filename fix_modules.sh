set -euo pipefail

# 1) Create missing modules
mkdir -p commons/src/main/java/com/medmail/commons
mkdir -p core/src/main/java/com/medmail/core
mkdir -p api/src/main/java/com/medmail/api

# 2) commons/pom.xml
cat > commons/pom.xml <<'XML'
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>com.medmail</groupId>
    <artifactId>medmail</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
  </parent>
  <artifactId>commons</artifactId>
  <name>medmail-commons</name>
</project>
XML

# 3) core/pom.xml (depends on commons)
cat > core/pom.xml <<'XML'
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>com.medmail</groupId>
    <artifactId>medmail</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
  </parent>
  <artifactId>core</artifactId>
  <name>medmail-core</name>
  <dependencies>
    <dependency>
      <groupId>com.medmail</groupId>
      <artifactId>commons</artifactId>
      <version>${project.version}</version>
    </dependency>
  </dependencies>
</project>
XML

# 4) tiny marker classes so modules compile
cat > commons/src/main/java/com/medmail/commons/CommonsMarker.java <<'JAVA'
package com.medmail.commons;
public final class CommonsMarker {}
JAVA

cat > core/src/main/java/com/medmail/core/CoreMarker.java <<'JAVA'
package com.medmail.core;
public final class CoreMarker {}
JAVA

# 5) ensure API app class exists (safe if already present)
if [ ! -f api/src/main/java/com/medmail/api/MedmailApiApplication.java ]; then
  cat > api/src/main/java/com/medmail/api/MedmailApiApplication.java <<'JAVA'
package com.medmail.api;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class MedmailApiApplication {
  public static void main(String[] args) { SpringApplication.run(MedmailApiApplication.class, args); }
}
JAVA
fi

# 6) sanity check for leftover org.example in POMs
if grep -R -nE 'org\.example' admin-portal/pom.xml commons/pom.xml core/pom.xml 2>/dev/null; then
  echo "WARNING: Found 'org.example' in a POM above. Replace with 'com.medmail' if present."
fi

# 7) build
mvn -q -DskipTests clean package
echo "✅ Build completed."
