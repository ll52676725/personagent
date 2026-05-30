package com.example.agentplatform.agent.rules.enums;

/**
 * 冲突处理策略枚举
 * 
 * @author System
 * @since 2025-01-01
 */
public enum ConflictStrategy {

    ASK("ask", "询问用户"),
    OVERWRITE("overwrite", "直接覆盖"),
    KEEP_LOCAL("keep_local", "保留本地"),
    MERGE("merge", "智能合并"),
    RENAME("rename", "重命名保存");

    private final String code;
    private final String desc;

    ConflictStrategy(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ConflictStrategy fromCode(String code) {
        if (code == null) {
            return ASK;
        }
        for (ConflictStrategy strategy : values()) {
            if (strategy.getCode().equals(code.toLowerCase())) {
                return strategy;
            }
        }
        return ASK;
    }
}
