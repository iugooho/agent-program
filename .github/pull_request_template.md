## 改了什么

<!-- 一两句说清目的，不要只贴文件列表 -->

## 类型

- [ ] 业务功能（`travel-planner/`）
- [ ] 接口契约变更（改了 DTO / Controller，需同步 `docs/openapi.json` 与 `schema.ts`）
- [ ] 数据库迁移（新增 `db/migration/` 脚本）
- [ ] 脚手架 / 模板（`scaffold-cli/`）
- [ ] 规范 / CI（`.github/`、`.editorconfig`、`AGENTS.md`）
- [ ] 纯文档

## 自检

- [ ] 后端：`mvn -B -f travel-planner/backend/pom.xml -Pquality verify`
- [ ] 前端：`npm --prefix travel-planner/frontend run lint:ci`、`run test`、`run build`
- [ ] 脚手架：`mvn -B -f scaffold-cli/pom.xml clean verify`（改了 `scaffold-cli/` 才需要）
- [ ] 改了接口：已执行 `test -Dopenapi.write=true` 与 `npm run gen:api`，后端代码 / `docs/openapi.json` / `src/api/schema.ts` 在同一个提交里

## 影响面

<!-- 有破坏性接口变更吗？需要前端同步发版吗？迁移脚本可以回滚吗？没有就写「无」 -->

## 关联 issue

<!-- Closes #123 -->
