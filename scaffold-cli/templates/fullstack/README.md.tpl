# {{projectName}}

由 `scaffold-cli` 生成的全栈骨架：`{{backendDir}}/` 是 Java 后端（Spring Boot {{springBootVersion}}），
`{{frontendDir}}/` 是 Vue 3 前端（Vite），两端通过 `/api` 接口契约联调。

## 快速开始

```powershell
# 后端：编译 + 单元测试
mvn -f backend/pom.xml test

# 后端：启动服务（默认 8080）
mvn -f backend/pom.xml spring-boot:run

# 前端：安装依赖并启动开发服务器（默认 http://localhost:5173）
cd frontend
npm install
npm run dev
```

Windows 下一键启动（后端在新窗口运行，前端在当前窗口）：

```powershell
.\scripts\dev.ps1
```

## 联调约定

- 前端开发服务器已把 `/api` 代理到 `http://localhost:8080`（见 `frontend/vite.config.ts`），后端监听 8080 即可。
- 后端健康检查：{{healthEndpoint}}；前端封装在 `frontend/src/api/health.ts`。
- 后端 artifactId 为 `{{backendArtifactId}}`，前端包名为 `{{frontendArtifactId}}`。
- Java 版本：{{javaVersion}}；包名：{{packageName}}。

## 各自的规范

| 位置 | 编码 | 校验命令 |
| --- | --- | --- |
| `{{backendDir}}/` | UTF-8、4 空格、JDK {{javaVersion}} | `mvn -f backend/pom.xml -Pquality verify` |
| `{{frontendDir}}/` | UTF-8、2 空格、ESLint + Prettier | `npm --prefix frontend run lint` |
| 前端测试 | Vitest + jsdom | `npm --prefix frontend run test` |

生成信息（脚手架版本、选择的依赖）见根目录 `scaffold.json`。
