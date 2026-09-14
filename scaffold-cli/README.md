# scaffold-cli 项目脚手架生成器

一个纯 Java 实现的命令行脚手架生成器：**一条命令生成统一规范的项目骨架**，团队成员新建项目不再各写各的。

支持三种类型：

| 类型 | 生成内容 | 默认依赖 |
| --- | --- | --- |
| `backend` | Maven 工程（src/main/java、src/main/resources、src/test/java、pom.xml、Checkstyle、EditorConfig、命令脚本） | spring-core、junit、logback |
| `frontend` | Vue 3 + TypeScript + Vite 工程（package.json、tsconfig.json、ESLint、Prettier、路由、代理） | vue、vue-router |
| `fullstack` | backend/ + frontend/ + 一键启动脚本 | 上面两者 |

## 快速开始

```powershell
# 首次运行会自动 mvn package
.\scaffold.ps1 list
.\scaffold.ps1 init my-service --type backend --group com.acme
.\scaffold.ps1 init my-web --type frontend --frontend-deps vue,vue-router,pinia
.\scaffold.ps1 init my-platform --type fullstack --deps spring-core,junit,logback,jackson
```

也可以直接跑 jar：

```bash
mvn -B package
java -jar target/scaffold-cli-1.0.0.jar init my-app --type backend
```

生成后立刻可用：

```bash
cd my-service
mvn compile            # 编译
mvn test               # 单元测试（脚手架自带冒烟测试）
mvn exec:java          # 运行 App
mvn -Pquality verify   # Checkstyle 代码规范检查
```

```bash
cd my-web
npm install
npm run dev            # 开发服务器
npm run build          # 生产构建
npm run lint           # ESLint + Prettier
```

## 命令与选项

```
scaffold-cli init <项目名> [选项]
scaffold-cli list
```

| 选项 | 默认值 | 说明 |
| --- | --- | --- |
| -t, --type | backend | backend / frontend / fullstack |
| -g, --group | com.example | Maven groupId |
| -a, --artifact | 项目名 | Maven artifactId |
| -p, --package | 由 groupId + 项目名推导 | Java 包名 |
| -v, --version | 1.0.0-SNAPSHOT | 项目版本 |
| -j, --java | 21 | 编译目标 JDK（17~25） |
| -d, --deps | spring-core,junit,logback | Java 依赖，可选值见 scaffold-cli list |
| -f, --frontend-deps | vue,vue-router | 前端依赖，可选 vue / vue-router / pinia / axios |
| -o, --output | 当前目录 | 输出目录 |
| --templates | 自动查找 | 自定义模板目录 |
| --description | 自动生成 | 项目描述 |
| --force | 关闭 | 目标目录非空时覆盖 |
| --dry-run | 关闭 | 只列出将要生成的文件 |
| -i, --interactive | 终端下自动开启 | 交互式问答选择类型与依赖 |

不带参数运行会打印帮助；`list` 列出所有可选类型与依赖。在 CI、管道等没有终端的环境里会自动跳过问答、使用默认值。

## 生成的骨架里有什么

以 backend 为例：

```
my-service/
├── pom.xml                    # 统一 UTF-8 编码、JDK 21、依赖版本集中在 properties
├── scaffold.json              # 记录生成器版本、类型、依赖，便于追溯初始结构
├── .editorconfig              # 缩进 / 换行 / 编码约定
├── .gitignore
├── config/checkstyle.xml      # Java 代码校验规则
├── scripts/run.ps1|run.cmd    # 编译并运行
└── src
    ├── main/java/com/acme/myservice/App.java
    ├── main/resources/logback.xml
    └── test/java/com/acme/myservice/AppTest.java
```

frontend 会生成 package.json（含 dev / build / lint / format 命令）、tsconfig.json、vite.config.ts（@ 别名 + /api 代理到 8080）、eslint.config.js、.prettierrc.json、src/router、src/views 以及 scripts/dev.cmd。

fullstack 额外生成 backend/、frontend/、根 README.md、统一 .editorconfig 和 scripts/dev.ps1（后端新窗口启动 + 前端开发服务器）。

## 统一规范做了什么

1. 编码与换行：所有文本文件 UTF-8 + LF，.editorconfig 按语言规定缩进（Java 4 空格、前端 2 空格）。
2. JDK 版本：maven.compiler.release 与 java.version 统一写入 pom.xml，编译参数固定带 -parameters -Xlint:all。
3. 代码校验：后端 mvn -Pquality verify 走 Checkstyle（config/checkstyle.xml）；前端 npm run lint 走 ESLint 9 扁平配置 + Prettier。
4. 依赖治理：版本号只写在 properties 里，依赖通过 catalog 统一注入，避免每人各写一个版本。
5. 命令统一：mvn compile / mvn test / npm run dev / npm run build 开箱即用，另有 scripts/ 下的封装脚本。
6. 可追溯：生成的 scaffold.json 记录生成器版本与本次选择的依赖。

## 扩展方式

加一个 Java 依赖：在 src/main/java/com/example/scaffold/core/DependencyCatalog.java 的 static 块里 register(...) 一行，随后即可用 --deps 选择。

加一个前端依赖：在 FrontendDependencyCatalog.java 里 runtime(...) 一行。

改模板：直接编辑 templates/ 下的文件。*.tpl 会被渲染并去掉后缀，其它文件原样拷贝；文件与目录名里的 {{key}} 同样会被替换（例如 src/main/java/{{packagePath}}/App.java.tpl）。占位符取值见 ScaffoldEngine#buildValues。

加一种项目类型：在 ProjectType 增加枚举值 + templates/<类型>/ 目录，并在 ScaffoldEngine#mounts 里登记挂载位置。

## 开发者命令

```bash
mvn test        # 运行生成器自身的单元测试
mvn -B clean package  # 打包，同时把 templates/ 复制到 target/templates
```

改动或重命名 `templates/` 下的文件后，请用 `mvn clean package`（`clean` 会清掉 `target/templates` 里的旧模板，避免新旧文件同时被生成）。

模板目录查找顺序：--templates > 系统属性 scaffold.templates > 环境变量 SCAFFOLD_TEMPLATES > jar 同级 templates/ > 当前目录 templates/。
