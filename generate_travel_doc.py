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
    p.add_run("\n技术路线：前后端分离（Vue 3 SPA + Spring Boot REST API）+ Spring AI + MyBatis-Plus + Flyway + PostgreSQL + RabbitMQ + Docker")

    doc.add_page_break()

    add_heading(doc, "目录", 1)
    add_toc(doc)
    doc.add_page_break()

    add_heading(doc, "1. 项目概述", 1)
    doc.add_paragraph("旅游规划智能体系统是一套基于大语言模型、智能体工作流和实时旅行数据服务的个性化旅行规划平台。用户只需要用自然语言说明出发地、目的地、天数、人数、预算和偏好，系统即可自动完成信息查询、路线计算、预算估算、行程编排和方案导出。")
    doc.add_paragraph("本方案以 Java 为主要开发语言，采用 Spring Boot 构建后端业务平台，通过 Spring AI 接入大模型，通过工具层连接地图、天气、交通、酒店和餐饮等数据源。")
    doc.add_paragraph("系统采用前后端分离架构：前端是以 Vue 3 + TypeScript 构建的单页应用，只负责界面与交互；后端是以 Spring Boot 构建的 REST 服务，只负责业务与数据；两端通过 /api 接口契约通信，独立构建、独立部署。")

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

    add_heading(doc, "2. 总体技术架构与前后端分离", 1)
    add_code(doc, """
前端：Vue 3 SPA（独立构建、独立部署，产物为静态资源）
          │  HTTPS + JSON，统一 /api 前缀
          ▼
Nginx / CDN（静态资源托管 + /api 反向代理）
          │
          ▼
后端：Spring Boot REST API（独立进程、无状态、可水平扩展）
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
        ["后端", "Java 21、Spring Boot 3（REST）", "只提供 JSON 接口：业务逻辑、权限、事务和企业级服务"],
        ["前端", "Vue 3 + TypeScript + Vite（SPA）", "独立工程，只通过 HTTP 调用后端，构建产物为静态资源"],
        ["前后端通信", "REST + JSON、SpringDoc OpenAPI", "契约先行，统一 /api 前缀、统一响应结构与接口文档"],
        ["AI 编排", "Spring AI", "统一封装模型调用、结构化输出和工具调用"],
        ["持久层", "MyBatis-Plus", "单表 CRUD 与分页开箱可用，复杂查询写 XML"],
        ["数据库", "PostgreSQL + PostGIS", "业务数据、地理位置和空间距离计算"],
        ["数据库版本管理", "Flyway", "建表与数据变更全部走迁移脚本，禁止手工改库"],
        ["连接池", "HikariCP（默认），Druid 可选", "需要 SQL 监控与慢 SQL 统计时再替换为 Druid"],
        ["缓存", "Redis", "会话、热点数据、限流和任务状态"],
        ["消息队列", "RabbitMQ", "异步查询、导出任务、通知与数据同步（定版说明见 3.1）"],
        ["实时通信", "Spring WebSocket(STOMP) + SSE", "对话进度与协作消息自建，不接第三方 IM SDK"],
        ["文件存储", "MinIO / OSS / S3", "Word、PDF、图片和原始数据文件"],
        ["认证与鉴权", "Spring Security + JWT", "用户登录、接口权限和企业版角色控制（不使用 Shiro）"],
        ["日志", "SLF4J + Logback（可换 Log4j2）", "门面统一用 SLF4J，实现可替换，Log4j2 要求 2.17 以上"],
        ["数据可视化", "ECharts", "企业后台的数据大屏与统计图表"],
        ["前端测试", "Vitest + Vue Test Utils", "组件与工具函数单元测试，端到端测试预留 Playwright"],
        ["部署与交付", "Docker + GitHub Actions / Jenkins", "后端容器镜像 + 前端静态产物，两端流水线独立"],
    ])

    add_heading(doc, "2.2 前后端分离原则", 2)
    doc.add_paragraph("本项目要求前后端分离：前端只负责界面与交互，后端只负责数据与业务，两端通过 HTTP + JSON 接口通信，各自独立开发、独立构建、独立部署。前端不嵌入任何后端代码，后端不输出任何页面模板。")
    add_table(doc, ["维度", "前端（Vue 3 SPA）", "后端（Spring Boot REST API）"], [
        ["运行形态", "构建为静态资源（HTML/JS/CSS），部署到 Nginx 或 CDN", "独立 JVM 进程，对外只暴露 JSON 接口，不渲染页面"],
        ["数据访问", "只能通过 /api 调用后端，不直连数据库", "访问数据库、缓存和外部数据源"],
        ["业务职责", "页面渲染、交互状态、表单校验、结果展示", "业务规则、路线与预算计算、事务、权限、入参校验"],
        ["状态管理", "只保存界面状态（Pinia）和临时缓存", "保存业务状态、对话上下文和行程版本"],
        ["交付方式", "独立构建产物 dist，独立发版", "独立构建产物 jar 或容器镜像，独立发版"],
    ])
    add_bullets(doc, [
        "前端禁止：直连数据库或第三方密钥服务、硬编码后端地址、在组件里复制业务规则、在代码中保存密钥。",
        "后端禁止：返回 HTML 页面或依赖前端模板、信任前端传入的计算结果、把界面文案写进业务逻辑。",
        "接口变更必须先改契约（OpenAPI）再改实现，并保持向后兼容；破坏性变更走 /api/v2 并同步修改前端。",
        "业务规则只存在后端一处，前端只做体验层的即时校验，最终以服务端校验结果为准。",
    ])

    add_heading(doc, "2.3 项目结构与目录约定", 2)
    add_code(doc, """
travel-planner/
├── backend/                         Spring Boot REST 服务（独立构建、独立部署）
│   ├── pom.xml
│   └── src/main/java/com/travelagent/travelplanner/
│       ├── api/                   Controller：参数校验、权限注解、响应封装
│       ├── application/           应用服务：用例编排（对话、行程生成、导出）
│       ├── domain/                领域模型与业务规则（纯 Java，不依赖框架）
│       └── infrastructure/        工具层与外部适配（地图、天气、酒店、大模型、持久化）
└── frontend/                        Vue 3 SPA（独立构建、独立部署）
    ├── package.json
    ├── vite.config.ts             开发代理：/api -> http://localhost:8080
    └── src/
        ├── api/                   axios 实例与按模块划分的接口封装
        ├── views/                 对话页、行程详情页、行程编辑页、企业后台
        ├── stores/                Pinia 状态
        └── router/                前端路由
""")
    doc.add_paragraph("后端分层依赖方向固定为 api → application → domain，infrastructure 实现 domain 定义的接口；前端组件只允许通过 src/api 调用接口，不允许在页面里直接拼接 URL。")

    add_heading(doc, "2.4 接口契约", 2)
    add_bullets(doc, [
        "统一前缀 /api，主版本写在路径里（/api/v1/...），版本升级不影响老前端。",
        "统一响应结构：code（业务码）、message（提示文案）、data（业务数据）；分页统一为 page、size、total、items。",
        "错误处理：HTTP 状态码表达类别，业务码表达细节，错误响应不带堆栈信息，参数校验错误返回字段级说明。",
        "接口文档：后端用 SpringDoc / Swagger UI 自动生成 OpenAPI 文档，作为前后端联调和自动化测试的唯一依据。",
        "前端约定：请求集中在 src/api 封装，统一处理 token、超时、错误提示和重试，组件不直接调用 axios。",
    ])
    add_code(doc, """
GET /api/v1/trips/{tripId}
{
  "code": 0,
  "message": "ok",
  "data": {
    "tripId": "T20260915001",
    "days": 5,
    "itinerary": [ { "dayNumber": 1, "items": [ ... ] } ]
  }
}
""")

    add_heading(doc, "2.5 联调、构建与独立部署", 2)
    add_table(doc, ["环境", "前端", "后端", "连接方式"], [
        ["本地开发", "npm run dev（5173）", "mvn spring-boot:run（8080）", "Vite 代理 /api 到 localhost:8080，开发期不涉及跨域"],
        ["测试环境", "npm run build 产出 dist，部署到 Nginx", "jar 或容器镜像部署到测试机", "Nginx 反向代理 /api 到后端，对外单一域名"],
        ["生产环境", "dist 上传 CDN 或 Nginx，独立发版", "多实例部署加负载均衡，独立发版", "同域反向代理，或独立 API 域名加 CORS 白名单"],
    ])
    add_bullets(doc, [
        "前端通过环境变量配置后端地址（VITE_API_BASE_URL）：开发走代理，生产读环境变量，代码中不写死 IP 或域名。",
        "后端通过配置项控制允许来源（CORS 白名单）、超时和限流：开发放开本地端口，生产只允许正式域名。",
        "两端独立流水线：前端产出静态资源，后端产出可执行 jar 或镜像；任一端升级都不需要重新构建另一端。",
        "发布顺序：接口保持向后兼容的前提下后端先发布，前端随后发布；出问题时前端可回滚到上一个静态版本。",
    ])

    doc.add_paragraph("完整的容器化、镜像构建与 CI/CD 流程见第 13 章。")

    add_heading(doc, "2.6 跨域、鉴权与安全边界", 2)
    add_bullets(doc, [
        "跨域：开发环境用 Vite 代理规避；生产环境用 Nginx 同域反向代理，或后端配置精确的 CORS 白名单，不使用通配符。",
        "鉴权：登录后由后端签发 JWT，前端在请求头携带 Authorization: Bearer token；SSE 流式接口通过请求头或一次性票据传递凭证。",
        "越权防护：所有资源接口在后端校验数据归属，前端传入的 tripId、用户标识一律不可信。",
        "敏感信息：密钥和模型 API Key 只放在后端，前端只能调用后端封装的接口，不直连大模型或第三方数据服务。",
        "日志与审计：接口层记录请求 ID 便于前后端联调排查，敏感字段脱敏，错误响应不暴露内部实现。",
    ])

    add_heading(doc, "3. 技术栈选型与替换说明", 1)
    doc.add_paragraph("本章逐项说明候选技术在本项目中的取舍：哪些采纳、哪些暂缓、哪些可以按运维条件替换。这些结论已经落在脚手架 scaffold-cli 的依赖目录里，生成项目时按 id 选择即可，脚手架会把版本号、配置文件和示例代码一并写好。")

    add_heading(doc, "3.1 选型结论总览", 2)
    add_table(doc, ["候选技术", "结论", "本项目落地方式"], [
        ["Vue 3 / React", "采纳 Vue 3", "前端骨架已是 Vue 3 + TypeScript + Vite，不换 React：与 Vite、Pinia、ECharts 组合成熟，团队上手成本低"],
        ["MI 即时通信（IM）", "自建，不接第三方 IM SDK", "后端 Spring WebSocket(STOMP) + SSE，前端 stomp 客户端订阅；消息、审计与合规都在自有后端完成"],
        ["ECharts 数据大屏", "采纳", "企业后台统计与数据大屏统一用 ECharts，图表数据全部来自后端统计接口，前端不做业务计算"],
        ["前端应用安全", "采纳为规范条款", "CSP、XSS/CSRF 防护、依赖漏洞扫描、密钥不落前端，写成强制规范，不额外引入框架"],
        ["Vite + ESLint", "已在用，补齐测试", "补 Vitest + Vue Test Utils 单元测试与覆盖率；端到端测试预留 Playwright"],
        ["Spring Cloud + Nacos", "暂缓", "第一版做单体模块化（包分层），出现多实例治理、灰度与集中配置需求时再引入注册与配置中心"],
        ["RabbitMQ / Kafka", "定为 RabbitMQ", "本项目消息场景是导出、通知、异步查询，RabbitMQ 的路由与延迟队列更贴合，运维成本低于 Kafka"],
        ["Spring Security / Shiro", "采纳 Spring Security", "不使用 Shiro：与 Spring Boot 自动配置、JWT、方法级注解、OAuth2 扩展的集成度更好"],
        ["SLF4J + Log4j / Log4j2", "SLF4J 门面 + Logback 默认，可换 Log4j2", "门面统一 SLF4J；默认实现 Logback，需要异步日志或更高吞吐时换 Log4j2（2.17 以上），禁止 Log4j 1.x"],
        ["Elasticsearch 全文搜索", "暂缓，预留扩展点", "第一版用 PostgreSQL 全文检索覆盖景点与线路搜索；搜索能力按接口抽象，后续可平滑替换为 ES"],
        ["接口安全", "采纳并细化", "签名 + 时间戳 + nonce 防重放、幂等键、限流、入参校验、审计日志（见 3.3）"],
        ["MyBatis", "采纳 MyBatis-Plus", "单表 CRUD 与分页由 MyBatis-Plus 提供，复杂查询保留 XML；表结构变更统一走 Flyway"],
        ["Druid / HikariCP", "默认 HikariCP，Druid 可选", "只有需要 SQL 监控与慢 SQL 统计时才替换为 Druid，且监控页必须鉴权并限制来源 IP"],
        ["Docker + Jenkins", "采纳容器化，CI 用 GitHub Actions", "仓库托管在 GitHub，优先 GitHub Actions；企业内部已有 Jenkins 时复用同一份 Dockerfile"],
    ])

    add_heading(doc, "3.2 即时通信与实时推送", 2)
    doc.add_paragraph("即时通信在本项目里承担两类不同的事情，方案上分开处理：一类是 Agent 生成进度的单向推送，一类是多轮对话的消息收发。")
    add_bullets(doc, [
        "进度推送用 SSE：单向、实现成本最低、便于排查，前端凭 tripId 断线重连。",
        "会话消息用 WebSocket + STOMP：服务端按 /topic/trips/{tripId} 广播，前端订阅对应主题。",
        "不引入第三方 IM SDK：用户体系、消息落库、审计和敏感内容过滤都在自有后端完成；手机端离线推送等需求再叠加厂商推送通道。",
        "消息一律先落库再推送：messages 表保存发送方、内容、时间和投递状态，SSE 与 WebSocket 只承担传输职责。",
        "连接鉴权与限流：WebSocket 握手阶段校验 JWT，限制单用户连接数与消息频率，断线重连复用会话 ID。",
    ])
    add_code(doc, """
// 前端：订阅行程消息主题（stomp 客户端按需引入）
const client = new Client({
  brokerURL: import.meta.env.VITE_WS_BASE_URL,
  connectHeaders: { Authorization: `Bearer ${token}` }
})
client.onConnect = () => client.subscribe(`/topic/trips/${tripId}`, (frame) => onMessage(frame.body))

// 后端：推送
messagingTemplate.convertAndSend("/topic/trips/" + tripId, payload);
""")

    add_heading(doc, "3.3 权限控制与接口安全", 2)
    doc.add_paragraph("权限控制统一使用 Spring Security：登录签发 JWT，接口按角色和数据归属双重校验，企业后台再叠加角色与菜单权限。接口安全在网关与业务层同时设防。")
    add_table(doc, ["安全措施", "落地方式", "说明"], [
        ["身份认证", "Spring Security + JWT", "登录换取 access token，刷新 token 单独域校验；密码使用 BCrypt 存储"],
        ["接口授权", "注解 + 方法级校验", "@PreAuthorize 按角色控制，资源接口额外校验数据归属，防止改 ID 越权"],
        ["防重放", "签名 + 时间戳 + nonce", "对外接口校验签名与 5 分钟内时间戳，nonce 放 Redis 去重"],
        ["幂等", "幂等键 Idempotency-Key", "创建行程、下单类接口按幂等键返回同一结果，避免重复提交产生多份行程"],
        ["限流", "Redis + 令牌桶", "按用户与 IP 限流；大模型调用、导出、外部数据源查询单独设配额"],
        ["入参校验", "Bean Validation + 白名单", "所有入参在 Controller 层校验，枚举类字段只接受白名单值，禁止拼接 SQL"],
        ["异常处理", "统一异常处理 + 错误码", "对外只返回业务码与提示，不返回堆栈、SQL 和内部类名"],
        ["审计日志", "关键操作留痕", "记录操作人、时间、对象、结果与请求 ID，敏感字段脱敏后入库"],
    ])

    add_heading(doc, "3.4 日志与可观测性", 2)
    add_bullets(doc, [
        "门面统一 SLF4J：业务代码只依赖 SLF4J，不直接调用具体实现类，替换日志实现不影响业务代码。",
        "实现默认 Logback（与 Spring Boot 默认一致）；需要异步日志或更高吞吐时替换为 Log4j2，版本必须 2.17 以上，禁止引入 Log4j 1.x。",
        "日志格式固定包含时间、级别、线程、请求 ID 与消息；请求 ID 由过滤器生成并透传到前端，便于前后端联调定位。",
        "敏感信息治理：手机号、身份证、地址在日志中脱敏，密钥与 token 禁止打印。",
        "可观测性预留 Micrometer + Actuator：暴露健康检查与指标端点（仅内网或加鉴权），后续接 Prometheus + Grafana。",
    ])
    add_code(doc, """
%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] [%X{traceId}] %logger{36} - %msg%n
""")

    add_heading(doc, "3.5 持久层与数据库变更", 2)
    doc.add_paragraph("持久层采用 MyBatis-Plus + Flyway：前者负责单表 CRUD、分页与代码量控制，后者负责表结构版本管理，两者边界清晰。")
    add_bullets(doc, [
        "MyBatis-Plus：实体与 Mapper 集中在 infrastructure 层，应用服务只依赖领域接口；复杂查询写 XML，不做字符串拼接 SQL。",
        "Flyway：建表与数据变更以 V{版本}__{说明}.sql 放在 src/main/resources/db/migration，禁止直接在数据库手工改表。",
        "连接池：默认 HikariCP（Spring Boot 内置，性能足够）；需要 SQL 监控、慢 SQL 统计与防火墙时替换为 Druid，监控页必须加鉴权并限制来源 IP。",
        "分页统一使用 MyBatis-Plus 分页插件，接口层返回统一分页结构（page、size、total、items）。",
        "时区与字符集：数据库统一 UTC 存储，接口用 ISO-8601 时间，字符集统一 UTF-8。",
    ])
    add_code(doc, """
-- backend/src/main/resources/db/migration/V1__init.sql
CREATE TABLE IF NOT EXISTS users (
    id         BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    phone      VARCHAR(32) NOT NULL UNIQUE,
    nickname   VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
""")
    doc.add_paragraph("迁移脚本使用标准 SQL 写法（不使用 BIGSERIAL、TIMESTAMPTZ 等数据库方言），保证本地 H2 与生产 PostgreSQL 都能执行同一份脚本。")

    add_heading(doc, "3.6 搜索能力与微服务边界", 2)
    add_table(doc, ["候选技术", "第一版方案", "升级触发条件"], [
        ["Elasticsearch", "PostgreSQL 全文检索（tsvector / pg_trgm），搜索能力封装为 SearchService 接口", "文档量到百万级、需要分词相关性排序或搜索与分析共用时替换为 ES"],
        ["Spring Cloud + Nacos", "单体应用内部按包分层，模块之间只通过接口调用", "需要多团队并行、独立扩缩容、灰度发布或集中配置管理时拆分服务"],
        ["RabbitMQ", "第一版就引入，承担导出、通知与异步查询", "吞吐或顺序性要求显著提升时再评估 Kafka，不提前替换"],
        ["MinIO / OSS", "第一版就引入，导出文件用签名 URL 下发", "需要 CDN 加速或云端存储时替换为 OSS / S3，接口保持不变"],
    ])
    doc.add_paragraph("结论：第一版不为微服务而微服务。跨模块调用统一按接口与 REST 契约设计，后续拆分服务时业务代码不需要重写。")

    add_heading(doc, "3.7 前端技术栈与工具链", 2)
    add_bullets(doc, [
        "保留 Vue 3 + TypeScript：与 Vite、Pinia、axios 组合已覆盖页面、状态与请求，不替换为 React。",
        "数据可视化用 ECharts：企业后台统计图表与数据大屏统一使用，图表数据由后端聚合接口提供。",
        "测试：Vitest + Vue Test Utils 做组件与工具函数单元测试，覆盖率作为流水线门禁；端到端测试预留 Playwright。",
        "质量门禁：ESLint 9 + Prettier 统一风格，提交前用 husky + lint-staged 只校验改动文件，流水线跑全量校验与构建。",
        "应用安全：配置 CSP 响应头、默认转义防 XSS、跨站请求按同源加 token 防护、定期执行依赖漏洞扫描，密钥与模型 API Key 一律留在后端。",
    ])
    add_table(doc, ["命令", "作用", "说明"], [
        ["npm run dev", "开发服务器", "Vite 代理 /api 到后端 8080"],
        ["npm run build", "生产构建", "先做类型检查再打包静态资源"],
        ["npm run lint", "代码校验", "ESLint + Prettier"],
        ["npm run test", "单元测试", "Vitest 运行一次（CI 使用）"],
        ["npm run test:watch", "开发期测试", "监听模式，改动即跑"],
        ["npm run coverage", "覆盖率报告", "输出覆盖率，供流水线门禁使用"],
    ])

    add_heading(doc, "3.8 与脚手架 scaffold-cli 的对应关系", 2)
    doc.add_paragraph("上述选型已经固化到脚手架中：依赖 id 决定引入哪些依赖，以及生成哪些配置与示例代码，团队新建项目不需要再手工挑版本、抄配置。")
    add_table(doc, ["依赖 id", "引入内容", "生成的代码与配置"], [
        ["spring-boot-web", "Spring Boot Web + 内嵌 Tomcat", "启动类、/api/v1/health 接口、统一响应、CORS 配置、application.yml"],
        ["mybatis-plus", "MyBatis-Plus 3.5.x", "持久层依赖与配置，Mapper 扫描入口"],
        ["flyway", "Flyway 迁移工具", "db/migration 目录与首个迁移脚本"],
        ["postgresql / h2", "数据库驱动", "application.yml 数据源配置（H2 便于本地启动与测试）"],
        ["springdoc", "SpringDoc OpenAPI", "Swagger UI 接口文档 /swagger-ui.html"],
        ["druid", "Druid 连接池（替换 HikariCP）", "连接池与监控端点配置"],
        ["log4j2", "Log4j2 日志实现（替换 Logback）", "log4j2-spring.xml 日志配置"],
        ["spring-boot-security", "Spring Security", "安全配置骨架：健康检查放行，其余接口需认证"],
        ["websocket", "Spring WebSocket(STOMP)", "消息代理与推送端点"],
        ["redis / rabbitmq / actuator", "缓存、消息队列、健康检查", "对应自动配置与开关"],
        ["echarts", "ECharts 图表组件", "图表组件封装与示例页面"],
        ["stomp", "@stomp/stompjs 客户端", "WebSocket 连接与订阅封装"],
        ["vitest", "Vitest + Vue Test Utils", "vitest.config.ts、示例测试、test 与 coverage 命令"],
    ])
    add_code(doc, """
# 按本章选型生成前后端分离骨架
scaffold-cli init travel-planner --type fullstack --java 21 ^
  --deps spring-boot-web,mybatis-plus,flyway,postgresql,h2,springdoc,junit,logback ^
  --frontend-deps vue,vue-router,pinia,axios,echarts,stomp
""")

    add_heading(doc, "4. 功能模块设计", 1)
    add_heading(doc, "4.1 用户与对话模块", 2)
    add_bullets(doc, [
        "支持注册、登录、游客体验和第三方登录扩展。",
        "保存用户历史行程、常用出发地和旅行偏好。",
        "保存多轮对话消息，并关联当前旅行需求和行程版本。",
        "支持 SSE 流式输出 Agent 进度。",
    ])

    add_heading(doc, "4.2 旅行需求解析", 2)
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

    add_heading(doc, "4.3 Agent 编排模块", 2)
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

    add_heading(doc, "4.4 旅行工具层", 2)
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

    add_heading(doc, "5. 行程生成与优化", 1)
    add_heading(doc, "5.1 规划流程", 2)
    add_numbered(doc, [
        "解析用户需求并检查必要字段。",
        "并行查询景点、交通、酒店、天气和餐饮数据。",
        "按照城市和地理位置生成候选路线。",
        "根据开放时间、游玩时长、交通时间和排队时间排期。",
        "计算交通、住宿、门票、餐饮和其他费用。",
        "校验预算、景点冲突、预约条件和每日强度。",
        "生成结构化行程并由大模型转化为易读的旅行方案。",
    ])

    add_heading(doc, "5.2 路线优化规则", 2)
    add_bullets(doc, [
        "优先将地理位置相近的景点安排在同一天。",
        "避免跨城市往返和无意义的重复换乘。",
        "检查景点开放时间、预约时间和景区末班交通。",
        "为交通、排队、用餐和休息预留缓冲时间。",
        "控制每日游玩时长，匹配休闲、亲子、徒步或摄影风格。",
        "根据天气将户外景点替换为室内景点或备用方案。",
        "用户修改景点或酒店后，只重新计算受影响的日期和路线。",
    ])

    add_heading(doc, "5.3 行程模型", 2)
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

    add_heading(doc, "6. 核心 Java 服务设计", 1)
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

    add_heading(doc, "7. 多轮对话和行程编辑", 1)
    doc.add_paragraph("用户提出“加一天丽江”“不要购物”“酒店换成靠近古城的”等要求时，系统先识别修改意图，再更新结构化需求或行程版本，并仅重新查询受影响的数据。")
    add_table(doc, ["操作类型", "示例", "处理方式"], [
        ["ADD_ATTRACTION", "增加玉龙雪山", "插入行程并重新检查当天时间"],
        ["REMOVE_ATTRACTION", "不要购物", "删除购物点并重新安排餐饮或景点"],
        ["REPLACE_HOTEL", "换成古城附近酒店", "按位置和预算重新筛选住宿"],
        ["CHANGE_DAYS", "加一天丽江", "扩展日期并重算交通和住宿"],
        ["CHANGE_BUDGET", "预算改为 6000", "重新筛选交通、酒店和景点组合"],
        ["REORDER_ITEMS", "把古城放到晚上", "调整顺序并校验开放时间"],
    ])

    add_heading(doc, "8. 接口设计", 1)
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
    doc.add_paragraph("以上接口全部由后端提供、前端调用，是前后端分离后的唯一交互面：接口一律返回 JSON，不使用服务端渲染页面，前端按第 2.4 节的统一响应结构解析。")
    add_bullets(doc, [
        "路径统一带版本前缀（/api/v1），便于前后端独立发版和灰度发布。",
        "登录接口返回 JWT，前端后续请求统一携带 Authorization 请求头；后端接口做权限校验和数据归属校验。",
        "SSE 接口（/stream）把 Agent 生成进度实时推给前端，前端断线后可凭 tripId 重新订阅。",
        "导出接口返回文件标识或下载地址，由前端触发下载；后端不拼接任何前端页面。",
    ])

    add_heading(doc, "9. 数据库设计", 1)
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

    add_heading(doc, "10. Word、PDF 和图片导出", 1)
    doc.add_paragraph("系统可以将结构化 Itinerary 转换为正式旅行行程单。Word 导出适合编辑和二次分发，PDF 适合打印，图片适合移动端分享。")
    add_bullets(doc, [
        "Word：使用 docx4j 或 Apache POI 生成标题、表格、页眉页脚和图片。",
        "PDF：使用 HTML 模板转 PDF，或直接使用 OpenPDF、iText 等方案。",
        "图片：使用 HTML 渲染截图或服务端图片排版。",
        "微信长文：输出带标题、分段、地图地址和注意事项的 HTML 或 Markdown。",
    ])
    doc.add_paragraph("导出的行程建议包含：旅行概览、每日时间表、酒店地址、交通方式、餐饮建议、预算明细、预约方式、天气提醒、注意事项、紧急电话和数据更新时间。")

    add_heading(doc, "11. 企业后台设计", 1)
    add_bullets(doc, [
        "线路库管理：维护目的地、主题线路、景点组合和推荐文案。",
        "Agent 配置：维护高端、平价、亲子、情侣等风格的 Prompt 和规则。",
        "内容审核：人工审核景点、酒店、餐厅和风险提示。",
        "数据统计：咨询量、行程生成量、导出量、用户留存和转化率。",
        "操作审计：记录管理员修改、数据源异常和 Agent 失败信息。",
    ])

    add_heading(doc, "12. 安全、稳定性与合规", 1)
    add_bullets(doc, [
        "使用 Spring Security 进行身份认证和接口授权，企业后台采用角色权限控制。",
        "对用户手机号、行程地址等敏感信息进行脱敏和访问控制。",
        "外部接口设置超时、重试、熔断、限流和缓存机制。",
        "对大模型输出执行 JSON Schema 校验，禁止未经校验直接写入核心业务表。",
        "所有实时事实数据标注来源和更新时间，不把模型生成内容当作实时事实。",
        "对航班、酒店和门票价格明确提示以实际预订页面为准。",
    ])

    add_heading(doc, "13. 部署、容器化与 CI/CD", 1)
    doc.add_paragraph("前后端分离之后，两端各自产出独立的部署物：后端是可执行 jar 或容器镜像，前端是静态资源。二者通过 Nginx 组合成同一个对外域名，接口统一走 /api 前缀。")

    add_heading(doc, "13.1 部署拓扑", 2)
    add_code(doc, """
用户浏览器
   │  HTTPS（443）
   ▼
Nginx / CDN
   ├── /            → 前端静态资源（dist）
   └── /api, /ws    → 后端服务（8080，可多实例负载均衡）
                          │
                          ├── PostgreSQL（业务库 + 备份）
                          ├── Redis（缓存、限流、会话）
                          ├── RabbitMQ（异步任务）
                          └── MinIO / OSS（导出文件）
""")

    add_heading(doc, "13.2 容器化", 2)
    doc.add_paragraph("后端与前端各写一份 Dockerfile，本地用 Docker Compose 起依赖组件；镜像里不包含任何密钥与环境差异。")
    add_code(doc, """
# backend/Dockerfile（多阶段构建）
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
""")
    add_code(doc, """
# frontend/Dockerfile（静态资源交给 Nginx）
FROM node:22-alpine AS build
WORKDIR /build
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
""")
    add_code(doc, """
# docker-compose.yml（本地开发依赖；后端与前端仍可本地启动）
services:
  postgres:
    image: postgis/postgis:16-3.4
    environment: [POSTGRES_DB=travel, POSTGRES_USER=travel, POSTGRES_PASSWORD=travel]
    ports: ["5432:5432"]
    volumes: ["pgdata:/var/lib/postgresql/data"]
  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
  rabbitmq:
    image: rabbitmq:3.13-management
    ports: ["5672:5672", "15672:15672"]
  minio:
    image: minio/minio
    command: server /data --console-address ":9001"
    ports: ["9000:9000", "9001:9001"]
volumes:
  pgdata:
""")

    add_heading(doc, "13.3 环境与配置管理", 2)
    add_table(doc, ["环境", "后端配置", "前端配置", "密钥来源"], [
        ["本地开发", "application-local.yml + H2 或本地 Compose 依赖", "VITE_API_BASE_URL 走 Vite 代理", ".env.local（不入库）"],
        ["测试环境", "application-test.yml + 容器内服务名", "构建时注入 VITE_API_BASE_URL", "CI 变量 / 密钥管理服务"],
        ["生产环境", "application-prod.yml + 外置配置", "dist + CDN 版本号", "密钥管理服务，禁止写进镜像"],
    ])
    add_bullets(doc, [
        "镜像里不打包密钥：数据库口令、模型 API Key、地图与酒店 Key 一律由环境变量或密钥服务注入。",
        "前端只保留构建期变量（VITE_ 前缀），敏感信息不进入前端产物。",
        "同一份镜像靠环境变量区分环境，禁止为每个环境单独改代码重新构建。",
    ])

    add_heading(doc, "13.4 持续集成流水线", 2)
    add_table(doc, ["阶段", "后端（backend/）", "前端（frontend/）"], [
        ["代码检出", "checkout + 缓存 Maven 仓库", "checkout + 缓存 npm"],
        ["依赖安装", "mvn -B dependency:go-offline", "npm ci"],
        ["质量校验", "mvn -B -Pquality verify（Checkstyle）", "npm run lint"],
        ["测试", "mvn -B test（单元 + 集成）", "npm run test -- --coverage"],
        ["构建", "mvn -B clean package（可执行 jar）", "npm run build（dist 静态资源）"],
        ["制品与镜像", "后端镜像打版本号推送到镜像仓库", "dist 上传 CDN 或打包前端镜像"],
        ["部署", "按环境发布，健康检查通过再切流量", "静态资源发布，支持按版本回滚"],
    ])
    add_code(doc, """
# .github/workflows/ci.yml（仓库托管在 GitHub 时使用）
name: ci
on:
  push: { branches: [ main ] }
  pull_request:
jobs:
  backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '21', cache: maven }
      - run: mvn -B -f backend/pom.xml verify
  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '22', cache: npm, cache-dependency-path: frontend/package-lock.json }
      - run: npm ci --prefix frontend
      - run: npm run lint --prefix frontend
      - run: npm run test --prefix frontend
      - run: npm run build --prefix frontend
""")
    doc.add_paragraph("企业内部已有 Jenkins 时，把上表阶段映射成 Jenkinsfile 的 stage 即可，命令与镜像完全复用；CI 中必须包含 Flyway 迁移检查，避免生产环境执行未评审的表结构变更。")

    add_heading(doc, "13.5 发布、回滚与运行保障", 2)
    add_bullets(doc, [
        "发布顺序：接口保持向后兼容时后端先发布、前端随后发布；破坏性变更先上 v2 接口，再切换前端。",
        "回滚：后端保留上一个镜像版本，前端保留上一个静态版本，回滚不需要重新构建。",
        "数据库变更：Flyway 脚本只向前执行，删列或改类型按“先加新列、再迁数据、最后删旧列”三步走，保证可回滚。",
        "健康检查与观测：/actuator/health 作为容器健康检查，接 Prometheus + Grafana 观察接口耗时、错误率、大模型调用与外部数据源成功率。",
        "容量与限流：对外部数据源与大模型调用设配额与熔断，避免单次规划触发过多外部请求拖垮服务。",
    ])

    add_heading(doc, "14. MVP 实施范围", 1)
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

    add_heading(doc, "15. 开发计划", 1)
    add_table(doc, ["阶段", "主要工作", "交付结果"], [
        ["阶段一：基础工程", "backend 与 frontend 两端工程、REST 契约、MyBatis-Plus + Flyway、Docker 镜像与 CI 流水线、认证", "可独立构建、独立部署、可自动构建发布的前后端分离骨架"],
        ["阶段二：AI 对话", "模型接入、结构化解析、多轮会话、SSE", "可对话的旅行需求入口"],
        ["阶段三：旅行工具", "景点、地图、天气、酒店和餐饮接口", "可获取实时旅行数据"],
        ["阶段四：行程引擎", "路线、时间、预算和冲突校验", "可用的自动规划结果"],
        ["阶段五：编辑导出", "增删替换、重新优化、Word/PDF 导出", "完整用户闭环"],
        ["阶段六：企业后台", "线路库、Prompt、统计和审核", "旅行社或企业版能力"],
        ["阶段七：上线保障", "镜像仓库、环境与密钥管理、监控告警、容量与限流", "具备上线与快速回滚能力的运行环境"],
    ])

    add_heading(doc, "16. 结论与实施建议", 1)
    doc.add_paragraph("本项目建议采用 Java 作为主开发语言，以 Spring Boot 构建稳定的业务后端，以 Spring AI 负责大模型接入和 Agent 工具调用，以 PostgreSQL、Redis 和外部旅行数据服务支撑实时规划。")
    doc.add_paragraph("系统设计应坚持以下分工：")
    add_bullets(doc, [
        "大模型负责自然语言理解、方案解释、用户沟通和推荐理由生成。",
        "Java 程序负责路线计算、预算计算、开放时间检查、状态管理和权限控制。",
        "外部 API 负责天气、地图、交通、酒店、门票和餐饮等实时事实数据。",
        "数据库负责保存行程版本、工具调用记录、数据来源和导出文件。",
        "前后端分离：前端只负责界面与交互，后端只负责业务与数据，两端以 REST 接口契约协作，独立构建、独立部署。",
        "交付方式：后端容器镜像 + 前端静态产物，通过 Docker 与 CI 流水线实现可重复发布与快速回滚。",
    ])
    doc.add_paragraph("先完成单目的地规划和 Word/PDF 导出，再逐步扩展到多目的地、自驾、跨境和企业旅行社场景，可以有效控制研发风险并快速验证产品价值。")

    doc.core_properties.title = "旅游规划智能体系统 Java 技术方案"
    doc.core_properties.subject = "基于 Spring Boot 和 Spring AI 的旅游规划智能体系统"
    doc.core_properties.author = "Codex"
    doc.core_properties.keywords = "Java, Spring Boot, Spring AI, Agent, MyBatis-Plus, Flyway, Docker, 旅游规划, 前后端分离"
    doc.save(OUTPUT)


if __name__ == "__main__":
    build_document()
    print(OUTPUT)
