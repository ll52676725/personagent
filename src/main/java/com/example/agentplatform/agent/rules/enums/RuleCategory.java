package com.example.agentplatform.agent.rules.enums;

/**
 * 规则分类枚举
 * 
 * @author System
 * @since 2025-01-01
 */
public enum RuleCategory {

    GLOBAL("global", "全局规则"),
    PROJECT("project", "项目规则"),
    CODING_STANDARD("coding_standard", "编码规范"),
    DOCUMENTATION("documentation", "文档规范"),
    AI_TOOL("ai_tool", "AI工具规则");

    private final String code;
    private final String desc;

    RuleCategory(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static RuleCategory fromCode(String code) {
        for (RuleCategory category : values()) {
            if (category.getCode().equals(code)) {
                return category;
            }
        }
        throw new IllegalArgumentException("未知规则分类: " + code);
    }
}
