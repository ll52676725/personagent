package com.example.agentplatform.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionApplyDTO {
    @NotBlank(message = "申请理由不能为空")
    private String applyReason;
}