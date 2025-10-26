# ---------- build stage ----------
  FROM maven:3.9.5-eclipse-temurin-21 AS build
  WORKDIR /build
  
  # Build the framework module (fat jar via spring-boot-maven-plugin)
  COPY . .
  RUN mvn -q -pl atas-framework -am -DskipTests clean package
  
  # Install Playwright browsers into this layer (optional but handy)
  RUN mvn -q -pl atas-framework exec:java \
    -Dexec.mainClass=com.microsoft.playwright.CLI \
    -Dexec.args="install chromium firefox webkit"
  
  # ---------- runtime stage ----------
  FROM eclipse-temurin:21-jre-jammy AS runtime
  WORKDIR /app
  
  # Copy the Spring Boot *fat* jar by its deterministic name
  COPY --from=build /build/atas-framework/target/atas-framework-exec.jar /app/app.jar
  
  # (Optional) ship the installed Playwright browsers with the image
  # The CLI puts them under /root/.cache/ms-playwright in the build stage
  COPY --from=build /root/.cache/ms-playwright/ /ms-playwright/
  ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
  ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
  # (sometimes helpful on slim images)
  ENV PLAYWRIGHT_SKIP_VALIDATE_HOST_REQUIREMENTS=1
  
  EXPOSE 8080
  ENTRYPOINT ["java","-jar","/app/app.jar"]
  