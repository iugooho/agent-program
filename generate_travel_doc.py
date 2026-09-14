from datetime import date
from pathlib import Path

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.style import WD_STYLE_TYPE
from docx.enum.table import WD_CELL_VERTICAL_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt, RGBColor


OUTPUT = Path(__file__).parent / "旅游规划智能体系统_Java技术方案.docx"


def set_cell_shading(cell, fill):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = tc_pr.find(qn("w:shd"))
    if shd is None:
        shd = OxmlElement("w:shd")
        tc_pr.append(shd)
    shd.set(qn("w:fill"), fill)


def set_cell_text(cell, text, bold=False, color=None):
    cell.text = ""
    paragraph = cell.paragraphs[0]
    paragraph.paragraph_format.space_after = Pt(0)
    run = paragraph.add_run(str(text))
    run.bold = bold
    if color:
        run.font.color.rgb = RGBColor(*color)
    cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER


def add_table(doc, headers, rows, widths=None):
    table = doc.add_table(rows=1, cols=len(headers))
    table.style = "Table Grid"
    table.autofit = True
    for i, header in enumerate(headers):
        set_cell_text(table.rows[0].cells[i], header, bold=True, color=(255, 255, 255))
        set_cell_shading(table.rows[0].cells[i], "1F4E79")
    for row in rows:
        cells = table.add_row().cells
        for i, value in enumerate(row):
            set_cell_text(cells[i], value)
    if widths:
        for row in table.rows:
            for i, width in enumerate(widths):
                row.cells[i].width = Inches(width)
    doc.add_paragraph().paragraph_format.space_after = Pt(2)
    return table


def add_bullets(doc, items, level=0):
    for item in items:
        p = doc.add_paragraph(style="List Bullet" if level == 0 else "List Bullet 2")
        p.paragraph_format.space_after = Pt(3)
        p.add_run(item)


def add_numbered(doc, items):
    for item in items:
        p = doc.add_paragraph(style="List Number")
        p.paragraph_format.space_after = Pt(3)
        p.add_run(item)


def add_code(doc, code):
    p = doc.add_paragraph(style="Code Block")
    p.paragraph_format.space_before = Pt(4)
    p.paragraph_format.space_after = Pt(8)
    p.add_run(code.strip("\n"))


def add_heading(doc, text, level=1):
    p = doc.add_heading(text, level=level)
    p.paragraph_format.keep_with_next = True
    return p


def add_page_number(paragraph):
    paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = paragraph.add_run("第 ")
    fld_char1 = OxmlElement("w:fldChar")
    fld_char1.set(qn("w:fldCharType"), "begin")
    instr_text = OxmlElement("w:instrText")
    instr_text.set(qn("xml:space"), "preserve")
    instr_text.text = "PAGE"
    fld_char2 = OxmlElement("w:fldChar")
    fld_char2.set(qn("w:fldCharType"), "end")
    run._r.append(fld_char1)
    run._r.append(instr_text)
    run._r.append(fld_char2)
    paragraph.add_run(" 页")


def configure_document(doc):
    section = doc.sections[0]
    section.top_margin = Inches(0.75)
    section.bottom_margin = Inches(0.7)
    section.left_margin = Inches(0.85)
    section.right_margin = Inches(0.85)

    styles = doc.styles
    normal = styles["Normal"]
    normal.font.name = "Microsoft YaHei"
    normal._element.rPr.rFonts.set(qn("w:eastAsia"), "Microsoft YaHei")
    normal.font.size = Pt(10.5)
    normal.paragraph_format.line_spacing = 1.25
    normal.paragraph_format.space_after = Pt(6)

    for name, size, color in [("Title", 28, "1F4E79"), ("Heading 1", 18, "1F4E79"), ("Heading 2", 14, "2F75B5"), ("Heading 3", 11.5, "404040")]:
        style = styles[name]
        style.font.name = "Microsoft YaHei"
        style._element.rPr.rFonts.set(qn("w:eastAsia"), "Microsoft YaHei")
        style.font.size = Pt(size)
        style.font.color.rgb = RGBColor.from_string(color)
        style.font.bold = True

    if "Code Block" not in styles:
        code_style = styles.add_style("Code Block", WD_STYLE_TYPE.PARAGRAPH)
    else:
        code_style = styles["Code Block"]
    code_style.font.name = "Consolas"
    code_style._element.rPr.rFonts.set(qn("w:eastAsia"), "Consolas")
    code_style.font.size = Pt(9)
    code_style.font.color.rgb = RGBColor(45, 45, 45)
    code_style.paragraph_format.left_indent = Inches(0.2)
    code_style.paragraph_format.right_indent = Inches(0.2)
    code_style.paragraph_format.space_before = Pt(4)
    code_style.paragraph_format.space_after = Pt(8)

    footer = section.footer.paragraphs[0]
    add_page_number(footer)


def add_toc(doc):
    p = doc.add_paragraph()
    run = p.add_run()
    fld_char1 = OxmlElement("w:fldChar")
    fld_char1.set(qn("w:fldCharType"), "begin")
    instr_text = OxmlElement("w:instrText")
    instr_text.set(qn("xml:space"), "preserve")
    instr_text.text = 'TOC \\o "1-3" \\h \\z \\u'
    fld_char2 = OxmlElement("w:fldChar")
    fld_char2.set(qn("w:fldCharType"), "separate")
    run._r.append(fld_char1)
    run._r.append(instr_text)
    run._r.append(fld_char2)
    p.add_run("打开文档后右键目录并选择“更新域”，即可更新页码。")
    fld_char3 = OxmlElement("w:fldChar")
    fld_char3.set(qn("w:fldCharType"), "end")
    run._r.append(fld_char3)


def build_document():
    doc = Document()
    configure_document(doc)

    # Cover
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(70)
    p.add_run("旅游规划智能体系统").bold = True
    p.runs[0].font.name = "Microsoft YaHei"
    p.runs[0]._element.rPr.rFonts.set(qn("w:eastAsia"), "Microsoft YaHei")
    p.runs[0].font.size = Pt(30)
    p.runs[0].font.color.rgb = RGBColor(31, 78, 121)

    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(12)
    run = p.add_run("基于 Java 的大模型智能体技术方案")
    run.font.size = Pt(16)
    run.font.color.rgb = RGBColor(89, 89, 89)

    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(110)
    run = p.add_run("版本：V1.0")
    run.font.size = Pt(11)
    p.add_run("\n编制日期：" + date.today().isoformat())
    p.add_run("\n技术路线：Spring Boot + Spring AI + PostgreSQL")

    doc.add_page_break()

    add_heading(doc, "目录", 1)
    add_toc(doc)
    doc.add_page_break()

    add_heading(doc, "1. 项目概述", 1)
    doc.add_paragraph("旅游规划智能体系统是一套基于大语言模型、智能体工作流和实时旅行数据服务的个性化旅行规划平台。用户只需要用自然语言说明出发地、目的地、天数、人数、预算和偏好，系统即可自动完成信息查询、路线计算、预算估算、行程编排和方案导出。")
    doc.add_paragraph("本方案以 Java 为主要开发语言，采用 Spring Boot 构建后端业务平台，通过 Spring AI 接入大模型，通过工具层连接地图、天气、交通、酒店和餐饮等数据源。")

    add_heading(doc, "1.1 核心能力", 2)
    add_bullets(doc, [
        "自然语言对话规划：识别人数、天数、预算、目的地、出行风格和禁忌。",
        "全自动行程生成：按天编排上午、下午、晚上、住宿、交通和餐饮。",
        "实时数据查询：获取景点、交通、酒店、天气和美食信息。",
        "智能路线优化：综合距离、开放时间、游玩时长、预算和用户偏好。",
        "多轮对话修改：支持增删景点、替换酒店、调整天数和重新优化。",
        "行程导出：支持 Word、PDF、图片和适合微信发送的长文格式。",
        "企业后台：支持线路库、Agent 风格配置、用户咨询和转化统计。",
    ])

    add_heading(doc, "1.2 典型使用场景", 2)
    doc.add_paragraph("用户输入：")
    add_code(doc, "成都出发，去云南 5 天 4 晚，两个人，预算 8000，休闲不累，不要购物，要吃当地特色。")
    doc.add_paragraph("系统输出完整的旅行方案，包括往返交通建议、城市间路线、每日景点安排、酒店推荐、餐饮建议、预算明细、预约事项、天气提醒和旅行避坑信息。")

    add_heading(doc, "2. 总体技术架构", 1)
    add_code(doc, """
Vue 3 / React / 小程序
          │
          ▼
Spring Boot API 网关与业务接口
          │
          ├── 用户与权限模块
          ├── 对话会话模块
          ├── Agent 编排模块
          ├── 旅行工具模块
          ├── 行程优化模块
          ├── 导出服务模块
          └── 企业管理后台
                    │
                    ▼
       PostgreSQL + PostGIS + Redis
                    │
                    ▼
地图 / 天气 / 交通 / 酒店 / 餐饮数据源
""")

    add_heading(doc, "2.1 技术栈选型", 2)
    add_table(doc, ["模块", "推荐技术", "说明"], [
        ["后端", "Java 21、Spring Boot 3", "业务接口、权限、事务和企业级服务"],
        ["AI 编排", "Spring AI", "统一封装模型调用、结构化输出和工具调用"],
        ["前端", "Vue 3 + TypeScript", "用户端、对话界面和管理后台"],
        ["数据库", "PostgreSQL + PostGIS", "业务数据、地理位置和空间距离计算"],
        ["缓存", "Redis", "会话、热点数据、限流和任务状态"],
        ["消息队列", "RabbitMQ 或 Kafka", "异步查询、导出任务和数据同步"],
        ["文件存储", "MinIO / OSS / S3", "Word、PDF、图片和原始数据文件"],
        ["认证", "Spring Security + JWT", "用户登录、权限和企业版角色控制"],
        ["部署", "Docker Compose / Kubernetes", "开发环境和生产环境部署"],
    ])

    add_heading(doc, "2.2 Java 项目模块结构", 2)
    add_code(doc, """
travel-agent/
├── travel-common/          # 通用响应、异常、枚举和工具
├── travel-user/            # 用户、偏好、认证和权限
├── travel-conversation/    # 对话、消息和会话上下文
├── travel-agent-core/      # Agent 状态、工作流和 Prompt
├── travel-tools/           # 景点、交通、酒店、天气和地图工具
├── travel-itinerary/       # 行程模型、路线和预算优化
├── travel-export/          # Word、PDF、图片和微信长文导出
├── travel-admin/           # 线路库、Prompt 配置和统计后台
└── travel-api/             # Spring Boot 启动模块和 API 聚合
""")

    add_heading(doc, "3. 功能模块设计", 1)
    add_heading(doc, "3.1 用户与对话模块", 2)
    add_bullets(doc, [
        "支持注册、登录、游客体验和第三方登录扩展。",
        "保存用户历史行程、常用出发地和旅行偏好。",
        "保存多轮对话消息，并关联当前旅行需求和行程版本。",
        "支持 SSE 流式输出 Agent 进度。",
    ])

    add_heading(doc, "3.2 旅行需求解析", 2)
    doc.add_paragraph("大模型将用户的自然语言转换为结构化 TripRequest 对象。信息不完整时，只询问影响规划结果的关键问题。")
    add_code(doc, """
public record TripRequest(
        String departureCity,
        List<String> destinations,
        LocalDate startDate,
        Integer days,
        Integer travelers,
        BigDecimal budget,
        List<String> travelStyles,
        String transportPreference,
        List<String> foodPreferences,
        List<String> forbiddenItems,
        List<String> accommodationPreferences
) {}
""")

    add_heading(doc, "3.3 Agent 编排模块", 2)
    add_code(doc, """
TripPlannerAgent
├── RequirementParser       # 解析人数、日期、预算和偏好
├── AttractionResearcher    # 查询景点及开放、预约信息
├── TransportPlanner        # 规划往返和城市间交通
├── HotelPlanner             # 根据位置、价格和偏好筛选住宿
├── FoodPlanner              # 推荐当地特色餐饮
├── WeatherAdvisor           # 天气、穿衣和雨天替代方案
├── RouteOptimizer           # 距离、时间和强度优化
└── RiskChecker              # 检查冲突、预约和风险提醒
""")
    doc.add_paragraph("Agent 的内部状态采用结构化对象保存，不直接依赖长文本上下文。大模型负责理解需求、调用工具和生成解释，路线、预算和时间冲突由 Java 程序校验。")

    add_heading(doc, "3.4 旅行工具层", 2)
    add_table(doc, ["工具", "主要功能", "数据要求"], [
        ["search_attractions", "查询景点列表和标签", "城市、日期、风格、游玩时长"],
        ["get_attraction_detail", "获取门票、开放时间和预约要求", "景点 ID、出行日期"],
        ["search_transport", "查询飞机、高铁、火车等方案", "出发地、目的地、日期"],
        ["search_hotels", "筛选酒店和民宿", "位置、入住日期、预算、设施"],
        ["get_weather", "获取天气和穿衣建议", "城市、日期区间"],
        ["search_restaurants", "推荐餐厅、小吃和特色菜", "位置、口味、预算"],
        ["get_route_matrix", "获取距离和交通时间", "多个地点坐标"],
        ["check_reservation", "检查预约、证件和购票要求", "景点、日期、人数"],
    ])
    add_code(doc, """
public interface TravelTool<T, R> {
    String name();
    R execute(T request);
}

@Component
public class WeatherTool
        implements TravelTool<WeatherRequest, WeatherInfo> {

    @Override
    public String name() {
        return "get_weather";
    }

    @Override
    public WeatherInfo execute(WeatherRequest request) {
        return weatherClient.query(request.city(), request.date());
    }
}
""")

    add_heading(doc, "4. 行程生成与优化", 1)
    add_heading(doc, "4.1 规划流程", 2)
    add_numbered(doc, [
        "解析用户需求并检查必要字段。",
        "并行查询景点、交通、酒店、天气和餐饮数据。",
        "按照城市和地理位置生成候选路线。",
        "根据开放时间、游玩时长、交通时间和排队时间排期。",
        "计算交通、住宿、门票、餐饮和其他费用。",
        "校验预算、景点冲突、预约条件和每日强度。",
        "生成结构化行程并由大模型转化为易读的旅行方案。",
    ])

    add_heading(doc, "4.2 路线优化规则", 2)
    add_bullets(doc, [
        "优先将地理位置相近的景点安排在同一天。",
        "避免跨城市往返和无意义的重复换乘。",
        "检查景点开放时间、预约时间和景区末班交通。",
        "为交通、排队、用餐和休息预留缓冲时间。",
        "控制每日游玩时长，匹配休闲、亲子、徒步或摄影风格。",
        "根据天气将户外景点替换为室内景点或备用方案。",
        "用户修改景点或酒店后，只重新计算受影响的日期和路线。",
    ])

    add_heading(doc, "4.3 行程模型", 2)
    add_code(doc, """
public record Itinerary(
        String title,
        BigDecimal totalBudget,
        List<DayPlan> days,
        List<TransportPlan> transportPlans,
        List<HotelPlan> hotelPlans,
        List<String> warnings,
        List<ToolEvidence> evidences
) {}

public record DayPlan(
        Integer dayNumber,
        LocalDate date,
        String city,
        List<ItineraryItem> morning,
        List<ItineraryItem> afternoon,
        List<ItineraryItem> evening,
        HotelPlan hotel,
        List<String> notes
) {}
""")

    add_heading(doc, "5. 核心 Java 服务设计", 1)
    add_code(doc, """
@Service
public class TripPlannerService {

    public Itinerary plan(String userMessage) {
        TripRequest request = requirementParser.parse(userMessage);
        PlanningContext context = travelDataService.collect(request);

        Itinerary itinerary = itineraryOptimizer.optimize(
                request,
                context.getAttractions(),
                context.getTransports(),
                context.getHotels(),
                context.getWeather()
        );

        itineraryValidator.validate(itinerary);
        return itineraryWriter.write(itinerary);
    }
}
""")
    doc.add_paragraph("生产环境建议将上面的同步流程拆分为可追踪的工作流步骤，通过任务状态保存每一步结果，便于失败重试、人工审核和问题定位。")
    add_code(doc, """
public interface PlanningStep {
    PlanningContext execute(PlanningContext context);
}

@Component
public class PlanningWorkflow {
    public PlanningContext run(PlanningContext context) {
        return steps.stream()
                .reduce(context, (current, step) -> step.execute(current),
                        (left, right) -> right);
    }
}
""")

    add_heading(doc, "6. 多轮对话和行程编辑", 1)
    doc.add_paragraph("用户提出“加一天丽江”“不要购物”“酒店换成靠近古城的”等要求时，系统先识别修改意图，再更新结构化需求或行程版本，并仅重新查询受影响的数据。")
    add_table(doc, ["操作类型", "示例", "处理方式"], [
        ["ADD_ATTRACTION", "增加玉龙雪山", "插入行程并重新检查当天时间"],
        ["REMOVE_ATTRACTION", "不要购物", "删除购物点并重新安排餐饮或景点"],
        ["REPLACE_HOTEL", "换成古城附近酒店", "按位置和预算重新筛选住宿"],
        ["CHANGE_DAYS", "加一天丽江", "扩展日期并重算交通和住宿"],
        ["CHANGE_BUDGET", "预算改为 6000", "重新筛选交通、酒店和景点组合"],
        ["REORDER_ITEMS", "把古城放到晚上", "调整顺序并校验开放时间"],
    ])

    add_heading(doc, "7. 接口设计", 1)
    add_table(doc, ["方法", "接口", "用途"], [
        ["POST", "/api/auth/login", "用户登录"],
        ["POST", "/api/trips", "创建旅行规划"],
        ["GET", "/api/trips/{tripId}", "获取行程详情"],
        ["POST", "/api/trips/{tripId}/messages", "发送对话消息"],
        ["POST", "/api/trips/{tripId}/generate", "生成或重新生成行程"],
        ["GET", "/api/trips/{tripId}/stream", "SSE 返回 Agent 进度"],
        ["PATCH", "/api/trips/{tripId}/itinerary", "手动编辑行程"],
        ["POST", "/api/trips/{tripId}/optimize", "重新优化行程"],
        ["POST", "/api/trips/{tripId}/export/word", "导出 Word 文档"],
        ["POST", "/api/trips/{tripId}/export/pdf", "导出 PDF 文档"],
        ["GET", "/api/trips/{tripId}/sources", "查看数据来源和证据"],
    ])
    doc.add_paragraph("生成过程建议使用 SSE，将以下状态推送给前端：解析旅行需求、查询交通、筛选景点、匹配酒店、计算路线、检查预约、行程生成完成。")

    add_heading(doc, "8. 数据库设计", 1)
    add_table(doc, ["表名", "用途", "关键字段"], [
        ["users", "用户信息", "id、phone、nickname、created_at"],
        ["user_preferences", "旅行偏好", "travel_styles、food_preferences、forbidden_items"],
        ["conversations", "对话会话", "user_id、trip_id、status"],
        ["messages", "对话消息", "conversation_id、role、content、metadata"],
        ["trip_requests", "结构化需求", "departure_city、days、travelers、budget"],
        ["itineraries", "行程主表", "trip_id、version、total_budget、status"],
        ["itinerary_items", "每日行程项目", "day_number、start_time、end_time、sort_order"],
        ["attractions", "景点基础信息", "name、city、location、opening_hours"],
        ["hotels", "酒店基础信息", "name、location、price、rating、facilities"],
        ["tool_calls", "工具调用记录", "tool_name、params、result、status"],
        ["source_evidences", "事实数据证据", "source、fetched_at、source_url、raw_data"],
        ["export_files", "导出文件", "file_type、file_url、expires_at"],
    ])
    doc.add_paragraph("实时价格、天气、开放时间和预约要求必须记录来源、查询时间和原始响应，便于审计、纠错和向用户说明信息有效期。")

    add_heading(doc, "9. Word、PDF 和图片导出", 1)
    doc.add_paragraph("系统可以将结构化 Itinerary 转换为正式旅行行程单。Word 导出适合编辑和二次分发，PDF 适合打印，图片适合移动端分享。")
    add_bullets(doc, [
        "Word：使用 docx4j 或 Apache POI 生成标题、表格、页眉页脚和图片。",
        "PDF：使用 HTML 模板转 PDF，或直接使用 OpenPDF、iText 等方案。",
        "图片：使用 HTML 渲染截图或服务端图片排版。",
        "微信长文：输出带标题、分段、地图地址和注意事项的 HTML 或 Markdown。",
    ])
    doc.add_paragraph("导出的行程建议包含：旅行概览、每日时间表、酒店地址、交通方式、餐饮建议、预算明细、预约方式、天气提醒、注意事项、紧急电话和数据更新时间。")

    add_heading(doc, "10. 企业后台设计", 1)
    add_bullets(doc, [
        "线路库管理：维护目的地、主题线路、景点组合和推荐文案。",
        "Agent 配置：维护高端、平价、亲子、情侣等风格的 Prompt 和规则。",
        "内容审核：人工审核景点、酒店、餐厅和风险提示。",
        "数据统计：咨询量、行程生成量、导出量、用户留存和转化率。",
        "操作审计：记录管理员修改、数据源异常和 Agent 失败信息。",
    ])

    add_heading(doc, "11. 安全、稳定性与合规", 1)
    add_bullets(doc, [
        "使用 Spring Security 进行身份认证和接口授权，企业后台采用角色权限控制。",
        "对用户手机号、行程地址等敏感信息进行脱敏和访问控制。",
        "外部接口设置超时、重试、熔断、限流和缓存机制。",
        "对大模型输出执行 JSON Schema 校验，禁止未经校验直接写入核心业务表。",
        "所有实时事实数据标注来源和更新时间，不把模型生成内容当作实时事实。",
        "对航班、酒店和门票价格明确提示以实际预订页面为准。",
    ])

    add_heading(doc, "12. MVP 实施范围", 1)
    doc.add_paragraph("第一版建议先实现国内单目的地、3 到 7 天游的完整闭环，优先验证用户是否愿意使用和修改 AI 行程。")
    add_table(doc, ["优先级", "功能", "第一版目标"], [
        ["P0", "自然语言需求解析", "输出结构化旅行需求"],
        ["P0", "景点、天气和地图查询", "接入稳定 API 或 Mock 数据"],
        ["P0", "每日行程生成", "完成路线、时间和预算计算"],
        ["P0", "多轮对话修改", "支持增删景点、换酒店和改预算"],
        ["P1", "Word / PDF 导出", "输出可直接发送的行程单"],
        ["P1", "酒店和餐饮推荐", "基于位置、预算和偏好筛选"],
        ["P2", "跨省复杂路线", "支持多城市串联"],
        ["P2", "多国跨境和实时出票", "后续接入专业供应商"],
    ])

    add_heading(doc, "13. 开发计划", 1)
    add_table(doc, ["阶段", "主要工作", "交付结果"], [
        ["阶段一：基础工程", "Spring Boot、数据库、Redis、认证、Docker", "可运行的 Java 后端骨架"],
        ["阶段二：AI 对话", "模型接入、结构化解析、多轮会话、SSE", "可对话的旅行需求入口"],
        ["阶段三：旅行工具", "景点、地图、天气、酒店和餐饮接口", "可获取实时旅行数据"],
        ["阶段四：行程引擎", "路线、时间、预算和冲突校验", "可用的自动规划结果"],
        ["阶段五：编辑导出", "增删替换、重新优化、Word/PDF 导出", "完整用户闭环"],
        ["阶段六：企业后台", "线路库、Prompt、统计和审核", "旅行社或企业版能力"],
    ])

    add_heading(doc, "14. 结论与实施建议", 1)
    doc.add_paragraph("本项目建议采用 Java 作为主开发语言，以 Spring Boot 构建稳定的业务后端，以 Spring AI 负责大模型接入和 Agent 工具调用，以 PostgreSQL、Redis 和外部旅行数据服务支撑实时规划。")
    doc.add_paragraph("系统设计应坚持以下分工：")
    add_bullets(doc, [
        "大模型负责自然语言理解、方案解释、用户沟通和推荐理由生成。",
        "Java 程序负责路线计算、预算计算、开放时间检查、状态管理和权限控制。",
        "外部 API 负责天气、地图、交通、酒店、门票和餐饮等实时事实数据。",
        "数据库负责保存行程版本、工具调用记录、数据来源和导出文件。",
    ])
    doc.add_paragraph("先完成单目的地规划和 Word/PDF 导出，再逐步扩展到多目的地、自驾、跨境和企业旅行社场景，可以有效控制研发风险并快速验证产品价值。")

    doc.core_properties.title = "旅游规划智能体系统 Java 技术方案"
    doc.core_properties.subject = "基于 Spring Boot 和 Spring AI 的旅游规划智能体系统"
    doc.core_properties.author = "Codex"
    doc.core_properties.keywords = "Java, Spring Boot, Spring AI, Agent, 旅游规划"
    doc.save(OUTPUT)


if __name__ == "__main__":
    build_document()
    print(OUTPUT)
