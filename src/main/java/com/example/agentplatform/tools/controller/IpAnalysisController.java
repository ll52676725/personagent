package com.example.agentplatform.tools.controller;

import com.example.agentplatform.common.dto.Result;
import com.example.agentplatform.tools.dto.*;
import com.example.agentplatform.tools.service.IpAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/agent-tools/ip")
@RequiredArgsConstructor
public class IpAnalysisController {

    private final IpAnalysisService ipAnalysisService;

    @GetMapping("/current")
    public ResponseEntity<Result<CurrentIpInfoDTO>> getCurrentIpInfo() {
        try {
            CurrentIpInfoDTO result = ipAnalysisService.getCurrentIpInfo();
            return ResponseEntity.ok(Result.success("获取当前IP信息成功", result));
        } catch (Exception e) {
            log.error("获取当前IP信息失败", e);
            return ResponseEntity.status(500).body(Result.serverError("获取当前IP信息失败: " + e.getMessage()));
        }
    }

    @GetMapping("/ping")
    public ResponseEntity<Result<PingResultDTO>> ping(
            @RequestParam String target,
            @RequestParam(required = false) Integer count) {
        if (target == null || target.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Result.badRequest("目标地址不能为空"));
        }
        try {
            PingResultDTO result = ipAnalysisService.pingNative(target.trim(), count);
            return ResponseEntity.ok(Result.success("Ping完成", result));
        } catch (Exception e) {
            log.error("Ping失败: {}", target, e);
            return ResponseEntity.status(500).body(Result.serverError("Ping执行失败: " + e.getMessage()));
        }
    }

    @GetMapping("/traceroute")
    public ResponseEntity<Result<TracerouteResultDTO>> traceroute(@RequestParam String target) {
        if (target == null || target.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Result.badRequest("目标地址不能为空"));
        }
        try {
            TracerouteResultDTO result = ipAnalysisService.traceroute(target.trim());
            return ResponseEntity.ok(Result.success("路由追踪完成", result));
        } catch (Exception e) {
            log.error("Traceroute失败: {}", target, e);
            return ResponseEntity.status(500).body(Result.serverError("路由追踪失败: " + e.getMessage()));
        }
    }

    @GetMapping("/dns")
    public ResponseEntity<Result<DnsResultDTO>> resolveDns(@RequestParam String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Result.badRequest("域名不能为空"));
        }
        try {
            DnsResultDTO result = ipAnalysisService.resolveDnsNative(domain.trim());
            return ResponseEntity.ok(Result.success("DNS解析完成", result));
        } catch (Exception e) {
            log.error("DNS解析失败: {}", domain, e);
            return ResponseEntity.status(500).body(Result.serverError("DNS解析失败: " + e.getMessage()));
        }
    }

    @GetMapping("/lan-scan")
    public ResponseEntity<Result<LanScanResultDTO>> scanLan() {
        try {
            LanScanResultDTO result = ipAnalysisService.scanLan();
            return ResponseEntity.ok(Result.success("局域网扫描完成", result));
        } catch (Exception e) {
            log.error("局域网扫描失败", e);
            return ResponseEntity.status(500).body(Result.serverError("局域网扫描失败: " + e.getMessage()));
        }
    }

    @GetMapping("/analyze")
    public ResponseEntity<Result<ConnectivityAnalysisDTO>> analyzeConnectivity(@RequestParam String target) {
        if (target == null || target.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Result.badRequest("目标地址不能为空"));
        }
        try {
            ConnectivityAnalysisDTO result = ipAnalysisService.analyzeConnectivity(target.trim());
            return ResponseEntity.ok(Result.success("连通性分析完成", result));
        } catch (Exception e) {
            log.error("连通性分析失败: {}", target, e);
            return ResponseEntity.status(500).body(Result.serverError("连通性分析失败: " + e.getMessage()));
        }
    }
}
