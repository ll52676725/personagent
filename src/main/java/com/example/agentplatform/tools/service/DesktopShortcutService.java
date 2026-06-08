package com.example.agentplatform.tools.service;

import com.example.agentplatform.tools.dto.DesktopShortcutRequestDTO;
import com.example.agentplatform.tools.dto.DesktopShortcutResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

@Slf4j
@Service
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
        sb.append("set \"TOOL_URL=").append(toolUrl).append("\"\r\n");
        sb.append("set \"BROWSER_PATH=\"\r\n");
        sb.append("\r\n");

        if (Boolean.TRUE.equals(openAsApp)) {
            sb.append("rem 优先检测 Chrome\r\n");
            sb.append("if exist \"%LOCALAPPDATA%\\Google\\Chrome\\Application\\chrome.exe\" set \"BROWSER_PATH=%LOCALAPPDATA%\\Google\\Chrome\\Application\\chrome.exe\"\r\n");
            sb.append("if not defined BROWSER_PATH if exist \"%PROGRAMFILES%\\Google\\Chrome\\Application\\chrome.exe\" set \"BROWSER_PATH=%PROGRAMFILES%\\Google\\Chrome\\Application\\chrome.exe\"\r\n");
            sb.append("if not defined BROWSER_PATH if exist \"%PROGRAMFILES(X86)%\\Google\\Chrome\\Application\\chrome.exe\" set \"BROWSER_PATH=%PROGRAMFILES(X86)%\\Google\\Chrome\\Application\\chrome.exe\"\r\n");
            sb.append("\r\n");
            sb.append("rem 检测 Edge（Chromium 内核同样支持 --app）\r\n");
            sb.append("if not defined BROWSER_PATH if exist \"%LOCALAPPDATA%\\Microsoft\\Edge\\Application\\msedge.exe\" set \"BROWSER_PATH=%LOCALAPPDATA%\\Microsoft\\Edge\\Application\\msedge.exe\"\r\n");
            sb.append("if not defined BROWSER_PATH if exist \"%PROGRAMFILES%\\Microsoft\\Edge\\Application\\msedge.exe\" set \"BROWSER_PATH=%PROGRAMFILES%\\Microsoft\\Edge\\Application\\msedge.exe\"\r\n");
            sb.append("if not defined BROWSER_PATH if exist \"%PROGRAMFILES(X86)%\\Microsoft\\Edge\\Application\\msedge.exe\" set \"BROWSER_PATH=%PROGRAMFILES(X86)%\\Microsoft\\Edge\\Application\\msedge.exe\"\r\n");
            sb.append("\r\n");
            sb.append("if defined BROWSER_PATH (\r\n");
            sb.append("  start \"\" \"%BROWSER_PATH%\" --app=\"%TOOL_URL%\"\r\n");
            sb.append("  goto :eof\r\n");
            sb.append(")\r\n");
            sb.append("\r\n");
        }

        sb.append("rem 使用系统默认浏览器\r\n");
        sb.append("start \"\" \"%TOOL_URL%\"\r\n");
        sb.append("if errorlevel 1 (\r\n");
        sb.append("  echo.\r\n");
        sb.append("  echo [错误] 无法打开浏览器，请检查浏览器是否安装\r\n");
        sb.append("  echo URL: %TOOL_URL%\r\n");
        sb.append("  pause\r\n");
        sb.append(")\r\n");

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

        Path zhDesktop = Paths.get(userHome, "桌面");
        if (Files.isDirectory(zhDesktop)) {
            return zhDesktop.toString();
        }

        Path desktopPath = Paths.get(userHome, "Desktop");
        if (Files.isDirectory(desktopPath)) {
            return desktopPath.toString();
        }

        Path oneDriveZhDesktop = Paths.get(userHome, "OneDrive", "桌面");
        if (Files.isDirectory(oneDriveZhDesktop)) {
            return oneDriveZhDesktop.toString();
        }

        Path oneDriveDesktop = Paths.get(userHome, "OneDrive", "Desktop");
        if (Files.isDirectory(oneDriveDesktop)) {
            return oneDriveDesktop.toString();
        }

        Path oneDriveBusinessZh = Paths.get(userHome, "OneDrive", "桌面");
        if (Files.isDirectory(oneDriveBusinessZh)) {
            return oneDriveBusinessZh.toString();
        }

        String[] envVars = {"USERPROFILE", "HOMEPATH"};
        for (String env : envVars) {
            String envPath = System.getenv(env);
            if (envPath != null) {
                Path envZhDesktop = Paths.get(envPath, "桌面");
                if (Files.isDirectory(envZhDesktop)) {
                    return envZhDesktop.toString();
                }
                Path envDesktop = Paths.get(envPath, "Desktop");
                if (Files.isDirectory(envDesktop)) {
                    return envDesktop.toString();
                }
            }
        }

        return zhDesktop.toString();
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
