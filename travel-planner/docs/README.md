# docs

本目录存放前后端共享的接口契约。

## openapi.json

`openapi.json` 是**自动生成**的接口契约，不是手写文件，也是前后端唯一的可信源：

```
backend DTO / Controller  --(springdoc /v3/api-docs)-->  docs/openapi.json  --(npm run gen:api)-->  frontend/src/api/schema.ts
        真源                                                 契约（提交进仓库）                          生成的 TS 类型（不手改）
```

- 真源是后端的 DTO 与 Controller，由 springdoc 扫描成 `/v3/api-docs`。
- `backend/src/test/java/com/travelagent/travelplanner/OpenApiContractTest.java` 负责把契约固化进仓库：
  默认把后端实际输出与 `openapi.json` 比对，加 `-Dopenapi.write=true` 则写回本文件。
- 前端 `frontend/scripts/gen-api-types.mjs` 读取本文件生成 `src/api/schema.ts` 里的 TypeScript 类型，
  只依赖 Node 内置模块，不需要装任何依赖。

## 改接口的完整流程

```powershell
# 1. 改后端 DTO / Controller（真源）
# 2. 让契约测试把最新结构写进 docs/openapi.json
mvn -f backend/pom.xml test -Dopenapi.write=true

# 3. 前端重新生成类型
npm --prefix frontend run gen:api

# 4. 本地自检
mvn -f backend/pom.xml -Pquality verify
npm --prefix frontend run lint:ci
npm --prefix frontend run build
```

第 4 步通过后，把「后端代码 + `docs/openapi.json` + `frontend/src/api/schema.ts`」放进**同一个提交**。

## 两道防线

- `OpenApiContractTest`：后端实际输出的 OpenAPI 与已提交的契约不一致就失败，普通 `mvn test` 就会跑到，
  报错信息里带合约路径。所以「改了后端接口忘了更新契约」在同一个 PR 内就会被拦下。
- CI 前端 job：重新执行 `npm run gen:api` 后比对 `src/api/schema.ts`，与契约对不上就失败，
  覆盖「契约更新了但前端类型忘了生成」。

## 注意

- `openapi.json` 与 `frontend/src/api/schema.ts` 都不要手改，下次生成会覆盖。
- 契约里出现非预期的差异（字段改名、必填性变化）说明后端 DTO 被改了，先确认这是有意为之再提交。
- 契约里目前的 schema 只有健康检查与 demo 这两个接口的结果类型，新增模块后 schema 会自动变多，
  前端同步 `npm run gen:api` 即可。
