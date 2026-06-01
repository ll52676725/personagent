package com.example.agentplatform.tools.service.impl;

import com.example.agentplatform.agent.article.service.GenerateService;
import com.example.agentplatform.common.exception.BusinessException;
import com.example.agentplatform.tools.dto.SqlErrorDetailDTO;
import com.example.agentplatform.tools.dto.SqlFormatRequestDTO;
import com.example.agentplatform.tools.dto.SqlFormatResultDTO;
import com.example.agentplatform.tools.service.SqlFormatService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL格式化服务实现类
 * <p>
 * 提供专业的SQL格式化、压缩、语法校验、AI修复和性能优化功能。
 * 采用词法分析器（Tokenizer）将SQL解析为Token流，再通过状态机进行格式化，
 * 支持复杂的窗口函数、子查询、CTE表达式等高级SQL语法。
 * </p>
 * <p>
 * 设计特点：
 * <ul>
 *   <li>基于Token的词法分析，而非简单正则替换，格式化效果专业可靠</li>
 *   <li>多词关键字识别（GROUP BY, LEFT JOIN等），确保语义正确</li>
 *   <li>智能状态管理，支持子查询嵌套缩进、CASE表达式、窗口函数</li>
 *   <li>本地格式化优先，失败时自动降级到AI格式化</li>
 *   <li>支持MySQL、PostgreSQL、Oracle、SQL Server、SQLite等多种数据库</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SqlFormatServiceImpl implements SqlFormatService {

    private final ObjectMapper objectMapper;
    private final GenerateService generateService;

    @Value("${agent.platform.ai.fallback-enabled:true}")
    private boolean fallbackEnabled;

    private static final String SQL_FORMAT_SYSTEM_PROMPT = """
        你是一个专业的SQL格式化和优化专家，精通各种数据库（MySQL、PostgreSQL、Oracle、SQL Server、SQLite等）的SQL语法。
        
        格式化规则：
        1. 关键字（SELECT、FROM、WHERE、JOIN、GROUP BY、ORDER BY等）大写
        2. 每个主要子句换行并适当缩进
        3. 长WHERE条件每个条件单独一行
        4. JOIN语句单独一行并对齐
        5. 函数名和参数之间保持适当空格
        6. 保持原SQL的语义不变，只改变格式
        
        如果用户提供了数据库类型，请针对该数据库的语法特点进行格式化。
        
        必须返回严格的JSON格式：
        {
          "formattedSql": "格式化后的SQL语句",
          "sqlType": "SQL类型（SELECT/INSERT/UPDATE/DELETE/CREATE等）",
          "statistics": "统计信息（表数量、条件数量等）",
          "suggestions": ["优化建议1", "优化建议2"]
        }
        只返回JSON，不要包含其他内容。
        """;

    private static final String SQL_FIX_SYSTEM_PROMPT = """
        你是一个专业的SQL语法修复专家，擅长分析和修复各种SQL语法错误。
        
        修复原则：
        1. 保持原始SQL的语义和意图不变，只修复语法错误
        2. 常见错误类型及修复方法：
           - 缺少逗号：在列名、表名之间添加缺失的逗号
           - 括号不匹配：补充缺失的括号或删除多余的括号
           - 引号不匹配：修复单引号或双引号的闭合问题
           - 关键字拼写错误：修正SQL关键字的拼写（如SELEC→SELECT, FORM→FROM）
           - 缺少WHERE：根据上下文添加WHERE子句
           - 无效的表名或列名：根据常见命名模式进行修正
           - JOIN缺少ON条件：根据表名推断并添加合理的ON条件
        
        必须返回严格的JSON格式：
        {
          "fixedSql": "修复后的SQL语句",
          "fixDescription": "详细描述修复了哪些问题",
          "sqlType": "SQL类型",
          "valid": true,
          "suggestions": ["使用建议1", "使用建议2"]
        }
        只返回JSON，不要包含其他内容。
        """;

    private static final String SQL_OPTIMIZE_SYSTEM_PROMPT = """
        你是一个资深的SQL性能优化专家，精通各种数据库的查询优化技术。
        
        优化原则：
        1. 分析SQL的执行计划潜在问题
        2. 提供具体的优化建议，包括：
           - 索引建议：哪些列需要建索引
           - 查询改写：如何改写SQL提高性能
           - 避免全表扫描：如何利用索引
           - 减少数据扫描：添加过滤条件
           - 避免SELECT *：只查询需要的列
           - JOIN优化：选择合适的JOIN顺序和类型
           - 子查询优化：考虑使用JOIN代替子查询
           - 避免在WHERE条件中使用函数
        
        必须返回严格的JSON格式：
        {
          "optimizedSql": "优化后的SQL语句（如果需要改写）",
          "originalSql": "原始SQL语句",
          "optimizationSuggestions": [
            {"suggestion": "具体优化建议", "impact": "影响程度（高/中/低）", "reason": "优化原因"}
          ],
          "indexSuggestions": [
            {"table": "表名", "columns": ["列1", "列2"], "type": "索引类型"}
          ],
          "estimatedImprovement": "预估性能提升描述",
          "warnings": ["需要注意的问题1", "需要注意的问题2"]
        }
        只返回JSON，不要包含其他内容。
        """;

    /**
     * 标准SQL关键字集合，用于词法分析时识别关键字
     * 注意：这里存储的是单个词，多词关键字（如GROUP BY）在后续合并阶段处理
     */
    private static final Set<String> SQL_KEYWORDS = new LinkedHashSet<>(Arrays.asList(
            "SELECT", "FROM", "WHERE", "GROUP BY", "ORDER BY", "HAVING", "JOIN",
            "LEFT JOIN", "RIGHT JOIN", "INNER JOIN", "FULL JOIN", "OUTER JOIN",
            "CROSS JOIN", "NATURAL JOIN", "ON", "AS", "AND", "OR", "NOT", "IN",
            "EXISTS", "BETWEEN", "LIKE", "IS NULL", "IS NOT NULL", "DISTINCT",
            "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE", "CREATE",
            "ALTER", "DROP", "TABLE", "INDEX", "VIEW", "DATABASE", "SCHEMA",
            "PRIMARY KEY", "FOREIGN KEY", "UNIQUE", "AUTO_INCREMENT",
            "LIMIT", "OFFSET", "UNION", "UNION ALL", "INTERSECT", "EXCEPT",
            "CASE", "WHEN", "THEN", "ELSE", "END", "CAST", "CONVERT",
            "COUNT", "SUM", "AVG", "MIN", "MAX", "ROUND", "CONCAT",
            "SUBSTRING", "UPPER", "LOWER", "TRIM", "LENGTH",
            "ASC", "DESC", "NULLS FIRST", "NULLS LAST",
            "WITH", "RECURSIVE", "CTE", "WINDOW", "OVER",
            "PARTITION BY", "ROWS BETWEEN", "RANGE BETWEEN",
            "FOR UPDATE", "FOR SHARE", "LOCK IN SHARE MODE",
            "COMMENT", "COMMIT", "ROLLBACK", "BEGIN", "START TRANSACTION"
    ));

    /**
     * 各数据库特有关键字映射
     * 不同数据库有各自的扩展语法，格式化时需要特殊处理
     */
    private static final Map<String, List<String>> DB_SPECIFIC_KEYWORDS = new HashMap<>();

    static {
        DB_SPECIFIC_KEYWORDS.put("MYSQL", Arrays.asList("LIMIT", "OFFSET", "AUTO_INCREMENT", "ENGINE", "CHARSET"));
        DB_SPECIFIC_KEYWORDS.put("POSTGRESQL", Arrays.asList("LIMIT", "OFFSET", "SERIAL", "ILIKE", "JSONB", "ARRAY"));
        DB_SPECIFIC_KEYWORDS.put("ORACLE", Arrays.asList("ROWNUM", "SYSDATE", "VARCHAR2", "NUMBER", "SEQUENCE"));
        DB_SPECIFIC_KEYWORDS.put("SQLSERVER", Arrays.asList("TOP", "IDENTITY", "GETDATE()", "NVARCHAR", "WITH (NOLOCK)"));
        DB_SPECIFIC_KEYWORDS.put("SQLITE", Arrays.asList("LIMIT", "AUTOINCREMENT", "TEXT", "INTEGER", "REAL"));
    }

    /**
     * 检测SQL类型的正则表达式，匹配SQL开头的第一个关键字
     * 用于快速识别是SELECT/INSERT/UPDATE/DELETE等操作类型
     */
    private static final Pattern SQL_TYPE_PATTERN = Pattern.compile(
            "^\\s*(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|WITH|MERGE|CALL|EXPLAIN|SHOW|DESCRIBE|DESC)",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public SqlFormatResultDTO format(SqlFormatRequestDTO request) {
        String content = request.getContent().trim();
        int indentSize = request.getIndentSize() != null ? request.getIndentSize() : 4;
        boolean uppercase = request.getUppercase() != null && request.getUppercase();
        String dbType = request.getDbType() != null ? request.getDbType().toUpperCase() : "MYSQL";

        log.debug("开始格式化SQL，数据库类型: {}, 缩进大小: {}, 关键字大写: {}", dbType, indentSize, uppercase);

        try {
            String formattedSql = formatSqlLocally(content, indentSize, uppercase, dbType);
            String compactSql = compactSql(content);
            String sqlType = detectSqlType(content);
            String statistics = generateStatistics(content);
            List<String> suggestions = generateSuggestions(content, dbType);

            log.info("SQL格式化成功，类型: {}, 数据库: {}, 原始长度: {}字符, 格式化后长度: {}字符",
                    sqlType, dbType, content.length(), formattedSql.length());

            return SqlFormatResultDTO.builder()
                    .success(true)
                    .formattedSql(formattedSql)
                    .compactSql(compactSql)
                    .indentSize(indentSize)
                    .uppercase(uppercase)
                    .dbType(dbType)
                    .sqlType(sqlType)
                    .statistics(statistics)
                    .suggestions(suggestions)
                    .errors(Collections.emptyList())
                    .build();

        } catch (Exception e) {
            log.warn("本地SQL格式化失败，尝试使用AI格式化，错误信息: {}", e.getMessage(), e);

            if (request.getEnableAI() != null && request.getEnableAI()) {
                log.debug("启用了AI格式化，调用AI接口");
                return formatWithAI(request);
            }

            log.debug("未启用AI格式化，返回错误信息");
            List<SqlErrorDetailDTO> errors = parseSqlError(e, content);
            return SqlFormatResultDTO.builder()
                    .success(false)
                    .errors(errors)
                    .indentSize(indentSize)
                    .uppercase(uppercase)
                    .dbType(dbType)
                    .build();
        }
    }

    /**
     * 本地SQL格式化入口方法
     * <p>
     * 采用两阶段格式化策略：
     * 1. 词法分析阶段（tokenize）：将SQL字符串解析为结构化的Token流
     * 2. 格式化输出阶段（formatTokens）：根据Token类型和上下文进行智能缩进和换行
     * </p>
     *
     * @param sql        原始SQL字符串
     * @param indentSize 缩进空格数
     * @param uppercase  是否将关键字转为大写
     * @param dbType     数据库类型（MYSQL/POSTGRESQL/ORACLE/SQLSERVER/SQLITE）
     * @return 格式化后的SQL字符串
     */
    private String formatSqlLocally(String sql, int indentSize, boolean uppercase, String dbType) {
        log.debug("开始本地SQL格式化，SQL长度: {}字符", sql.length());

        List<Token> tokens = tokenize(sql, uppercase, dbType);
        log.debug("SQL词法分析完成，生成 {} 个Token", tokens.size());

        String formatted = formatTokens(tokens, indentSize);
        log.debug("SQL格式化完成，输出长度: {}字符", formatted.length());

        return formatted;
    }

    /**
     * SQL词法单元类型枚举
     * <p>
     * 定义了SQL解析器能够识别的所有Token类型，
     * 每个字符序列都会被归类到其中一种类型中进行后续处理。
     * </p>
     */
    private enum TokenType {
        /** SQL关键字（SELECT, FROM, WHERE等） */
        KEYWORD,
        /** 标识符（表名、列名、别名等） */
        IDENTIFIER,
        /** 字符串常量（'string'） */
        STRING,
        /** 数值常量（123, 45.67） */
        NUMBER,
        /** 操作符（=, <, >, !=等） */
        OPERATOR,
        /** 逗号分隔符 */
        COMMA,
        /** 点号（用于表名.列名） */
        DOT,
        /** 左括号 */
        OPEN_PAREN,
        /** 右括号 */
        CLOSE_PAREN,
        /** 分号结束符 */
        SEMICOLON,
        /** 空白字符（空格、制表符、换行符等） */
        WHITESPACE,
        /** SQL注释（-- 或 /* */
        COMMENT,
        /** 其他无法识别的字符 */
        OTHER,
        /** 多词关键字（GROUP BY, LEFT JOIN等合并后的结果） */
        MULTI_KEYWORD
    }

    /**
     * SQL词法单元（Token）
     * <p>
     * 表示SQL中具有独立语义的最小单元，包含类型和值两个属性。
     * Token化是SQL格式化的基础，它将无结构的字符串转换为有类型的数据流。
     * </p>
     */
    private static final class Token {
        /** Token的类型 */
        final TokenType type;
        /** Token的原始值 */
        final String value;

        Token(TokenType type, String value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public String toString() {
            return type + "(" + value + ")";
        }
    }

    /**
     * 词法分析阶段使用的关键字集合
     * <p>
     * 注意：这里只存储单个词，像"GROUP BY"、"LEFT JOIN"这类多词关键字
     * 需要在{@link #mergeMultiKeywords(List)}阶段进行合并处理。
     * </p>
     */
    private static final Set<String> KEYWORD_SET = new HashSet<>(Arrays.asList(
            "SELECT", "FROM", "WHERE", "GROUP", "BY", "ORDER", "HAVING",
            "JOIN", "LEFT", "RIGHT", "INNER", "FULL", "OUTER", "CROSS", "NATURAL",
            "ON", "AS", "AND", "OR", "NOT", "IN", "EXISTS", "BETWEEN", "LIKE",
            "IS", "NULL", "DISTINCT", "ALL", "ANY", "SOME",
            "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE",
            "CREATE", "ALTER", "DROP", "TABLE", "INDEX", "VIEW", "DATABASE", "SCHEMA",
            "PRIMARY", "FOREIGN", "KEY", "UNIQUE", "AUTO_INCREMENT", "REFERENCES",
            "LIMIT", "OFFSET", "UNION", "INTERSECT", "EXCEPT",
            "CASE", "WHEN", "THEN", "ELSE", "END",
            "CAST", "CONVERT", "COALESCE", "NULLIF",
            "COUNT", "SUM", "AVG", "MIN", "MAX", "ROUND", "CONCAT",
            "SUBSTRING", "UPPER", "LOWER", "TRIM", "LENGTH", "REPLACE",
            "ASC", "DESC", "WITH", "RECURSIVE",
            "OVER", "PARTITION", "ROWS", "RANGE", "UNBOUNDED", "PRECEDING", "FOLLOWING", "CURRENT", "ROW",
            "FOR", "UPDATE", "SHARE", "LOCK", "MODE",
            "IF", "EXISTS", "DEFAULT", "CHECK", "CONSTRAINT",
            "BEGIN", "COMMIT", "ROLLBACK", "START", "TRANSACTION",
            "GRANT", "REVOKE", "TRUNCATE", "MERGE", "USING",
            "TOP", "IDENTITY", "ROW_NUMBER", "RANK", "DENSE_RANK", "LAG", "LEAD",
            "BOOLEAN", "TRUE", "FALSE", "RETURN", "RETURNING"
    ));

    /**
     * 多词关键字模式列表
     * <p>
     * 按长度从长到短匹配，确保更长的模式优先匹配（例如优先匹配"LEFT OUTER JOIN"而非"LEFT JOIN"）。
     * 这些模式在{@link #mergeMultiKeywords(List)}阶段用于合并多个独立的关键字Token。
     * </p>
     */
    private static final List<String[]> MULTI_KEYWORD_PATTERNS = Arrays.asList(
            new String[]{"GROUP", "BY"},
            new String[]{"ORDER", "BY"},
            new String[]{"LEFT", "JOIN"},
            new String[]{"RIGHT", "JOIN"},
            new String[]{"INNER", "JOIN"},
            new String[]{"FULL", "JOIN"},
            new String[]{"OUTER", "JOIN"},
            new String[]{"CROSS", "JOIN"},
            new String[]{"NATURAL", "JOIN"},
            new String[]{"IS", "NULL"},
            new String[]{"IS", "NOT"},
            new String[]{"NOT", "NULL"},
            new String[]{"IS", "NOT", "NULL"},
            new String[]{"UNION", "ALL"},
            new String[]{"PARTITION", "BY"},
            new String[]{"FOR", "UPDATE"},
            new String[]{"FOR", "SHARE"},
            new String[]{"INSERT", "INTO"},
            new String[]{"NOT", "IN"},
            new String[]{"NOT", "EXISTS"},
            new String[]{"NOT", "BETWEEN"},
            new String[]{"NOT", "LIKE"},
            new String[]{"LEFT", "OUTER", "JOIN"},
            new String[]{"RIGHT", "OUTER", "JOIN"},
            new String[]{"FULL", "OUTER", "JOIN"},
            new String[]{"NULLS", "FIRST"},
            new String[]{"NULLS", "LAST"},
            new String[]{"UNBOUNDED", "PRECEDING"},
            new String[]{"UNBOUNDED", "FOLLOWING"},
            new String[]{"CURRENT", "ROW"},
            new String[]{"ROWS", "BETWEEN"},
            new String[]{"RANGE", "BETWEEN"},
            new String[]{"START", "TRANSACTION"},
            new String[]{"LOCK", "IN", "SHARE", "MODE"},
            new String[]{"GROUP", "BY"},
            new String[]{"ORDER", "BY"}
    );

    /**
     * SQL词法分析器（Tokenizer）
     * <p>
     * 将SQL字符串逐字符扫描，识别并分割为结构化的Token流。
     * 这是格式化引擎的核心基础，正确的Token化是后续格式化的前提。
     * </p>
     * <p>
     * 支持的语法特性：
     * <ul>
     *   <li>单引号字符串（支持转义''）</li>
     *   <li>双引号、反引号、方括号包裹的标识符</li>
     *   <li>单行注释 -- 和多行注释 /* *&#47; </li>
     *   <li>科学计数法数字（1e10, 2.5e-3）</li>
     *   <li>多字符操作符（!=, <=, >=, <>, ||, &&）</li>
     * </ul>
     * </p>
     *
     * @param sql       原始SQL字符串
     * @param uppercase 是否将关键字转为大写
     * @param dbType    数据库类型
     * @return 原始Token列表, 后续会经过多词关键字合并
     */
    private List<Token> tokenize(String sql, boolean uppercase, String dbType) {
        List<Token> rawTokens = new ArrayList<>();
        int i = 0;
        int len = sql.length();

        while (i < len) {
            char c = sql.charAt(i);

            // 1. 空白字符：连续的空格、制表符、换行符合并为一个Token
            if (Character.isWhitespace(c)) {
                int start = i;
                while (i < len && Character.isWhitespace(sql.charAt(i))) i++;
                rawTokens.add(new Token(TokenType.WHITESPACE, sql.substring(start, i)));
            }
            // 2. 单引号字符串：处理转义的单引号（'' 表示一个 '）
            else if (c == '\'') {
                int start = i;
                i++;
                while (i < len) {
                    if (sql.charAt(i) == '\'' && (i + 1 >= len || sql.charAt(i + 1) != '\'')) {
                        i++;
                        break;
                    }
                    if (sql.charAt(i) == '\'' && i + 1 < len && sql.charAt(i + 1) == '\'') {
                        i += 2;
                    } else {
                        i++;
                    }
                }
                rawTokens.add(new Token(TokenType.STRING, sql.substring(start, i)));
            }
            // 3. 双引号标识符（标准SQL、PostgreSQL）
            else if (c == '"') {
                int start = i;
                i++;
                while (i < len && sql.charAt(i) != '"') i++;
                if (i < len) i++;
                rawTokens.add(new Token(TokenType.IDENTIFIER, sql.substring(start, i)));
            }
            // 4. 反引号标识符（MySQL）
            else if (c == '`') {
                int start = i;
                i++;
                while (i < len && sql.charAt(i) != '`') i++;
                if (i < len) i++;
                rawTokens.add(new Token(TokenType.IDENTIFIER, sql.substring(start, i)));
            }
            // 5. 方括号标识符（SQL Server）
            else if (c == '[') {
                int start = i;
                i++;
                while (i < len && sql.charAt(i) != ']') i++;
                if (i < len) i++;
                rawTokens.add(new Token(TokenType.IDENTIFIER, sql.substring(start, i)));
            }
            // 6. 单行注释（--）
            else if (c == '-' && i + 1 < len && sql.charAt(i + 1) == '-') {
                int start = i;
                while (i < len && sql.charAt(i) != '\n') i++;
                rawTokens.add(new Token(TokenType.COMMENT, sql.substring(start, i)));
            }
            // 7. 多行注释（/* */）
            else if (c == '/' && i + 1 < len && sql.charAt(i + 1) == '*') {
                int start = i;
                i += 2;
                while (i + 1 < len && !(sql.charAt(i) == '*' && sql.charAt(i + 1) == '/')) i++;
                if (i + 1 < len) i += 2;
                rawTokens.add(new Token(TokenType.COMMENT, sql.substring(start, i)));
            }
            // 8. 左括号
            else if (c == '(') {
                rawTokens.add(new Token(TokenType.OPEN_PAREN, "("));
                i++;
            }
            // 9. 右括号
            else if (c == ')') {
                rawTokens.add(new Token(TokenType.CLOSE_PAREN, ")"));
                i++;
            }
            // 10. 逗号
            else if (c == ',') {
                rawTokens.add(new Token(TokenType.COMMA, ","));
                i++;
            }
            // 11. 点号
            else if (c == '.') {
                rawTokens.add(new Token(TokenType.DOT, "."));
                i++;
            }
            // 12. 分号
            else if (c == ';') {
                rawTokens.add(new Token(TokenType.SEMICOLON, ";"));
                i++;
            }
            // 13. 操作符（支持单字符和双字符操作符）
            else if (isOperatorChar(c)) {
                int start = i;
                if (i + 1 < len && isTwoCharOperator(sql.substring(i, Math.min(i + 2, len)))) {
                    i += 2;
                } else {
                    i++;
                }
                rawTokens.add(new Token(TokenType.OPERATOR, sql.substring(start, i)));
            }
            // 14. 数字（支持小数点和科学计数法）
            else if (Character.isDigit(c) || (c == '.' && i + 1 < len && Character.isDigit(sql.charAt(i + 1)))) {
                int start = i;
                while (i < len && (Character.isDigit(sql.charAt(i)) || sql.charAt(i) == '.')) i++;
                if (i < len && (sql.charAt(i) == 'e' || sql.charAt(i) == 'E')) {
                    i++;
                    if (i < len && (sql.charAt(i) == '+' || sql.charAt(i) == '-')) i++;
                    while (i < len && Character.isDigit(sql.charAt(i))) i++;
                }
                rawTokens.add(new Token(TokenType.NUMBER, sql.substring(start, i)));
            }
            // 15. 标识符或关键字（字母或下划线开头）
            else if (Character.isLetter(c) || c == '_') {
                int start = i;
                while (i < len && (Character.isLetterOrDigit(sql.charAt(i)) || sql.charAt(i) == '_')) i++;
                String word = sql.substring(start, i);
                String upper = word.toUpperCase();
                // 判断是关键字还是标识符
                if (KEYWORD_SET.contains(upper)) {
                    rawTokens.add(new Token(TokenType.KEYWORD, uppercase ? upper : word));
                } else {
                    rawTokens.add(new Token(TokenType.IDENTIFIER, word));
                }
            }
            // 16. 其他无法识别的字符
            else {
                rawTokens.add(new Token(TokenType.OTHER, String.valueOf(c)));
                i++;
            }
        }

        log.debug("原始Token化完成，共 {} 个Token，开始合并多词关键字", rawTokens.size());
        return mergeMultiKeywords(rawTokens);
    }

    /**
     * 判断字符是否为SQL操作符的起始字符
     *
     * @param c 待检查的字符
     * @return true表示是操作符字符
     */
    private boolean isOperatorChar(char c) {
        return c == '=' || c == '<' || c == '>' || c == '!' || c == '+' || c == '-' || c == '*' || c == '/' || c == '%' || c == '|' || c == '&' || c == '^' || c == '~';
    }

    /**
     * 判断字符串是否为双字符操作符
     *
     * @param s 待检查的双字符字符串
     * @return true表示是双字符操作符
     */
    private boolean isTwoCharOperator(String s) {
        return s.equals("<>") || s.equals("!=") || s.equals("<=") || s.equals(">=") || s.equals("||") || s.equals("&&") || s.equals("<<") || s.equals(">>");
    }

    /**
     * 合并多词关键字
     * <p>
     * 将分词阶段识别的独立关键字（如"GROUP"、"BY"）合并为一个语义单元（"GROUP BY"）。
     * 这是必要的步骤，因为词法分析器只能识别单个词，而"GROUP BY"等在SQL中
     * 是一个不可分割的语义单元。
     * </p>
     * <p>
     * 匹配策略：
     * <ul>
     *   <li>优先匹配最长的模式（例如先匹配"LEFT OUTER JOIN"再匹配"LEFT JOIN"）</li>
     *   <li>匹配时跳过中间的空白Token</li>
     *   <li>合并后标记为MULTI_KEYWORD类型</li>
     * </ul>
     * </p>
     *
     * @param raw 原始Token列表
     * @return 合并多词关键字后的Token列表
     */
    private List<Token> mergeMultiKeywords(List<Token> raw) {
        List<Token> merged = new ArrayList<>();
        int i = 0;
        int mergedCount = 0;

        while (i < raw.size()) {
            // 跳过空白Token
            if (raw.get(i).type == TokenType.WHITESPACE) {
                i++;
                continue;
            }
            // 尝试匹配多词关键字模式
            if (raw.get(i).type == TokenType.KEYWORD) {
                String bestMatch = null;
                int bestLen = 0;
                for (String[] pattern : MULTI_KEYWORD_PATTERNS) {
                    if (pattern[0].equalsIgnoreCase(raw.get(i).value) && pattern.length > 1) {
                        boolean match = true;
                        int ri = i;
                        for (int pi = 1; pi < pattern.length; pi++) {
                            ri++;
                            // 跳过中间的空白
                            while (ri < raw.size() && raw.get(ri).type == TokenType.WHITESPACE) ri++;
                            if (ri >= raw.size() || raw.get(ri).type != TokenType.KEYWORD || !raw.get(ri).value.equalsIgnoreCase(pattern[pi])) {
                                match = false;
                                break;
                            }
                        }
                        if (match && pattern.length > bestLen) {
                            bestLen = pattern.length;
                            bestMatch = String.join(" ", pattern).toUpperCase();
                        }
                    }
                }
                if (bestMatch != null) {
                    merged.add(new Token(TokenType.MULTI_KEYWORD, bestMatch));
                    // 跳过已合并的Token
                    int skip = bestLen - 1;
                    i++;
                    while (skip > 0 && i < raw.size()) {
                        if (raw.get(i).type != TokenType.WHITESPACE) skip--;
                        i++;
                    }
                    mergedCount++;
                    continue;
                }
            }
            merged.add(raw.get(i));
            i++;
        }

        log.debug("多词关键字合并完成，合并了 {} 个多词关键字，最终 {} 个Token", mergedCount, merged.size());
        return merged;
    }

    /**
     * Token格式化器 - 核心格式化逻辑
     * <p>
     * 采用状态机模式遍历Token流，根据Token类型和上下文状态
     * 决定如何输出（换行、缩进、空格控制等），实现专业的SQL格式化效果。
     * </p>
     * <p>
     * 主要格式化规则：
     * <ul>
     *   <li>顶层子句（SELECT, FROM, WHERE等）独占一行并对齐</li>
     *   <li>JOIN子句独占一行并与FROM对齐</li>
     *   <li>AND/OR条件换行并多缩进一级</li>
     *   <li>逗号后换行，下一个元素多缩进一级</li>
     *   <li>子查询自动增加缩进层级</li>
     *   <li>窗口函数OVER子句内部的PARTITION BY、ORDER BY独立缩进</li>
     *   <li>CASE表达式的WHEN/THEN/ELSE分层缩进</li>
     *   <li>函数名后紧跟括号，不添加空格（如COUNT(*)）</li>
     *   <li>操作符两侧各留一个空格</li>
     * </ul>
     * </p>
     *
     * @param tokens     已分词的Token列表（包含合并后的多词关键字）
     * @param indentSize 缩进空格数
     * @return 格式化后的SQL字符串
     */
    private String formatTokens(List<Token> tokens, int indentSize) {
        log.debug("开始Token格式化，共 {} 个Token，缩进大小: {}", tokens.size(), indentSize);

        StringBuilder sb = new StringBuilder();
        String indent = " ".repeat(indentSize);

        // 格式化状态变量
        int baseIndent = 0;                 // 当前基础缩进层级（进入子查询时递增）
        int parenDepth = 0;                 // 括号嵌套深度
        boolean afterComma = false;         // 前一个Token是否是逗号
        boolean insideOver = false;         // 是否在OVER窗口函数内部
        boolean insideCase = false;         // 是否在CASE表达式内部
        boolean insideCreate = false;       // 是否在CREATE语句内部
        boolean insideInsertValues = false; // 是否在INSERT的VALUES内部
        boolean needSpaceBefore = false;    // 下一个Token前是否需要空格

        for (int idx = 0; idx < tokens.size(); idx++) {
            Token t = tokens.get(idx);
            Token next = peek(tokens, idx, 1);
            Token prev = peekPrev(tokens, idx);

            // 空白Token直接跳过，格式由我们完全控制
            if (t.type == TokenType.WHITESPACE) {
                continue;
            }

            // 注释Token：保留原始内容，注释后换行并保持当前缩进
            if (t.type == TokenType.COMMENT) {
                sb.append(t.value);
                if (idx + 1 < tokens.size()) {
                    sb.append("\n").append(indent.repeat(baseIndent));
                    needSpaceBefore = false;
                }
                continue;
            }

            String val = t.value;
            String upperVal = val.toUpperCase();

            // ========== 关键字和多词关键字处理 ==========
            if (t.type == TokenType.KEYWORD || t.type == TokenType.MULTI_KEYWORD) {

                // 1. 顶层子句（SELECT, FROM, WHERE等）：换行 + 基础缩进
                if (isTopLevelClause(upperVal) && !insideOver) {
                    if (sb.length() > 0 && !endsWithNewline(sb)) {
                        sb.append("\n");
                    }
                    sb.append(indent.repeat(baseIndent)).append(val);
                    needSpaceBefore = true;
                    afterComma = false;
                    // 标记进入DDL语句或INSERT语句的特殊状态
                    if (upperVal.equals("CREATE") || upperVal.equals("ALTER") || upperVal.equals("DROP")) {
                        insideCreate = true;
                    }
                    if (upperVal.equals("VALUES")) {
                        insideInsertValues = true;
                    }
                    continue;
                }

                // 2. JOIN子句：换行 + 基础缩进（与FROM对齐）
                if (isJoinClause(upperVal) && !insideOver) {
                    if (sb.length() > 0 && !endsWithNewline(sb)) {
                        sb.append("\n");
                    }
                    sb.append(indent.repeat(baseIndent)).append(val);
                    needSpaceBefore = true;
                    afterComma = false;
                    continue;
                }

                // 3. AND/OR条件：换行 + 多缩进一级（比WHERE多缩进一级便于阅读）
                if ((upperVal.equals("AND") || upperVal.equals("OR")) && !insideOver) {
                    sb.append("\n").append(indent.repeat(baseIndent + 1)).append(val);
                    needSpaceBefore = true;
                    afterComma = false;
                    continue;
                }

                // 4. OVER关键字：标记进入窗口函数，不换行
                if (upperVal.equals("OVER")) {
                    insideOver = true;
                    sb.append(" ").append(val);
                    needSpaceBefore = true;
                    continue;
                }

                // 5. CASE关键字：标记进入CASE表达式
                if (upperVal.equals("CASE")) {
                    insideCase = true;
                    sb.append(val);
                    needSpaceBefore = true;
                    continue;
                }

                // 6. WHEN子句（CASE内部）：换行 + 缩进一级
                if (upperVal.equals("WHEN") && insideCase) {
                    sb.append("\n").append(indent.repeat(baseIndent + 1)).append(val);
                    needSpaceBefore = true;
                    continue;
                }

                // 7. THEN子句（CASE内部）：换行 + 缩进二级（比WHEN再缩进一级）
                if (upperVal.equals("THEN") && insideCase) {
                    sb.append("\n").append(indent.repeat(baseIndent + 2)).append(val);
                    needSpaceBefore = true;
                    continue;
                }

                // 8. ELSE子句（CASE内部）：与WHEN同级缩进
                if (upperVal.equals("ELSE") && insideCase) {
                    sb.append("\n").append(indent.repeat(baseIndent + 1)).append(val);
                    needSpaceBefore = true;
                    continue;
                }

                // 9. END关键字：CASE结束，回到基础缩进
                if (upperVal.equals("END")) {
                    if (insideCase) insideCase = false;
                    sb.append("\n").append(indent.repeat(baseIndent)).append(val);
                    needSpaceBefore = true;
                    continue;
                }

                // 10. PARTITION BY（OVER内部）：换行 + 缩进二级
                if (upperVal.equals("PARTITION") && insideOver) {
                    sb.append("\n").append(indent.repeat(baseIndent + 2)).append(val);
                    needSpaceBefore = true;
                    continue;
                }

                // 11. ORDER BY / ROWS BETWEEN等（OVER内部）：换行 + 缩进二级
                if (isWindowClauseKeyword(upperVal) && insideOver) {
                    sb.append("\n").append(indent.repeat(baseIndent + 2)).append(val);
                    needSpaceBefore = true;
                    continue;
                }

                // 12. 其他关键字：普通输出，根据前后文决定空格
                if (needSpaceBefore) {
                    sb.append(" ");
                }
                sb.append(val);
                // 函数关键字后面紧跟左括号时不加空格（如COUNT(*), ROW_NUMBER()）
                needSpaceBefore = !(next != null && next.type == TokenType.OPEN_PAREN && isFunctionKeyword(upperVal));
                if (needSpaceBefore) {
                    sb.append(" ");
                } else {
                    needSpaceBefore = false;
                }
                continue;
            }

            // ========== 左括号处理 ==========
            if (t.type == TokenType.OPEN_PAREN) {
                if (insideOver) {
                    // OVER()内部的括号：换行并缩进二级
                    sb.append("(").append("\n").append(indent.repeat(baseIndent + 2));
                    needSpaceBefore = false;
                } else if (isSubqueryStart(tokens, idx)) {
                    // 子查询开始：换行并缩进一级，同时增加缩进层级
                    sb.append("(").append("\n").append(indent.repeat(baseIndent + 1));
                    baseIndent++;
                    parenDepth++;
                    needSpaceBefore = false;
                } else {
                    // 普通括号（函数参数、分组括号等）：直接输出，不换行
                    sb.append("(");
                    parenDepth++;
                    needSpaceBefore = false;
                }
                afterComma = false;
                continue;
            }

            // ========== 右括号处理 ==========
            if (t.type == TokenType.CLOSE_PAREN) {
                parenDepth--;
                if (insideOver) {
                    // OVER()结束：回到基础缩进，标记退出OVER状态
                    insideOver = false;
                    sb.append("\n").append(indent.repeat(baseIndent)).append(")");
                } else if (isSubqueryClose(tokens, idx) && baseIndent > 0) {
                    // 子查询结束：减少缩进层级，换行并对齐到外层缩进
                    baseIndent--;
                    sb.append("\n").append(indent.repeat(baseIndent)).append(")");
                } else {
                    // 普通右括号：直接输出
                    sb.append(")");
                }
                needSpaceBefore = true;
                afterComma = false;
                continue;
            }

            // ========== 逗号处理 ==========
            if (t.type == TokenType.COMMA) {
                sb.append(",");
                // 逗号后换行，下一行多缩进一级（列名列表、值列表等）
                sb.append("\n").append(indent.repeat(baseIndent + 1));
                needSpaceBefore = false;
                afterComma = true;
                continue;
            }

            // ========== 分号处理 ==========
            if (t.type == TokenType.SEMICOLON) {
                sb.append(";").append("\n");
                needSpaceBefore = false;
                afterComma = false;
                continue;
            }

            // ========== 点号处理（表名.列名） ==========
            if (t.type == TokenType.DOT) {
                sb.append(".");
                needSpaceBefore = false;
                continue;
            }

            // ========== 操作符处理 ==========
            if (t.type == TokenType.OPERATOR) {
                // 操作符两侧各留一个空格
                // 注意：SELECT * 的 * 也要正确处理（虽然和乘号*是同一个字符）
                if (val.equals("*") && isSelectStarContext(prev)) {
                    sb.append(" ").append(val).append(" ");
                } else {
                    sb.append(" ").append(val).append(" ");
                }
                needSpaceBefore = false;
                afterComma = false;
                continue;
            }

            // ========== 字符串处理 ==========
            if (t.type == TokenType.STRING) {
                sb.append(val);
                needSpaceBefore = shouldSpaceAfter(next);
                afterComma = false;
                continue;
            }

            // ========== 数字处理 ==========
            if (t.type == TokenType.NUMBER) {
                sb.append(val);
                needSpaceBefore = shouldSpaceAfter(next);
                afterComma = false;
                continue;
            }

            // ========== 标识符处理（表名、列名、别名） ==========
            if (t.type == TokenType.IDENTIFIER) {
                sb.append(val);
                // 标识符后面紧跟括号通常是函数调用（用户自定义函数）
                if (next != null && next.type == TokenType.OPEN_PAREN) {
                    needSpaceBefore = false;
                } else {
                    needSpaceBefore = shouldSpaceAfter(next);
                }
                afterComma = false;
                continue;
            }

            // ========== 其他无法识别的Token（兜底） ==========
            sb.append(val);
            needSpaceBefore = false;
            afterComma = false;
        }

        // 最终清理：移除多余空格、空行
        String result = sb.toString()
                .replaceAll("  +", " ")      // 移除连续空格
                .replaceAll(" \\n", "\n")     // 移除行尾空格
                .replaceAll("\\n{3,}", "\n\n") // 限制连续空行最多2个
                .trim();

        log.debug("Token格式化完成，输出长度: {}字符", result.length());
        return result;
    }

    /**
     * 判断关键字是否为SQL函数名
     * <p>
     * 用于控制函数名和括号之间是否需要空格：
     * 函数名后面紧跟括号（如COUNT(*)），不添加空格。
     * </p>
     *
     * @param upper 大写的关键字
     * @return true表示是函数关键字
     */
    private boolean isFunctionKeyword(String upper) {
        return upper.equals("COUNT") || upper.equals("SUM") || upper.equals("AVG")
                || upper.equals("MIN") || upper.equals("MAX") || upper.equals("ROUND")
                || upper.equals("CONCAT") || upper.equals("SUBSTRING") || upper.equals("UPPER")
                || upper.equals("LOWER") || upper.equals("TRIM") || upper.equals("LENGTH")
                || upper.equals("REPLACE") || upper.equals("COALESCE") || upper.equals("NULLIF")
                || upper.equals("CAST") || upper.equals("CONVERT") || upper.equals("ROW_NUMBER")
                || upper.equals("RANK") || upper.equals("DENSE_RANK") || upper.equals("LAG")
                || upper.equals("LEAD") || upper.equals("IF") || upper.equals("EXISTS");
    }

    /**
     * 判断是否为SELECT * 上下文（区分乘号*和SELECT *）
     * <p>
     * 由于*号既是乘号又是通配符，需要根据前一个Token是否为SELECT来判断。
     * </p>
     *
     * @param prev 前一个非空白Token
     * @return true表示当前*是SELECT * 中的通配符
     */
    private boolean isSelectStarContext(Token prev) {
        if (prev == null) return false;
        return prev.type == TokenType.KEYWORD && prev.value.equalsIgnoreCase("SELECT");
    }

    /**
     * 判断当前Token后面是否需要空格
     * <p>
     * 某些语法结构后面不需要空格，例如：
     * <ul>
     *   <li>逗号后紧跟换行，不需要空格</li>
     *   <li>右括号后紧跟分号，不需要空格</li>
     *   <li>点号前后不需要空格（table.column）</li>
     *   <li>操作符前后会单独处理空格</li>
     * </ul>
     * </p>
     *
     * @param next 下一个非空白Token
     * @return true表示需要空格
     */
    private boolean shouldSpaceAfter(Token next) {
        if (next == null) return false;
        return next.type != TokenType.COMMA
                && next.type != TokenType.CLOSE_PAREN
                && next.type != TokenType.SEMICOLON
                && next.type != TokenType.OPERATOR
                && next.type != TokenType.DOT;
    }

    /**
     * 判断是否为SQL顶层子句（需要换行并左对齐的关键字）
     * <p>
     * 顶层子句包括SELECT, FROM, WHERE, GROUP BY, ORDER BY, HAVING, LIMIT等。
     * 这些子句是SQL的主要结构，每个独占一行便于阅读。
     * </p>
     *
     * @param upper 大写的关键字
     * @return true表示是顶层子句
     */
    private boolean isTopLevelClause(String upper) {
        return upper.equals("SELECT") || upper.equals("FROM") || upper.equals("WHERE")
                || upper.equals("GROUP BY") || upper.equals("ORDER BY") || upper.equals("HAVING")
                || upper.equals("LIMIT") || upper.equals("OFFSET")
                || upper.equals("INSERT") || upper.equals("INSERT INTO")
                || upper.equals("UPDATE") || upper.equals("DELETE")
                || upper.equals("SET") || upper.equals("VALUES")
                || upper.equals("CREATE") || upper.equals("ALTER") || upper.equals("DROP")
                || upper.equals("WITH") || upper.equals("UNION") || upper.equals("UNION ALL")
                || upper.equals("INTERSECT") || upper.equals("EXCEPT")
                || upper.equals("FOR UPDATE") || upper.equals("FOR SHARE")
                || upper.equals("RETURNING") || upper.equals("DISTINCT");
    }

    /**
     * 判断是否为JOIN子句（需要换行并与FROM对齐）
     * <p>
     * JOIN子句是SQL查询中表连接的核心部分，
     * 每个JOIN独占一行便于查看表之间的关联关系。
     * </p>
     *
     * @param upper 大写的关键字
     * @return true表示是JOIN子句
     */
    private boolean isJoinClause(String upper) {
        return upper.equals("JOIN") || upper.equals("LEFT JOIN") || upper.equals("RIGHT JOIN")
                || upper.equals("INNER JOIN") || upper.equals("FULL JOIN")
                || upper.equals("OUTER JOIN") || upper.equals("CROSS JOIN")
                || upper.equals("NATURAL JOIN") || upper.equals("LEFT OUTER JOIN")
                || upper.equals("RIGHT OUTER JOIN") || upper.equals("FULL OUTER JOIN");
    }

    /**
     * 判断是否为窗口子句关键字（OVER内部的ORDER BY, ROWS等）
     * <p>
     * 窗口函数OVER子句内部的结构也需要格式化缩进，
     * 这些关键字需要独立一行并增加缩进。
     * </p>
     *
     * @param upper 大写的关键字
     * @return true表示是窗口子句关键字
     */
    private boolean isWindowClauseKeyword(String upper) {
        return upper.equals("ORDER BY") || upper.equals("ROWS BETWEEN") || upper.equals("RANGE BETWEEN")
                || upper.equals("ROWS") || upper.equals("RANGE");
    }

    /**
     * 判断左括号后是否跟随子查询
     * <p>
     * 通过查看左括号后的第一个非空白Token是否为SELECT来判断。
     * 子查询需要增加缩进层级，而普通函数参数不需要。
     * </p>
     *
     * @param tokens   Token列表
     * @param parenIdx 左括号在Token列表中的索引
     * @return true表示括号后是子查询
     */
    private boolean isSubqueryStart(List<Token> tokens, int parenIdx) {
        for (int i = parenIdx + 1; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            if (t.type == TokenType.WHITESPACE) continue;
            return t.type == TokenType.KEYWORD && t.value.equalsIgnoreCase("SELECT");
        }
        return false;
    }

    /**
     * 判断右括号是否为子查询的结束
     * <p>
     * 通过向前查找对应的SELECT关键字来判断。
     * 子查询结束时需要减少缩进层级。
     * </p>
     *
     * @param tokens   Token列表
     * @param closeIdx 右括号在Token列表中的索引
     * @return true表示是子查询结束的括号
     */
    private boolean isSubqueryClose(List<Token> tokens, int closeIdx) {
        int openCount = 0;
        for (int i = 0; i < closeIdx; i++) {
            if (tokens.get(i).type == TokenType.OPEN_PAREN) openCount++;
            if (tokens.get(i).type == TokenType.CLOSE_PAREN) openCount--;
        }
        for (int i = closeIdx - 1; i >= 0; i--) {
            if (tokens.get(i).type == TokenType.WHITESPACE) continue;
            if (tokens.get(i).type == TokenType.KEYWORD && tokens.get(i).value.equalsIgnoreCase("SELECT")) {
                return true;
            }
            return false;
        }
        return false;
    }

    /**
     * 向后查看第N个非空白Token
     * <p>
     * 用于在格式化时预判下一个Token的类型，决定是否需要空格、换行等。
     * </p>
     *
     * @param tokens     Token列表
     * @param currentIdx 当前Token索引
     * @param offset     偏移量（1表示下一个，2表示下下个）
     * @return 目标Token，超出范围返回null
     */
    private Token peek(List<Token> tokens, int currentIdx, int offset) {
        int count = 0;
        for (int i = currentIdx + 1; i < tokens.size() && count < offset; i++) {
            if (tokens.get(i).type != TokenType.WHITESPACE) {
                count++;
                if (count == offset) return tokens.get(i);
            }
        }
        return null;
    }

    /**
     * 向前查看上一个非空白Token
     *
     * @param tokens     Token列表
     * @param currentIdx 当前Token索引
     * @return 前一个非空白Token，超出范围返回null
     */
    private Token peekPrev(List<Token> tokens, int currentIdx) {
        for (int i = currentIdx - 1; i >= 0; i--) {
            if (tokens.get(i).type != TokenType.WHITESPACE) return tokens.get(i);
        }
        return null;
    }

    /**
     * 判断StringBuilder最后一个字符是否为换行符
     *
     * @param sb 待检查的StringBuilder
     * @return true表示最后一个字符是换行
     */
    private boolean endsWithNewline(StringBuilder sb) {
        return sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n';
    }

    /**
     * 压缩SQL语句
     * <p>
     * 将所有空白字符（空格、换行、制表符）替换为单个空格，
     * 减小SQL体积便于网络传输或日志记录。
     * </p>
     *
     * @param sql 原始SQL
     * @return 压缩后的SQL
     */
    private String compactSql(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    /**
     * 检测SQL类型（SELECT/INSERT/UPDATE/DELETE等）
     * <p>
     * 通过匹配SQL开头的第一个关键字来判断SQL的操作类型。
     * 用于结果展示和统计分析。
     * </p>
     *
     * @param sql 原始SQL
     * @return SQL类型字符串，未知返回"UNKNOWN"
     */
    private String detectSqlType(String sql) {
        Matcher matcher = SQL_TYPE_PATTERN.matcher(sql);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase();
        }
        return "UNKNOWN";
    }

    /**
     * 生成SQL统计信息
     * <p>
     * 统计SQL中的表数量、JOIN数量、条件数量等信息，
     * 帮助用户了解SQL的复杂度。
     * </p>
     *
     * @param sql 原始SQL
     * @return 统计信息字符串
     */
    private String generateStatistics(String sql) {
        log.debug("开始生成SQL统计信息");
        StringBuilder stats = new StringBuilder();

        int tableCount = 0;
        // 统计FROM子句中的表数量
        Pattern fromPattern = Pattern.compile("(?i)\\bFROM\\s+([\\w,\\s]+?)(?:\\s+(?:WHERE|GROUP|ORDER|LIMIT|JOIN|UNION|$))", Pattern.CASE_INSENSITIVE);
        Matcher fromMatcher = fromPattern.matcher(sql);
        if (fromMatcher.find()) {
            String tables = fromMatcher.group(1);
            tableCount = tables.split(",").length;
        }

        // 统计JOIN数量
        int joinCount = 0;
        Pattern joinPattern = Pattern.compile("(?i)\\b(?:LEFT|RIGHT|INNER|FULL|CROSS)?\\s*JOIN\\b", Pattern.CASE_INSENSITIVE);
        Matcher joinMatcher = joinPattern.matcher(sql);
        while (joinMatcher.find()) {
            joinCount++;
        }
        tableCount += joinCount;

        // 统计WHERE条件数量
        int conditionCount = 0;
        Pattern wherePattern = Pattern.compile("(?i)\\bWHERE\\s+(.+?)(?:\\s+(?:GROUP|ORDER|LIMIT|HAVING|$))", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher whereMatcher = wherePattern.matcher(sql);
        if (whereMatcher.find()) {
            String conditions = whereMatcher.group(1);
            Pattern andOrPattern = Pattern.compile("(?i)\\b(AND|OR)\\b");
            Matcher andOrMatcher = andOrPattern.matcher(conditions);
            while (andOrMatcher.find()) {
                conditionCount++;
            }
            conditionCount++;
        }

        stats.append("表数量: ").append(tableCount);
        stats.append("，JOIN数量: ").append(joinCount);
        stats.append("，条件数量: ").append(conditionCount);
        stats.append("，SQL长度: ").append(sql.length()).append("字符");

        log.debug("SQL统计信息生成完成: {}", stats);
        return stats.toString();
    }

    /**
     * 生成SQL优化建议
     * <p>
     * 基于常见的SQL性能最佳实践，分析SQL并生成可操作的优化建议，
     * 帮助用户发现潜在的性能问题。
     * </p>
     *
     * @param sql    原始SQL
     * @param dbType 数据库类型
     * @return 优化建议列表
     */
    private List<String> generateSuggestions(String sql, String dbType) {
        log.debug("开始生成SQL优化建议");
        List<String> suggestions = new ArrayList<>();

        // 检查是否使用了SELECT *
        if (sql.toUpperCase().contains("SELECT *")) {
            suggestions.add("建议避免使用 SELECT *，只查询需要的列以提高性能");
        }

        // 检查LIKE条件是否以%开头（无法使用索引）
        if (sql.toUpperCase().contains("LIKE") && sql.toUpperCase().contains("%")) {
            suggestions.add("注意：以 % 开头的 LIKE 条件无法使用索引，考虑优化查询方式");
        }

        // 检查是否存在子查询
        Pattern subqueryPattern = Pattern.compile("\\(\\s*SELECT", Pattern.CASE_INSENSITIVE);
        Matcher subqueryMatcher = subqueryPattern.matcher(sql);
        if (subqueryMatcher.find()) {
            suggestions.add("检测到子查询，考虑使用 JOIN 替代以提高可读性和性能");
        }

        // 检查SELECT查询是否缺少LIMIT
        if (!sql.toUpperCase().contains("LIMIT") && sql.toUpperCase().startsWith("SELECT")) {
            suggestions.add("建议添加 LIMIT 限制返回行数，特别是在数据量大的表上");
        }

        // 检查SELECT查询是否缺少WHERE条件
        if (!sql.toUpperCase().contains("WHERE") && sql.toUpperCase().startsWith("SELECT")) {
            suggestions.add("查询缺少 WHERE 条件，可能导致全表扫描，请确认是否需要添加过滤条件");
        }

        // 如果没有发现问题，给出通用建议
        if (suggestions.isEmpty()) {
            suggestions.add("SQL格式良好，建议根据实际执行计划进一步优化");
        }

        log.debug("SQL优化建议生成完成，共 {} 条建议", suggestions.size());
        return suggestions;
    }

    /**
     * 使用AI大模型进行SQL格式化
     * <p>
     * 当本地格式化失败或用户明确要求使用AI时调用此方法。
     * 调用项目现有的GenerateService，将SQL和格式化要求发送给LLM，
     * 解析返回的JSON结果。
     * </p>
     *
     * @param request 格式化请求参数
     * @return 格式化结果
     */
    private SqlFormatResultDTO formatWithAI(SqlFormatRequestDTO request) {
        String content = request.getContent().trim();
        int indentSize = request.getIndentSize() != null ? request.getIndentSize() : 4;
        boolean uppercase = request.getUppercase() != null && request.getUppercase();
        String dbType = request.getDbType() != null ? request.getDbType().toUpperCase() : "MYSQL";

        log.info("开始AI格式化SQL，数据库类型: {}, 原始长度: {}字符", dbType, content.length());

        try {
            // 构建用户提示词，包含SQL和格式化要求
            String userPrompt = String.format("""
                请格式化以下SQL语句：
                    
                原始SQL：
                %s
                    
                格式化要求：
                - 数据库类型：%s
                - 缩进空格数：%d
                - 关键字大写：%s
                    
                请返回格式化后的SQL。
                """, content, dbType, indentSize, uppercase ? "是" : "否");

            log.debug("调用GenerateService进行AI格式化");
            var result = generateService.generateWithPrompt(SQL_FORMAT_SYSTEM_PROMPT, userPrompt, "sql-format");
            String aiResponse = result.getContent();

            if (aiResponse == null || aiResponse.trim().isEmpty()) {
                log.warn("AI格式化返回空结果");
                throw new BusinessException("AI格式化失败，未返回有效内容");
            }

            // 从AI响应中提取JSON
            String jsonContent = extractJson(aiResponse);
            JsonNode root = objectMapper.readTree(jsonContent);

            // 解析返回结果
            String formattedSql = root.path("formattedSql").asText();
            String sqlType = root.path("sqlType").asText(detectSqlType(content));
            String statistics = root.path("statistics").asText(generateStatistics(content));

            List<String> suggestions = new ArrayList<>();
            JsonNode suggestionsNode = root.path("suggestions");
            if (suggestionsNode.isArray()) {
                for (JsonNode s : suggestionsNode) {
                    suggestions.add(s.asText());
                }
            }

            String compactSql = compactSql(formattedSql);

            log.info("AI格式化SQL成功，使用模型: {}, 输出长度: {}字符", result.getModel(), formattedSql.length());

            return SqlFormatResultDTO.builder()
                    .success(true)
                    .formattedSql(formattedSql)
                    .compactSql(compactSql)
                    .indentSize(indentSize)
                    .uppercase(uppercase)
                    .dbType(dbType)
                    .sqlType(sqlType)
                    .statistics(statistics)
                    .suggestions(suggestions)
                    .errors(Collections.emptyList())
                    .build();

        } catch (Exception e) {
            log.error("AI格式化SQL失败: {}", e.getMessage(), e);
            List<SqlErrorDetailDTO> errors = parseSqlError(e, content);
            return SqlFormatResultDTO.builder()
                    .success(false)
                    .errors(errors)
                    .indentSize(indentSize)
                    .uppercase(uppercase)
                    .dbType(dbType)
                    .build();
        }
    }

    /**
     * 压缩SQL接口实现
     * <p>
     * 将SQL中的所有空白字符替换为单个空格，减小SQL体积。
     * 同时计算压缩率并返回统计信息。
     * </p>
     *
     * @param request 压缩请求参数
     * @return 压缩结果
     */
    @Override
    public SqlFormatResultDTO compact(SqlFormatRequestDTO request) {
        String content = request.getContent().trim();
        String dbType = request.getDbType() != null ? request.getDbType().toUpperCase() : "MYSQL";

        log.debug("开始SQL压缩，原始长度: {}字符", content.length());

        try {
            String compactSql = compactSql(content);
            String sqlType = detectSqlType(content);
            int compressionRate = (int) Math.round((1 - (double) compactSql.length() / content.length()) * 100);

            log.info("SQL压缩成功，压缩前: {}字符，压缩后: {}字符，压缩率: {}%",
                    content.length(), compactSql.length(), compressionRate);

            return SqlFormatResultDTO.builder()
                    .success(true)
                    .compactSql(compactSql)
                    .formattedSql(null)
                    .dbType(dbType)
                    .sqlType(sqlType)
                    .statistics("压缩率: " + compressionRate + "%")
                    .errors(Collections.emptyList())
                    .build();

        } catch (Exception e) {
            log.warn("SQL压缩失败: {}", e.getMessage(), e);
            return SqlFormatResultDTO.builder()
                    .success(false)
                    .errors(List.of(SqlErrorDetailDTO.builder()
                            .errorType("压缩失败")
                            .message(e.getMessage())
                            .suggestion("请检查SQL内容是否完整")
                            .build()))
                    .dbType(dbType)
                    .build();
        }
    }

    /**
     * SQL语法校验接口实现
     * <p>
     * 对SQL进行多维度语法检查，包括括号匹配、引号闭合、
     * 必要子句存在性、JOIN条件完整性等，返回详细的错误信息。
     * </p>
     *
     * @param request 校验请求参数
     * @return 校验结果（成功返回空错误列表，失败返回详细错误信息）
     */
    @Override
    public SqlFormatResultDTO validate(SqlFormatRequestDTO request) {
        String content = request.getContent().trim();
        String dbType = request.getDbType() != null ? request.getDbType().toUpperCase() : "MYSQL";

        log.debug("开始SQL语法校验，数据库类型: {}, SQL长度: {}字符", dbType, content.length());

        List<SqlErrorDetailDTO> errors = validateSqlSyntax(content, dbType);

        if (errors.isEmpty()) {
            log.info("SQL校验通过，数据库类型: {}", dbType);
            return SqlFormatResultDTO.builder()
                    .success(true)
                    .sqlType(detectSqlType(content))
                    .statistics(generateStatistics(content))
                    .suggestions(generateSuggestions(content, dbType))
                    .dbType(dbType)
                    .errors(Collections.emptyList())
                    .build();
        } else {
            log.warn("SQL校验失败，发现 {} 个问题", errors.size());
            return SqlFormatResultDTO.builder()
                    .success(false)
                    .errors(errors)
                    .dbType(dbType)
                    .build();
        }
    }

    /**
     * SQL语法校验核心逻辑
     * <p>
     * 执行以下检查：
     * <ol>
     *   <li>括号匹配检查：确保左右括号数量一致</li>
     *   <li>引号闭合检查：确保单引号数量为偶数</li>
     *   <li>必要子句检查：SELECT语句必须有FROM等</li>
     *   <li>JOIN条件检查：JOIN语句必须有ON或USING条件</li>
     * </ol>
     * </p>
     *
     * @param sql    待校验的SQL
     * @param dbType 数据库类型
     * @return 错误列表，空列表表示校验通过
     */
    private List<SqlErrorDetailDTO> validateSqlSyntax(String sql, String dbType) {
        log.debug("开始SQL语法校验分析");
        List<SqlErrorDetailDTO> errors = new ArrayList<>();

        // 1. 括号匹配检查
        int openParens = countUnescapedChar(sql, '(');
        int closeParens = countUnescapedChar(sql, ')');
        if (openParens != closeParens) {
            log.debug("括号不匹配：左括号 {} 个，右括号 {} 个", openParens, closeParens);
            errors.add(SqlErrorDetailDTO.builder()
                    .errorType("括号不匹配")
                    .message("检测到括号不匹配：左括号 " + openParens + " 个，右括号 " + closeParens + " 个")
                    .suggestion("请检查并补充缺失的括号或删除多余的括号")
                    .build());
        }

        // 2. 单引号闭合检查
        int singleQuotes = countUnescapedChar(sql, '\'');
        if (singleQuotes % 2 != 0) {
            log.debug("单引号数量为奇数: {}", singleQuotes);
            errors.add(SqlErrorDetailDTO.builder()
                    .errorType("引号不匹配")
                    .message("单引号数量为奇数，可能存在未闭合的字符串")
                    .suggestion("请检查字符串的单引号是否正确闭合")
                    .build());
        }

        // 3. 必要子句检查：SELECT语句必须包含FROM/INTO/SET/VALUES
        if (!sql.toUpperCase().matches("(?s).*\\b(FROM|INTO|SET|VALUES)\\b.*") && sql.toUpperCase().startsWith("SELECT")) {
            log.debug("SELECT语句缺少FROM子句");
            errors.add(SqlErrorDetailDTO.builder()
                    .errorType("缺少子句")
                    .message("SELECT语句缺少 FROM 子句")
                    .suggestion("请在SELECT后添加 FROM 子句指定查询的表")
                    .build());
        }

        // 4. JOIN条件检查（两步验证，避免有别名时误报）
        Pattern joinPattern = Pattern.compile(
                "(?i)\\b(?:LEFT|RIGHT|INNER|FULL|CROSS|NATURAL)?\\s*JOIN\\s+\\w+(?:\\s+(?:AS\\s+)?\\w+)?\\s+(?!ON\\b|USING\\b)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher joinMatcher = joinPattern.matcher(sql);
        if (joinMatcher.find()) {
            // 第一步匹配到了"JOIN 表名 [别名] 后面不是ON/USING"的情况
            // 需要第二步全局确认是否确实不存在ON条件（避免别名导致误判）
            Pattern onPattern = Pattern.compile(
                    "(?i)\\b(?:LEFT|RIGHT|INNER|FULL|CROSS|NATURAL)?\\s*JOIN\\s+\\w+(?:\\s+(?:AS\\s+)?\\w+)?\\s+ON\\b",
                    Pattern.CASE_INSENSITIVE
            );
            if (!onPattern.matcher(sql).find()) {
                log.debug("检测到JOIN缺少ON条件");
                errors.add(SqlErrorDetailDTO.builder()
                        .errorType("JOIN缺少条件")
                        .message("检测到JOIN语句可能缺少ON条件")
                        .suggestion("请为JOIN语句添加ON条件指定关联关系")
                        .build());
            }
        }

        log.debug("SQL语法校验分析完成，发现 {} 个问题", errors.size());
        return errors;
    }

    /**
     * 统计SQL中未转义的指定字符出现次数
     * <p>
     * 跳过反斜杠转义的字符，只统计实际有效的字符数量。
     * 用于括号匹配和引号闭合检查。
     * </p>
     *
     * @param sql SQL字符串
     * @param c   待统计的字符
     * @return 未转义字符的出现次数
     */
    private int countUnescapedChar(String sql, char c) {
        int count = 0;
        boolean escape = false;
        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (ch == '\\') {
                escape = true;
                continue;
            }
            if (ch == c) {
                count++;
            }
        }
        return count;
    }

    /**
     * AI修复SQL接口实现
     * <p>
     * 修复流程：
     * <ol>
     *   <li>优先尝试本地简单修复（括号补全、引号补全、拼写纠正）</li>
     *   <li>本地修复失败则调用AI大模型进行智能修复</li>
     *   <li>AI修复后再次校验，确保修复结果正确</li>
     *   <li>AI降级时回退到增强版本地修复</li>
     * </ol>
     * </p>
     *
     * @param request 修复请求参数
     * @return 修复结果
     */
    @Override
    public SqlFormatResultDTO fixWithAI(SqlFormatRequestDTO request) {
        String content = request.getContent().trim();
        String dbType = request.getDbType() != null ? request.getDbType().toUpperCase() : "MYSQL";
        String userIntent = request.getUserIntent();

        log.info("开始SQL修复，数据库类型: {}, 原始长度: {}字符", dbType, content.length());

        try {
            // 第一优先级：尝试本地简单修复
            String simpleFixed = trySimpleFix(content);
            if (simpleFixed != null && !simpleFixed.equals(content)) {
                List<SqlErrorDetailDTO> validationErrors = validateSqlSyntax(simpleFixed, dbType);
                if (validationErrors.isEmpty()) {
                    log.info("SQL本地简单修复成功");
                    return buildFixResult(simpleFixed, content, "本地简单修复成功：自动处理了常见格式问题", request);
                }
                log.debug("本地简单修复后仍有 {} 个问题，尝试AI修复", validationErrors.size());
            }

            // 第二优先级：调用AI大模型修复
            String userPrompt = String.format("""
                请修复以下SQL语句中的语法错误：
                    
                原始SQL：
                %s
                    
                数据库类型：%s
                %s
                    
                请分析问题并提供准确的修复方案，返回JSON格式结果。
                """, content, dbType, userIntent != null && !userIntent.isEmpty() ? "用户意图：" + userIntent : "");

            log.info("调用AI修复SQL，原始长度: {}字符", content.length());

            var result = generateService.generateWithPrompt(SQL_FIX_SYSTEM_PROMPT, userPrompt, "sql-fix");
            String aiResponse = result.getContent();

            if (aiResponse == null || aiResponse.trim().isEmpty()) {
                log.warn("AI修复返回空结果");
                throw new BusinessException("AI修复失败，未返回有效内容");
            }

            // 检测AI是否处于降级模式
            if ("fallback".equals(result.getModel()) || aiResponse.contains("降级模式")) {
                log.info("AI处于降级模式，使用本地修复策略");
                return tryEnhancedLocalFix(content, request);
            }

            // 解析AI返回的JSON结果
            String jsonContent = extractJson(aiResponse);
            JsonNode root = objectMapper.readTree(jsonContent);

            String fixedSql = root.path("fixedSql").asText();
            String fixDescription = root.path("fixDescription").asText();
            String sqlType = root.path("sqlType").asText(detectSqlType(content));
            boolean valid = root.path("valid").asBoolean(true);

            List<String> suggestions = new ArrayList<>();
            JsonNode suggestionsNode = root.path("suggestions");
            if (suggestionsNode.isArray()) {
                for (JsonNode s : suggestionsNode) {
                    suggestions.add(s.asText());
                }
            }

            // AI修复后再次校验，确保修复结果语法正确
            List<SqlErrorDetailDTO> validationErrors = validateSqlSyntax(fixedSql, dbType);
            if (!validationErrors.isEmpty()) {
                valid = false;
                log.warn("AI修复后的SQL仍存在 {} 个问题", validationErrors.size());
            }

            log.info("AI修复SQL成功，使用模型: {}, 修复描述: {}", result.getModel(), fixDescription);

            return SqlFormatResultDTO.builder()
                    .success(true)
                    .aiFixSuccess(valid)
                    .aiFixedSql(formatSqlLocally(fixedSql, 4, true, dbType))
                    .formattedSql(formatSqlLocally(fixedSql, 4, true, dbType))
                    .compactSql(compactSql(fixedSql))
                    .aiFixDescription(fixDescription)
                    .sqlType(sqlType)
                    .dbType(dbType)
                    .suggestions(suggestions)
                    .errors(validationErrors)
                    .build();

        } catch (Exception e) {
            log.error("AI修复SQL失败: {}", e.getMessage(), e);

            // AI修复失败时的降级策略
            if (fallbackEnabled) {
                try {
                    log.info("AI修复失败，尝试增强版本地修复");
                    return tryEnhancedLocalFix(content, request);
                } catch (Exception ex) {
                    log.warn("增强版本地修复也失败: {}", ex.getMessage());
                }
            }

            return SqlFormatResultDTO.builder()
                    .success(false)
                    .aiFixSuccess(false)
                    .aiFixDescription("AI修复失败：" + e.getMessage())
                    .errors(List.of(SqlErrorDetailDTO.builder()
                            .errorType("AI修复失败")
                            .message(e.getMessage())
                            .suggestion("请检查SQL内容是否有过多错误，或手动修复后重试")
                            .build()))
                    .build();
        }
    }

    /**
     * 本地简单修复策略
     * <p>
     * 处理以下常见错误：
     * <ul>
     *   <li>括号不匹配：在SQL末尾补充缺失的右括号</li>
     *   <li>单引号不闭合：在SQL末尾补充缺失的单引号</li>
     *   <li>关键字拼写错误：修正常见的拼写错误（SELEC→SELECT, FORM→FROM等）</li>
     * </ul>
     * </p>
     *
     * @param sql 原始SQL
     * @return 修复后的SQL，可能和原始SQL相同（无需修复时）
     */
    private String trySimpleFix(String sql) {
        log.debug("尝试本地简单SQL修复");
        String fixed = sql;

        // 补充缺失的右括号
        int open = countUnescapedChar(fixed, '(');
        int close = countUnescapedChar(fixed, ')');
        while (close < open) {
            fixed += ")";
            close++;
        }

        // 补充缺失的单引号
        int quotes = countUnescapedChar(fixed, '\'');
        if (quotes % 2 != 0) {
            fixed += "'";
        }

        // 修正常见的关键字拼写错误
        fixed = fixed.replaceAll("(?i)\\bSELEC\\b", "SELECT");
        fixed = fixed.replaceAll("(?i)\\bFORM\\b", "FROM");
        fixed = fixed.replaceAll("(?i)\\bWHER\\b", "WHERE");
        fixed = fixed.replaceAll("(?i)\\bWHEARE\\b", "WHERE");

        log.debug("本地简单修复完成，是否有变更: {}", !fixed.equals(sql));
        return fixed;
    }

    /**
     * 增强版本地修复策略
     * <p>
     * 在AI不可用或降级时作为备选方案，
     * 先执行简单修复，再校验结果，全部通过则返回修复结果。
     * </p>
     *
     * @param content 原始SQL内容
     * @param request 请求参数
     * @return 修复结果
     * @throws BusinessException 所有本地修复策略均失败时抛出
     */
    private SqlFormatResultDTO tryEnhancedLocalFix(String content, SqlFormatRequestDTO request) {
        String dbType = request.getDbType() != null ? request.getDbType().toUpperCase() : "MYSQL";

        log.info("尝试增强版本地SQL修复");

        String fixed = trySimpleFix(content);
        List<SqlErrorDetailDTO> errors = validateSqlSyntax(fixed, dbType);

        if (errors.isEmpty()) {
            log.info("增强版本地修复成功");
            return buildFixResult(fixed, content, "本地智能修复成功：自动处理了括号、引号和常见拼写错误", request);
        }

        log.warn("增强版本地修复失败，仍有 {} 个问题", errors.size());
        throw new BusinessException("所有本地修复策略均失败");
    }

    /**
     * 构建修复结果DTO
     * <p>
     * 将修复后的SQL进行格式化和压缩，构建统一的返回结果。
     * </p>
     *
     * @param fixedSql        修复后的SQL
     * @param originalSql     原始SQL
     * @param fixDescription  修复描述
     * @param request         原始请求参数
     * @return 格式化后的修复结果
     */
    private SqlFormatResultDTO buildFixResult(String fixedSql, String originalSql, String fixDescription,
                                              SqlFormatRequestDTO request) {
        String dbType = request.getDbType() != null ? request.getDbType().toUpperCase() : "MYSQL";
        int indentSize = request.getIndentSize() != null ? request.getIndentSize() : 4;

        String formattedSql = formatSqlLocally(fixedSql, indentSize, true, dbType);
        String compactSql = compactSql(fixedSql);

        return SqlFormatResultDTO.builder()
                .success(true)
                .aiFixSuccess(true)
                .aiFixedSql(formattedSql)
                .formattedSql(formattedSql)
                .compactSql(compactSql)
                .aiFixDescription(fixDescription)
                .sqlType(detectSqlType(fixedSql))
                .dbType(dbType)
                .indentSize(indentSize)
                .uppercase(true)
                .statistics(generateStatistics(fixedSql))
                .suggestions(generateSuggestions(fixedSql, dbType))
                .errors(Collections.emptyList())
                .build();
    }

    /**
     * AI性能优化接口实现
     * <p>
     * 调用AI大模型分析SQL的执行计划潜在问题，
     * 提供索引建议、查询改写建议和预估性能提升。
     * </p>
     *
     * @param request 优化请求参数
     * @return 优化结果（包含优化建议和索引建议）
     */
    @Override
    public SqlFormatResultDTO optimizeWithAI(SqlFormatRequestDTO request) {
        String content = request.getContent().trim();
        String dbType = request.getDbType() != null ? request.getDbType().toUpperCase() : "MYSQL";
        String userIntent = request.getUserIntent();

        log.info("开始AI性能优化，数据库类型: {}, SQL长度: {}字符", dbType, content.length());

        try {
            // 构建优化提示词
            String userPrompt = String.format("""
                请分析并优化以下SQL语句：
                    
                原始SQL：
                %s
                    
                数据库类型：%s
                %s
                    
                请提供详细的性能分析和优化建议，返回JSON格式结果。
                """, content, dbType, userIntent != null && !userIntent.isEmpty() ? "用户特殊要求：" + userIntent : "");

            log.debug("调用GenerateService进行AI性能优化");
            var result = generateService.generateWithPrompt(SQL_OPTIMIZE_SYSTEM_PROMPT, userPrompt, "sql-optimize");
            String aiResponse = result.getContent();

            if (aiResponse == null || aiResponse.trim().isEmpty()) {
                log.warn("AI优化返回空结果");
                throw new BusinessException("AI优化失败，未返回有效内容");
            }

            // 解析AI返回的JSON
            String jsonContent = extractJson(aiResponse);
            JsonNode root = objectMapper.readTree(jsonContent);

            String optimizedSql = root.path("optimizedSql").asText(content);
            String estimatedImprovement = root.path("estimatedImprovement").asText("根据SQL复杂度分析");

            // 解析优化建议（包含影响程度和原因）
            List<String> suggestions = new ArrayList<>();
            JsonNode optimizationNode = root.path("optimizationSuggestions");
            if (optimizationNode.isArray()) {
                for (JsonNode s : optimizationNode) {
                    String suggestion = s.path("suggestion").asText();
                    String impact = s.path("impact").asText("");
                    String reason = s.path("reason").asText("");
                    if (!impact.isEmpty()) {
                        suggestion += "（影响：" + impact + "）";
                    }
                    if (!reason.isEmpty()) {
                        suggestion += " - " + reason;
                    }
                    suggestions.add(suggestion);
                }
            }

            // 解析索引建议
            JsonNode indexNode = root.path("indexSuggestions");
            if (indexNode.isArray()) {
                for (JsonNode idx : indexNode) {
                    String table = idx.path("table").asText();
                    String columns = idx.path("columns").toString();
                    String type = idx.path("type").asText("普通索引");
                    suggestions.add("索引建议：在表 " + table + " 的列 " + columns + " 上创建" + type);
                }
            }

            // 解析警告信息
            JsonNode warningsNode = root.path("warnings");
            if (warningsNode.isArray()) {
                for (JsonNode w : warningsNode) {
                    suggestions.add("注意：" + w.asText());
                }
            }

            // 格式化优化后的SQL
            String formattedOptimized = formatSqlLocally(optimizedSql,
                    request.getIndentSize() != null ? request.getIndentSize() : 4,
                    request.getUppercase() != null && request.getUppercase(),
                    dbType);

            log.info("AI优化SQL成功，使用模型: {}, 建议 {} 条", result.getModel(), suggestions.size());

            return SqlFormatResultDTO.builder()
                    .success(true)
                    .formattedSql(formattedOptimized)
                    .compactSql(compactSql(optimizedSql))
                    .sqlType(detectSqlType(optimizedSql))
                    .dbType(dbType)
                    .statistics(estimatedImprovement)
                    .suggestions(suggestions)
                    .errors(Collections.emptyList())
                    .build();

        } catch (Exception e) {
            log.error("AI优化SQL失败: {}", e.getMessage(), e);
            return SqlFormatResultDTO.builder()
                    .success(false)
                    .errors(List.of(SqlErrorDetailDTO.builder()
                            .errorType("AI优化失败")
                            .message(e.getMessage())
                            .suggestion("请检查SQL内容是否正确，或手动进行优化")
                            .build()))
                    .dbType(dbType)
                    .build();
        }
    }

    /**
     * 将异常解析为SQL错误详情列表
     * <p>
     * 提取异常信息和SQL上下文，生成结构化的错误详情，
     * 用于统一错误返回格式。
     * </p>
     *
     * @param e       异常对象
     * @param content 原始SQL内容
     * @return 错误详情列表
     */
    private List<SqlErrorDetailDTO> parseSqlError(Exception e, String content) {
        List<SqlErrorDetailDTO> errors = new ArrayList<>();
        errors.add(SqlErrorDetailDTO.builder()
                .errorType("处理错误")
                .message(e.getMessage())
                .errorContext(content.substring(0, Math.min(content.length(), 100)))
                .suggestion("请检查SQL内容或使用AI修复功能")
                .build());
        return errors;
    }

    /**
     * 从AI响应中提取JSON内容
     * <p>
     * AI返回的内容可能包裹在Markdown代码块中（如 ```json ... ```），
     * 也可能包含额外的说明文字。此方法负责提取其中有效的JSON部分。
     * </p>
     *
     * @param content AI返回的原始内容
     * @return 提取出的JSON字符串
     */
    private String extractJson(String content) {
        String trimmed = content.trim();

        // 移除Markdown代码块标记
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        trimmed = trimmed.trim();

        // 提取第一个完整的JSON对象（从第一个 { 到最后一个 }）
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }

        return trimmed;
    }
}
