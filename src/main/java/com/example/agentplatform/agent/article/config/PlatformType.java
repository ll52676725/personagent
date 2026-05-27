package com.example.agentplatform.agent.article.config;

import lombok.Getter;

@Getter
public enum PlatformType {
    CSDN("csdn", "CSDN博客", 1500, 3, true, true),
    TOUTIAO("toutiao", "今日头条", 2000, 5, true, false),
    ZHIHU("zhihu", "知乎", 2500, 4, true, true),
    JUEJIN("juejin", "掘金", 1800, 4, true, true),
    WECHAT("wechat", "微信公众号", 2000, 6, false, true),
    BILIBILI("bilibili", "B站专栏", 1500, 3, true, true);

    private final String code;
    private final String name;
    private final int minWordCount;
    private final int minImageCount;
    private final boolean supportCodeBlock;
    private final boolean supportTable;

    PlatformType(String code, String name, int minWordCount, int minImageCount,
                 boolean supportCodeBlock, boolean supportTable) {
        this.code = code;
        this.name = name;
        this.minWordCount = minWordCount;
        this.minImageCount = minImageCount;
        this.supportCodeBlock = supportCodeBlock;
        this.supportTable = supportTable;
    }

    public static PlatformType fromCode(String code) {
        for (PlatformType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown platform: " + code);
    }
}
