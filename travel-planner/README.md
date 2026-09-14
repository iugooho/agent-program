# travel-planner

由 `scaffold-cli` 生成的全栈骨架：`backend/` 是 Java 后端，`frontend/` 是 Vue 3 前端。

## 快速开始

```powershell
# 后端：编译 + 单元测试
mvn -f backend/pom.xml test

# 后端：运行 App
mvn -f backend/pom.xml compile exec:java

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

- 前端开发服务器已把 `/api` 代理到 `http://localhost:8080`（见 `frontend/vite.config.ts`），后端直接监听 8080 即可，不需要额外配 CORS。
- 后端 artifactId 为 `travel-planner-backend`，前端包名为 `travel-planner-frontend`。
- Java 版本：21；包名：com.travelagent.travelplanner。

## 各自的规范

| 位置 | 编码 | 校验命令 |
| --- | --- | --- |
| `backend/` | UTF-8、4 空格、JDK 21 | `mvn -f backend/pom.xml -Pquality verify` |
| `frontend/` | UTF-8、2 空格、ESLint + Prettier | `npm --prefix frontend run lint` |

生成信息（脚手架版本、选择的依赖）见根目录 `scaffold.json`。
