# 项目开发规则

## Maven 配置规则

**重要：本项目必须使用项目自带的 Maven settings.xml 文件**

- 配置文件位置：`./.mvn/settings.xml`
- 该文件包含 Spring Milestones 和 Maven Central 仓库的特殊配置
- 请勿使用全局 Maven settings.xml，否则可能导致依赖无法下载

## 依赖仓库说明

项目配置了以下 Maven 仓库：

1. **Spring Milestones** - 用于下载 Spring AI 相关依赖
2. **Maven Central** - 官方 Maven 中央仓库
3. **内网镜像 (nkrepo)** - 其他依赖的镜像加速（排除上述两个仓库）

## 编译命令

```bash
# 使用项目配置编译
mvn compile -s ./.mvn/settings.xml

# 完整打包
mvn clean package -s ./.mvn/settings.xml
```

## MinerU 文档解析集成

项目已集成 MinerU 精细化文档解析引擎，支持：
- PDF、Word、Excel、PPT、图片等多格式
- 表格、图片、公式等结构化数据提取
- 高质量 Markdown 输出

详细配置见 `application.yml` 中的 `agent.platform.knowledge.mineru` 节点。
