import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  ArrowLeft, Download, Type, Palette, Shapes, Maximize2, RefreshCw,
  Sparkles, Check, Copy, Layers, Wand2, Loader2, Lightbulb, MessageSquare
} from 'lucide-react';
import { toolsApi } from '@/api';
import { IconDesignResult } from '@/types';

type IconType = 'app' | 'mini' | 'logo';
type IconShape = 'square' | 'rounded' | 'circle';
type IconCategory = 'text' | 'graphic';
type TextStyle = 'gradient' | 'minimal' | 'geometric' | 'tech' | 'playful' | 'elegant';
type GraphicStyle = 'gradient' | 'outline' | 'solid' | 'duotone';
type GraphicShape = 'star' | 'heart' | 'shield' | 'bolt' | 'flame' | 'leaf' | 'target' | 'cloud' | 'diamond' | 'rocket' | 'music' | 'camera' | 'cart' | 'location' | 'message' | 'wallet';

interface IconConfig {
  type: IconType;
  shape: IconShape;
  category: IconCategory;
  textStyle: TextStyle;
  graphicStyle: GraphicStyle;
  graphicShape: GraphicShape;
  size: number;
  text: string;
  subText: string;
  primaryColor: string;
  secondaryColor: string;
  bgColor: string;
}

const PRESET_SIZES = {
  app: [
    { label: 'iPhone Notification', size: 20 },
    { label: 'iPhone Settings', size: 29 },
    { label: 'iPhone Spotlight', size: 40 },
    { label: 'iPhone App', size: 60 },
    { label: 'iPad Pro', size: 84 },
    { label: 'Google Play', size: 512 },
  ],
  mini: [
    { label: '小程序图标', size: 108 },
    { label: '分享卡片', size: 240 },
    { label: '搜索结果', size: 120 },
  ],
  logo: [
    { label: '小尺寸', size: 128 },
    { label: '中尺寸', size: 256 },
    { label: '大尺寸', size: 512 },
    { label: '超大尺寸', size: 1024 },
  ],
};

const COLOR_PRESETS = [
  { primary: '#6366F1', secondary: '#8B5CF6', bg: '#1E1B4B', name: '靛紫渐变' },
  { primary: '#06B6D4', secondary: '#3B82F6', bg: '#0C4A6E', name: '海洋蓝' },
  { primary: '#10B981', secondary: '#34D399', bg: '#064E3B', name: '翡翠绿' },
  { primary: '#F59E0B', secondary: '#EF4444', bg: '#451A03', name: '日落橙' },
  { primary: '#EC4899', secondary: '#8B5CF6', bg: '#500724', name: '梦幻粉' },
  { primary: '#14B8A6', secondary: '#22D3EE', bg: '#042F2E', name: '薄荷青' },
  { primary: '#F97316', secondary: '#FBBF24', bg: '#431407', name: '暖阳橘' },
  { primary: '#8B5CF6', secondary: '#A855F7', bg: '#2E1065', name: '神秘紫' },
];

const TEXT_STYLE_DESCRIPTIONS: Record<TextStyle, string> = {
  gradient: '现代渐变风格，色彩丰富有层次感',
  minimal: '极简主义，简洁大方',
  geometric: '几何图形组合，科技感强',
  tech: '科技风格，适合互联网产品',
  playful: '活泼有趣，适合年轻化品牌',
  elegant: '优雅精致，适合高端品牌',
};

const GRAPHIC_STYLE_DESCRIPTIONS: Record<GraphicStyle, string> = {
  gradient: '现代渐变填充，立体感强',
  outline: '简洁轮廓线条，精致优雅',
  solid: '纯色填充，简约现代',
  duotone: '双色撞色，时尚年轻',
};

const INDUSTRY_OPTIONS = [
  { value: '科技/互联网', label: '科技/互联网' },
  { value: '金融/银行', label: '金融/银行' },
  { value: '教育/培训', label: '教育/培训' },
  { value: '医疗/健康', label: '医疗/健康' },
  { value: '餐饮/美食', label: '餐饮/美食' },
  { value: '电商/零售', label: '电商/零售' },
  { value: '旅游/出行', label: '旅游/出行' },
  { value: '社交/娱乐', label: '社交/娱乐' },
  { value: '制造/工业', label: '制造/工业' },
  { value: '其他', label: '其他' },
];

export default function AppIconGenerator() {
  const navigate = useNavigate();
  const svgRef = useRef<SVGSVGElement>(null);
  const [config, setConfig] = useState<IconConfig>({
    type: 'app',
    shape: 'rounded',
    category: 'graphic',
    textStyle: 'gradient',
    graphicStyle: 'gradient',
    graphicShape: 'star',
    size: 512,
    text: '',
    subText: '',
    primaryColor: '#6366F1',
    secondaryColor: '#8B5CF6',
    bgColor: '#1E1B4B',
  });
  const [copied, setCopied] = useState(false);
  const [aiGenerating, setAiGenerating] = useState(false);
  const [aiDescription, setAiDescription] = useState('不要文字，纯图形图标');
  const [aiIndustry, setAiIndustry] = useState('');
  const [aiColorPref, setAiColorPref] = useState('');
  const [aiCategoryPref, setAiCategoryPref] = useState<IconCategory>('graphic');
  const [aiResult, setAiResult] = useState<IconDesignResult | null>(null);
  const [aiError, setAiError] = useState('');

  const updateConfig = (key: keyof IconConfig, value: any) => {
    setConfig(prev => ({ ...prev, [key]: value }));
  };

  const applyAiDesign = (design: IconDesignResult) => {
    const validTextStyles: TextStyle[] = ['gradient', 'minimal', 'geometric', 'tech', 'playful', 'elegant'];
    const validGraphicStyles: GraphicStyle[] = ['gradient', 'outline', 'solid', 'duotone'];
    const validShapes: IconShape[] = ['square', 'rounded', 'circle'];
    const validGraphicShapes: GraphicShape[] = ['star', 'heart', 'shield', 'bolt', 'flame', 'leaf', 'target', 'cloud', 'diamond', 'rocket', 'music', 'camera', 'cart', 'location', 'message', 'wallet'];

    const category: IconCategory = design.iconCategory === 'graphic' ? 'graphic' : 'text';
    const shape = validShapes.includes(design.suggestedShape as IconShape)
      ? design.suggestedShape as IconShape : 'rounded';

    let textStyle: TextStyle = 'gradient';
    let graphicStyle: GraphicStyle = 'gradient';
    let graphicShape: GraphicShape = 'star';

    if (category === 'text') {
      textStyle = validTextStyles.includes(design.suggestedStyle as TextStyle)
        ? design.suggestedStyle as TextStyle : 'gradient';
    } else {
      graphicStyle = validGraphicStyles.includes(design.suggestedStyle as GraphicStyle)
        ? design.suggestedStyle as GraphicStyle : 'gradient';
      graphicShape = validGraphicShapes.includes(design.graphicShape as GraphicShape)
        ? design.graphicShape as GraphicShape : 'star';
    }

    setConfig(prev => ({
      ...prev,
      category,
      text: category === 'text' ? (design.brandName || prev.text) : '',
      subText: category === 'text' ? (design.subText || '') : '',
      textStyle,
      graphicStyle,
      graphicShape,
      shape,
      primaryColor: design.primaryColor || prev.primaryColor,
      secondaryColor: design.secondaryColor || prev.secondaryColor,
      bgColor: design.bgColor || prev.bgColor,
    }));
  };

  const handleAiGenerate = async () => {
    setAiGenerating(true);
    setAiError('');
    setAiResult(null);

    try {
      const res = await toolsApi.generateIconDesign({
        brandName: config.text || undefined,
        description: aiDescription || undefined,
        iconType: config.type,
        iconCategory: aiCategoryPref,
        industry: aiIndustry || undefined,
        stylePreference: undefined,
        colorPreference: aiColorPref || undefined,
      });

      if (res.code === 200 && res.data) {
        setAiResult(res.data);
        applyAiDesign(res.data);
      } else {
        setAiError(res.message || 'AI 设计生成失败');
      }
    } catch (err: any) {
      setAiError(err.response?.data?.message || 'AI 设计生成失败，请稍后重试');
    } finally {
      setAiGenerating(false);
    }
  };

  const randomizeColors = () => {
    const preset = COLOR_PRESETS[Math.floor(Math.random() * COLOR_PRESETS.length)];
    setConfig(prev => ({
      ...prev,
      primaryColor: preset.primary,
      secondaryColor: preset.secondary,
      bgColor: preset.bg,
    }));
  };

  const getBorderRadius = () => {
    switch (config.shape) {
      case 'square': return 0;
      case 'rounded': return 90;
      case 'circle': return 256;
    }
  };

  const renderGraphicShape = (size: number = 180, x: number = 256, y: number = 256) => {
    const s = size;
    const cx = x;
    const cy = y;
    const color = config.primaryColor;
    const color2 = config.secondaryColor;

    const getGradientDef = (id: string) => (
      <linearGradient id={id} x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stopColor={color} />
        <stop offset="100%" stopColor={color2} />
      </linearGradient>
    );

    const getFill = (solidColor: string = color, gradId: string = 'graphicGrad') => {
      switch (config.graphicStyle) {
        case 'gradient': return `url(#${gradId})`;
        case 'outline': return 'none';
        case 'solid': return solidColor;
        case 'duotone': return solidColor;
      }
    };

    const getStroke = (strokeColor: string = color) => {
      if (config.graphicStyle === 'outline') return strokeColor;
      return 'none';
    };

    const strokeWidth = config.graphicStyle === 'outline' ? 8 : 0;

    switch (config.graphicShape) {
      case 'star':
        return (
          <>
            <defs>{getGradientDef('graphicGrad')}</defs>
            <polygon
              points={`${cx},${cy - s / 2} ${cx + s * 0.15},${cy - s * 0.15} ${cx + s / 2},${cy - s * 0.15} ${cx + s * 0.2},${cy + s * 0.1} ${cx + s * 0.3},${cy + s / 2} ${cx},${cy + s * 0.25} ${cx - s * 0.3},${cy + s / 2} ${cx - s * 0.2},${cy + s * 0.1} ${cx - s / 2},${cy - s * 0.15} ${cx - s * 0.15},${cy - s * 0.15}`}
              fill={getFill()}
              stroke={getStroke()}
              strokeWidth={strokeWidth}
              strokeLinejoin="round"
            />
            {config.graphicStyle === 'duotone' && (
              <polygon
                points={`${cx},${cy - s * 0.3} ${cx + s * 0.09},${cy - s * 0.09} ${cx + s * 0.3},${cy - s * 0.09} ${cx + s * 0.12},${cy + s * 0.06} ${cx + s * 0.18},${cy + s * 0.3} ${cx},${cy + s * 0.15} ${cx - s * 0.18},${cy + s * 0.3} ${cx - s * 0.12},${cy + s * 0.06} ${cx - s * 0.3},${cy - s * 0.09} ${cx - s * 0.09},${cy - s * 0.09}`}
                fill={color2}
                opacity="0.6"
              />
            )}
          </>
        );
      case 'heart':
        return (
          <>
            <defs>{getGradientDef('graphicGrad')}</defs>
            <path
              d={`M${cx},${cy + s * 0.35} C${cx - s * 0.7},${cy - s * 0.1} ${cx - s * 0.35},${cy - s * 0.55} ${cx},${cy - s * 0.2} C${cx + s * 0.35},${cy - s * 0.55} ${cx + s * 0.7},${cy - s * 0.1} ${cx},${cy + s * 0.35} Z`}
              fill={getFill()}
              stroke={getStroke()}
              strokeWidth={strokeWidth}
              strokeLinejoin="round"
            />
            {config.graphicStyle === 'duotone' && (
              <path
                d={`M${cx},${cy + s * 0.1} C${cx - s * 0.25},${cy - s * 0.15} ${cx - s * 0.12},${cy - s * 0.3} ${cx},${cy - s * 0.1} C${cx + s * 0.12},${cy - s * 0.3} ${cx + s * 0.25},${cy - s * 0.15} ${cx},${cy + s * 0.1} Z`}
                fill={color2}
                opacity="0.7"
              />
            )}
          </>
        );
      case 'shield':
        return (
          <>
            <defs>{getGradientDef('graphicGrad')}</defs>
            <path
              d={`M${cx},${cy - s / 2} L${cx + s / 2},${cy - s * 0.25} L${cx + s * 0.4},${cy + s / 2} L${cx},${cy + s * 0.4} L${cx - s * 0.4},${cy + s / 2} L${cx - s / 2},${cy - s * 0.25} Z`}
              fill={getFill()}
              stroke={getStroke()}
              strokeWidth={strokeWidth}
              strokeLinejoin="round"
            />
            {config.graphicStyle === 'duotone' && (
              <path
                d={`M${cx},${cy - s * 0.1} L${cx + s * 0.15},${cy} L${cx + s * 0.12},${cy + s * 0.2} L${cx},${cy + s * 0.15} L${cx - s * 0.12},${cy + s * 0.2} L${cx - s * 0.15},${cy} Z`}
                fill={color2}
                opacity="0.7"
              />
            )}
          </>
        );
      case 'bolt':
        return (
          <>
            <defs>{getGradientDef('graphicGrad')}</defs>
            <path
              d={`M${cx + s * 0.1},${cy - s / 2} L${cx - s * 0.25},${cy - s * 0.05} L${cx + s * 0.05},${cy - s * 0.05} L${cx - s * 0.1},${cy + s / 2} L${cx + s * 0.25},${cy + s * 0.05} L${cx - s * 0.05},${cy + s * 0.05} Z`}
              fill={getFill()}
              stroke={getStroke()}
              strokeWidth={strokeWidth}
              strokeLinejoin="round"
            />
          </>
        );
      case 'flame':
        return (
          <>
            <defs>{getGradientDef('graphicGrad')}</defs>
            <path
              d={`M${cx},${cy + s / 2} C${cx - s * 0.4},${cy + s * 0.1} ${cx - s * 0.3},${cy - s * 0.1} ${cx - s * 0.15},${cy - s * 0.25} C${cx - s * 0.1},${cy - s * 0.05} ${cx - s * 0.05},${cy * 0.95} ${cx},${cy - s * 0.05} C${cx + s * 0.05},${cy * 0.95} ${cx + s * 0.1},${cy - s * 0.05} ${cx + s * 0.15},${cy - s * 0.25} C${cx + s * 0.3},${cy - s * 0.1} ${cx + s * 0.4},${cy + s * 0.1} ${cx},${cy + s / 2} Z`}
              fill={getFill()}
              stroke={getStroke()}
              strokeWidth={strokeWidth}
              strokeLinejoin="round"
            />
          </>
        );
      case 'leaf':
        return (
          <>
            <defs>{getGradientDef('graphicGrad')}</defs>
            <path
              d={`M${cx},${cy - s / 2} C${cx + s * 0.35},${cy - s * 0.2} ${cx + s * 0.35},${cy + s * 0.2} ${cx},${cy + s / 2} C${cx - s * 0.35},${cy + s * 0.2} ${cx - s * 0.35},${cy - s * 0.2} ${cx},${cy - s / 2} Z`}
              fill={getFill()}
              stroke={getStroke()}
              strokeWidth={strokeWidth}
              strokeLinejoin="round"
            />
            <path
              d={`M${cx},${cy - s * 0.4} L${cx},${cy + s * 0.4}`}
              stroke={config.graphicStyle === 'outline' ? color : color2}
              strokeWidth="6"
              strokeLinecap="round"
            />
          </>
        );
      case 'target':
        return (
          <>
            <defs>{getGradientDef('graphicGrad')}</defs>
            <circle cx={cx} cy={cy} r={s / 2} fill={getFill()} stroke={getStroke()} strokeWidth={strokeWidth} />
            <circle cx={cx} cy={cy} r={s * 0.35} fill={config.bgColor} stroke={config.primaryColor} strokeWidth="6" />
            <circle cx={cx} cy={cy} r={s * 0.12} fill={config.primaryColor} />
            {config.graphicStyle === 'duotone' && (
              <circle cx={cx} cy={cy} r={s * 0.23} fill={color2} opacity="0.7" />
            )}
          </>
        );
      case 'cloud':
        return (
          <>
            <defs>{getGradientDef('graphicGrad')}</defs>
            <path
              d={`M${cx - s * 0.4},${cy + s * 0.15} a${s * 0.25},${s * 0.25} 0 0 1 ${s * 0.05},${-s * 0.32} a${s * 0.3},${s * 0.3} 0 0 1 ${s * 0.55},${-s * 0.08} a${s * 0.22},${s * 0.22} 0 0 1 ${s * 0.15},${s * 0.4} Z`}
              fill={getFill()}
              stroke={getStroke()}
              strokeWidth={strokeWidth}
              strokeLinejoin="round"
            />
          </>
        );
      case 'diamond':
        return (
          <>
            <defs>{getGradientDef('graphicGrad')}</defs>
            <polygon
              points={`${cx},${cy - s / 2} ${cx + s * 0.35},${cy - s * 0.15} ${cx + s * 0.25},${cy + s / 2} ${cx - s * 0.25},${cy + s / 2} ${cx - s * 0.35},${cy - s * 0.15}`}
              fill={getFill()}
              stroke={getStroke()}
              strokeWidth={strokeWidth}
              strokeLinejoin="round"
            />
            <polygon
              points={`${cx},${cy - s / 2} ${cx + s * 0.35},${cy - s * 0.15} ${cx},${cy - s * 0.05} ${cx - s * 0.35},${cy - s * 0.15}`}
              fill={config.graphicStyle === 'gradient' ? 'rgba(255,255,255,0.2)' : color2}
              opacity={config.graphicStyle === 'duotone' ? 0.6 : 1}
            />
          </>
        );
      case 'rocket':
        return (
          <>
            <defs>{getGradientDef('graphicGrad')}</defs>
            <path
              d={`M${cx},${cy - s / 2} C${cx + s * 0.2},${cy - s * 0.3} ${cx + s * 0.25},${cy} ${cx + s * 0.15},${cy + s * 0.25} L${cx - s * 0.15},${cy + s * 0.25} C${cx - s * 0.25},${cy} ${cx - s * 0.2},${cy - s * 0.3} ${cx},${cy - s / 2} Z`}
              fill={getFill()}
              stroke={getStroke()}
              strokeWidth={strokeWidth}
              strokeLinejoin="round"
            />
            <circle cx={cx} cy={cy - s * 0.1} r={s * 0.08} fill={config.bgColor} />
            <path d={`M${cx - s * 0.15},${cy + s * 0.25} L${cx - s * 0.3},${cy + s * 0.4} L${cx - s * 0.05},${cy + s * 0.35} Z`} fill={color} opacity={config.graphicStyle === 'outline' ? 1 : 0.8} />
            <path d={`M${cx + s * 0.15},${cy + s * 0.25} L${cx + s * 0.3},${cy + s * 0.4} L${cx + s * 0.05},${cy + s * 0.35} Z`} fill={color} opacity={config.graphicStyle === 'outline' ? 1 : 0.8} />
            <path d={`M${cx},${cy + s * 0.25} L${cx},${cy + s * 0.45} L${cx - s * 0.08},${cy + s * 0.35} L${cx + s * 0.08},${cy + s * 0.35} Z`} fill={color2} />
          </>
        );
      case 'music':
        return (
          <>
            <defs>{getGradientDef('graphicGrad')}</defs>
            <path
              d={`M${cx + s * 0.1},${cy - s / 2} L${cx + s * 0.1},${cy + s * 0.15} a${s * 0.15},${s * 0.12} 0 1 1 ${-s * 0.1},${-s * 0.12} L${cx},${cy - s * 0.15} L${cx},${cy + s * 0.15} a${s * 0.15},${s * 0.12} 0 1 1 ${-s * 0.1},${-s * 0.12} L${cx - s * 0.1},${cy - s * 0.2} L${cx - s * 0.1},${cy - s / 2} Z`}
              fill={config.graphicStyle === 'outline' ? 'none' : getFill()}
              stroke={color}
              strokeWidth={strokeWidth || 10}
              strokeLinecap="round"
              strokeLinejoin="round"
            />
            {config.graphicStyle === 'duotone' && (
              <>
                <ellipse cx={cx + s * 0.02} cy={cy + s * 0.12} rx={s * 0.12} ry={s * 0.09} fill={color2} />
                <ellipse cx={cx - s * 0.08} cy={cy + s * 0.03} rx={s * 0.12} ry={s * 0.09} fill={color2} />
              </>
            )}
          </>
        );
      case 'camera':
        return (
          <>
            <defs>{getGradientDef('graphicGrad')}</defs>
            <rect x={cx - s / 2} y={cy - s * 0.3} width={s} height={s * 0.6} rx={s * 0.12} fill={getFill()} stroke={getStroke()} strokeWidth={strokeWidth} />
            <path d={`M${cx - s * 0.3},${cy - s * 0.3} L${cx - s * 0.18},${cy - s * 0.45} L${cx + s * 0.18},${cy - s * 0.45} L${cx + s * 0.3},${cy - s * 0.3}`} fill={getFill()} stroke={getStroke()} strokeWidth={strokeWidth} strokeLinejoin="round" />
            <circle cx={cx} cy={cy} r={s * 0.18} fill={config.bgColor} stroke={color} strokeWidth={strokeWidth || 8} />
            <circle cx={cx} cy={cy} r={s * 0.1} fill={color2} />
            <circle cx={cx + s * 0.28} cy={cy - s * 0.18} r={s * 0.05} fill={config.graphicStyle === 'outline' ? color : color2} />
          </>
        );
      case 'cart':
        return (
          <>
            <defs>{getGradientDef('graphicGrad')}</defs>
            <path d={`M${cx - s * 0.45},${cy - s * 0.1} L${cx - s * 0.35},${cy + s * 0.15} L${cx + s * 0.35},${cy + s * 0.15} L${cx + s * 0.4},${cy - s * 0.1} Z`} fill={getFill()} stroke={getStroke()} strokeWidth={strokeWidth} strokeLinejoin="round" />
            <path d={`M${cx - s * 0.35},${cy + s * 0.15} L${cx - s * 0.4},${cy + s * 0.35} L${cx + s * 0.4},${cy + s * 0.35} L${cx + s * 0.35},${cy + s * 0.15}`} fill="none" stroke={color} strokeWidth={strokeWidth || 8} strokeLinecap="round" />
            <circle cx={cx - s * 0.2} cy={cy + s * 0.4} r={s * 0.08} fill={color} stroke={config.bgColor} strokeWidth="4" />
            <circle cx={cx + s * 0.2} cy={cy + s * 0.4} r={s * 0.08} fill={color} stroke={config.bgColor} strokeWidth="4" />
            <path d={`M${cx - s * 0.4},${cy - s * 0.1} L${cx - s * 0.5},${cy - s * 0.25} L${cx - s * 0.4},${cy - s * 0.25}`} fill="none" stroke={color} strokeWidth={strokeWidth || 8} strokeLinecap="round" />
          </>
        );
      case 'location':
        return (
          <>
            <defs>{getGradientDef('graphicGrad')}</defs>
            <path
              d={`M${cx},${cy - s / 2} C${cx + s * 0.4},${cy - s / 2} ${cx + s * 0.4},${cy + s * 0.05} ${cx},${cy + s * 0.35} C${cx - s * 0.4},${cy + s * 0.05} ${cx - s * 0.4},${cy - s / 2} ${cx},${cy - s / 2} Z`}
              fill={getFill()}
              stroke={getStroke()}
              strokeWidth={strokeWidth}
              strokeLinejoin="round"
            />
            <circle cx={cx} cy={cy - s * 0.05} r={s * 0.15} fill={config.bgColor} />
            <circle cx={cx} cy={cy - s * 0.05} r={s * 0.08} fill={config.graphicStyle === 'outline' ? color : color2} />
          </>
        );
      case 'message':
        return (
          <>
            <defs>{getGradientDef('graphicGrad')}</defs>
            <path
              d={`M${cx - s / 2},${cy - s * 0.35} h${s} v${s * 0.5} h${-s * 0.3} l${-s * 0.2},${s * 0.25} l${-s * 0.05},${-s * 0.25} h${-s * 0.45} Z`}
              rx={s * 0.1}
              fill={getFill()}
              stroke={getStroke()}
              strokeWidth={strokeWidth}
              strokeLinejoin="round"
            />
            {config.graphicStyle === 'duotone' && (
              <>
                <circle cx={cx - s * 0.15} cy={cy - s * 0.02} r={s * 0.06} fill={color2} />
                <circle cx={cx} cy={cy - s * 0.02} r={s * 0.06} fill={color2} />
                <circle cx={cx + s * 0.15} cy={cy - s * 0.02} r={s * 0.06} fill={color2} />
              </>
            )}
          </>
        );
      case 'wallet':
        return (
          <>
            <defs>{getGradientDef('graphicGrad')}</defs>
            <rect x={cx - s / 2} y={cy - s * 0.3} width={s} height={s * 0.6} rx={s * 0.08} fill={getFill()} stroke={getStroke()} strokeWidth={strokeWidth} />
            <rect x={cx - s * 0.45} y={cy - s * 0.25} width={s * 0.7} height={s * 0.1} rx={s * 0.04} fill={color2} opacity={config.graphicStyle === 'outline' ? 1 : 0.7} />
            <circle cx={cx + s * 0.3} cy={cy} r={s * 0.08} fill={config.bgColor} />
            <circle cx={cx + s * 0.3} cy={cy} r={s * 0.04} fill={color2} />
          </>
        );
    }
  };

  const renderTextIcon = () => {
    const displayText = config.text || 'APP';
    const initial = displayText.charAt(0).toUpperCase();

    switch (config.textStyle) {
      case 'gradient':
        return (
          <>
            <defs>
              <linearGradient id="bgGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stopColor={config.primaryColor} />
                <stop offset="100%" stopColor={config.secondaryColor} />
              </linearGradient>
            </defs>
            <rect width="512" height="512" fill="url(#bgGrad)" rx={getBorderRadius()} />
            {config.subText ? (
              <>
                <text x="256" y="240" textAnchor="middle" fill="white" fontSize="120" fontWeight="bold" fontFamily="system-ui, -apple-system, sans-serif">{initial}</text>
                <text x="256" y="320" textAnchor="middle" fill="rgba(255,255,255,0.7)" fontSize="32" fontFamily="system-ui, -apple-system, sans-serif">{config.subText}</text>
              </>
            ) : (
              <text x="256" y="280" textAnchor="middle" fill="white" fontSize="140" fontWeight="bold" fontFamily="system-ui, -apple-system, sans-serif">{initial}</text>
            )}
          </>
        );
      case 'minimal':
        return (
          <>
            <rect width="512" height="512" fill={config.bgColor} rx={getBorderRadius()} />
            <circle cx="256" cy="230" r="100" fill={config.primaryColor} opacity="0.2" />
            {config.subText ? (
              <>
                <text x="256" y="240" textAnchor="middle" fill={config.primaryColor} fontSize="120" fontWeight="300" fontFamily="system-ui, -apple-system, sans-serif">{initial}</text>
                <text x="256" y="330" textAnchor="middle" fill="rgba(255,255,255,0.5)" fontSize="28" letterSpacing="4" fontFamily="system-ui, -apple-system, sans-serif">{config.subText.toUpperCase()}</text>
              </>
            ) : (
              <text x="256" y="280" textAnchor="middle" fill={config.primaryColor} fontSize="140" fontWeight="300" fontFamily="system-ui, -apple-system, sans-serif">{initial}</text>
            )}
          </>
        );
      case 'geometric':
        return (
          <>
            <rect width="512" height="512" fill={config.bgColor} rx={getBorderRadius()} />
            <polygon points="256,80 400,200 340,380 172,380 112,200" fill={config.primaryColor} opacity="0.9" />
            <polygon points="256,130 350,210 310,330 202,330 162,210" fill={config.secondaryColor} opacity="0.7" />
            <text x="256" y="270" textAnchor="middle" fill="white" fontSize="80" fontWeight="bold" fontFamily="system-ui, -apple-system, sans-serif">{initial}</text>
            {config.subText && <text x="256" y="330" textAnchor="middle" fill="rgba(255,255,255,0.6)" fontSize="24" fontFamily="system-ui, -apple-system, sans-serif">{config.subText}</text>}
          </>
        );
      case 'tech':
        return (
          <>
            <defs>
              <linearGradient id="techGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stopColor={config.primaryColor} />
                <stop offset="100%" stopColor={config.secondaryColor} />
              </linearGradient>
            </defs>
            <rect width="512" height="512" fill="#0F172A" rx={getBorderRadius()} />
            <rect x="80" y="80" width="352" height="352" fill="none" stroke="url(#techGrad)" strokeWidth="4" rx={Math.max(0, getBorderRadius() - 50)} />
            <circle cx="256" cy="256" r="120" fill="none" stroke="url(#techGrad)" strokeWidth="3" strokeDasharray="10 5" />
            <text x="256" y="275" textAnchor="middle" fill="url(#techGrad)" fontSize="90" fontWeight="bold" fontFamily="system-ui, -apple-system, sans-serif">{initial}</text>
            <circle cx="420" cy="100" r="15" fill={config.primaryColor} opacity="0.6" />
            <circle cx="90" cy="410" r="10" fill={config.secondaryColor} opacity="0.6" />
          </>
        );
      case 'playful':
        return (
          <>
            <defs>
              <linearGradient id="playGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stopColor={config.primaryColor} />
                <stop offset="100%" stopColor={config.secondaryColor} />
              </linearGradient>
            </defs>
            <rect width="512" height="512" fill="url(#playGrad)" rx={getBorderRadius()} />
            <circle cx="256" cy="220" r="100" fill="white" opacity="0.95" />
            <text x="256" y="255" textAnchor="middle" fill={config.primaryColor} fontSize="100" fontWeight="bold" fontFamily="system-ui, -apple-system, sans-serif">{initial}</text>
            <circle cx="180" cy="150" r="20" fill="white" opacity="0.8" />
            <circle cx="340" cy="130" r="15" fill="white" opacity="0.6" />
          </>
        );
      case 'elegant':
        return (
          <>
            <rect width="512" height="512" fill={config.bgColor} rx={getBorderRadius()} />
            <circle cx="256" cy="256" r="160" fill="none" stroke={config.primaryColor} strokeWidth="2" opacity="0.5" />
            <circle cx="256" cy="256" r="130" fill="none" stroke={config.secondaryColor} strokeWidth="1" opacity="0.3" />
            <text x="256" y="240" textAnchor="middle" fill={config.primaryColor} fontSize="72" fontWeight="200" letterSpacing="8" fontFamily="Georgia, serif">{initial}</text>
            <line x1="180" y1="290" x2="332" y2="290" stroke={config.secondaryColor} strokeWidth="1" opacity="0.6" />
            {config.subText && <text x="256" y="330" textAnchor="middle" fill="rgba(255,255,255,0.4)" fontSize="24" letterSpacing="6" fontFamily="Georgia, serif">{config.subText.toUpperCase()}</text>}
          </>
        );
    }
  };

  const renderIconContent = () => {
    if (config.category === 'text') {
      return renderTextIcon();
    } else {
      return (
        <>
          <rect width="512" height="512" fill={config.bgColor} rx={getBorderRadius()} />
          {renderGraphicShape(180, 256, 256)}
        </>
      );
    }
  };

  const downloadIcon = (format: 'png' | 'svg', targetSize?: number) => {
    if (!svgRef.current) return;
    const size = targetSize || config.size;

    if (format === 'svg') {
      const svgData = new XMLSerializer().serializeToString(svgRef.current);
      const blob = new Blob([svgData], { type: 'image/svg+xml' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${config.text || config.graphicShape || 'icon'}_${size}x${size}.svg`;
      a.click();
      URL.revokeObjectURL(url);
    } else {
      const canvas = document.createElement('canvas');
      canvas.width = size;
      canvas.height = size;
      const ctx = canvas.getContext('2d');
      const svgData = new XMLSerializer().serializeToString(svgRef.current);
      const img = new window.Image();
      img.onload = () => {
        ctx?.drawImage(img, 0, 0, size, size);
        const url = canvas.toDataURL('image/png');
        const a = document.createElement('a');
        a.href = url;
        a.download = `${config.text || config.graphicShape || 'icon'}_${size}x${size}.png`;
        a.click();
      };
      img.src = 'data:image/svg+xml;base64,' + btoa(unescape(encodeURIComponent(svgData)));
    }
  };

  const copyToClipboard = () => {
    if (!svgRef.current) return;
    const svgData = new XMLSerializer().serializeToString(svgRef.current);
    navigator.clipboard.writeText(svgData).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    });
  };

  const downloadAllSizes = async () => {
    const sizes = PRESET_SIZES[config.type];
    for (const preset of sizes) {
      downloadIcon('png', preset.size);
      await new Promise(r => setTimeout(r, 300));
    }
  };

  const getCurrentStyleDescription = () => {
    if (config.category === 'text') {
      return TEXT_STYLE_DESCRIPTIONS[config.textStyle];
    }
    return GRAPHIC_STYLE_DESCRIPTIONS[config.graphicStyle];
  };

  return (
    <div className="space-y-8">
      <div className="flex items-center gap-4">
        <button onClick={() => navigate('/tools')} className="p-3 rounded-xl hover:bg-white/5 transition-all">
          <ArrowLeft className="w-5 h-5 text-gray-400" />
        </button>
        <div>
          <h1 className="text-3xl font-bold text-white mb-1">APP ICON 生成器</h1>
          <p className="text-gray-400">AI 创意设计 + 模板渲染，支持纯图形图标和文字图标</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 space-y-6">
          <div className="glass-card rounded-3xl p-8">
            <h2 className="text-xl font-bold text-white mb-6 flex items-center gap-2">
              <Sparkles className="w-5 h-5 text-cyan-400" /> 实时预览
            </h2>
            <div className="flex flex-col items-center">
              <div className="relative p-8 rounded-3xl bg-gradient-to-br from-gray-900 to-gray-800 shadow-2xl" style={{ width: 'fit-content' }}>
                <svg ref={svgRef} width="256" height="256" viewBox="0 0 512 512" className="drop-shadow-2xl">
                  {renderIconContent()}
                </svg>
              </div>
              <p className="text-gray-400 mt-4 text-sm">
                {config.size} × {config.size} px · {config.type === 'app' ? 'APP图标' : config.type === 'mini' ? '小程序图标' : '公司Logo'} · {config.category === 'text' ? '文字首字母' : '纯图形'}
              </p>
            </div>

            <div className="flex flex-wrap gap-3 mt-8 justify-center">
              <button onClick={() => downloadIcon('png')} className="btn-primary flex items-center gap-2">
                <Download className="w-4 h-4" />下载 PNG
              </button>
              <button onClick={() => downloadIcon('svg')} className="btn-secondary flex items-center gap-2">
                <Download className="w-4 h-4" />下载 SVG
              </button>
              <button onClick={copyToClipboard} className="btn-secondary flex items-center gap-2">
                {copied ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
                {copied ? '已复制' : '复制代码'}
              </button>
              <button onClick={downloadAllSizes} className="btn-secondary flex items-center gap-2">
                <Layers className="w-4 h-4" />批量下载
              </button>
            </div>
          </div>

          {aiResult && (
            <div className="glass-card rounded-3xl p-6 border border-cyan-400/30 bg-gradient-to-br from-cyan-500/5 to-blue-600/5">
              <h2 className="text-lg font-bold text-white mb-3 flex items-center gap-2">
                <Lightbulb className="w-5 h-5 text-cyan-400" /> AI 设计分析
              </h2>
              <div className="space-y-3">
                {aiResult.designRationale && (
                  <div className="flex items-start gap-3">
                    <MessageSquare className="w-4 h-4 text-cyan-400 mt-1 flex-shrink-0" />
                    <div>
                      <p className="text-sm font-medium text-cyan-300 mb-1">设计理念</p>
                      <p className="text-gray-300 text-sm">{aiResult.designRationale}</p>
                    </div>
                  </div>
                )}
                <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                  <div className="bg-white/5 rounded-xl p-3">
                    <p className="text-xs text-gray-500 mb-1">分类</p>
                    <p className="text-sm text-white font-medium">{aiResult.iconCategory === 'graphic' ? '纯图形' : '文字'}</p>
                  </div>
                  <div className="bg-white/5 rounded-xl p-3">
                    <p className="text-xs text-gray-500 mb-1">图形</p>
                    <p className="text-sm text-white font-medium">{aiResult.graphicShape || '无'}</p>
                  </div>
                  <div className="bg-white/5 rounded-xl p-3">
                    <p className="text-xs text-gray-500 mb-1">风格</p>
                    <p className="text-sm text-white font-medium">{aiResult.suggestedStyle}</p>
                  </div>
                  <div className="bg-white/5 rounded-xl p-3">
                    <p className="text-xs text-gray-500 mb-1">副标题</p>
                    <p className="text-sm text-white font-medium">{aiResult.subText || '无'}</p>
                  </div>
                </div>
              </div>
            </div>
          )}

          <div className="glass-card rounded-3xl p-6">
            <h2 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
              <Maximize2 className="w-5 h-5 text-cyan-400" /> 预设尺寸
            </h2>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
              {PRESET_SIZES[config.type].map((preset) => (
                <button
                  key={preset.size}
                  onClick={() => updateConfig('size', preset.size)}
                  className={`p-4 rounded-xl transition-all ${config.size === preset.size ? 'bg-gradient-to-br from-cyan-500/20 to-blue-600/20 border border-cyan-400/50' : 'bg-white/5 hover:bg-white/10 border border-white/5'}`}
                >
                  <p className="text-white font-medium text-sm">{preset.label}</p>
                  <p className="text-gray-400 text-xs mt-1">{preset.size} × {preset.size}</p>
                </button>
              ))}
            </div>
          </div>
        </div>

        <div className="space-y-6">
          <div className="glass-card rounded-3xl p-6 border border-fuchsia-400/20 bg-gradient-to-br from-fuchsia-500/5 to-pink-600/5">
            <h2 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
              <Wand2 className="w-5 h-5 text-fuchsia-400" /> AI 创意设计
            </h2>
            <div className="space-y-3">
              <div>
                <label className="block text-sm text-gray-400 mb-2">偏好类型</label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    onClick={() => setAiCategoryPref('graphic')}
                    className={`py-2 px-3 rounded-lg text-xs font-medium transition-all ${aiCategoryPref === 'graphic' ? 'bg-gradient-to-r from-fuchsia-500 to-pink-600 text-white' : 'bg-white/5 text-gray-400 hover:bg-white/10'}`}
                  >
                    纯图形（推荐）
                  </button>
                  <button
                    onClick={() => setAiCategoryPref('text')}
                    className={`py-2 px-3 rounded-lg text-xs font-medium transition-all ${aiCategoryPref === 'text' ? 'bg-gradient-to-r from-fuchsia-500 to-pink-600 text-white' : 'bg-white/5 text-gray-400 hover:bg-white/10'}`}
                  >
                    文字首字母
                  </button>
                </div>
              </div>
              {aiCategoryPref === 'text' && (
                <div>
                  <label className="block text-sm text-gray-400 mb-1">品牌名称</label>
                  <input
                    type="text"
                    value={config.text}
                    onChange={(e) => updateConfig('text', e.target.value)}
                    maxLength={10}
                    className="w-full px-4 py-2 bg-white/5 border border-white/10 rounded-xl text-white placeholder-gray-500 focus:outline-none focus:border-fuchsia-400/50 transition-colors text-sm"
                    placeholder="输入品牌名称"
                  />
                </div>
              )}
              <div>
                <label className="block text-sm text-gray-400 mb-1">品牌描述</label>
                <textarea
                  value={aiDescription}
                  onChange={(e) => setAiDescription(e.target.value)}
                  maxLength={200}
                  rows={2}
                  className="w-full px-4 py-2 bg-white/5 border border-white/10 rounded-xl text-white placeholder-gray-500 focus:outline-none focus:border-fuchsia-400/50 transition-colors resize-none text-sm"
                  placeholder="描述品牌特点，如：面向年轻人的社交APP，注重隐私保护..."
                />
              </div>
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-sm text-gray-400 mb-1">所属行业</label>
                  <select
                    value={aiIndustry}
                    onChange={(e) => setAiIndustry(e.target.value)}
                    className="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-xl text-white focus:outline-none focus:border-fuchsia-400/50 transition-colors text-sm appearance-none"
                  >
                    <option value="" className="bg-gray-900">选择行业</option>
                    {INDUSTRY_OPTIONS.map(opt => (
                      <option key={opt.value} value={opt.value} className="bg-gray-900">{opt.label}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-sm text-gray-400 mb-1">颜色偏好</label>
                  <input
                    type="text"
                    value={aiColorPref}
                    onChange={(e) => setAiColorPref(e.target.value)}
                    maxLength={20}
                    className="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-xl text-white placeholder-gray-500 focus:outline-none focus:border-fuchsia-400/50 transition-colors text-sm"
                    placeholder="如：蓝色调"
                  />
                </div>
              </div>
              <button
                onClick={handleAiGenerate}
                disabled={aiGenerating}
                className="w-full py-3 rounded-xl font-medium text-sm transition-all flex items-center justify-center gap-2 bg-gradient-to-r from-fuchsia-500 to-pink-600 text-white hover:from-fuchsia-600 hover:to-pink-700 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {aiGenerating ? (
                  <><Loader2 className="w-4 h-4 animate-spin" /> AI 设计中...</>
                ) : (
                  <><Wand2 className="w-4 h-4" /> AI 生成设计方案</>
                )}
              </button>
              {aiError && <p className="text-red-400 text-xs">{aiError}</p>}
              {aiResult && !aiError && (
                <p className="text-emerald-400 text-xs flex items-center gap-1">
                  <Check className="w-3 h-3" /> 设计方案已应用，可在右侧继续微调
                </p>
              )}
            </div>
          </div>

          <div className="glass-card rounded-3xl p-6">
            <h2 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
              <Type className="w-5 h-5 text-cyan-400" /> 基础设置
            </h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm text-gray-400 mb-2">图标类型</label>
                <div className="grid grid-cols-3 gap-2">
                  {[{ key: 'app', label: 'APP' }, { key: 'mini', label: '小程序' }, { key: 'logo', label: 'Logo' }].map((item) => (
                    <button
                      key={item.key}
                      onClick={() => updateConfig('type', item.key as IconType)}
                      className={`py-3 px-2 rounded-xl text-sm font-medium transition-all ${config.type === item.key ? 'bg-gradient-to-br from-cyan-500 to-blue-600 text-white shadow-lg' : 'bg-white/5 text-gray-300 hover:bg-white/10'}`}
                    >
                      {item.label}
                    </button>
                  ))}
                </div>
              </div>
              <div>
                <label className="block text-sm text-gray-400 mb-2">图标分类</label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    onClick={() => updateConfig('category', 'graphic')}
                    className={`py-3 px-2 rounded-xl text-sm font-medium transition-all ${config.category === 'graphic' ? 'bg-gradient-to-br from-cyan-500 to-blue-600 text-white shadow-lg' : 'bg-white/5 text-gray-300 hover:bg-white/10'}`}
                  >
                    纯图形
                  </button>
                  <button
                    onClick={() => updateConfig('category', 'text')}
                    className={`py-3 px-2 rounded-xl text-sm font-medium transition-all ${config.category === 'text' ? 'bg-gradient-to-br from-cyan-500 to-blue-600 text-white shadow-lg' : 'bg-white/5 text-gray-300 hover:bg-white/10'}`}
                  >
                    文字首字母
                  </button>
                </div>
              </div>
              {config.category === 'graphic' && (
                <div>
                  <label className="block text-sm text-gray-400 mb-2">选择图形</label>
                  <div className="grid grid-cols-4 gap-2">
                    {['star', 'heart', 'shield', 'bolt', 'flame', 'leaf', 'target', 'cloud', 'diamond', 'rocket', 'music', 'camera', 'cart', 'location', 'message', 'wallet'].map((shape) => (
                      <button
                        key={shape}
                        onClick={() => updateConfig('graphicShape', shape as GraphicShape)}
                        className={`py-3 px-2 rounded-xl text-xs font-medium transition-all flex flex-col items-center gap-1 ${config.graphicShape === shape ? 'bg-cyan-500/20 text-cyan-400 border border-cyan-400/50' : 'bg-white/5 text-gray-400 hover:bg-white/10'}`}
                      >
                        <span className="text-base">{
                          shape === 'star' ? '★' :
                          shape === 'heart' ? '♥' :
                          shape === 'shield' ? '⛨' :
                          shape === 'bolt' ? '⚡' :
                          shape === 'flame' ? '🔥' :
                          shape === 'leaf' ? '🍃' :
                          shape === 'target' ? '🎯' :
                          shape === 'cloud' ? '☁' :
                          shape === 'diamond' ? '◆' :
                          shape === 'rocket' ? '🚀' :
                          shape === 'music' ? '♪' :
                          shape === 'camera' ? '📷' :
                          shape === 'cart' ? '🛒' :
                          shape === 'location' ? '📍' :
                          shape === 'message' ? '💬' :
                          shape === 'wallet' ? '👛' : '●'
                        }</span>
                        <span className="text-[10px]">{
                          shape === 'star' ? '星形' :
                          shape === 'heart' ? '心形' :
                          shape === 'shield' ? '盾牌' :
                          shape === 'bolt' ? '闪电' :
                          shape === 'flame' ? '火焰' :
                          shape === 'leaf' ? '叶子' :
                          shape === 'target' ? '靶心' :
                          shape === 'cloud' ? '云朵' :
                          shape === 'diamond' ? '钻石' :
                          shape === 'rocket' ? '火箭' :
                          shape === 'music' ? '音符' :
                          shape === 'camera' ? '相机' :
                          shape === 'cart' ? '购物车' :
                          shape === 'location' ? '定位' :
                          shape === 'message' ? '气泡' :
                          shape === 'wallet' ? '钱包' : ''
                        }</span>
                      </button>
                    ))}
                  </div>
                </div>
              )}
              {config.category === 'text' && (
                <div>
                  <label className="block text-sm text-gray-400 mb-2">文字内容</label>
                  <input
                    type="text"
                    value={config.text}
                    onChange={(e) => updateConfig('text', e.target.value)}
                    maxLength={10}
                    className="w-full px-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white placeholder-gray-500 focus:outline-none focus:border-cyan-400/50 transition-colors text-sm"
                    placeholder="输入品牌名称"
                  />
                </div>
              )}
            </div>
          </div>

          <div className="glass-card rounded-3xl p-6">
            <h2 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
              <Shapes className="w-5 h-5 text-cyan-400" /> 形状风格
            </h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm text-gray-400 mb-2">外框形状</label>
                <div className="grid grid-cols-3 gap-2">
                  {[{ key: 'square', label: '方形' }, { key: 'rounded', label: '圆角' }, { key: 'circle', label: '圆形' }].map((item) => (
                    <button
                      key={item.key}
                      onClick={() => updateConfig('shape', item.key as IconShape)}
                      className={`py-3 px-2 rounded-xl text-sm font-medium transition-all ${config.shape === item.key ? 'bg-gradient-to-br from-cyan-500 to-blue-600 text-white shadow-lg' : 'bg-white/5 text-gray-300 hover:bg-white/10'}`}
                    >
                      {item.label}
                    </button>
                  ))}
                </div>
              </div>
              <div>
                <label className="block text-sm text-gray-400 mb-2">
                  {config.category === 'text' ? '文字风格' : '图形风格'}
                </label>
                <div className="grid grid-cols-2 gap-2">
                  {(config.category === 'text'
                    ? [{ key: 'gradient', label: '渐变' }, { key: 'minimal', label: '极简' }, { key: 'geometric', label: '几何' }, { key: 'tech', label: '科技' }, { key: 'playful', label: '活泼' }, { key: 'elegant', label: '优雅' }]
                    : [{ key: 'gradient', label: '渐变' }, { key: 'outline', label: '轮廓' }, { key: 'solid', label: '纯色' }, { key: 'duotone', label: '撞色' }]
                  ).map((item) => (
                    <button
                      key={item.key}
                      onClick={() => config.category === 'text' ? updateConfig('textStyle', item.key as TextStyle) : updateConfig('graphicStyle', item.key as GraphicStyle)}
                      className={`py-2 px-3 rounded-lg text-xs font-medium transition-all ${
                        (config.category === 'text' ? config.textStyle : config.graphicStyle) === item.key
                          ? 'bg-cyan-500/20 text-cyan-400 border border-cyan-400/50'
                          : 'bg-white/5 text-gray-400 hover:bg-white/10'
                      }`}
                    >
                      {item.label}
                    </button>
                  ))}
                </div>
                <p className="text-gray-500 text-xs mt-3">{getCurrentStyleDescription()}</p>
              </div>
            </div>
          </div>

          <div className="glass-card rounded-3xl p-6">
            <h2 className="text-lg font-bold text-white mb-4 flex items-center justify-between">
              <span className="flex items-center gap-2"><Palette className="w-5 h-5 text-cyan-400" /> 配色方案</span>
              <button onClick={randomizeColors} className="p-2 rounded-lg bg-white/5 hover:bg-white/10 transition-colors" title="随机配色">
                <RefreshCw className="w-4 h-4 text-gray-400" />
              </button>
            </h2>
            <div className="grid grid-cols-4 gap-2 mb-4">
              {COLOR_PRESETS.map((preset, idx) => (
                <button
                  key={idx}
                  onClick={() => {
                    updateConfig('primaryColor', preset.primary);
                    updateConfig('secondaryColor', preset.secondary);
                    updateConfig('bgColor', preset.bg);
                  }}
                  className={`h-12 rounded-xl transition-all hover:scale-105 ${config.primaryColor === preset.primary ? 'ring-2 ring-cyan-400 ring-offset-2 ring-offset-gray-900' : ''}`}
                  style={{ background: `linear-gradient(135deg, ${preset.primary}, ${preset.secondary})` }}
                  title={preset.name}
                />
              ))}
            </div>
            <div className="space-y-3">
              {[{ key: 'primaryColor', label: '主色调' }, { key: 'secondaryColor', label: '辅助色' }, { key: 'bgColor', label: '背景色' }].map((item) => (
                <div key={item.key} className="flex items-center gap-3">
                  <input
                    type="color"
                    value={config[item.key as keyof IconConfig] as string}
                    onChange={(e) => updateConfig(item.key as keyof IconConfig, e.target.value)}
                    className="w-10 h-10 rounded-lg cursor-pointer border-0"
                  />
                  <div className="flex-1">
                    <p className="text-xs text-gray-500">{item.label}</p>
                    <p className="text-sm text-white font-mono">{(config[item.key as keyof IconConfig] as string).toUpperCase()}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="glass-card rounded-3xl p-6">
            <h2 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
              <Maximize2 className="w-5 h-5 text-cyan-400" /> 自定义尺寸
            </h2>
            <div className="flex items-center gap-3">
              <input
                type="range"
                min="16"
                max="1024"
                value={config.size}
                onChange={(e) => updateConfig('size', Number(e.target.value))}
                className="flex-1 h-2 bg-white/10 rounded-lg appearance-none cursor-pointer accent-cyan-400"
              />
              <input
                type="number"
                value={config.size}
                onChange={(e) => updateConfig('size', Math.min(1024, Math.max(16, Number(e.target.value))))}
                className="w-20 px-3 py-2 bg-white/5 border border-white/10 rounded-xl text-white text-center focus:outline-none focus:border-cyan-400/50 transition-colors"
                min="16"
                max="1024"
              />
            </div>
            <p className="text-gray-500 text-xs mt-2 text-center">尺寸: {config.size} × {config.size} px</p>
          </div>
        </div>
      </div>
    </div>
  );
}
