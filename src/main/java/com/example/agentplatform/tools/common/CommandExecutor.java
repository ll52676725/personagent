package com.example.agentplatform.tools.common;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 命令执行工具类
 * <p>提供系统命令执行相关的通用工具方法，支持超时控制和字符集指定
 * <p>封装 ProcessBuilder 进行系统命令调用，自动处理输入流读取和资源释放
 * <p>所有方法均为静态方法，无需实例化
 * 
 * @author System
 * @since 2025-01-01
 */
@Slf4j
public class CommandExecutor {

    /**
     * 私有构造函数，防止实例化
     */
    private CommandExecutor() {
    }

    /**
     * 执行系统命令，使用默认配置
     * <p>使用 GBK 字符集，超时时间 30 秒
     * 
     * @param command 要执行的命令字符串
     * @return 命令输出的行列表，空行已被过滤
     */
    public static List<String> executeCommand(String command) {
        return executeCommand(command, "GBK", 30);
    }

    /**
     * 执行系统命令，指定字符集和超时时间
     * <p>通过 cmd.exe /c 执行命令，自动合并标准输出和错误输出
     * <p>执行超时后会强制终止进程
     * 
     * @param command 要执行的命令字符串
     * @param charset 字符集名称（如：GBK、UTF-8）
     * @param timeoutSeconds 超时时间（秒）
     * @return 命令输出的行列表，空行已被过滤
     */
    public static List<String> executeCommand(String command, String charset, int timeoutSeconds) {
        List<String> result = new ArrayList<>();
        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);
            pb.redirectErrorStream(true);
            process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), Charset.forName(charset)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        result.add(line.trim());
                    }
                }
            }

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                log.warn("命令执行超时: {}", command);
                process.destroyForcibly();
            }

        } catch (Exception e) {
            log.debug("执行命令失败: {}", command, e);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return result;
    }

    /**
     * 执行系统命令并返回完整输出字符串，使用默认配置
     * <p>使用 GBK 字符集，超时时间 30 秒
     * 
     * @param command 要执行的命令字符串
     * @return 命令的完整输出字符串，包含换行符
     */
    public static String executeCommandForOutput(String command) {
        return executeCommandForOutput(command, "GBK", 30);
    }

    /**
     * 执行系统命令并返回完整输出字符串，指定字符集和超时时间
     * <p>通过 cmd.exe /c 执行命令，自动合并标准输出和错误输出
     * <p>执行超时后会强制终止进程
     * 
     * @param command 要执行的命令字符串
     * @param charset 字符集名称（如：GBK、UTF-8）
     * @param timeoutSeconds 超时时间（秒）
     * @return 命令的完整输出字符串，包含换行符
     */
    public static String executeCommandForOutput(String command, String charset, int timeoutSeconds) {
        StringBuilder result = new StringBuilder();
        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);
            pb.redirectErrorStream(true);
            process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), Charset.forName(charset)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                log.warn("命令执行超时: {}", command);
                process.destroyForcibly();
            }

        } catch (Exception e) {
            log.debug("执行命令失败: {}", command, e);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return result.toString();
    }
}
