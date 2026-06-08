package com.example.agentplatform.tools.service;

import com.example.agentplatform.tools.dto.DockerDeployResultDTO;
import com.example.agentplatform.tools.dto.DockerGenerateRequestDTO;
import com.example.agentplatform.tools.dto.DockerGenerateResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Service
public class DockerGeneratorService {

    private static final Pattern JAVA_VERSION_PATTERN = Pattern.compile("<java\\.version>(\\d+)</java\\.version>");
    private static final Pattern ARTIFACT_ID_PATTERN = Pattern.compile("<artifactId>([^<]+)</artifactId>");
    private static final Pattern VERSION_PATTERN = Pattern.compile("<version>([^<]+)</version>");
    private static final Pattern PACKAGING_PATTERN = Pattern.compile("<packaging>([^<]+)</packaging>");
    private static final Pattern MAIN_CLASS_PATTERN = Pattern.compile("<mainClass>([^<]+)</mainClass>");
    private static final Pattern SPRING_BOOT_MAIN = Pattern.compile("@SpringBootApplication");
    private static final Pattern GRADLE_JAVA_VERSION = Pattern.compile("sourceCompatibility\\s*=\\s*['\"]?(\\d+)['\"]?");
    private static final Pattern GRADLE_JAVA_TOOLCHAIN = Pattern.compile("languageVersion\\s*=\\s*JavaLanguageVersion\\.of\\(\\s*['\"]?(\\d+)['\"]?\\s*\\)");
    private static final Pattern GRADLE_BOOT_JAR_MAIN = Pattern.compile("mainClass\\.set\\s*=\\s*['\"]([^'\"]+)['\"]");

    public DockerGenerateResultDTO generate(DockerGenerateRequestDTO request) {
        String projectPath = request.getProjectPath();
        File projectDir = new File(projectPath);

        if (!projectDir.exists() || !projectDir.isDirectory()) {
            throw new IllegalArgumentException("项目路径不存在或不是目录: " + projectPath);
        }

        String buildTool = detectBuildTool(projectDir, request.getBuildTool());
        String jdkVersion = detectJdkVersion(projectDir, buildTool, request.getJdkVersion());
        String projectName = detectProjectName(projectDir, buildTool);
        String packaging = detectPackaging(projectDir, buildTool);
        String mainClass = detectMainClass(projectDir, buildTool);

        String dockerfileContent = generateDockerfile(projectName, jdkVersion, buildTool, packaging, mainClass, request);
        String dockerignoreContent = generateDockerignore(buildTool);
        String dockerComposeContent = request.getIncludeDockerCompose() != null && request.getIncludeDockerCompose()
                ? generateDockerCompose(projectName, request) : null;

        boolean dockerfileWritten = writeFile(projectDir, "Dockerfile", dockerfileContent);
        boolean dockerignoreWritten = writeFile(projectDir, ".dockerignore", dockerignoreContent);
        boolean dockerComposeWritten = dockerComposeContent != null && writeFile(projectDir, "docker-compose.yml", dockerComposeContent);

        return DockerGenerateResultDTO.builder()
                .projectPath(projectPath)
                .projectName(projectName)
                .buildTool(buildTool)
                .jdkVersion(jdkVersion)
                .packaging(packaging)
                .mainClass(mainClass)
                .dockerfileContent(dockerfileContent)
                .dockerignoreContent(dockerignoreContent)
                .dockerComposeContent(dockerComposeContent)
                .dockerfileWritten(dockerfileWritten)
                .dockerignoreWritten(dockerignoreWritten)
                .dockerComposeWritten(dockerComposeWritten)
                .build();
    }

    public DockerDeployResultDTO deploy(DockerGenerateRequestDTO request) {
        String projectPath = request.getProjectPath();
        File projectDir = new File(projectPath);

        if (!projectDir.exists() || !projectDir.isDirectory()) {
            return DockerDeployResultDTO.builder()
                    .success(false)
                    .errorMessage("项目路径不存在或不是目录: " + projectPath)
                    .build();
        }

        File dockerfile = new File(projectDir, "Dockerfile");
        if (!dockerfile.exists()) {
            generate(request);
        }

        String projectName = detectProjectName(projectDir, detectBuildTool(projectDir, request.getBuildTool()));
        String imageName = StringUtils.hasText(request.getImageName()) ? request.getImageName() : projectName.toLowerCase().replaceAll("[^a-z0-9._-]", "-");
        String imageTag = StringUtils.hasText(request.getImageTag()) ? request.getImageTag() : "latest";
        String fullImageName = imageName + ":" + imageTag;
        int port = request.getPort() != null ? request.getPort() : 8080;
        String containerName = imageName + "-container";

        StringBuilder buildLog = new StringBuilder();
        StringBuilder runLog = new StringBuilder();

        try {
            ProcessBuilder buildPb = new ProcessBuilder("docker", "build", "-t", fullImageName, projectPath);
            buildPb.redirectErrorStream(true);
            Process buildProcess = buildPb.start();
            String buildOutput = readProcessOutput(buildProcess);
            buildLog.append(buildOutput);
            boolean buildFinished = buildProcess.waitFor(600, java.util.concurrent.TimeUnit.SECONDS);

            if (!buildFinished) {
                buildProcess.destroyForcibly();
                return DockerDeployResultDTO.builder()
                        .imageName(fullImageName)
                        .buildLog(buildLog.toString())
                        .success(false)
                        .errorMessage("Docker build 超时")
                        .build();
            }

            int buildExitCode = buildProcess.exitValue();
            if (buildExitCode != 0) {
                return DockerDeployResultDTO.builder()
                        .imageName(fullImageName)
                        .buildLog(buildLog.toString())
                        .success(false)
                        .errorMessage("Docker build 失败，退出码: " + buildExitCode)
                        .build();
            }

            stopExistingContainer(containerName, runLog);

            ProcessBuilder runPb = new ProcessBuilder(
                    "docker", "run", "-d",
                    "--name", containerName,
                    "-p", port + ":" + port,
                    fullImageName
            );
            runPb.redirectErrorStream(true);
            Process runProcess = runPb.start();
            String runOutput = readProcessOutput(runProcess);
            runLog.append(runOutput);
            boolean runFinished = runProcess.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);

            if (!runFinished) {
                runProcess.destroyForcibly();
                return DockerDeployResultDTO.builder()
                        .imageName(fullImageName)
                        .buildLog(buildLog.toString())
                        .runLog(runLog.toString())
                        .success(false)
                        .errorMessage("Docker run 超时")
                        .build();
            }

            int runExitCode = runProcess.exitValue();
            String containerId = runOutput.trim();

            if (runExitCode != 0 || containerId.isEmpty()) {
                return DockerDeployResultDTO.builder()
                        .imageName(fullImageName)
                        .containerName(containerName)
                        .buildLog(buildLog.toString())
                        .runLog(runLog.toString())
                        .success(false)
                        .errorMessage("Docker run 失败，退出码: " + runExitCode)
                        .build();
            }

            return DockerDeployResultDTO.builder()
                    .imageName(fullImageName)
                    .containerId(containerId.substring(0, Math.min(12, containerId.length())))
                    .containerName(containerName)
                    .mappedPort(port)
                    .buildLog(buildLog.toString())
                    .runLog(runLog.toString())
                    .success(true)
                    .status("running")
                    .build();

        } catch (Exception e) {
            log.error("Docker 部署失败", e);
            return DockerDeployResultDTO.builder()
                    .imageName(fullImageName)
                    .containerName(containerName)
                    .buildLog(buildLog.toString())
                    .runLog(runLog.toString())
                    .success(false)
                    .errorMessage("Docker 部署异常: " + e.getMessage())
                    .build();
        }
    }

    private void stopExistingContainer(String containerName, StringBuilder runLog) {
        try {
            ProcessBuilder stopPb = new ProcessBuilder("docker", "rm", "-f", containerName);
            stopPb.redirectErrorStream(true);
            Process stopProcess = stopPb.start();
            String stopOutput = readProcessOutput(stopProcess);
            stopProcess.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
            if (!stopOutput.contains("No such container") && !stopOutput.trim().isEmpty()) {
                runLog.append("已移除旧容器: ").append(containerName).append("\n");
            }
        } catch (Exception ignored) {
        }
    }

    String detectBuildTool(File projectDir, String requestedBuildTool) {
        if (StringUtils.hasText(requestedBuildTool)) {
            return requestedBuildTool;
        }
        boolean hasPom = new File(projectDir, "pom.xml").exists();
        boolean hasGradle = new File(projectDir, "build.gradle").exists() || new File(projectDir, "build.gradle.kts").exists();
        if (hasPom && hasGradle) {
            return "maven";
        }
        if (hasGradle) {
            return "gradle";
        }
        if (hasPom) {
            return "maven";
        }
        return "maven";
    }

    String detectJdkVersion(File projectDir, String buildTool, String requestedJdkVersion) {
        if (StringUtils.hasText(requestedJdkVersion)) {
            return requestedJdkVersion;
        }

        if ("maven".equals(buildTool)) {
            File pomFile = new File(projectDir, "pom.xml");
            if (pomFile.exists()) {
                try {
                    String pomContent = Files.readString(pomFile.toPath(), StandardCharsets.UTF_8);
                    Matcher matcher = JAVA_VERSION_PATTERN.matcher(pomContent);
                    if (matcher.find()) {
                        return matcher.group(1);
                    }
                    if (pomContent.contains("spring-boot-starter-parent")) {
                        Matcher verMatcher = Pattern.compile("<version>(\\d+\\.\\d+)").matcher(pomContent);
                        if (verMatcher.find()) {
                            double ver = Double.parseDouble(verMatcher.group(1));
                            if (ver >= 3.2) return "17";
                            if (ver >= 3.0) return "17";
                            if (ver >= 2.7) return "11";
                            return "8";
                        }
                    }
                } catch (IOException ignored) {
                }
            }
        } else if ("gradle".equals(buildTool)) {
            File buildGradle = new File(projectDir, "build.gradle");
            File buildGradleKts = new File(projectDir, "build.gradle.kts");
            File gradleFile = buildGradle.exists() ? buildGradle : buildGradleKts;
            if (gradleFile.exists()) {
                try {
                    String content = Files.readString(gradleFile.toPath(), StandardCharsets.UTF_8);
                    Matcher matcher = GRADLE_JAVA_VERSION.matcher(content);
                    if (matcher.find()) {
                        return matcher.group(1);
                    }
                    Matcher toolchainMatcher = GRADLE_JAVA_TOOLCHAIN.matcher(content);
                    if (toolchainMatcher.find()) {
                        return toolchainMatcher.group(1);
                    }
                } catch (IOException ignored) {
                }
            }
        }

        return "17";
    }

    String detectProjectName(File projectDir, String buildTool) {
        if ("maven".equals(buildTool)) {
            File pomFile = new File(projectDir, "pom.xml");
            if (pomFile.exists()) {
                try {
                    String pomContent = Files.readString(pomFile.toPath(), StandardCharsets.UTF_8);
                    Matcher matcher = ARTIFACT_ID_PATTERN.matcher(pomContent);
                    if (matcher.find()) {
                        return matcher.group(1);
                    }
                } catch (IOException ignored) {
                }
            }
        } else if ("gradle".equals(buildTool)) {
            return projectDir.getName();
        }
        return projectDir.getName();
    }

    String detectPackaging(File projectDir, String buildTool) {
        if ("maven".equals(buildTool)) {
            File pomFile = new File(projectDir, "pom.xml");
            if (pomFile.exists()) {
                try {
                    String pomContent = Files.readString(pomFile.toPath(), StandardCharsets.UTF_8);
                    Matcher matcher = PACKAGING_PATTERN.matcher(pomContent);
                    if (matcher.find()) {
                        return matcher.group(1);
                    }
                } catch (IOException ignored) {
                }
            }
        }
        return "jar";
    }

    String detectMainClass(File projectDir, String buildTool) {
        if ("maven".equals(buildTool)) {
            File pomFile = new File(projectDir, "pom.xml");
            if (pomFile.exists()) {
                try {
                    String pomContent = Files.readString(pomFile.toPath(), StandardCharsets.UTF_8);
                    Matcher matcher = MAIN_CLASS_PATTERN.matcher(pomContent);
                    if (matcher.find()) {
                        return matcher.group(1);
                    }
                } catch (IOException ignored) {
                }
            }
        }

        try {
            Path srcMainJava = Paths.get(projectDir.getAbsolutePath(), "src", "main", "java");
            if (Files.exists(srcMainJava)) {
                try (Stream<Path> paths = Files.walk(srcMainJava)) {
                    List<Path> springMainClasses = paths
                            .filter(p -> p.toString().endsWith(".java"))
                            .filter(p -> {
                                try {
                                    return Files.readString(p, StandardCharsets.UTF_8).contains("@SpringBootApplication");
                                } catch (IOException e) {
                                    return false;
                                }
                            })
                            .toList();

                    if (!springMainClasses.isEmpty()) {
                        Path mainFile = springMainClasses.get(0);
                        String relativePath = srcMainJava.relativize(mainFile).toString();
                        return relativePath.replace(File.separatorChar, '.').replace(".java", "");
                    }
                }
            }
        } catch (IOException ignored) {
        }

        return null;
    }

    String generateDockerfile(String projectName, String jdkVersion, String buildTool, String packaging,
                             String mainClass, DockerGenerateRequestDTO request) {
        StringBuilder sb = new StringBuilder();
        int port = request.getPort() != null ? request.getPort() : 8080;

        sb.append("# ===== 多阶段构建 Dockerfile =====\n");
        sb.append("# 自动生成 by Docker Generator Tool\n\n");

        sb.append("# ---- 阶段1: 构建 ----\n");
        sb.append("FROM maven:3.9-eclipse-temurin-").append(jdkVersion).append(" AS builder\n");
        sb.append("WORKDIR /build\n\n");

        if ("maven".equals(buildTool)) {
            sb.append("# 先复制 pom.xml，利用 Docker 缓存加速依赖下载\n");
            sb.append("COPY pom.xml .\n");
            sb.append("RUN mvn dependency:go-offline -B\n\n");
            sb.append("# 复制源码并构建\n");
            sb.append("COPY src ./src\n");
            sb.append("RUN mvn package -DskipTests -B\n\n");
        } else {
            sb.append("# 先复制 Gradle 配置文件，利用 Docker 缓存加速依赖下载\n");
            sb.append("COPY build.gradle settings.gradle* gradle.properties* ./\n");
            sb.append("COPY gradle ./gradle\n");
            sb.append("COPY gradlew ./gradlew\n");
            sb.append("RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon\n\n");
            sb.append("# 复制源码并构建\n");
            sb.append("COPY src ./src\n");
            sb.append("RUN ./gradlew build -x test --no-daemon\n\n");
        }

        sb.append("# ---- 阶段2: 运行 ----\n");
        sb.append("FROM eclipse-temurin:").append(jdkVersion).append("-jre-alpine\n\n");
        sb.append("LABEL maintainer=\"docker-generator\"\n");
        sb.append("LABEL description=\"").append(projectName).append("\"\n\n");

        sb.append("WORKDIR /app\n\n");

        if ("maven".equals(buildTool)) {
            sb.append("COPY --from=builder /build/target/*.jar app.jar\n\n");
        } else {
            sb.append("COPY --from=builder /build/build/libs/*.jar app.jar\n\n");
        }

        sb.append("# 创建非 root 用户\n");
        sb.append("RUN addgroup -S appgroup && adduser -S appuser -G appgroup\n");
        sb.append("USER appuser\n\n");

        sb.append("EXPOSE ").append(port).append("\n\n");

        StringBuilder entrypoint = new StringBuilder("java");
        if (StringUtils.hasText(request.getJvmOpts())) {
            entrypoint.append(" ").append(request.getJvmOpts());
        } else {
            entrypoint.append(" -XX:+UseG1GC");
            entrypoint.append(" -XX:MaxRAMPercentage=75.0");
            entrypoint.append(" -Djava.security.egd=file:/dev/./urandom");
        }
        if (StringUtils.hasText(request.getSpringProfile())) {
            entrypoint.append(" -Dspring.profiles.active=").append(request.getSpringProfile());
        }
        entrypoint.append(" -jar app.jar");

        sb.append("ENTRYPOINT [");
        String[] parts = entrypoint.toString().split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            sb.append("\"").append(parts[i]).append("\"");
            if (i < parts.length - 1) sb.append(", ");
        }
        sb.append("]\n");

        return sb.toString();
    }

    String generateDockerignore(String buildTool) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 构建产物\n");
        sb.append("target/\n");
        sb.append("build/\n");
        sb.append("out/\n\n");
        sb.append("# IDE\n");
        sb.append(".idea/\n");
        sb.append(".vscode/\n");
        sb.append("*.iml\n");
        sb.append(".settings/\n");
        sb.append(".classpath\n");
        sb.append(".project\n\n");
        sb.append("# Git\n");
        sb.append(".git/\n");
        sb.append(".gitignore\n\n");
        sb.append("# Docker\n");
        sb.append("Dockerfile\n");
        sb.append("docker-compose*.yml\n");
        sb.append(".dockerignore\n\n");
        sb.append("# 日志\n");
        sb.append("*.log\n\n");
        sb.append("# 临时文件\n");
        sb.append("*.tmp\n");
        sb.append("*.bak\n");
        sb.append("*.swp\n\n");
        sb.append("# 文档\n");
        sb.append("*.md\n");
        sb.append("docs/\n\n");
        sb.append("# OS\n");
        sb.append(".DS_Store\n");
        sb.append("Thumbs.db\n");
        return sb.toString();
    }

    String generateDockerCompose(String projectName, DockerGenerateRequestDTO request) {
        int port = request.getPort() != null ? request.getPort() : 8080;
        String imageName = StringUtils.hasText(request.getImageName()) ? request.getImageName() : projectName.toLowerCase().replaceAll("[^a-z0-9._-]", "-");
        String imageTag = StringUtils.hasText(request.getImageTag()) ? request.getImageTag() : "latest";

        StringBuilder sb = new StringBuilder();
        sb.append("version: '3.8'\n\n");
        sb.append("services:\n");
        sb.append("  ").append(imageName).append(":\n");
        sb.append("    build:\n");
        sb.append("      context: .\n");
        sb.append("      dockerfile: Dockerfile\n");
        sb.append("    image: ").append(imageName).append(":").append(imageTag).append("\n");
        sb.append("    container_name: ").append(imageName).append("-container\n");
        sb.append("    ports:\n");
        sb.append("      - \"").append(port).append(":").append(port).append("\"\n");
        sb.append("    environment:\n");
        sb.append("      - TZ=Asia/Shanghai\n");
        sb.append("      - JAVA_OPTS=-XX:+UseG1GC -XX:MaxRAMPercentage=75.0\n");
        if (StringUtils.hasText(request.getSpringProfile())) {
            sb.append("      - SPRING_PROFILES_ACTIVE=").append(request.getSpringProfile()).append("\n");
        }
        sb.append("    restart: unless-stopped\n");

        return sb.toString();
    }

    private boolean writeFile(File projectDir, String fileName, String content) {
        try {
            Path filePath = Paths.get(projectDir.getAbsolutePath(), fileName);
            Files.writeString(filePath, content, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            log.error("写入文件失败: {}", fileName, e);
            return false;
        }
    }

    private String readProcessOutput(Process process) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (var reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }
}
