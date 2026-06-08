package com.example.agentplatform.tools.controller;

import com.example.agentplatform.common.dto.Result;
import com.example.agentplatform.tools.dto.DockerDeployResultDTO;
import com.example.agentplatform.tools.dto.DockerGenerateRequestDTO;
import com.example.agentplatform.tools.dto.DockerGenerateResultDTO;
import com.example.agentplatform.tools.service.DockerGeneratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/agent-tools/docker")
@RequiredArgsConstructor
public class DockerGeneratorController {

    private final DockerGeneratorService dockerGeneratorService;

    @PostMapping("/generate")
    public ResponseEntity<Result<DockerGenerateResultDTO>> generate(@Valid @RequestBody DockerGenerateRequestDTO request) {
        log.info("生成 Dockerfile: projectPath={}", request.getProjectPath());
        try {
            DockerGenerateResultDTO result = dockerGeneratorService.generate(request);
            return ResponseEntity.ok(Result.success(result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Result.badRequest(e.getMessage()));
        } catch (Exception e) {
            log.error("生成 Dockerfile 失败", e);
            return ResponseEntity.internalServerError().body(Result.serverError("生成失败: " + e.getMessage()));
        }
    }

    @PostMapping("/deploy")
    public ResponseEntity<Result<DockerDeployResultDTO>> deploy(@Valid @RequestBody DockerGenerateRequestDTO request) {
        log.info("一键部署: projectPath={}", request.getProjectPath());
        try {
            DockerDeployResultDTO result = dockerGeneratorService.deploy(request);
            if (result.getSuccess()) {
                return ResponseEntity.ok(Result.success(result));
            } else {
                return ResponseEntity.ok(Result.error(500, result.getErrorMessage()));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Result.badRequest(e.getMessage()));
        } catch (Exception e) {
            log.error("部署失败", e);
            return ResponseEntity.internalServerError().body(Result.serverError("部署失败: " + e.getMessage()));
        }
    }

    @GetMapping("/check-docker")
    public ResponseEntity<Result<Boolean>> checkDocker() {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                return ResponseEntity.ok(Result.success(true));
            }
            return ResponseEntity.ok(Result.success(false));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.success(false));
        }
    }
}
