package com.example.agentplatform.tools.controller;

import com.example.agentplatform.common.dto.Result;
import com.example.agentplatform.tools.dto.IconDesignRequestDTO;
import com.example.agentplatform.tools.dto.IconDesignResultDTO;
import com.example.agentplatform.tools.service.IconDesignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/agent-tools/icon")
@RequiredArgsConstructor
public class IconDesignController {

    private final IconDesignService iconDesignService;

    @PostMapping("/design")
    public ResponseEntity<Result<IconDesignResultDTO>> generateDesign(
            @Valid @RequestBody IconDesignRequestDTO request) {
        log.info("生成图标设计方案，品牌: {}", request.getBrandName());
        IconDesignResultDTO result = iconDesignService.generateDesign(request);
        return ResponseEntity.ok(Result.success("图标设计方案生成成功", result));
    }
}
