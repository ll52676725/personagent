package com.example.agentplatform.tools.service;

import com.example.agentplatform.tools.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class IpAnalysisService {

    private static final int PING_TIMEOUT_MS = 5000;
    private static final int TRACERT_MAX_HOPS = 30;
    private static final int TRACERT_TIMEOUT_S = 3;
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$");
    private static final Pattern TRACERT_HOP_PATTERN = Pattern.compile(
            "^\\s*(\\d+)\\s+(.{1,30}?)\\s+(\\d+\\s*ms|\\*)\\s+(\\d+\\s*ms|\\*)\\s+(\\d+\\s*ms|\\*).*$");
    private static final Pattern ARP_LINE_PATTERN = Pattern.compile(
            "^\\s*(\\d+\\.\\d+\\.\\d+\\.\\d+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+).*$");

    public CurrentIpInfoDTO getCurrentIpInfo() {
        String publicIp = fetchPublicIp();
        String hostname = getHostname();
        List<NetworkInterfaceDTO> interfaces = getNetworkInterfaces();
        String defaultGateway = detectDefaultGateway();
        String dnsServer = detectDnsServer();

        return CurrentIpInfoDTO.builder()
                .publicIp(publicIp)
                .publicIpSource(publicIp != null ? "外部API" : "获取失败")
                .hostname(hostname)
                .networkInterfaces(interfaces)
                .defaultGateway(defaultGateway)
                .dnsServer(dnsServer)
                .build();
    }

    public PingResultDTO ping(String target, Integer count) {
        int packetCount = count != null ? Math.min(Math.max(count, 1), 10) : 4;
        try {
            InetAddress address = InetAddress.getByName(target);
            String ipAddress = address.getHostAddress();

            long totalPingTime = 0;
            int packetsReceived = 0;
            Integer ttl = null;

            for (int i = 0; i < packetCount; i++) {
                long start = System.currentTimeMillis();
                boolean reached = address.isReachable(PING_TIMEOUT_MS);
                long elapsed = System.currentTimeMillis() - start;

                if (reached) {
                    packetsReceived++;
                    totalPingTime += elapsed;
                }
            }

            long avgPingTime = packetsReceived > 0 ? totalPingTime / packetsReceived : 0;
            double lossRate = ((packetCount - packetsReceived) * 100.0) / packetCount;

            return PingResultDTO.builder()
                    .target(target)
                    .reachable(packetsReceived > 0)
                    .ipAddress(ipAddress)
                    .pingTimeMs(avgPingTime)
                    .ttl(ttl)
                    .packetsSent(packetCount)
                    .packetsReceived(packetsReceived)
                    .packetLossRate(Math.round(lossRate * 100.0) / 100.0)
                    .build();

        } catch (UnknownHostException e) {
            return PingResultDTO.builder()
                    .target(target)
                    .reachable(false)
                    .errorMessage("无法解析主机名: " + target)
                    .packetsSent(packetCount)
                    .packetsReceived(0)
                    .packetLossRate(100.0)
                    .build();
        } catch (Exception e) {
            log.error("Ping失败: {}", target, e);
            return PingResultDTO.builder()
                    .target(target)
                    .reachable(false)
                    .errorMessage("Ping执行失败: " + e.getMessage())
                    .packetsSent(packetCount)
                    .packetsReceived(0)
                    .packetLossRate(100.0)
                    .build();
        }
    }

    public PingResultDTO pingNative(String target, Integer count) {
        int packetCount = count != null ? Math.min(Math.max(count, 1), 10) : 4;
        try {
            ProcessBuilder pb = new ProcessBuilder("ping", "-n", String.valueOf(packetCount), "-w", "5000", target);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return PingResultDTO.builder()
                        .target(target)
                        .reachable(false)
                        .errorMessage("Ping超时")
                        .packetsSent(packetCount)
                        .packetsReceived(0)
                        .packetLossRate(100.0)
                        .build();
            }

            return parsePingOutput(target, output.toString(), packetCount);

        } catch (Exception e) {
            log.error("Native ping执行失败: {}", target, e);
            return ping(target, count);
        }
    }

    private PingResultDTO parsePingOutput(String target, String output, int packetCount) {
        String ipAddress = extractIpFromPing(output);
        boolean reachable = output.contains("TTL=") || output.contains("ttl=");
        long avgTime = extractAvgPingTime(output);
        int packetsSent = packetCount;
        int packetsReceived = extractPacketsReceived(output, packetCount);
        double lossRate = ((packetsSent - packetsReceived) * 100.0) / packetsSent;
        Integer ttl = extractTtl(output);

        return PingResultDTO.builder()
                .target(target)
                .reachable(reachable)
                .ipAddress(ipAddress)
                .pingTimeMs(avgTime)
                .ttl(ttl)
                .packetsSent(packetsSent)
                .packetsReceived(packetsReceived)
                .packetLossRate(Math.round(lossRate * 100.0) / 100.0)
                .build();
    }

    private String extractIpFromPing(String output) {
        Pattern p = Pattern.compile("\\[(\\d+\\.\\d+\\.\\d+\\.\\d+)\\]");
        Matcher m = p.matcher(output);
        if (m.find()) return m.group(1);

        Pattern p2 = Pattern.compile("从\\s*(\\d+\\.\\d+\\.\\d+\\.\\d+)\\s*回复");
        Matcher m2 = p2.matcher(output);
        if (m2.find()) return m2.group(1);

        Pattern p3 = Pattern.compile("Reply from\\s*(\\d+\\.\\d+\\.\\d+\\.\\d+)");
        Matcher m3 = p3.matcher(output);
        if (m3.find()) return m3.group(1);

        return "";
    }

    private long extractAvgPingTime(String output) {
        Pattern p = Pattern.compile("平均\\s*=\\s*(\\d+)ms");
        Matcher m = p.matcher(output);
        if (m.find()) return Long.parseLong(m.group(1));

        Pattern p2 = Pattern.compile("Minimum\\s*=\\s*(\\d+)ms.*Average\\s*=\\s*(\\d+)ms", Pattern.CASE_INSENSITIVE);
        Matcher m2 = p2.matcher(output);
        if (m2.find()) return Long.parseLong(m2.group(2));

        Pattern p3 = Pattern.compile("时间[=<](\\d+)ms");
        Matcher m3 = p3.matcher(output);
        if (m3.find()) return Long.parseLong(m3.group(1));

        Pattern p4 = Pattern.compile("time[=<](\\d+)ms");
        Matcher m4 = p4.matcher(output);
        if (m4.find()) return Long.parseLong(m4.group(1));

        return 0;
    }

    private int extractPacketsReceived(String output, int sent) {
        Pattern p = Pattern.compile("已接收\\s*=\\s*(\\d+)");
        Matcher m = p.matcher(output);
        if (m.find()) return Integer.parseInt(m.group(1));

        Pattern p2 = Pattern.compile("Received\\s*=\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher m2 = p2.matcher(output);
        if (m2.find()) return Integer.parseInt(m2.group(1));

        return output.contains("TTL=") || output.contains("ttl=") ? sent : 0;
    }

    private Integer extractTtl(String output) {
        Pattern p = Pattern.compile("TTL=(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(output);
        if (m.find()) return Integer.parseInt(m.group(1));
        return null;
    }

    public TracerouteResultDTO traceroute(String target) {
        long startTime = System.currentTimeMillis();
        String targetIp = resolveHostname(target);
        List<TracerouteHopDTO> hops = new ArrayList<>();

        try {
            ProcessBuilder pb = new ProcessBuilder("tracert", "-d", "-h",
                    String.valueOf(TRACERT_MAX_HOPS), "-w",
                    String.valueOf(TRACERT_TIMEOUT_S * 1000), target);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    TracerouteHopDTO hop = parseTracertLine(line);
                    if (hop != null) {
                        hops.add(hop);
                    }
                }
            }

            boolean finished = process.waitFor(120, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
            }

        } catch (Exception e) {
            log.error("Traceroute执行失败: {}", target, e);
            return TracerouteResultDTO.builder()
                    .target(target)
                    .targetIp(targetIp)
                    .hops(hops)
                    .totalHops(hops.size())
                    .reachedTarget(false)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .errorMessage("Traceroute执行失败: " + e.getMessage())
                    .build();
        }

        boolean reachedTarget = checkReachedTarget(hops, targetIp);
        Integer blockHop = null;
        String blockIp = null;
        String blockAnalysis = null;

        if (!reachedTarget) {
            Integer[] blockInfo = findBlockHop(hops);
            blockHop = blockInfo[0];
            if (blockHop != null && blockHop > 0 && blockHop <= hops.size()) {
                TracerouteHopDTO blockHopData = hops.get(blockHop - 1);
                blockIp = blockHopData.getIp();
                blockAnalysis = analyzeBlockPoint(blockHopData, blockHop, hops.size());
            }
        }

        return TracerouteResultDTO.builder()
                .target(target)
                .targetIp(targetIp)
                .hops(hops)
                .totalHops(hops.size())
                .reachedTarget(reachedTarget)
                .blockHop(blockHop)
                .blockIp(blockIp)
                .blockAnalysis(blockAnalysis)
                .durationMs(System.currentTimeMillis() - startTime)
                .build();
    }

    private TracerouteHopDTO parseTracertLine(String line) {
        if (line == null || line.trim().isEmpty()) return null;

        String trimmed = line.trim();
        if (!trimmed.matches("^\\d+\\s+.*")) return null;

        String[] parts = trimmed.split("\\s+");
        if (parts.length < 5) return null;

        try {
            int hopNum = Integer.parseInt(parts[0]);

            long lat1 = parseLatencyFromPart(parts[1]);
            long lat2 = parseLatencyFromPart(parts[2]);
            long lat3 = parseLatencyFromPart(parts[3]);

            boolean timeout = lat1 < 0 && lat2 < 0 && lat3 < 0;

            String ip = "";
            String host = "";

            for (int i = 4; i < parts.length; i++) {
                String part = parts[i];
                if (part.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                    ip = part;
                    break;
                }
            }

            if (ip.isEmpty()) {
                if (parts.length > 4) {
                    host = parts[4];
                    if (host.contains("请求超时") || host.contains("timed") || host.contains("*")) {
                        host = "*";
                    }
                }
            } else {
                host = ip;
            }

            return TracerouteHopDTO.builder()
                    .hop(hopNum)
                    .host(host)
                    .ip(ip)
                    .latency1(lat1)
                    .latency2(lat2)
                    .latency3(lat3)
                    .timeout(timeout)
                    .build();

        } catch (Exception e) {
            log.debug("解析tracert行失败: {}", line, e);
            return null;
        }
    }

    private long parseLatencyFromPart(String part) {
        if (part == null || part.equals("*")) return -1;
        if (part.contains("<")) return 0;
        try {
            String num = part.replaceAll("[^0-9]", "");
            if (num.isEmpty()) return -1;
            return Long.parseLong(num);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private long parseLatency(String value) {
        if (value == null || "<1".equals(value)) return 0;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private boolean checkReachedTarget(List<TracerouteHopDTO> hops, String targetIp) {
        if (targetIp == null || hops.isEmpty()) return false;
        TracerouteHopDTO lastHop = hops.get(hops.size() - 1);
        return targetIp.equals(lastHop.getIp()) && !lastHop.isTimeout();
    }

    private Integer[] findBlockHop(List<TracerouteHopDTO> hops) {
        int consecutiveTimeouts = 0;
        for (int i = hops.size() - 1; i >= 0; i--) {
            if (hops.get(i).isTimeout()) {
                consecutiveTimeouts++;
            } else {
                break;
            }
        }

        if (consecutiveTimeouts > 0 && consecutiveTimeouts < hops.size()) {
            return new Integer[]{hops.size() - consecutiveTimeouts + 1};
        }

        for (int i = 0; i < hops.size() - 1; i++) {
            if (hops.get(i).isTimeout() && !hops.get(i + 1).isTimeout()) {
                return new Integer[]{i + 1};
            }
        }

        return new Integer[]{null};
    }

    private String analyzeBlockPoint(TracerouteHopDTO blockHopData, int blockHopNum, int totalHops) {
        StringBuilder analysis = new StringBuilder();
        analysis.append(String.format("在第 %d 跳检测到网络中断。", blockHopNum));

        if (blockHopData.isTimeout()) {
            analysis.append("该节点请求超时，可能原因：");
            analysis.append("1) 该路由器配置了禁止ICMP响应；");
            analysis.append("2) 防火墙阻断了ICMP流量；");
            analysis.append("3) 网络链路在该节点处中断。");
        } else {
            analysis.append(String.format("最后可达节点IP: %s。", blockHopData.getIp()));
            analysis.append("该节点之后无法继续转发，可能原因：");
            analysis.append("1) 下一跳路由器故障；");
            analysis.append("2) 目标网络不可达；");
            analysis.append("3) 中间链路故障。");
        }

        return analysis.toString();
    }

    public DnsResultDTO resolveDns(String domain) {
        long startTime = System.currentTimeMillis();
        try {
            InetAddress address = InetAddress.getByName(domain);
            long queryTime = System.currentTimeMillis() - startTime;

            return DnsResultDTO.builder()
                    .domain(domain)
                    .resolvedIp(address.getHostAddress())
                    .success(true)
                    .queryTimeMs(queryTime)
                    .build();

        } catch (UnknownHostException e) {
            long queryTime = System.currentTimeMillis() - startTime;
            return DnsResultDTO.builder()
                    .domain(domain)
                    .success(false)
                    .queryTimeMs(queryTime)
                    .errorMessage("DNS解析失败: " + e.getMessage())
                    .build();
        }
    }

    public DnsResultDTO resolveDnsNative(String domain) {
        long startTime = System.currentTimeMillis();
        try {
            ProcessBuilder pb = new ProcessBuilder("nslookup", domain);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS);
            long queryTime = System.currentTimeMillis() - startTime;

            Pattern ipPattern = Pattern.compile("Address:\\s*(\\d+\\.\\d+\\.\\d+\\.\\d+)");
            Matcher m = ipPattern.matcher(output.toString());

            String resolvedIp = null;
            while (m.find()) {
                resolvedIp = m.group(1);
            }

            Pattern serverPattern = Pattern.compile("Server:\\s*(\\d+\\.\\d+\\.\\d+\\.\\d+)");
            Matcher sm = serverPattern.matcher(output.toString());
            String dnsServer = sm.find() ? sm.group(1) : null;

            if (resolvedIp != null) {
                return DnsResultDTO.builder()
                        .domain(domain)
                        .resolvedIp(resolvedIp)
                        .success(true)
                        .dnsServer(dnsServer)
                        .queryTimeMs(queryTime)
                        .build();
            } else {
                return DnsResultDTO.builder()
                        .domain(domain)
                        .success(false)
                        .dnsServer(dnsServer)
                        .queryTimeMs(queryTime)
                        .errorMessage("DNS解析失败，未找到IP地址")
                        .build();
            }

        } catch (Exception e) {
            log.error("Native DNS解析失败: {}", domain, e);
            return resolveDns(domain);
        }
    }

    public LanScanResultDTO scanLan() {
        long startTime = System.currentTimeMillis();
        List<ArpEntryDTO> entries = new ArrayList<>();
        String subnet = "";
        String interfaceName = "";

        try {
            ProcessBuilder pb = new ProcessBuilder("arp", "-a");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"))) {
                String line;
                boolean inTable = false;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("---") || line.contains("接口")) {
                        Pattern ifacePattern = Pattern.compile("接口:\\s*(\\S+)\\s*-+\\s*(0x\\S+)?");
                        Matcher ifaceMatcher = ifacePattern.matcher(line);
                        if (ifaceMatcher.find()) {
                            interfaceName = ifaceMatcher.group(1);
                        }

                        Pattern subnetPattern = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)");
                        Matcher subnetMatcher = subnetPattern.matcher(line);
                        if (subnetMatcher.find()) {
                            String[] parts = subnetMatcher.group(1).split("\\.");
                            subnet = parts[0] + "." + parts[1] + "." + parts[2] + ".0/24";
                        }
                        inTable = true;
                        continue;
                    }

                    if (inTable) {
                        ArpEntryDTO entry = parseArpLine(line);
                        if (entry != null) {
                            entries.add(entry);
                        }
                    }
                }
            }

            process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);

        } catch (Exception e) {
            log.error("ARP扫描失败", e);
        }

        if (subnet.isEmpty()) {
            subnet = detectSubnet();
        }

        return LanScanResultDTO.builder()
                .subnet(subnet)
                .interfaceName(interfaceName)
                .arpEntries(entries)
                .totalDevices(entries.size())
                .scanDurationMs(System.currentTimeMillis() - startTime)
                .build();
    }

    private ArpEntryDTO parseArpLine(String line) {
        if (line == null || line.trim().isEmpty()) return null;

        Pattern p = Pattern.compile(
                "^\\s*(\\d+\\.\\d+\\.\\d+\\.\\d+)\\s+([0-9a-fA-F]{2}[:-][0-9a-fA-F]{2}[:-][0-9a-fA-F]{2}[:-][0-9a-fA-F]{2}[:-][0-9a-fA-F]{2}[:-][0-9a-fA-F]{2})\\s+(\\S+)\\s*(\\S*)\\s*$"
        );
        Matcher m = p.matcher(line.trim());
        if (!m.find()) return null;

        return ArpEntryDTO.builder()
                .ipAddress(m.group(1))
                .macAddress(m.group(2).replace('-', ':'))
                .type(m.group(3))
                .interfaceName(m.group(4))
                .build();
    }

    public ConnectivityAnalysisDTO analyzeConnectivity(String target) {
        String targetIp = resolveHostname(target);
        boolean isLan = isLanAddress(targetIp);

        DnsResultDTO dnsResult = null;
        if (!isIpAddress(target)) {
            dnsResult = resolveDnsNative(target);
        }

        PingResultDTO pingResult = pingNative(target, 4);

        TracerouteResultDTO tracerouteResult = traceroute(target);

        String overallStatus = determineOverallStatus(pingResult, tracerouteResult, dnsResult, isLan);
        String diagnosis = generateDiagnosis(pingResult, tracerouteResult, dnsResult, isLan);
        List<String> suggestions = generateSuggestions(pingResult, tracerouteResult, dnsResult, isLan);

        String sourceIp = getLocalIpAddress();

        return ConnectivityAnalysisDTO.builder()
                .sourceIp(sourceIp)
                .targetIp(targetIp)
                .targetHost(target)
                .isLan(isLan)
                .pingable(pingResult.isReachable())
                .pingResult(pingResult)
                .tracerouteResult(tracerouteResult)
                .dnsResult(dnsResult)
                .overallStatus(overallStatus)
                .diagnosis(diagnosis)
                .suggestions(suggestions)
                .build();
    }

    private String determineOverallStatus(PingResultDTO ping, TracerouteResultDTO trace, DnsResultDTO dns, boolean isLan) {
        if (dns != null && !dns.isSuccess()) {
            return "DNS解析失败";
        }
        if (ping != null && ping.isReachable()) {
            if (ping.getPacketLossRate() > 0 && ping.getPacketLossRate() < 100) {
                return "连接不稳定（丢包率" + String.format("%.1f", ping.getPacketLossRate()) + "%）";
            }
            return "连接正常";
        }
        if (trace != null && trace.getBlockHop() != null) {
            return "连接中断（第" + trace.getBlockHop() + "跳阻断）";
        }
        return "目标不可达";
    }

    private String generateDiagnosis(PingResultDTO ping, TracerouteResultDTO trace, DnsResultDTO dns, boolean isLan) {
        StringBuilder sb = new StringBuilder();

        if (dns != null && !dns.isSuccess()) {
            sb.append("DNS解析失败，无法将域名解析为IP地址。");
            sb.append("请检查DNS服务器配置或尝试使用其他DNS服务器（如8.8.8.8或114.114.114.114）。");
            return sb.toString();
        }

        if (ping == null || !ping.isReachable()) {
            sb.append("目标主机不可达。");
            if (isLan) {
                sb.append("该地址属于局域网，可能原因：目标设备未开机、IP地址冲突、不在同一网段、防火墙阻止ICMP请求。");
            } else {
                sb.append("该地址属于互联网，可能原因：目标服务器故障、网络链路中断、防火墙/安全组策略阻止。");
            }
            if (trace != null && trace.getBlockAnalysis() != null) {
                sb.append(trace.getBlockAnalysis());
            }
        } else {
            sb.append("目标主机可达。");
            if (ping.getPingTimeMs() > 100) {
                sb.append(String.format("延迟较高（%dms），", ping.getPingTimeMs()));
                if (isLan) {
                    sb.append("局域网延迟通常应低于10ms，可能存在网络拥堵或链路质量问题。");
                } else {
                    sb.append("可能存在网络拥堵或物理距离较远。");
                }
            } else {
                sb.append(String.format("延迟正常（%dms）。", ping.getPingTimeMs()));
            }
            if (ping.getPacketLossRate() > 0) {
                sb.append(String.format("存在%.1f%%丢包，网络质量不佳。", ping.getPacketLossRate()));
            }
        }

        return sb.toString();
    }

    private List<String> generateSuggestions(PingResultDTO ping, TracerouteResultDTO trace, DnsResultDTO dns, boolean isLan) {
        List<String> suggestions = new ArrayList<>();

        if (dns != null && !dns.isSuccess()) {
            suggestions.add("检查DNS配置：尝试切换到公共DNS（8.8.8.8 / 114.114.114.114）");
            suggestions.add("清除DNS缓存：运行 ipconfig /flushdns");
            suggestions.add("确认域名拼写正确");
            return suggestions;
        }

        if (ping == null || !ping.isReachable()) {
            if (isLan) {
                suggestions.add("确认目标设备已开机并连接到网络");
                suggestions.add("检查是否在同一网段/VLAN");
                suggestions.add("尝试通过ARP表查看设备MAC地址：arp -a");
                suggestions.add("检查交换机/路由器端口状态");
                suggestions.add("确认没有IP地址冲突");
            } else {
                suggestions.add("检查本地网络连接是否正常");
                suggestions.add("尝试ping网关确认局域网正常");
                suggestions.add("检查防火墙/安全组是否阻止了ICMP");
                suggestions.add("尝试使用其他网络工具（如telnet测试端口连通性）");
                if (trace != null && trace.getBlockHop() != null) {
                    suggestions.add(String.format("路由追踪显示第%d跳中断，联系网络管理员排查该路由节点", trace.getBlockHop()));
                }
            }
        } else {
            if (ping.getPingTimeMs() > 100) {
                suggestions.add("网络延迟较高，检查是否有带宽占用大的应用");
                suggestions.add("尝试重启路由器/调制解调器");
                if (!isLan) {
                    suggestions.add("考虑使用CDN或更换就近的服务节点");
                }
            }
            if (ping.getPacketLossRate() > 0) {
                suggestions.add("存在丢包，检查网络链路质量");
                suggestions.add("检查网线/WiFi连接是否稳定");
                suggestions.add("排除网络设备过载的可能性");
            }
            if (ping.getPingTimeMs() <= 100 && ping.getPacketLossRate() == 0) {
                suggestions.add("网络连接正常，无需额外操作");
            }
        }

        return suggestions;
    }

    private String fetchPublicIp() {
        String[] apis = {
                "https://api.ipify.org",
                "https://ifconfig.me/ip",
                "https://api.ip.sb/ip"
        };

        for (String api : apis) {
            try {
                ProcessBuilder pb = new ProcessBuilder("curl", "-s", "--connect-timeout", "5", api);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                String result;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    result = reader.readLine();
                }
                process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);

                if (result != null && IPV4_PATTERN.matcher(result.trim()).matches()) {
                    return result.trim();
                }
            } catch (Exception e) {
                log.debug("从 {} 获取公网IP失败", api, e);
            }
        }
        return null;
    }

    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "未知";
        }
    }

    private List<NetworkInterfaceDTO> getNetworkInterfaces() {
        List<NetworkInterfaceDTO> result = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;

                String ipv4 = "";
                String ipv6 = "";
                String subnetMask = "";
                String macAddress = "";

                for (java.net.InterfaceAddress addr : ni.getInterfaceAddresses()) {
                    if (addr.getAddress() instanceof Inet4Address) {
                        ipv4 = addr.getAddress().getHostAddress();
                        short prefixLen = addr.getNetworkPrefixLength();
                        subnetMask = prefixLengthToSubnetMask(prefixLen);
                    } else if (addr.getAddress() instanceof java.net.Inet6Address) {
                        String v6 = addr.getAddress().getHostAddress();
                        if (v6 != null && !v6.startsWith("fe80")) {
                            ipv6 = v6;
                        }
                    }
                }

                byte[] macBytes = ni.getHardwareAddress();
                if (macBytes != null) {
                    macAddress = formatMacAddress(macBytes);
                }

                if (ipv4.isEmpty() && ipv6.isEmpty()) continue;

                result.add(NetworkInterfaceDTO.builder()
                        .name(ni.getName())
                        .displayName(ni.getDisplayName())
                        .ipv4Address(ipv4)
                        .ipv6Address(ipv6)
                        .subnetMask(subnetMask)
                        .macAddress(macAddress)
                        .up(ni.isUp())
                        .loopback(ni.isLoopback())
                        .mtu(ni.getMTU())
                        .build());
            }
        } catch (Exception e) {
            log.error("获取网络接口信息失败", e);
        }
        return result;
    }

    private String prefixLengthToSubnetMask(short prefixLength) {
        if (prefixLength < 0 || prefixLength > 32) return "";
        int mask = 0xFFFFFFFF << (32 - prefixLength);
        return String.format("%d.%d.%d.%d",
                (mask >> 24) & 0xFF,
                (mask >> 16) & 0xFF,
                (mask >> 8) & 0xFF,
                mask & 0xFF);
    }

    private String formatMacAddress(byte[] macBytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < macBytes.length; i++) {
            sb.append(String.format("%02X", macBytes[i]));
            if (i < macBytes.length - 1) sb.append(":");
        }
        return sb.toString();
    }

    private String detectDefaultGateway() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ipconfig");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);

            Pattern p = Pattern.compile("默认网关[^:]*:\\s*(\\d+\\.\\d+\\.\\d+\\.\\d+)");
            Matcher m = p.matcher(output.toString());
            if (m.find()) return m.group(1);

            Pattern p2 = Pattern.compile("Default Gateway[^:]*:\\s*(\\d+\\.\\d+\\.\\d+\\.\\d+)");
            Matcher m2 = p2.matcher(output.toString());
            if (m2.find()) return m2.group(1);

        } catch (Exception e) {
            log.debug("检测默认网关失败", e);
        }
        return "";
    }

    private String detectDnsServer() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ipconfig", "/all");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);

            Pattern p = Pattern.compile("DNS服务器[^:]*:\\s*(\\d+\\.\\d+\\.\\d+\\.\\d+)");
            Matcher m = p.matcher(output.toString());
            if (m.find()) return m.group(1);

            Pattern p2 = Pattern.compile("DNS Servers[^:]*:\\s*(\\d+\\.\\d+\\.\\d+\\.\\d+)");
            Matcher m2 = p2.matcher(output.toString());
            if (m2.find()) return m2.group(1);

        } catch (Exception e) {
            log.debug("检测DNS服务器失败", e);
        }
        return "";
    }

    private String resolveHostname(String host) {
        try {
            return InetAddress.getByName(host).getHostAddress();
        } catch (Exception e) {
            return host;
        }
    }

    private boolean isLanAddress(String ip) {
        if (ip == null || ip.isEmpty()) return false;
        try {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) return false;

            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);

            if (first == 10) return true;
            if (first == 172 && second >= 16 && second <= 31) return true;
            if (first == 192 && second == 168) return true;
            if (first == 127) return true;
            if (first == 169 && second == 254) return true;

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isIpAddress(String value) {
        if (value == null) return false;
        return IPV4_PATTERN.matcher(value.trim()).matches();
    }

    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;
                for (java.net.InterfaceAddress addr : ni.getInterfaceAddresses()) {
                    if (addr.getAddress() instanceof Inet4Address && !addr.getAddress().isLoopbackAddress()) {
                        return addr.getAddress().getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("获取本机IP失败", e);
        }
        return "127.0.0.1";
    }

    private String detectSubnet() {
        try {
            String localIp = getLocalIpAddress();
            if (!"127.0.0.1".equals(localIp)) {
                String[] parts = localIp.split("\\.");
                return parts[0] + "." + parts[1] + "." + parts[2] + ".0/24";
            }
        } catch (Exception e) {
            log.debug("检测子网失败", e);
        }
        return "";
    }
}
