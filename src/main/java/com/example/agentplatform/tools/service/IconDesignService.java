package com.example.agentplatform.tools.service;

import com.example.agentplatform.agent.article.dto.GenerateResult;
import com.example.agentplatform.agent.article.service.GenerateService;
import com.example.agentplatform.common.exception.BusinessException;
import com.example.agentplatform.tools.dto.IconDesignRequestDTO;
import com.example.agentplatform.tools.dto.IconDesignResultDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IconDesignService {

    private final GenerateService generateService;
    private final ObjectMapper objectMapper;

    private static final String ICON_DESIGN_SYSTEM_PROMPT = """
            你是一位资深品牌设计师和APP图标设计专家，擅长为各类产品创作独特的视觉标识。
            
            你需要根据用户提供的品牌信息，生成一套完整的图标设计方案。
            
            图标分类 iconCategory：
            - text: 文字首字母图标（如果用户提到品牌名、首字母、文字等，或者用户没有特别说明不要文字）
            - graphic: 纯图形图标（如果用户说"不要文字"、"纯图形"、"图标"、"符号"等）
            
            重要提示：如果用户在描述中明确说不要文字、要图形图标，或不想要字母，iconCategory 必须是 "graphic"！
            
            如果 iconCategory 是 "graphic"，请根据行业和描述选择合适的图形 graphicShape：
            - star: 星形（适合评分、收藏、优质、儿童教育类产品）
            - heart: 心形（适合社交、约会、健康、医疗、情感类产品）
            - shield: 盾牌（适合安全、金融、保险、防护、隐私类产品）
            - bolt: 闪电（适合能源、电力、速度、效率、即时通讯类产品）
            - flame: 火焰（适合热情、活力、运动、社交热点类产品）
            - leaf: 叶子（适合健康、环保、自然、农业、生活类产品）
            - target: 目标/靶心（适合运动、健身、目标管理、效率类产品）
            - cloud: 云朵（适合云服务、存储、天气、云端、轻量类产品）
            - diamond: 钻石（适合高端、奢华、品质、会员、增值服务类产品）
            - rocket: 火箭（适合科技、创业、增长、加速、旅行类产品）
            - music: 音符（适合音乐、音频、播客、娱乐类产品）
            - camera: 相机（适合摄影、图片、社交、短视频类产品）
            - cart: 购物车（适合电商、零售、购物、消费类产品）
            - location: 定位（适合地图、出行、旅游、本地生活类产品）
            - message: 对话气泡（适合聊天、社交、客服、通讯类产品）
            - wallet: 钱包（适合金融、支付、理财、银行类产品）
            
            如果 iconCategory 是 "text"，则 graphicShape 填 null。
            
            可选的设计风格（针对文字图标）：
            - gradient: 现代渐变风格，色彩丰富有层次感
            - minimal: 极简主义，简洁大方
            - geometric: 几何图形组合，科技感强
            - tech: 科技风格，适合互联网产品
            - playful: 活泼有趣，适合年轻化品牌
            - elegant: 优雅精致，适合高端品牌
            
            可选的设计风格（针对图形图标）：
            - gradient: 现代渐变填充，立体感强
            - outline: 简洁轮廓线条，精致优雅
            - solid: 纯色填充，简约现代
            - duotone: 双色撞色，时尚年轻
            
            可选的外框形状：
            - square: 方形
            - rounded: 圆角（推荐，最常用）
            - circle: 圆形
            
            你必须严格按照以下JSON格式返回，不要添加任何其他文字说明：
            {
              "brandName": "品牌名称，没有则填null",
              "initial": "品牌首字母大写，纯图形则填null",
              "iconCategory": "图标分类，text 或 graphic",
              "graphicShape": "图形形状代码，text分类则填null",
              "suggestedStyle": "推荐的风格代码",
              "suggestedShape": "推荐的外框形状代码（square/rounded/circle）",
              "primaryColor": "主色调HEX色值（如#6366F1）",
              "secondaryColor": "辅助色HEX色值",
              "bgColor": "背景色HEX色值",
              "textColor": "文字颜色HEX色值（通常为白色#FFFFFF，纯图形可忽略）",
              "subText": "副标题，可选，2-6个字，纯图形则填null",
              "designRationale": "设计理念说明，50字以内",
              "decorativeElements": ["装饰元素1", "装饰元素2"]
            }
            
            设计原则：
            1. 配色要和谐，主色和辅助色要有对比度但协调
            2. 风格要与行业属性匹配（科技用tech/geometric/bolt/rocket，金融用elegant/shield/wallet，餐饮用playful/heart/flame）
            3. 如果用户明确说不要文字，一定要返回 iconCategory: "graphic"
            4. 颜色要避免使用太亮或太暗的极端色值
            5. 确保所有HEX色值格式正确（如#6366F1）
            6. 图形选择要符合产品定位和用户描述
            """;

    public IconDesignResultDTO generateDesign(IconDesignRequestDTO request) {
        String brandName = request.getBrandName() != null ? request.getBrandName() : "";
        String description = request.getDescription() != null ? request.getDescription() : "";
        String iconType = request.getIconType() != null ? request.getIconType() : "app";
        String iconCategory = request.getIconCategory() != null ? request.getIconCategory() : "";
        String industry = request.getIndustry() != null ? request.getIndustry() : "";
        String stylePreference = request.getStylePreference() != null ? request.getStylePreference() : "";
        String colorPreference = request.getColorPreference() != null ? request.getColorPreference() : "";

        String userPrompt = String.format("""
                请为以下品牌/产品生成图标设计方案：
                品牌名称：%s
                品牌描述：%s
                图标类型：%s
                图标分类偏好：%s
                所属行业：%s
                风格偏好：%s
                颜色偏好：%s
                
                请仔细阅读品牌描述，如果用户明确表示不要文字、不要字母、要纯图形图标，
                请务必将 iconCategory 设为 "graphic"，并选择一个合适的图形。
                
                请生成一套完整的设计方案，严格按JSON格式返回。
                """, brandName, description, iconType, iconCategory, industry, stylePreference, colorPreference);

        try {
            GenerateResult result = generateService.generateWithPrompt(
                    ICON_DESIGN_SYSTEM_PROMPT, userPrompt, "icon-design"
            );

            String content = result.getContent();
            String jsonStr = extractJson(content);

            IconDesignResultDTO designResult = objectMapper.readValue(jsonStr, IconDesignResultDTO.class);
            designResult.setBrandName(brandName);

            if (!"graphic".equals(designResult.getIconCategory()) &&
                (designResult.getInitial() == null || designResult.getInitial().isBlank()) &&
                !brandName.isBlank()) {
                designResult.setInitial(brandName.substring(0, 1).toUpperCase());
            }

            if ("graphic".equals(designResult.getIconCategory())) {
                designResult.setInitial("");
                designResult.setSubText("");
            }

            log.info("为品牌 '{}' 生成了图标设计方案，分类: {}, 风格: {}",
                    brandName, designResult.getIconCategory(), designResult.getSuggestedStyle());
            return designResult;

        } catch (JsonProcessingException e) {
            log.error("解析图标设计方案JSON失败", e);
            return fallbackDesign(request);
        } catch (Exception e) {
            log.error("AI生成图标设计方案失败，品牌: {}", brandName, e);
            return fallbackDesign(request);
        }
    }

    private String extractJson(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        trimmed = trimmed.trim();

        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            trimmed = trimmed.substring(start, end + 1);
        }

        return trimmed;
    }

    private IconDesignResultDTO fallbackDesign(IconDesignRequestDTO request) {
        String brandName = request.getBrandName() != null ? request.getBrandName() : "";
        String description = request.getDescription() != null ? request.getDescription().toLowerCase() : "";
        String iconCategory = request.getIconCategory() != null ? request.getIconCategory() : "";
        String industry = request.getIndustry() != null ? request.getIndustry() : "";

        boolean preferGraphic = "graphic".equals(iconCategory) ||
                description.contains("不要文字") || description.contains("纯图形") ||
                description.contains("不要字母") || description.contains("图形图标") ||
                description.contains("no text") || description.contains("graphic");

        String primaryColor = "#6366F1";
        String secondaryColor = "#8B5CF6";
        String bgColor = "#1E1B4B";

        if (request.getColorPreference() != null) {
            String pref = request.getColorPreference().toLowerCase();
            if (pref.contains("红") || pref.contains("red")) {
                primaryColor = "#EF4444"; secondaryColor = "#F97316"; bgColor = "#450A0A";
            } else if (pref.contains("蓝") || pref.contains("blue")) {
                primaryColor = "#3B82F6"; secondaryColor = "#06B6D4"; bgColor = "#0C4A6E";
            } else if (pref.contains("绿") || pref.contains("green")) {
                primaryColor = "#10B981"; secondaryColor = "#34D399"; bgColor = "#064E3B";
            } else if (pref.contains("橙") || pref.contains("orange")) {
                primaryColor = "#F97316"; secondaryColor = "#FBBF24"; bgColor = "#431407";
            } else if (pref.contains("粉") || pref.contains("pink")) {
                primaryColor = "#EC4899"; secondaryColor = "#8B5CF6"; bgColor = "#500724";
            }
        }

        String style;
        String graphicShape = "";
        String rationale;

        if (preferGraphic) {
            style = "gradient";
            if (industry.contains("金融") || industry.contains("银行") || industry.contains("保险") || industry.contains("安全")) {
                graphicShape = "shield";
                rationale = "金融行业选择盾牌图标，传达安全可靠的品牌形象";
            } else if (industry.contains("社交") || industry.contains("医疗") || industry.contains("健康")) {
                graphicShape = "heart";
                rationale = "社交健康类产品用心形图标，传达温暖关怀";
            } else if (industry.contains("科技") || industry.contains("互联网") || industry.contains("创业")) {
                graphicShape = "rocket";
                rationale = "科技行业选择火箭图标，象征创新和高速增长";
            } else if (industry.contains("电商") || industry.contains("零售") || industry.contains("购物")) {
                graphicShape = "cart";
                rationale = "电商零售用购物车图标，清晰传达品类属性";
            } else if (industry.contains("音乐") || industry.contains("娱乐") || industry.contains("音频")) {
                graphicShape = "music";
                rationale = "音乐娱乐类用音符图标，直观识别产品功能";
            } else if (industry.contains("旅游") || industry.contains("出行") || industry.contains("地图")) {
                graphicShape = "location";
                rationale = "旅游出行用定位图标，传达位置服务属性";
            } else if (industry.contains("云") || industry.contains("存储") || industry.contains("天气")) {
                graphicShape = "cloud";
                rationale = "云服务/天气类用云朵图标，识别度高";
            } else if (industry.contains("能源") || industry.contains("电力") || industry.contains("效率")) {
                graphicShape = "bolt";
                rationale = "能源效率类用闪电图标，传达速度和力量";
            } else if (industry.contains("高端") || industry.contains("奢华") || industry.contains("会员")) {
                graphicShape = "diamond";
                rationale = "高端品牌用钻石图标，传达品质和价值感";
            } else if (industry.contains("运动") || industry.contains("健身")) {
                graphicShape = "target";
                rationale = "运动健身用目标靶心图标，传达目标达成";
            } else {
                graphicShape = "star";
                rationale = "通用星形图标，优质感强，识别度高";
            }

            log.warn("降级模式：为品牌 '{}' 使用纯图形设计方案，图形: {}", brandName, graphicShape);
            return IconDesignResultDTO.builder()
                    .brandName(brandName)
                    .initial("")
                    .iconCategory("graphic")
                    .graphicShape(graphicShape)
                    .suggestedStyle(style)
                    .suggestedShape("rounded")
                    .primaryColor(primaryColor)
                    .secondaryColor(secondaryColor)
                    .bgColor(bgColor)
                    .textColor("#FFFFFF")
                    .subText("")
                    .designRationale(rationale)
                    .decorativeElements(new String[]{})
                    .build();
        } else {
            style = switch (request.getIconType() != null ? request.getIconType() : "app") {
                case "logo" -> "elegant";
                case "mini" -> "minimal";
                default -> "gradient";
            };

            String initial = brandName.isBlank() ? "" : brandName.substring(0, 1).toUpperCase();
            rationale = "基于品牌名称的文字首字母设计方案";

            log.warn("降级模式：为品牌 '{}' 使用默认设计方案", brandName);
            return IconDesignResultDTO.builder()
                    .brandName(brandName)
                    .initial(initial)
                    .iconCategory("text")
                    .graphicShape("")
                    .suggestedStyle(style)
                    .suggestedShape("rounded")
                    .primaryColor(primaryColor)
                    .secondaryColor(secondaryColor)
                    .bgColor(bgColor)
                    .textColor("#FFFFFF")
                    .subText("")
                    .designRationale(rationale)
                    .decorativeElements(new String[]{})
                    .build();
        }
    }
}
