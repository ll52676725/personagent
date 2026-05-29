package com.example.agentplatform.tools.service;

import com.example.agentplatform.tools.dto.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiskAnalysisService {

    private static final int MAX_CALCULATE_FILES = 50000;
    private static final int MAX_DEPTH = 6;
    private static final int TOP_FOLDERS_LIMIT = 15;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Value("${agent.platform.default-model:spring-ai}")
    private String modelName;

    @Value("${agent.platform.ai.fallback-enabled:true}")
    private boolean fallbackEnabled;

    private static final Map<String, String> EXTENSION_CATEGORY_MAP = new HashMap<>();
    private static final Map<String, String> CATEGORY_LABEL_MAP = new LinkedHashMap<>();
    private static final Map<String, String> CATEGORY_COLOR_MAP = new HashMap<>();

    static {
        EXTENSION_CATEGORY_MAP.put("jpg", "image"); EXTENSION_CATEGORY_MAP.put("jpeg", "image");
        EXTENSION_CATEGORY_MAP.put("png", "image"); EXTENSION_CATEGORY_MAP.put("gif", "image");
        EXTENSION_CATEGORY_MAP.put("bmp", "image"); EXTENSION_CATEGORY_MAP.put("svg", "image");
        EXTENSION_CATEGORY_MAP.put("webp", "image"); EXTENSION_CATEGORY_MAP.put("ico", "image");
        EXTENSION_CATEGORY_MAP.put("tiff", "image"); EXTENSION_CATEGORY_MAP.put("tif", "image");
        EXTENSION_CATEGORY_MAP.put("raw", "image"); EXTENSION_CATEGORY_MAP.put("psd", "image");

        EXTENSION_CATEGORY_MAP.put("mp4", "video"); EXTENSION_CATEGORY_MAP.put("avi", "video");
        EXTENSION_CATEGORY_MAP.put("mkv", "video"); EXTENSION_CATEGORY_MAP.put("mov", "video");
        EXTENSION_CATEGORY_MAP.put("wmv", "video"); EXTENSION_CATEGORY_MAP.put("flv", "video");
        EXTENSION_CATEGORY_MAP.put("webm", "video"); EXTENSION_CATEGORY_MAP.put("m4v", "video");
        EXTENSION_CATEGORY_MAP.put("rmvb", "video"); EXTENSION_CATEGORY_MAP.put("3gp", "video");

        EXTENSION_CATEGORY_MAP.put("mp3", "audio"); EXTENSION_CATEGORY_MAP.put("wav", "audio");
        EXTENSION_CATEGORY_MAP.put("flac", "audio"); EXTENSION_CATEGORY_MAP.put("aac", "audio");
        EXTENSION_CATEGORY_MAP.put("ogg", "audio"); EXTENSION_CATEGORY_MAP.put("wma", "audio");
        EXTENSION_CATEGORY_MAP.put("m4a", "audio"); EXTENSION_CATEGORY_MAP.put("ape", "audio");

        EXTENSION_CATEGORY_MAP.put("zip", "archive"); EXTENSION_CATEGORY_MAP.put("rar", "archive");
        EXTENSION_CATEGORY_MAP.put("7z", "archive"); EXTENSION_CATEGORY_MAP.put("tar", "archive");
        EXTENSION_CATEGORY_MAP.put("gz", "archive"); EXTENSION_CATEGORY_MAP.put("bz2", "archive");
        EXTENSION_CATEGORY_MAP.put("xz", "archive"); EXTENSION_CATEGORY_MAP.put("cab", "archive");
        EXTENSION_CATEGORY_MAP.put("iso", "archive");

        EXTENSION_CATEGORY_MAP.put("exe", "program"); EXTENSION_CATEGORY_MAP.put("msi", "program");
        EXTENSION_CATEGORY_MAP.put("dll", "program"); EXTENSION_CATEGORY_MAP.put("sys", "program");
        EXTENSION_CATEGORY_MAP.put("bat", "program"); EXTENSION_CATEGORY_MAP.put("cmd", "program");
        EXTENSION_CATEGORY_MAP.put("ps1", "program"); EXTENSION_CATEGORY_MAP.put("sh", "program");
        EXTENSION_CATEGORY_MAP.put("jar", "program"); EXTENSION_CATEGORY_MAP.put("py", "program");
        EXTENSION_CATEGORY_MAP.put("java", "program"); EXTENSION_CATEGORY_MAP.put("class", "program");
        EXTENSION_CATEGORY_MAP.put("node", "program"); EXTENSION_CATEGORY_MAP.put("appx", "program");
        EXTENSION_CATEGORY_MAP.put("msix", "program");

        EXTENSION_CATEGORY_MAP.put("doc", "document"); EXTENSION_CATEGORY_MAP.put("docx", "document");
        EXTENSION_CATEGORY_MAP.put("pdf", "document"); EXTENSION_CATEGORY_MAP.put("txt", "document");
        EXTENSION_CATEGORY_MAP.put("xls", "document"); EXTENSION_CATEGORY_MAP.put("xlsx", "document");
        EXTENSION_CATEGORY_MAP.put("ppt", "document"); EXTENSION_CATEGORY_MAP.put("pptx", "document");
        EXTENSION_CATEGORY_MAP.put("odt", "document"); EXTENSION_CATEGORY_MAP.put("ods", "document");
        EXTENSION_CATEGORY_MAP.put("odp", "document"); EXTENSION_CATEGORY_MAP.put("rtf", "document");
        EXTENSION_CATEGORY_MAP.put("csv", "document"); EXTENSION_CATEGORY_MAP.put("md", "document");
        EXTENSION_CATEGORY_MAP.put("log", "document"); EXTENSION_CATEGORY_MAP.put("wps", "document");
        EXTENSION_CATEGORY_MAP.put("et", "document"); EXTENSION_CATEGORY_MAP.put("dps", "document");

        EXTENSION_CATEGORY_MAP.put("html", "code"); EXTENSION_CATEGORY_MAP.put("css", "code");
        EXTENSION_CATEGORY_MAP.put("js", "code"); EXTENSION_CATEGORY_MAP.put("ts", "code");
        EXTENSION_CATEGORY_MAP.put("jsx", "code"); EXTENSION_CATEGORY_MAP.put("tsx", "code");
        EXTENSION_CATEGORY_MAP.put("json", "code"); EXTENSION_CATEGORY_MAP.put("xml", "code");
        EXTENSION_CATEGORY_MAP.put("yaml", "code"); EXTENSION_CATEGORY_MAP.put("yml", "code");
        EXTENSION_CATEGORY_MAP.put("sql", "code"); EXTENSION_CATEGORY_MAP.put("vue", "code");
        EXTENSION_CATEGORY_MAP.put("c", "code"); EXTENSION_CATEGORY_MAP.put("cpp", "code");
        EXTENSION_CATEGORY_MAP.put("h", "code"); EXTENSION_CATEGORY_MAP.put("go", "code");
        EXTENSION_CATEGORY_MAP.put("rs", "code"); EXTENSION_CATEGORY_MAP.put("rb", "code");
        EXTENSION_CATEGORY_MAP.put("php", "code"); EXTENSION_CATEGORY_MAP.put("cs", "code");
        EXTENSION_CATEGORY_MAP.put("swift", "code"); EXTENSION_CATEGORY_MAP.put("kt", "code");
        EXTENSION_CATEGORY_MAP.put("properties", "code"); EXTENSION_CATEGORY_MAP.put("conf", "code");
        EXTENSION_CATEGORY_MAP.put("cfg", "code"); EXTENSION_CATEGORY_MAP.put("ini", "code");
        EXTENSION_CATEGORY_MAP.put("toml", "code");

        EXTENSION_CATEGORY_MAP.put("db", "database"); EXTENSION_CATEGORY_MAP.put("sqlite", "database");
        EXTENSION_CATEGORY_MAP.put("mdb", "database"); EXTENSION_CATEGORY_MAP.put("accdb", "database");

        EXTENSION_CATEGORY_MAP.put("ttf", "font"); EXTENSION_CATEGORY_MAP.put("otf", "font");
        EXTENSION_CATEGORY_MAP.put("woff", "font"); EXTENSION_CATEGORY_MAP.put("woff2", "font");
        EXTENSION_CATEGORY_MAP.put("eot", "font");

        EXTENSION_CATEGORY_MAP.put("tmp", "temp"); EXTENSION_CATEGORY_MAP.put("temp", "temp");
        EXTENSION_CATEGORY_MAP.put("bak", "temp"); EXTENSION_CATEGORY_MAP.put("old", "temp");
        EXTENSION_CATEGORY_MAP.put("cache", "temp");

        CATEGORY_LABEL_MAP.put("image", "图片");
        CATEGORY_LABEL_MAP.put("video", "视频");
        CATEGORY_LABEL_MAP.put("audio", "音频");
        CATEGORY_LABEL_MAP.put("archive", "压缩包");
        CATEGORY_LABEL_MAP.put("program", "程序/代码");
        CATEGORY_LABEL_MAP.put("document", "文档");
        CATEGORY_LABEL_MAP.put("code", "代码/配置");
        CATEGORY_LABEL_MAP.put("database", "数据库");
        CATEGORY_LABEL_MAP.put("font", "字体");
        CATEGORY_LABEL_MAP.put("temp", "临时文件");
        CATEGORY_LABEL_MAP.put("other", "其他");

        CATEGORY_COLOR_MAP.put("image", "#06b6d4");
        CATEGORY_COLOR_MAP.put("video", "#f43f5e");
        CATEGORY_COLOR_MAP.put("audio", "#8b5cf6");
        CATEGORY_COLOR_MAP.put("archive", "#f59e0b");
        CATEGORY_COLOR_MAP.put("program", "#10b981");
        CATEGORY_COLOR_MAP.put("document", "#3b82f6");
        CATEGORY_COLOR_MAP.put("code", "#6366f1");
        CATEGORY_COLOR_MAP.put("database", "#ec4899");
        CATEGORY_COLOR_MAP.put("font", "#14b8a6");
        CATEGORY_COLOR_MAP.put("temp", "#6b7280");
        CATEGORY_COLOR_MAP.put("other", "#9ca3af");
    }

    public List<DriveInfoDTO> getAvailableDrives() {
        List<DriveInfoDTO> drives = new ArrayList<>();
        for (File root : File.listRoots()) {
            try {
                String path = root.getAbsolutePath();
                String driveLetter = path.substring(0, 1);
                long totalSpace = root.getTotalSpace();
                long freeSpace = root.getFreeSpace();
                long usedSpace = totalSpace - freeSpace;

                String fileSystem = "";
                String driveType = "本地磁盘";
                try {
                    for (FileStore store : FileSystems.getDefault().getFileStores()) {
                        if (store.toString().startsWith(driveLetter + ":")) {
                            fileSystem = store.type();
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.debug("无法获取文件系统类型: {}", driveLetter);
                }

                drives.add(DriveInfoDTO.builder()
                        .driveLetter(driveLetter)
                        .displayName(driveLetter + ": 盘")
                        .totalSpace(totalSpace)
                        .usedSpace(usedSpace)
                        .freeSpace(freeSpace)
                        .fileSystem(fileSystem)
                        .driveType(driveType)
                        .build());
            } catch (Exception e) {
                log.warn("获取盘符信息失败: {}", root.getAbsolutePath(), e);
            }
        }
        return drives;
    }

    public DriveAnalysisResultDTO analyzeDrive(String driveLetter, Integer maxDepth) {
        long startTime = System.currentTimeMillis();
        int depth = maxDepth != null ? Math.min(maxDepth, MAX_DEPTH) : MAX_DEPTH;

        String drivePath = driveLetter + ":\\";
        File driveRoot = new File(drivePath);

        DriveInfoDTO driveInfo = buildDriveInfo(driveLetter, driveRoot);

        Map<String, AtomicLong> categorySizes = new ConcurrentHashMap<>();
        Map<String, AtomicLong> categoryCounts = new ConcurrentHashMap<>();
        Map<String, AtomicLong> folderSizes = new ConcurrentHashMap<>();
        Map<String, AtomicLong> folderCounts = new ConcurrentHashMap<>();
        AtomicLong totalScannedSize = new AtomicLong(0);
        AtomicLong totalScannedFiles = new AtomicLong(0);

        scanDirectory(driveRoot, 0, depth, categorySizes, categoryCounts,
                folderSizes, folderCounts, totalScannedSize, totalScannedFiles);

        List<FileTypeStatDTO> fileTypeStats = buildFileTypeStats(categorySizes, categoryCounts, totalScannedSize.get());
        List<FolderStatDTO> topFolders = buildTopFolders(folderSizes, folderCounts, totalScannedSize.get());
        List<CleanupSuggestionDTO> suggestions = generateCleanupSuggestions(driveLetter, categorySizes, folderSizes);

        long duration = System.currentTimeMillis() - startTime;

        return DriveAnalysisResultDTO.builder()
                .driveInfo(driveInfo)
                .fileTypeStats(fileTypeStats)
                .topFolders(topFolders)
                .cleanupSuggestions(suggestions)
                .totalScannedSize(totalScannedSize.get())
                .totalScannedFiles(totalScannedFiles.intValue())
                .scanDurationMs(duration)
                .build();
    }

    private DriveInfoDTO buildDriveInfo(String driveLetter, File driveRoot) {
        long totalSpace = driveRoot.getTotalSpace();
        long freeSpace = driveRoot.getFreeSpace();
        long usedSpace = totalSpace - freeSpace;

        String fileSystem = "";
        try {
            for (FileStore store : FileSystems.getDefault().getFileStores()) {
                if (store.toString().startsWith(driveLetter + ":")) {
                    fileSystem = store.type();
                    break;
                }
            }
        } catch (Exception e) {
            log.debug("无法获取文件系统类型: {}", driveLetter);
        }

        return DriveInfoDTO.builder()
                .driveLetter(driveLetter)
                .displayName(driveLetter + ": 盘")
                .totalSpace(totalSpace)
                .usedSpace(usedSpace)
                .freeSpace(freeSpace)
                .fileSystem(fileSystem)
                .driveType("本地磁盘")
                .build();
    }

    private void scanDirectory(File dir, int currentDepth, int maxDepth,
                               Map<String, AtomicLong> categorySizes,
                               Map<String, AtomicLong> categoryCounts,
                               Map<String, AtomicLong> folderSizes,
                               Map<String, AtomicLong> folderCounts,
                               AtomicLong totalScannedSize,
                               AtomicLong totalScannedFiles) {
        if (currentDepth > maxDepth || dir == null || !dir.exists()) {
            return;
        }

        File[] files;
        try {
            files = dir.listFiles();
        } catch (SecurityException e) {
            log.debug("无权限访问: {}", dir.getAbsolutePath());
            return;
        }

        if (files == null) return;

        for (File file : files) {
            try {
                if (file.isFile()) {
                    long size = file.length();
                    String category = getFileCategory(file.getName());
                    categorySizes.computeIfAbsent(category, k -> new AtomicLong(0)).addAndGet(size);
                    categoryCounts.computeIfAbsent(category, k -> new AtomicLong(0)).incrementAndGet();
                    totalScannedSize.addAndGet(size);
                    totalScannedFiles.incrementAndGet();

                    if (currentDepth <= 3) {
                        String topFolder = getTopLevelFolder(file, dir);
                        if (topFolder != null) {
                            folderSizes.computeIfAbsent(topFolder, k -> new AtomicLong(0)).addAndGet(size);
                            folderCounts.computeIfAbsent(topFolder, k -> new AtomicLong(0)).incrementAndGet();
                        }
                    }
                } else if (file.isDirectory()) {
                    scanDirectory(file, currentDepth + 1, maxDepth, categorySizes, categoryCounts,
                            folderSizes, folderCounts, totalScannedSize, totalScannedFiles);
                }
            } catch (SecurityException e) {
                log.debug("无权限访问文件: {}", file.getAbsolutePath());
            }
        }
    }

    private long calculateFolderSize(File folder, int currentDepth, int maxDepth) {
        return calculateFolderSize(folder, currentDepth, maxDepth, new AtomicLong(0));
    }

    private long calculateFolderSize(File folder, int currentDepth, int maxDepth, AtomicLong fileCount) {
        if (currentDepth > maxDepth || folder == null || !folder.exists()) {
            return 0;
        }
        if (fileCount.get() > MAX_CALCULATE_FILES) {
            return 0;
        }
        long size = 0;
        File[] files;
        try {
            files = folder.listFiles();
        } catch (SecurityException e) {
            return 0;
        }
        if (files == null) return 0;
        for (File file : files) {
            try {
                if (fileCount.incrementAndGet() > MAX_CALCULATE_FILES) {
                    break;
                }
                if (file.isFile()) {
                    size += file.length();
                } else if (file.isDirectory()) {
                    size += calculateFolderSize(file, currentDepth + 1, maxDepth, fileCount);
                }
            } catch (SecurityException e) {
                // skip
            }
        }
        return size;
    }

    private String getFileCategory(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == fileName.length() - 1) {
            return "other";
        }
        String ext = fileName.substring(dotIndex + 1).toLowerCase();
        return EXTENSION_CATEGORY_MAP.getOrDefault(ext, "other");
    }

    private String getTopLevelFolder(File file, File currentDir) {
        File parent = currentDir;
        while (parent != null) {
            File grandParent = parent.getParentFile();
            if (grandParent == null || grandParent.getAbsolutePath().length() <= 3) {
                return parent.getAbsolutePath();
            }
            parent = grandParent;
        }
        return currentDir.getAbsolutePath();
    }

    private List<FileTypeStatDTO> buildFileTypeStats(Map<String, AtomicLong> categorySizes,
                                                      Map<String, AtomicLong> categoryCounts,
                                                      long totalSize) {
        List<FileTypeStatDTO> stats = new ArrayList<>();
        for (Map.Entry<String, String> entry : CATEGORY_LABEL_MAP.entrySet()) {
            String category = entry.getKey();
            AtomicLong sizeRef = categorySizes.get(category);
            if (sizeRef == null || sizeRef.get() == 0) continue;

            long size = sizeRef.get();
            long count = categoryCounts.getOrDefault(category, new AtomicLong(0)).get();
            double percentage = totalSize > 0 ? (size * 100.0 / totalSize) : 0;

            stats.add(FileTypeStatDTO.builder()
                    .category(category)
                    .label(entry.getValue())
                    .size(size)
                    .fileCount(count)
                    .percentage(Math.round(percentage * 100.0) / 100.0)
                    .color(CATEGORY_COLOR_MAP.getOrDefault(category, "#9ca3af"))
                    .build());
        }

        stats.sort((a, b) -> Long.compare(b.getSize(), a.getSize()));
        return stats;
    }

    private List<FolderStatDTO> buildTopFolders(Map<String, AtomicLong> folderSizes,
                                                 Map<String, AtomicLong> folderCounts,
                                                 long totalSize) {
        List<FolderStatDTO> folders = new ArrayList<>();
        for (Map.Entry<String, AtomicLong> entry : folderSizes.entrySet()) {
            String path = entry.getKey();
            long size = entry.getValue().get();
            if (size == 0) continue;
            long count = folderCounts.getOrDefault(path, new AtomicLong(0)).get();
            double percentage = totalSize > 0 ? (size * 100.0 / totalSize) : 0;

            String name = path;
            int lastSep = path.lastIndexOf('\\');
            if (lastSep < 0) lastSep = path.lastIndexOf('/');
            if (lastSep >= 0 && lastSep < path.length() - 1) {
                name = path.substring(lastSep + 1);
            }

            folders.add(FolderStatDTO.builder()
                    .path(path)
                    .name(name)
                    .size(size)
                    .fileCount(count)
                    .percentage(Math.round(percentage * 100.0) / 100.0)
                    .build());
        }

        folders.sort((a, b) -> Long.compare(b.getSize(), a.getSize()));
        return folders.stream().limit(TOP_FOLDERS_LIMIT).collect(Collectors.toList());
    }

    private List<CleanupSuggestionDTO> generateCleanupSuggestions(String driveLetter,
                                                                    Map<String, AtomicLong> categorySizes,
                                                                    Map<String, AtomicLong> folderSizes) {
        List<CleanupSuggestionDTO> suggestions = new ArrayList<>();
        String drive = driveLetter + ":\\";
        String userHome = System.getProperty("user.home");

        checkWellKnownFolder(suggestions, drive + "Windows\\Temp", "Windows临时文件",
                "Windows系统运行过程中产生的临时文件，可安全清理", "temp", "low", 1);
        checkWellKnownFolder(suggestions, drive + "Windows\\SoftwareDistribution\\Download",
                "Windows更新缓存", "Windows Update下载的安装包缓存，清理后不影响系统运行", "cache", "low", 1);
        checkWellKnownFolder(suggestions, drive + "Windows\\Prefetch",
                "预读取缓存", "Windows预读取数据，清理后系统会自动重建", "cache", "low", 1);
        checkWellKnownFolder(suggestions, drive + "ProgramData\\Package Cache",
                "程序包缓存", "Visual Studio等软件的安装包缓存", "cache", "medium", 1);
        checkRecycleBin(suggestions, drive);

        if (userHome != null && userHome.startsWith(drive)) {
            checkWellKnownFolder(suggestions, userHome + "\\AppData\\Local\\Temp", "用户临时文件",
                    "当前用户的临时文件夹，存放应用程序临时数据", "temp", "low", 1);
            checkWellKnownFolder(suggestions, userHome + "\\AppData\\Local\\Microsoft\\Windows\\INetCache",
                    "IE/Edge浏览器缓存", "浏览器缓存数据，清理后需要重新加载网页资源", "cache", "low", 1);
            checkWellKnownFolder(suggestions, userHome + "\\AppData\\Local\\Google\\Chrome\\User Data\\Default\\Cache",
                    "Chrome浏览器缓存", "Chrome浏览器缓存数据，清理后需要重新加载网页资源", "cache", "low", 1);
            checkWellKnownFolder(suggestions, userHome + "\\AppData\\Local\\Mozilla\\Firefox\\Profiles",
                    "Firefox浏览器缓存", "Firefox浏览器缓存数据，清理后需要重新加载网页资源", "cache", "low", 1);
        }

        long tempSize = categorySizes.getOrDefault("temp", new AtomicLong(0)).get();
        if (tempSize > 100 * 1024 * 1024) {
            suggestions.add(CleanupSuggestionDTO.builder()
                    .type("temp")
                    .title("临时文件")
                    .description("发现大量临时文件(" + formatSize(tempSize) + ")，建议定期清理.tmp, .temp, .bak等文件")
                    .path(drive)
                    .size(tempSize)
                    .riskLevel("low")
                    .action("扫描并删除临时文件")
                    .build());
        }

        long archiveSize = categorySizes.getOrDefault("archive", new AtomicLong(0)).get();
        if (archiveSize > 5L * 1024 * 1024 * 1024) {
            suggestions.add(CleanupSuggestionDTO.builder()
                    .type("archive")
                    .title("压缩包文件")
                    .description("压缩包占用较大空间(" + formatSize(archiveSize) + ")，建议检查并删除已解压的旧压缩包")
                    .path(drive)
                    .size(archiveSize)
                    .riskLevel("medium")
                    .action("检查并删除旧压缩包")
                    .build());
        }

        long videoSize = categorySizes.getOrDefault("video", new AtomicLong(0)).get();
        if (videoSize > 20L * 1024 * 1024 * 1024) {
            suggestions.add(CleanupSuggestionDTO.builder()
                    .type("video")
                    .title("视频文件")
                    .description("视频文件占用大量空间(" + formatSize(videoSize) + ")，建议将不常观看的视频移至外部存储")
                    .path(drive)
                    .size(videoSize)
                    .riskLevel("low")
                    .action("整理视频文件")
                    .build());
        }

        long imageSize = categorySizes.getOrDefault("image", new AtomicLong(0)).get();
        if (imageSize > 5L * 1024 * 1024 * 1024) {
            suggestions.add(CleanupSuggestionDTO.builder()
                    .type("image")
                    .title("图片文件")
                    .description("图片文件占用较大空间(" + formatSize(imageSize) + ")，建议清理重复图片和缩略图缓存")
                    .path(drive)
                    .size(imageSize)
                    .riskLevel("low")
                    .action("清理重复图片")
                    .build());
        }

        for (Map.Entry<String, AtomicLong> entry : folderSizes.entrySet()) {
            String path = entry.getKey();
            long size = entry.getValue().get();
            if (path.endsWith("\\node_modules") && size > 500 * 1024 * 1024) {
                suggestions.add(CleanupSuggestionDTO.builder()
                        .type("dependency")
                        .title("Node.js依赖 (" + getFolderName(path) + ")")
                        .description("node_modules目录占用 " + formatSize(size) + "，可通过npm ci重新安装，删除不影响源代码")
                        .path(path)
                        .size(size)
                        .riskLevel("low")
                        .action("查看并清理")
                        .build());
            }
        }

        checkWellKnownFolder(suggestions, drive + "Users\\.gradle\\caches", "Gradle缓存",
                "Gradle构建缓存，可在需要时自动重新下载", "cache", "low", 1);
        checkWellKnownFolder(suggestions, drive + "Users\\.m2\\repository", "Maven缓存",
                "Maven本地仓库缓存，可在需要时自动重新下载", "cache", "low", 1);

        return suggestions;
    }

    private String getFolderName(String path) {
        int lastSep = path.lastIndexOf('\\');
        if (lastSep < 0) lastSep = path.lastIndexOf('/');
        if (lastSep >= 0 && lastSep < path.length() - 1) {
            return path.substring(lastSep + 1);
        }
        return path;
    }

    private void checkWellKnownFolder(List<CleanupSuggestionDTO> suggestions, String path,
                                       String title, String description, String type, String riskLevel, int maxDepth) {
        File folder = new File(path);
        if (!folder.exists() || !folder.isDirectory()) return;

        long size = calculateFolderSize(folder, 0, maxDepth);
        if (size < 10 * 1024 * 1024) return;

        suggestions.add(CleanupSuggestionDTO.builder()
                .type(type)
                .title(title)
                .description(description)
                .path(path)
                .size(size)
                .riskLevel(riskLevel)
                .action("查看并清理")
                .build());
    }

    private void checkRecycleBin(List<CleanupSuggestionDTO> suggestions, String drive) {
        try {
            File recycleBin = new File(drive + "$Recycle.Bin");
            if (recycleBin.exists()) {
                long size = calculateFolderSize(recycleBin, 0, 1);
                if (size > 0) {
                    suggestions.add(CleanupSuggestionDTO.builder()
                            .type("recycle")
                            .title("回收站")
                            .description("回收站中有 " + formatSize(size) + " 数据，清空回收站可释放空间")
                            .path(drive + "$Recycle.Bin")
                            .size(size)
                            .riskLevel("medium")
                            .action("清空回收站")
                            .build());
                }
            }
        } catch (Exception e) {
            log.debug("检查回收站失败: {}", drive);
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        if (bytes < 1024L * 1024 * 1024 * 1024) return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        return String.format("%.1f TB", bytes / (1024.0 * 1024 * 1024 * 1024));
    }

    private static final String AI_ANALYSIS_SYSTEM_PROMPT = """
        你是一个专业的磁盘空间分析专家，擅长分析磁盘文件结构并提供精准的空间释放建议。
        
        你的任务：
        1. 根据提供的磁盘分析数据，识别可以安全清理的文件和目录
        2. 按照优先级排序建议
        3. 评估每项清理的风险等级
        
        分析原则：
        - 优先识别以下类型的可清理文件：
          * 临时文件(.tmp, .temp, .bak, .old等
          * 系统缓存文件
          * 浏览器缓存
          * 已解压后不再需要的压缩包
          * 重复的大文件
          * 旧的安装包
          * 日志文件
          * node_modules等项目依赖缓存
          * 回收站文件
        
        - 风险等级定义：
          * low: 完全安全，清理后不会影响系统和程序运行
          * medium: 需要用户确认，清理前建议备份
          * high: 谨慎操作，可能影响程序运行
        
        输出要求：
        请严格按照以下JSON格式返回，不要有任何其他文字说明：
        {
          "summary": "对磁盘空间使用情况的简要总结",
          "totalReclaimableSpace": 可释放总空间字节数,
          "analysisInsight": "深入的分析见解和优化建议",
          "suggestions": [
            {
              "category": "建议分类，如：temp/cache/video/archive等",
              "title": "建议标题",
              "description": "详细说明",
              "path": "路径",
              "estimatedSize": 预估可释放大小字节数,
              "riskLevel": "low/medium/high",
              "action": "建议操作",
              "reason": "为什么可以清理",
              "priority": 优先级，1-10，数字越大优先级越高
            }
          ]
        }
        """;

    public AIAnalysisResultDTO aiAnalyzeDrive(String driveLetter, Integer maxDepth) {
        long startTime = System.currentTimeMillis();

        DriveAnalysisResultDTO analysisResult = analyzeDrive(driveLetter, maxDepth);

        try {
            String diskStructureJson = buildDiskStructureJson(driveLetter, analysisResult);

            String userPrompt = String.format("""
                请分析以下 %s 盘的磁盘空间使用情况：
                
                磁盘结构数据：
                %s
                
                请基于以上数据，提供详细的空间释放建议。
                """, driveLetter, diskStructureJson);

            ChatResponse response = callChatApi(AI_ANALYSIS_SYSTEM_PROMPT, userPrompt);
            String content = response.getResult().getOutput().getContent();
            Integer tokens = response.getMetadata().getUsage() != null ?
                response.getMetadata().getUsage().getTotalTokens().intValue() : null;

            AIAnalysisResultDTO result = parseAIResponse(driveLetter, content);
            result.setModel(modelName);
            result.setTokens(tokens);
            result.setAnalysisDurationMs(System.currentTimeMillis() - startTime);

            log.info("AI分析盘符 {} 完成，token消耗: {}", driveLetter, tokens);
            return result;

        } catch (Exception e) {
            log.error("AI分析盘符失败: {}", driveLetter, e);
            if (fallbackEnabled) {
                return fallbackAIAnalysis(driveLetter, analysisResult, startTime);
            }
            throw new RuntimeException("AI分析失败: " + e.getMessage());
        }
    }

    private String buildDiskStructureJson(String driveLetter, DriveAnalysisResultDTO analysisResult) {
        try {
            Map<String, Object> structure = new LinkedHashMap<>();

            Map<String, Object> driveInfo = new LinkedHashMap<>();
            DriveInfoDTO info = analysisResult.getDriveInfo();
            driveInfo.put("driveLetter", driveLetter);
            driveInfo.put("totalSpace", info.getTotalSpace());
            driveInfo.put("usedSpace", info.getUsedSpace());
            driveInfo.put("freeSpace", info.getFreeSpace());
            driveInfo.put("usagePercent", info.getTotalSpace() > 0 ?
                String.format("%.1f%%", (info.getUsedSpace() * 100.0 / info.getTotalSpace())) : "0%");
            driveInfo.put("fileSystem", info.getFileSystem());
            structure.put("driveInfo", driveInfo);

            List<Map<String, Object>> fileTypes = new ArrayList<>();
            for (FileTypeStatDTO stat : analysisResult.getFileTypeStats()) {
                Map<String, Object> typeMap = new LinkedHashMap<>();
                typeMap.put("category", stat.getCategory());
                typeMap.put("label", stat.getLabel());
                typeMap.put("size", stat.getSize());
                typeMap.put("sizeFormatted", formatSize(stat.getSize()));
                typeMap.put("fileCount", stat.getFileCount());
                typeMap.put("percentage", stat.getPercentage() + "%");
                fileTypes.add(typeMap);
            }
            structure.put("fileTypeStats", fileTypes);

            List<Map<String, Object>> folders = new ArrayList<>();
            for (FolderStatDTO folder : analysisResult.getTopFolders()) {
                Map<String, Object> folderMap = new LinkedHashMap<>();
                folderMap.put("name", folder.getName());
                folderMap.put("path", folder.getPath());
                folderMap.put("size", folder.getSize());
                folderMap.put("sizeFormatted", formatSize(folder.getSize()));
                folderMap.put("fileCount", folder.getFileCount());
                folderMap.put("percentage", folder.getPercentage() + "%");
                folders.add(folderMap);
            }
            structure.put("topFolders", folders);

            List<Map<String, Object>> existingSuggestions = new ArrayList<>();
            for (CleanupSuggestionDTO suggestion : analysisResult.getCleanupSuggestions()) {
                Map<String, Object> suggMap = new LinkedHashMap<>();
                suggMap.put("type", suggestion.getType());
                suggMap.put("title", suggestion.getTitle());
                suggMap.put("description", suggestion.getDescription());
                suggMap.put("path", suggestion.getPath());
                suggMap.put("size", suggestion.getSize());
                suggMap.put("sizeFormatted", formatSize(suggestion.getSize()));
                suggMap.put("riskLevel", suggestion.getRiskLevel());
                existingSuggestions.add(suggMap);
            }
            structure.put("existingSuggestions", existingSuggestions);

            Map<String, Object> scanInfo = new LinkedHashMap<>();
            scanInfo.put("totalScannedSize", analysisResult.getTotalScannedSize());
            scanInfo.put("totalScannedFiles", analysisResult.getTotalScannedFiles());
            structure.put("scanInfo", scanInfo);

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(structure);
        } catch (Exception e) {
            log.error("构建磁盘结构JSON失败", e);
            return "{}";
        }
    }

    private AIAnalysisResultDTO parseAIResponse(String driveLetter, String aiContent) {
        try {
            String jsonContent = extractJson(aiContent);
            JsonNode root = objectMapper.readTree(jsonContent);

            String summary = root.path("summary").asText("");
            long totalReclaimable = root.path("totalReclaimableSpace").asLong(0);
            String insight = root.path("analysisInsight").asText("");

            List<AICleanupSuggestionDTO> suggestions = new ArrayList<>();
            JsonNode suggestionsNode = root.path("suggestions");
            if (suggestionsNode.isArray()) {
                TypeReference<List<AICleanupSuggestionDTO>> typeRef = new TypeReference<List<AICleanupSuggestionDTO>>() {};
                suggestions = objectMapper.convertValue(suggestionsNode, typeRef);
                suggestions.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
            }

            return AIAnalysisResultDTO.builder()
                    .driveLetter(driveLetter)
                    .summary(summary)
                    .totalReclaimableSpace(totalReclaimable)
                    .suggestions(suggestions)
                    .analysisInsight(insight)
                    .build();

        } catch (Exception e) {
            log.error("解析AI响应失败，原始内容: {}", aiContent, e);
            throw new RuntimeException("AI响应解析失败: " + e.getMessage());
        }
    }

    private String extractJson(String content) {
        if (content == null) return "{}";
        content = content.trim();

        int firstBrace = content.indexOf('{');
        int lastBrace = content.lastIndexOf('}');

        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return content.substring(firstBrace, lastBrace + 1);
        }

        return content;
    }

    private ChatResponse callChatApi(String systemPrompt, String userPrompt) {
        List<Message> messages = List.of(
            new SystemMessage(systemPrompt),
            new UserMessage(userPrompt)
        );
        Prompt prompt = new Prompt(messages);
        return chatClient.call(prompt);
    }

    private AIAnalysisResultDTO fallbackAIAnalysis(String driveLetter,
                                                   DriveAnalysisResultDTO analysisResult,
                                                   long startTime) {
        log.warn("降级模式：为盘符 {} 使用模拟AI分析结果", driveLetter);

        List<AICleanupSuggestionDTO> suggestions = new ArrayList<>();
        long totalReclaimable = 0;

        for (CleanupSuggestionDTO suggestion : analysisResult.getCleanupSuggestions()) {
            long size = suggestion.getSize();
            totalReclaimable += size;

            int priority = calculatePriority(suggestion.getType(), size);

            suggestions.add(AICleanupSuggestionDTO.builder()
                    .category(suggestion.getType())
                    .title(suggestion.getTitle())
                    .description(suggestion.getDescription())
                    .path(suggestion.getPath())
                    .estimatedSize(size)
                    .riskLevel(suggestion.getRiskLevel())
                    .action(suggestion.getAction())
                    .reason(getReasonForType(suggestion.getType()))
                    .priority(priority)
                    .build());
        }

        suggestions.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));

        DriveInfoDTO info = analysisResult.getDriveInfo();
        String usagePercent = info.getTotalSpace() > 0 ?
            String.format("%.1f", (info.getUsedSpace() * 100.0 / info.getTotalSpace())) : "0";

        String summary = String.format(
            "%s盘总容量 %s，已使用 %s（占比 %s%%，剩余空间 %s。共扫描文件 %d 个，发现 %d 项可清理内容。",
            driveLetter,
            formatSize(info.getTotalSpace()),
            formatSize(info.getUsedSpace()),
            usagePercent,
            formatSize(info.getFreeSpace()),
            analysisResult.getTotalScannedFiles(),
            suggestions.size()
        );

        String insight = buildFallbackInsight(analysisResult, usagePercent);

        return AIAnalysisResultDTO.builder()
                .driveLetter(driveLetter)
                .summary(summary)
                .totalReclaimableSpace(totalReclaimable)
                .suggestions(suggestions)
                .analysisInsight(insight)
                .model("fallback")
                .analysisDurationMs(System.currentTimeMillis() - startTime)
                .build();
    }

    private int calculatePriority(String type, long size) {
        int basePriority = switch (type) {
            case "temp", "recycle" -> 10;
            case "cache" -> 8;
            case "archive", "video" -> 6;
            case "image" -> 5;
            case "dependency" -> 7;
            default -> 4;
        };

        if (size > 10L * 1024 * 1024 * 1024) {
            basePriority += 2;
        } else if (size > 5L * 1024 * 1024 * 1024) {
            basePriority += 1;
        }

        return Math.min(basePriority, 10);
    }

    private String getReasonForType(String type) {
        return switch (type) {
            case "temp" -> "临时文件是程序运行过程中产生的临时数据，删除后不会影响系统和程序的正常运行。";
            case "cache" -> "缓存文件是为了加速访问而临时存储的数据，删除后会在需要时自动重建。";
            case "recycle" -> "回收站中的文件是用户已删除的文件，清空后可立即释放磁盘空间。";
            case "archive" -> "压缩包如果已经解压过，且源文件不再需要时可以安全删除。";
            case "video" -> "视频文件通常较大，不常观看的视频可以考虑移动到外部存储设备。";
            case "image" -> "图片文件可能存在重复，建议清理重复和不需要的图片以释放空间。";
            case "dependency" -> "项目依赖目录（如node_modules）可以在需要时通过包管理器重新安装。";
            default -> "这些文件占用了较多空间，建议确认不再需要后可以清理。";
        };
    }

    private String buildFallbackInsight(DriveAnalysisResultDTO analysisResult, String usagePercent) {
        StringBuilder insight = new StringBuilder();
        double usage = Double.parseDouble(usagePercent);

        if (usage > 90) {
            insight.append("⚠️ 磁盘空间严重不足！");
        } else if (usage > 80) {
            insight.append("磁盘空间使用率较高，");
        } else if (usage > 70) {
            insight.append("磁盘空间使用情况正常，");
        } else {
            insight.append("磁盘空间充足，");
        }

        insight.append("建议按优先级进行清理：");

        List<FileTypeStatDTO> stats = analysisResult.getFileTypeStats();
        if (!stats.isEmpty()) {
            FileTypeStatDTO topType = stats.get(0);
            insight.append(String.format("主要占用类型是%s，占比%.1f%%。",
                topType.getLabel(), topType.getPercentage()));
        }

        insight.append("建议优先清理临时文件、系统缓存和回收站，这些操作风险较低且能快速释放空间。");

        long videoSize = stats.stream()
            .filter(s -> "video".equals(s.getCategory()))
            .mapToLong(FileTypeStatDTO::getSize)
            .sum();
        if (videoSize > 10L * 1024 * 1024 * 1024) {
            insight.append("视频文件占用较大空间，建议将不常观看的视频移至外部存储。");
        }

        return insight.toString();
    }
}
