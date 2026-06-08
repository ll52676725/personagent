package com.example.agentplatform.tools.service;

import com.example.agentplatform.tools.dto.DesktopShortcutRequestDTO;
import com.example.agentplatform.tools.dto.DesktopShortcutResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

@Slf4j
public class DesktopShortcutService {

    public DesktopShortcutResultDTO createShortcut(DesktopShortcutRequestDTO request) {
        try {
            String desktopPath = getDesktopPath();
            if (desktopPath == null) {
                return DesktopShortcutResultDTO.builder()
                        .success(false)
                        .toolName(request.getToolName())
                        .message("无法定位桌面路径")
                        .build();
            }

            String baseUrl = resolveBaseUrl(request.getBaseUrl());
            String toolUrl = baseUrl + request.getToolPath();
            String safeFileName = sanitizeFileName(request.getToolName());

            String batPath = createBatShortcut(desktopPath, safeFileName, toolUrl, request.getOpenAsApp());
            String urlPath = createUrlShortcut(desktopPath, safeFileName, toolUrl);

            String message = String.format("快捷方式已创建到桌面：%s", safeFileName);
            if (request.getOpenAsApp()) {
                message += "（双击将以独立窗口模式打开）";
            }

            return DesktopShortcutResultDTO.builder()
                    .success(true)
                    .shortcutPath(batPath)
                    .toolName(request.getToolName())
                    .url(toolUrl)
                    .message(message)
                    .build();

        } catch (Exception e) {
            log.error("创建桌面快捷方式失败: {}", e.getMessage(), e);
            return DesktopShortcutResultDTO.builder()
                    .success(false)
                    .toolName(request.getToolName())
                    .message("创建快捷方式失败: " + e.getMessage())
                    .build();
        }
    }

    private String createBatShortcut(String desktopPath, String toolName, String toolUrl, Boolean openAsApp) throws IOException {
        String fileName = toolName + ".bat";
        Path filePath = Paths.get(desktopPath, fileName);

        StringBuilder sb = new StringBuilder();
        sb.append("@echo off\r\n");
        sb.append("chcp 65001 >nul 2>&1\r\n");
        sb.append("title ").append(toolName).append("\r\n");

        if (Boolean.TRUE.equals(openAsApp)) {
            sb.append("set \"URL=").append(toolUrl).append("\"\r\n");
            sb.append("set \"CHROME_PATH=\"\r\n");
            sb.append("for %%p in (\r\n");
            sb.append("  \"%LOCALAPPDATA%\\Google\\Chrome\\Application\\chrome.exe\"\r\n");
            sb.append("  \"%PROGRAMFILES%\\Google\\Chrome\\Application\\chrome.exe\"\r\n");
            sb.append("  \"%PROGRAMFILES(X86)%\\Google\\Chrome\\Application\\chrome.exe\"\r\n");
            sb.append(") do (\r\n");
            sb.append("  if exist %%p set \"CHROME_PATH=%%~p\"\r\n");
            sb.append(")\r\n");
            sb.append("if defined CHROME_PATH (\r\n");
            sb.append("  start \"\" \"%CHROME_PATH%\" --app=%URL%\r\n");
            sb.append(") else (\r\n");
            sb.append("  start \"\" %URL%\r\n");
            sb.append(")\r\n");
        } else {
            sb.append("start \"\" \"").append(toolUrl).append("\"\r\n");
        }

        sb.append("exit\r\n");

        Files.writeString(filePath, sb.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        log.info("已创建BAT快捷方式: {}", filePath);
        return filePath.toString();
    }

    private String createUrlShortcut(String desktopPath, String toolName, String toolUrl) throws IOException {
        String fileName = toolName + ".url";
        Path filePath = Paths.get(desktopPath, fileName);

        StringBuilder sb = new StringBuilder();
        sb.append("[InternetShortcut]\r\n");
        sb.append("URL=").append(toolUrl).append("\r\n");
        sb.append("IconIndex=0\r\n");

        Files.writeString(filePath, sb.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        log.info("已创建URL快捷方式: {}", filePath);
        return filePath.toString();
    }

    String getDesktopPath() {
        String userHome = System.getProperty("user.home");
        if (userHome == null) {
            return null;
        }

        Path desktopPath = Paths.get(userHome, "Desktop");
        if (Files.isDirectory(desktopPath)) {
            return desktopPath.toString();
        }

        Path oneDriveDesktop = Paths.get(userHome, "OneDrive", "桌面");
        if (Files.isDirectory(oneDriveDesktop)) {
            return oneDriveDesktop.toString();
        }

        Path oneDriveDesktopEn = Paths.get(userHome, "OneDrive", "Desktop");
        if (Files.isDirectory(oneDriveDesktopEn)) {
            return oneDriveDesktopEn.toString();
        }

        String[] envVars = {"USERPROFILE", "HOMEPATH"};
        for (String env : envVars) {
            String envPath = System.getenv(env);
            if (envPath != null) {
                Path envDesktop = Paths.get(envPath, "Desktop");
                if (Files.isDirectory(envDesktop)) {
                    return envDesktop.toString();
                }
            }
        }

        return desktopPath.toString();
    }

    private String resolveBaseUrl(String baseUrl) {
        if (baseUrl != null && !baseUrl.isBlank()) {
            return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        }
        return "http://localhost:8080";
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
