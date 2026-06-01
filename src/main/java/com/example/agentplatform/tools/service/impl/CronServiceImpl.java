package com.example.agentplatform.tools.service.impl;

import com.example.agentplatform.tools.dto.*;
import com.example.agentplatform.tools.service.CronService;
import com.example.agentplatform.tools.service.ToolAIService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CronServiceImpl implements CronService {

    private final ToolAIService toolAIService;

    private static final String CRON_NL_SYSTEM_PROMPT = """
        你是一个Cron表达式解析专家。用户会输入自然语言描述，你需要将其转换为标准的Cron表达式。

        请严格按照以下JSON格式返回结果，不要包含任何其他内容：
        {"cronExpression":"0 0 12 * * ?","description":"每天中午12点执行","confidence":0.95}

        Cron表达式格式说明（6或7个字段，用空格分隔）：
        - 秒（0-59）
        - 分（0-59）
        - 时（0-23）
        - 日（1-31）
        - 月（1-12）
        - 星期（1-7或SUN-SAT，?表示不指定，1=周一，2=周二，...，6=周六，7=周日）
        - 年（可选）

        特殊字符：
        - * 表示所有值
        - ? 表示不指定值（只能用在日和星期字段，两者不能同时为具体值）
        - - 表示范围，如1-5
        - , 表示列举，如1,3,5
        - / 表示步长，如0/5表示从0开始每5单位
        - L 表示最后，如日字段L表示月末
        - W 表示最近工作日
        - # 表示第几个星期几，如6#3表示第3个星期五

        重要规则：
        1. 当指定了星期的具体值时，日字段必须为 ?
        2. 当指定了日的具体值时，星期字段必须为 ?
        3. 下午/晚上的时间需要加12小时，如下午3点 = 15点

        复杂场景处理指南：

        一、中文节日对应日期范围：
        - "春节"：农历正月初一前后，公历1月底到2月中旬，不同年份日期不同
        - "清明节"：4月4日-6日
        - "劳动节"：5月1日-5日
        - "端午节"：农历五月初五，公历5月底到6月下旬
        - "中秋节"：农历八月十五，公历9月前后
        - "国庆节"：10月1日
        - "国庆假期"：10月1日-7日
        - "国庆节前一周"：9月24日-30日
        - "国庆节前一个月"：9月1日-30日
        - "元旦"：1月1日

        二、当用户说"每X分钟执行1次，每天执行N次"时：
        这意味着从某个时间点开始，每隔X分钟执行，但每天只执行N次。
        需要将分钟字段转换为列举形式。
        例如："每天12点开始 每10分钟执行1次，每天执行5次"
        -> 12:00, 12:10, 12:20, 12:30, 12:40 共5次
        -> 分钟字段为: 0,10,20,30,40
        -> 表达式: 0 0,10,20,30,40 12 * * ?

        例如："每天8点开始 每30分钟执行1次，每天执行3次"
        -> 8:00, 8:30, 9:00 共3次
        -> 分钟字段为: 0,30,0 但可以简化为: 0 0,30 8-9 * * ?
        -> 更精确: 0 0,30 8 * * ? 和 0 0 9 * * ?，合并为 0 0,30 8-9 * * ?（多执行一次9:30也没关系）
        -> 如果用户要求严格3次，则: 0 0,30 8 * * ? (8:00, 8:30) 和 0 0 9 * * ?，需要用 0 0,30 8 * * ? 但注意8:30到9:00之间还有9:00
        -> 实际上如果要求严格，应该用列举: 0 0,30 8 * * ?（8:00,8:30）加上 0 0 9 * * ?（9:00）
        -> 可以合并表达: 0 0 8,9 * * ? 和 0 30 8 * * ?，但Cron只能是一个表达式
        -> 最佳方案: 0 0,30 8 * * ? 然后在description中说明只执行3次需要应用层控制

        三、当用户提到"XX前一周"、"XX前一个月"等相对日期时：
        需要计算具体日期范围。
        例如："国庆节前一周每天12点" -> 9月24日-30日每天12点
        -> 0 0 12 24-30 9 ?

        四、当用户提到"每天XX点开始 每Y分钟执行1次"但没有限制执行次数时：
        直接使用步长表达式。
        例如："每天12点开始 每10分钟执行1次" -> 0 0/10 12 * * ?
        即从12:00开始，12:00, 12:10, 12:20, ... 12:50，然后下一个小时不再执行

        五、复杂表达式组合示例：
        - "每天12点开始 每10分钟执行1次，每天执行5次" -> 0 0,10,20,30,40 12 * * ?
        - "国庆节前一周每天12点开始 每10分钟执行1次，每天执行5次" -> 0 0,10,20,30,40 12 24-30 9 ?
        - "工作日9点到18点 每2小时执行" -> 0 0 9,11,13,15,17 ? * 1-5
        - "每月最后3天每天8点" -> 0 0 8 L-2,L-1,L * ?（Spring Cron不支持L-2等，用具体日期列举或简化为 0 0 8 28-31 * ?）
        - "每天早上9点到下午5点 每30分钟执行" -> 0 0/30 9-17 * * ?
        - "每周一到周五早上8点和下午6点" -> 0 0 8,18 ? * 1-5
        - "每月1号和15号零点" -> 0 0 0 1,15 * ?
        - "国庆假期每天早上8点" -> 0 0 8 1-7 10 ?
        - "春节期间每天9点" -> 0 0 9 * * ?（春节期间日期每年不同，Cron无法表达农历，description中说明）

        六、注意Cron表达式的局限性：
        - Cron不支持"执行N次后停止"，当用户说"每天执行5次"时，只能在分钟字段用列举方式限定次数
        - Cron不支持农历，"春节"、"端午"等农历节日无法精确表达，应说明
        - 如果用户需求无法用一个Cron表达式精确表达，请在description中说明局限性，并给出最接近的表达式

        请务必准确提取时间中的小时和分钟数字！""";

    private static final Map<String, String> WEEK_DAY_NAMES = new LinkedHashMap<>();
    private static final Map<String, String> MONTH_NAMES = new LinkedHashMap<>();

    private static final Set<LocalDate> CHINA_HOLIDAYS = new HashSet<>();
    private static final Set<LocalDate> CHINA_WORKING_WEEKENDS = new HashSet<>();

    private static void initChinaHolidays2025() {
        CHINA_HOLIDAYS.add(LocalDate.of(2025, 1, 1));
        CHINA_HOLIDAYS.add(LocalDate.of(2025, 1, 28));
        CHINA_HOLIDAYS.add(LocalDate.of(2025, 1, 29));
        CHINA_HOLIDAYS.add(LocalDate.of(2025, 1, 30));
        CHINA_HOLIDAYS.add(LocalDate.of(2025, 1, 31));
        CHINA_HOLIDAYS.add(LocalDate.of(2025, 2, 1));
        CHINA_HOLIDAYS.add(LocalDate.of(2025, 2, 2));
        CHINA_HOLIDAYS.add(LocalDate.of(2025, 2, 3));
        CHINA_HOLIDAYS.add(LocalDate.of(2025, 2, 4));
        CHINA_HOLIDAYS.add(LocalDate.of(2025, 4, 4));
        CHINA_HOLIDAYS.add(LocalDate.of(2025, 4, 5));
        CHINA_HOLIDAYS.add(LocalDate.of(2025, 4, 6));
        CHINA_HOLIDAYS.add(LocalDate.of(2025, 5, 1));
        CHINA_HOLIDAYS.add(LocalDate.of(2025, 5, 2));
        CHINA_HOLIDAYS.add(LocalDate.of(2025, 5, 3));
        CHINA_HOLIDAYS.add(LocalDate.of(2025, 5, 4));
        CHINA_HOLIDAYS.add(LocalDate.of(2025, 5, 5));
        CHINA_HOLIDAYS.add(LocalDate.of(2025, 5, 31));
        CHINA_HOLIDAYS.add(LocalDate.of(2025, 6, 1));
        CHINA_HOLIDAYS.add(LocalDate.of(2025, 6, 2));
        CHINA_HOLIDAYS.add(LocalDate.of(2025, 10, 1));
        CHINA_HOLIDAYS.add(LocalDate.of(2025, 10, 2));
        CHINA_HOLIDAYS.add(LocalDate.of(2025, 10, 3));
        CHINA_HOLIDAYS.add(LocalDate.of(2025, 10, 4));
        CHINA_HOLIDAYS.add(LocalDate.of(2025, 10, 5));
        CHINA_HOLIDAYS.add(LocalDate.of(2025, 10, 6));
        CHINA_HOLIDAYS.add(LocalDate.of(2025, 10, 7));

        CHINA_WORKING_WEEKENDS.add(LocalDate.of(2025, 1, 26));
        CHINA_WORKING_WEEKENDS.add(LocalDate.of(2025, 2, 8));
        CHINA_WORKING_WEEKENDS.add(LocalDate.of(2025, 4, 27));
        CHINA_WORKING_WEEKENDS.add(LocalDate.of(2025, 5, 11));
        CHINA_WORKING_WEEKENDS.add(LocalDate.of(2025, 9, 28));
        CHINA_WORKING_WEEKENDS.add(LocalDate.of(2025, 10, 11));
    }

    private static void initChinaHolidays2026() {
        CHINA_HOLIDAYS.add(LocalDate.of(2026, 1, 1));
        CHINA_HOLIDAYS.add(LocalDate.of(2026, 1, 2));
        CHINA_HOLIDAYS.add(LocalDate.of(2026, 1, 3));
        CHINA_HOLIDAYS.add(LocalDate.of(2026, 2, 16));
        CHINA_HOLIDAYS.add(LocalDate.of(2026, 2, 17));
        CHINA_HOLIDAYS.add(LocalDate.of(2026, 2, 18));
        CHINA_HOLIDAYS.add(LocalDate.of(2026, 2, 19));
        CHINA_HOLIDAYS.add(LocalDate.of(2026, 2, 20));
        CHINA_HOLIDAYS.add(LocalDate.of(2026, 2, 21));
        CHINA_HOLIDAYS.add(LocalDate.of(2026, 2, 22));
        CHINA_HOLIDAYS.add(LocalDate.of(2026, 4, 4));
        CHINA_HOLIDAYS.add(LocalDate.of(2026, 4, 5));
        CHINA_HOLIDAYS.add(LocalDate.of(2026, 4, 6));
        CHINA_HOLIDAYS.add(LocalDate.of(2026, 5, 1));
        CHINA_HOLIDAYS.add(LocalDate.of(2026, 5, 2));
        CHINA_HOLIDAYS.add(LocalDate.of(2026, 5, 3));
        CHINA_HOLIDAYS.add(LocalDate.of(2026, 5, 4));
        CHINA_HOLIDAYS.add(LocalDate.of(2026, 5, 5));
        CHINA_HOLIDAYS.add(LocalDate.of(2026, 6, 19));
        CHINA_HOLIDAYS.add(LocalDate.of(2026, 6, 20));
        CHINA_HOLIDAYS.add(LocalDate.of(2026, 6, 21));
        CHINA_HOLIDAYS.add(LocalDate.of(2026, 9, 25));
        CHINA_HOLIDAYS.add(LocalDate.of(2026, 9, 26));
        CHINA_HOLIDAYS.add(LocalDate.of(2026, 9, 27));
        CHINA_HOLIDAYS.add(LocalDate.of(2026, 10, 1));
        CHINA_HOLIDAYS.add(LocalDate.of(2026, 10, 2));
        CHINA_HOLIDAYS.add(LocalDate.of(2026, 10, 3));
        CHINA_HOLIDAYS.add(LocalDate.of(2026, 10, 4));
        CHINA_HOLIDAYS.add(LocalDate.of(2026, 10, 5));
        CHINA_HOLIDAYS.add(LocalDate.of(2026, 10, 6));
        CHINA_HOLIDAYS.add(LocalDate.of(2026, 10, 7));

        CHINA_WORKING_WEEKENDS.add(LocalDate.of(2026, 2, 14));
        CHINA_WORKING_WEEKENDS.add(LocalDate.of(2026, 2, 28));
        CHINA_WORKING_WEEKENDS.add(LocalDate.of(2026, 4, 12));
        CHINA_WORKING_WEEKENDS.add(LocalDate.of(2026, 5, 9));
        CHINA_WORKING_WEEKENDS.add(LocalDate.of(2026, 9, 20));
        CHINA_WORKING_WEEKENDS.add(LocalDate.of(2026, 10, 10));
    }

    static {
        initChinaHolidays2025();
        initChinaHolidays2026();

        WEEK_DAY_NAMES.put("1", "星期一");
        WEEK_DAY_NAMES.put("2", "星期二");
        WEEK_DAY_NAMES.put("3", "星期三");
        WEEK_DAY_NAMES.put("4", "星期四");
        WEEK_DAY_NAMES.put("5", "星期五");
        WEEK_DAY_NAMES.put("6", "星期六");
        WEEK_DAY_NAMES.put("7", "星期日");
        WEEK_DAY_NAMES.put("MON", "星期一");
        WEEK_DAY_NAMES.put("TUE", "星期二");
        WEEK_DAY_NAMES.put("WED", "星期三");
        WEEK_DAY_NAMES.put("THU", "星期四");
        WEEK_DAY_NAMES.put("FRI", "星期五");
        WEEK_DAY_NAMES.put("SAT", "星期六");
        WEEK_DAY_NAMES.put("SUN", "星期日");

        MONTH_NAMES.put("1", "一月");
        MONTH_NAMES.put("2", "二月");
        MONTH_NAMES.put("3", "三月");
        MONTH_NAMES.put("4", "四月");
        MONTH_NAMES.put("5", "五月");
        MONTH_NAMES.put("6", "六月");
        MONTH_NAMES.put("7", "七月");
        MONTH_NAMES.put("8", "八月");
        MONTH_NAMES.put("9", "九月");
        MONTH_NAMES.put("10", "十月");
        MONTH_NAMES.put("11", "十一月");
        MONTH_NAMES.put("12", "十二月");
        MONTH_NAMES.put("JAN", "一月");
        MONTH_NAMES.put("FEB", "二月");
        MONTH_NAMES.put("MAR", "三月");
        MONTH_NAMES.put("APR", "四月");
        MONTH_NAMES.put("MAY", "五月");
        MONTH_NAMES.put("JUN", "六月");
        MONTH_NAMES.put("JUL", "七月");
        MONTH_NAMES.put("AUG", "八月");
        MONTH_NAMES.put("SEP", "九月");
        MONTH_NAMES.put("OCT", "十月");
        MONTH_NAMES.put("NOV", "十一月");
        MONTH_NAMES.put("DEC", "十二月");
    }

    @Override
    public CronGenerateResultDTO generate(CronGenerateRequestDTO request) {
        String second = request.getSecond();
        String minute = request.getMinute();
        String hour = request.getHour();
        String day = request.getDay();
        String month = request.getMonth();
        String weekDay = request.getWeekDay();
        String year = request.getYear();

        List<String> warnings = new ArrayList<>();

        if ("*".equals(day) && !"?".equals(weekDay) && !"*".equals(weekDay)) {
            warnings.add("检测到日字段为\"*\"且星期字段为具体值，已自动将日字段修正为\"?\"以确保星期限制生效（Cron表达式规则：日和星期同为具体值时为OR关系）");
            day = "?";
        }

        if ("*".equals(weekDay) && !"?".equals(day) && !"*".equals(day)) {
            warnings.add("检测到星期字段为\"*\"且日字段为具体值，已自动将星期字段修正为\"?\"以确保日期限制生效（Cron表达式规则：日和星期同为具体值时为OR关系）");
            weekDay = "?";
        }

        String cronExpression;
        if (year == null || year.trim().isEmpty()) {
            cronExpression = String.join(" ", second, minute, hour, day, month, weekDay);
        } else {
            cronExpression = String.join(" ", second, minute, hour, day, month, weekDay, year);
        }

        String description = buildDescription(second, minute, hour, day, month, weekDay);

        boolean valid;
        try {
            CronExpression.parse(cronExpression);
            valid = true;
        } catch (IllegalArgumentException e) {
            valid = false;
            log.warn("生成的Cron表达式无效: {}", cronExpression);
        }

        return CronGenerateResultDTO.builder()
                .success(true)
                .cronExpression(cronExpression)
                .description(description)
                .valid(valid)
                .warnings(warnings.isEmpty() ? null : warnings)
                .build();
    }

    @Override
    public CronParseResultDTO parse(CronParseRequestDTO request) {
        String cronExpression = request.getCronExpression().trim();
        String[] fields = cronExpression.split("\\s+");

        if (fields.length != 6 && fields.length != 7) {
            return CronParseResultDTO.builder()
                    .success(false)
                    .valid(false)
                    .errors(List.of("Cron表达式必须包含6或7个字段，当前包含" + fields.length + "个字段"))
                    .build();
        }

        Map<String, String> fieldDescriptions = new LinkedHashMap<>();
        fieldDescriptions.put("秒", describeField(fields[0], "秒", Map.of()));
        fieldDescriptions.put("分", describeField(fields[1], "分", Map.of()));
        fieldDescriptions.put("时", describeField(fields[2], "时", Map.of()));
        fieldDescriptions.put("日", describeField(fields[3], "日", Map.of()));
        fieldDescriptions.put("月", describeField(fields[4], "月", MONTH_NAMES));
        fieldDescriptions.put("星期", describeField(fields[5], "星期", WEEK_DAY_NAMES));
        if (fields.length == 7) {
            fieldDescriptions.put("年", describeField(fields[6], "年", Map.of()));
        }

        String description = buildDescription(fields[0], fields[1], fields[2], fields[3], fields[4], fields[5]);

        boolean valid;
        try {
            CronExpression.parse(cronExpression);
            valid = true;
        } catch (IllegalArgumentException e) {
            valid = false;
        }

        return CronParseResultDTO.builder()
                .success(true)
                .valid(valid)
                .description(description)
                .fields(fieldDescriptions)
                .build();
    }

    boolean isChinaHoliday(LocalDate date) {
        return CHINA_HOLIDAYS.contains(date);
    }

    boolean isChinaWorkingWeekend(LocalDate date) {
        return CHINA_WORKING_WEEKENDS.contains(date);
    }

    boolean isChinaWorkingDay(LocalDate date) {
        if (isChinaHoliday(date)) {
            return false;
        }
        if (isChinaWorkingWeekend(date)) {
            return true;
        }
        java.time.DayOfWeek dow = date.getDayOfWeek();
        return dow != java.time.DayOfWeek.SATURDAY && dow != java.time.DayOfWeek.SUNDAY;
    }

    String getHolidayName(LocalDate date) {
        if (!isChinaHoliday(date)) {
            return null;
        }
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();
        if (month == 1 && day == 1) return "元旦";
        if (month == 1 || month == 2) return "春节";
        if (month == 4 && (day == 4 || day == 5 || day == 6)) return "清明节";
        if (month == 5 && day >= 1 && day <= 5) return "劳动节";
        if (month == 5 || month == 6) return "端午节";
        if (month == 9) return "中秋节";
        if (month == 10) return "国庆节";
        if (month == 6) return "端午节";
        return "法定节假日";
    }

    @Override
    public CronNextTimesResultDTO nextExecutionTimes(CronNextTimesRequestDTO request) {
        String cronExpression = request.getCronExpression().trim();
        int count = request.getCount() != null ? request.getCount() : 5;
        boolean excludeHoliday = Boolean.TRUE.equals(request.getExcludeHoliday());
        boolean useChinaHoliday = Boolean.TRUE.equals(request.getUseChinaHoliday());

        CronExpression cron;
        try {
            cron = CronExpression.parse(cronExpression);
        } catch (IllegalArgumentException e) {
            return CronNextTimesResultDTO.builder()
                    .success(false)
                    .cronExpression(cronExpression)
                    .errors(List.of("无效的Cron表达式: " + e.getMessage()))
                    .build();
        }

        List<String> nextTimes = new ArrayList<>();
        List<String> excludedTimes = excludeHoliday ? new ArrayList<>() : null;
        List<String> holidayInfo = excludeHoliday ? new ArrayList<>() : null;
        LocalDateTime nextTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        int maxIterations = count * 20;
        int iterations = 0;

        while (nextTimes.size() < count && iterations < maxIterations) {
            nextTime = cron.next(nextTime);
            if (nextTime == null) {
                break;
            }
            iterations++;

            if (excludeHoliday && useChinaHoliday) {
                LocalDate date = nextTime.toLocalDate();
                if (isChinaHoliday(date)) {
                    if (excludedTimes != null) {
                        excludedTimes.add(nextTime.format(formatter));
                    }
                    String holidayName = getHolidayName(date);
                    if (holidayInfo != null && holidayName != null) {
                        String info = String.format("%s 为%s，已跳过", date, holidayName);
                        if (!holidayInfo.contains(info)) {
                            holidayInfo.add(info);
                        }
                    }
                    continue;
                }
            }

            nextTimes.add(nextTime.format(formatter));
        }

        return CronNextTimesResultDTO.builder()
                .success(true)
                .cronExpression(cronExpression)
                .nextTimes(nextTimes)
                .excludedTimes(excludedTimes != null && !excludedTimes.isEmpty() ? excludedTimes : null)
                .holidayInfo(holidayInfo != null && !holidayInfo.isEmpty() ? holidayInfo : null)
                .build();
    }

    @Override
    public CronNLResultDTO parseNaturalLanguage(CronNLRequestDTO request) {
        String naturalLanguage = request.getNaturalLanguage();

        String userPrompt = String.format("请将以下自然语言描述转换为Cron表达式：\n\n%s", naturalLanguage);

        ToolAIService.AIResponse<CronNLResultDTO> response = toolAIService.analyzeWithAI(
                CRON_NL_SYSTEM_PROMPT,
                userPrompt,
                root -> {
                    String cronExpr = root.path("cronExpression").asText("");
                    String desc = root.path("description").asText("");
                    double confidence = root.path("confidence").asDouble(0.5);

                    return CronNLResultDTO.builder()
                            .success(true)
                            .cronExpression(cronExpr)
                            .description(desc)
                            .confidence(confidence)
                            .aiModel(root.path("model").asText(""))
                            .fallback(false)
                            .build();
                },
                () -> {
                    log.warn("AI解析自然语言失败，使用本地降级解析");
                    return buildFallbackResult(naturalLanguage);
                }
        );

        CronNLResultDTO result = response.getData();
        if (response.isFallback() && result != null) {
            result.setFallback(true);
            result.setAiModel("fallback");
        } else if (result != null) {
            result.setAiModel(response.getModel());
        }

        return result;
    }

    String buildDescription(String second, String minute, String hour, String day, String month, String weekDay) {
        List<String> parts = new ArrayList<>();

        if ("*".equals(second) && "*".equals(minute) && "*".equals(hour)
                && "*".equals(day) && "*".equals(month) && "?".equals(weekDay)) {
            return "每秒执行";
        }

        if ("0".equals(second) && "*".equals(minute) && "*".equals(hour)
                && "*".equals(day) && "*".equals(month) && "?".equals(weekDay)) {
            return "每分钟执行";
        }

        if ("0".equals(second) && "0".equals(minute) && "*".equals(hour)
                && "*".equals(day) && "*".equals(month) && "?".equals(weekDay)) {
            return "每小时整点执行";
        }

        if ("0".equals(second) && "0".equals(minute) && isSpecific(hour)
                && "*".equals(day) && "*".equals(month) && "?".equals(weekDay)) {
            return "每天" + hour + "点执行";
        }

        if ("0".equals(second) && "0".equals(minute) && isSpecific(hour)
                && isSpecific(day) && "*".equals(month) && "?".equals(weekDay)) {
            return "每月" + day + "日" + hour + "点执行";
        }

        if ("0".equals(second) && "0".equals(minute) && isSpecific(hour)
                && "*".equals(day) && "*".equals(month) && isSpecificWeekDay(weekDay)) {
            String weekName = WEEK_DAY_NAMES.getOrDefault(weekDay, weekDay);
            return "每" + weekName + hour + "点执行";
        }

        if ("0".equals(second) && "0".equals(minute) && isSpecific(hour)
                && "*".equals(day) && "*".equals(month) && "1-5".equals(weekDay)) {
            return "工作日（周一至周五）" + hour + "点执行";
        }

        if ("0".equals(second) && isStep(minute) && "*".equals(hour)
                && "*".equals(day) && "*".equals(month) && "?".equals(weekDay)) {
            String step = minute.split("/")[1];
            return "每隔" + step + "分钟执行";
        }

        if ("0".equals(second) && "0".equals(minute) && isStep(hour)
                && "*".equals(day) && "*".equals(month) && "?".equals(weekDay)) {
            String step = hour.split("/")[1];
            return "每隔" + step + "小时执行";
        }

        if (!"*".equals(second) && !isStep(second)) {
            parts.add(describeField(second, "秒", Map.of()));
        }
        if (!"*".equals(minute) && !"0".equals(minute)) {
            parts.add(describeField(minute, "分", Map.of()));
        } else if ("0".equals(minute)) {
            parts.add("整分钟");
        }
        if (!"*".equals(hour)) {
            parts.add(describeField(hour, "时", Map.of()));
        }
        if (!"*".equals(day) && !"?".equals(day)) {
            parts.add(describeField(day, "日", Map.of()));
        }
        if (!"*".equals(month)) {
            parts.add(describeField(month, "月", MONTH_NAMES));
        }
        if (!"?".equals(weekDay) && !"*".equals(weekDay)) {
            parts.add(describeField(weekDay, "星期", WEEK_DAY_NAMES));
        }

        if (parts.isEmpty()) {
            return "每秒执行";
        }

        return String.join("，", parts) + "执行";
    }

    String describeField(String value, String fieldName, Map<String, String> fieldDescriptions) {
        if ("*".equals(value)) {
            return "每" + fieldName;
        }
        if ("?".equals(value)) {
            return "不指定" + fieldName;
        }

        if (value.contains("#")) {
            String[] parts = value.split("#");
            String weekDayName = WEEK_DAY_NAMES.getOrDefault(parts[0], parts[0]);
            return "第" + parts[1] + "个" + weekDayName;
        }

        if ("L".equals(value)) {
            if ("日".equals(fieldName)) {
                return "月末最后一天";
            }
            return "最后" + fieldName;
        }

        if ("W".equals(value)) {
            return "最近工作日";
        }

        if (value.endsWith("W")) {
            return value.substring(0, value.length() - 1) + "日最近的工作日";
        }

        if (value.startsWith("L") && value.length() > 1) {
            return "月末倒数第" + value.substring(1) + "天";
        }

        if (value.contains("/")) {
            String[] parts = value.split("/");
            String start = parts[0];
            String step = parts[1];
            String startDesc = "*".equals(start) ? "每" + fieldName : ("从" + resolveName(start, fieldDescriptions) + "开始");
            return startDesc + "每隔" + step + fieldName;
        }

        if (value.contains("-")) {
            String[] parts = value.split("-");
            return "从" + resolveName(parts[0], fieldDescriptions) + "到" + resolveName(parts[1], fieldDescriptions);
        }

        if (value.contains(",")) {
            String[] parts = value.split(",");
            List<String> names = new ArrayList<>();
            for (String part : parts) {
                names.add(resolveName(part.trim(), fieldDescriptions));
            }
            return String.join("、", names);
        }

        return resolveName(value, fieldDescriptions) + fieldName;
    }

    String buildFallbackCron(String naturalLanguage) {
        if (naturalLanguage == null) {
            return null;
        }
        String input = naturalLanguage.trim().toLowerCase();

        if (input.contains("每秒")) {
            return "* * * * * ?";
        }
        if (input.contains("每分钟") || input.contains("每一分钟")) {
            return "0 * * * * ?";
        }

        Integer hour = extractHour(input);
        Integer minute = extractMinute(input);
        String weekDayExpr = extractWeekDayExpr(input);
        String dayExpr = extractDayExpr(input);
        String monthExpr = extractMonthExpr(input);
        String dayRangeExpr = extractDayRangeExpr(input);

        boolean hasDayKeyword = input.contains("每天") || input.contains("每日");
        boolean hasWeekKeyword = weekDayExpr != null;
        boolean hasMonthKeyword = input.contains("每月") || input.contains("每个月");
        boolean hasWorkDayKeyword = input.contains("工作日") || input.contains("周一到周五") || input.contains("星期一到星期五");

        String stepMinuteExpr = null;
        Integer executeCount = null;
        Matcher stepMatcher = Pattern.compile("每(\\d+)分钟").matcher(input);
        if (stepMatcher.find()) {
            int step = Integer.parseInt(stepMatcher.group(1));
            if (step == 5) {
                stepMinuteExpr = "*/5";
            } else if (step == 10) {
                stepMinuteExpr = "*/10";
            } else if (step == 15) {
                stepMinuteExpr = "*/15";
            } else if (step == 20) {
                stepMinuteExpr = "*/20";
            } else if (step == 30) {
                stepMinuteExpr = "*/30";
            } else {
                stepMinuteExpr = "*/" + step;
            }
        }
        Matcher countMatcher = Pattern.compile("每天执行(\\d+)次").matcher(input);
        if (countMatcher.find()) {
            executeCount = Integer.parseInt(countMatcher.group(1));
        } else {
            Matcher countMatcher2 = Pattern.compile("执行(\\d+)次").matcher(input);
            if (countMatcher2.find()) {
                executeCount = Integer.parseInt(countMatcher2.group(1));
            }
        }

        String minuteField = null;
        String hourField = hour != null ? String.valueOf(hour) : null;
        if (stepMinuteExpr != null && executeCount != null && hour != null) {
            int stepVal = Integer.parseInt(stepMinuteExpr.replace("*/", ""));
            List<String> minutes = new ArrayList<>();
            int startMin = minute != null ? minute : 0;
            for (int i = 0; i < executeCount; i++) {
                int m = startMin + i * stepVal;
                if (m > 59) break;
                minutes.add(String.valueOf(m));
            }
            minuteField = String.join(",", minutes);
        } else if (stepMinuteExpr != null) {
            minuteField = stepMinuteExpr;
        } else if (minute != null) {
            minuteField = String.valueOf(minute);
        }

        if (hasWorkDayKeyword) {
            String m = minuteField != null ? minuteField : "0";
            String h = hourField != null ? hourField : "*";
            return String.format("0 %s %s ? * 1-5", m, h);
        }

        if (dayRangeExpr != null && monthExpr != null) {
            String m = minuteField != null ? minuteField : "0";
            String h = hourField != null ? hourField : "*";
            return String.format("0 %s %s %s %s ?", m, h, dayRangeExpr, monthExpr);
        }

        if (hasWeekKeyword) {
            String m = minuteField != null ? minuteField : "0";
            String h = hourField != null ? hourField : "*";
            return String.format("0 %s %s ? * %s", m, h, weekDayExpr);
        }

        if (hasMonthKeyword && dayExpr != null) {
            String m = minuteField != null ? minuteField : "0";
            String h = hourField != null ? hourField : "*";
            return String.format("0 %s %s %s * ?", m, h, dayExpr);
        }

        if (hasDayKeyword) {
            String m = minuteField != null ? minuteField : "0";
            String h = hourField != null ? hourField : "*";
            return String.format("0 %s %s * * ?", m, h);
        }

        if (minuteField != null && hourField != null) {
            return String.format("0 %s %s * * ?", minuteField, hourField);
        }
        if (hourField != null) {
            return String.format("0 0 %s * * ?", hourField);
        }

        if (input.contains("每周")) {
            return "0 0 * * ?";
        }
        if (input.contains("每月")) {
            return "0 0 0 * * ?";
        }
        if (input.contains("每年")) {
            return "0 0 0 1 1 ?";
        }

        return null;
    }

    private Integer extractHour(String input) {
        Pattern pattern = Pattern.compile("(凌晨|早上|上午|中午|下午|晚上|晚上)?(\\d{1,2})(点|时)");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            int h = Integer.parseInt(matcher.group(2));
            String period = matcher.group(1);
            if ((period != null && (period.contains("下午") || period.contains("晚上"))) && h < 12) {
                h += 12;
            }
            if (h >= 0 && h <= 23) {
                return h;
            }
        }
        return null;
    }

    private Integer extractMinute(String input) {
        Pattern pattern = Pattern.compile("(\\d{1,2})(分|点半)");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            String match = matcher.group(0);
            if (match.contains("点半")) {
                return 30;
            }
            int m = Integer.parseInt(matcher.group(1));
            if (m >= 0 && m <= 59) {
                return m;
            }
        }
        if (input.contains("点半") || input.contains("半")) {
            return 30;
        }
        return null;
    }

    private String extractWeekDayExpr(String input) {
        if (input.contains("每周一") || input.contains("每个周一") || input.contains("星期一")) {
            return "1";
        }
        if (input.contains("每周二") || input.contains("每个周二") || input.contains("星期二")) {
            return "2";
        }
        if (input.contains("每周三") || input.contains("每个周三") || input.contains("星期三")) {
            return "3";
        }
        if (input.contains("每周四") || input.contains("每个周四") || input.contains("星期四")) {
            return "4";
        }
        if (input.contains("每周五") || input.contains("每个周五") || input.contains("星期五")) {
            return "5";
        }
        if (input.contains("每周六") || input.contains("每个周六") || input.contains("星期六")) {
            return "6";
        }
        if (input.contains("每周日") || input.contains("每周天") || input.contains("每个周日") || input.contains("星期日") || input.contains("星期天")) {
            return "7";
        }
        return null;
    }

    private String extractDayExpr(String input) {
        Pattern pattern = Pattern.compile("每月?(\\d{1,2})(号|日)");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            int d = Integer.parseInt(matcher.group(1));
            if (d >= 1 && d <= 31) {
                return String.valueOf(d);
            }
        }
        return null;
    }

    private String extractMonthExpr(String input) {
        if (input.contains("国庆节前一周") || input.contains("国庆前一周") || input.contains("国庆节前一个月") || input.contains("国庆前一个月")) return "9";
        if (input.contains("1月") || input.contains("一月") || input.contains("正月")) return "1";
        if (input.contains("2月") || input.contains("二月")) return "2";
        if (input.contains("3月") || input.contains("三月")) return "3";
        if (input.contains("4月") || input.contains("四月")) return "4";
        if (input.contains("5月") || input.contains("五月")) return "5";
        if (input.contains("6月") || input.contains("六月")) return "6";
        if (input.contains("7月") || input.contains("七月")) return "7";
        if (input.contains("8月") || input.contains("八月")) return "8";
        if (input.contains("9月") || input.contains("九月")) return "9";
        if (input.contains("10月") || input.contains("十月") || input.contains("国庆假期") || input.contains("国庆节假期") || input.contains("国庆期间") || input.contains("国庆节") || input.contains("国庆")) return "10";
        if (input.contains("11月") || input.contains("十一月")) return "11";
        if (input.contains("12月") || input.contains("十二月")) return "12";
        return null;
    }

    private String extractDayRangeExpr(String input) {
        if (input.contains("国庆节前一周") || input.contains("国庆前一周")) {
            return "24-30";
        }
        if (input.contains("国庆假期") || input.contains("国庆节假期") || input.contains("国庆期间")) {
            return "1-7";
        }
        if (input.contains("劳动节假期") || input.contains("五一假期") || input.contains("五一期间") || input.contains("劳动节期间")) {
            return "1-5";
        }
        if (input.contains("清明假期") || input.contains("清明节假期") || input.contains("清明期间")) {
            return "4-6";
        }
        Matcher rangeMatcher = Pattern.compile("(\\d{1,2})-(\\d{1,2})[号日]").matcher(input);
        if (rangeMatcher.find()) {
            int start = Integer.parseInt(rangeMatcher.group(1));
            int end = Integer.parseInt(rangeMatcher.group(2));
            if (start >= 1 && end <= 31 && start <= end) {
                return start + "-" + end;
            }
        }
        return null;
    }

    private CronNLResultDTO buildFallbackResult(String naturalLanguage) {
        String cronExpr = buildFallbackCron(naturalLanguage);
        if (cronExpr == null) {
            return CronNLResultDTO.builder()
                    .success(false)
                    .fallback(true)
                    .errors(List.of("无法识别该自然语言描述，请尝试更明确的描述"))
                    .build();
        }

        String[] fields = cronExpr.split("\\s+");
        String description = buildDescription(fields[0], fields[1], fields[2], fields[3], fields[4], fields[5]);

        return CronNLResultDTO.builder()
                .success(true)
                .cronExpression(cronExpr)
                .description(description)
                .confidence(0.5)
                .fallback(true)
                .build();
    }

    private String resolveName(String value, Map<String, String> fieldDescriptions) {
        if (fieldDescriptions != null && fieldDescriptions.containsKey(value)) {
            return fieldDescriptions.get(value);
        }
        return value;
    }

    private boolean isSpecific(String value) {
        return value != null && !"*".equals(value) && !"?".equals(value) && !value.contains("/") && !value.contains(",");
    }

    private boolean isSpecificWeekDay(String value) {
        return value != null && !"?".equals(value) && !"*".equals(value);
    }

    private boolean isStep(String value) {
        return value != null && value.contains("/");
    }
}
