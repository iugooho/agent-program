/**
 * 从 docs/openapi.json 生成 src/api/schema.ts。
 *
 * 契约（docs/openapi.json）是前后端唯一可信源：
 *   后端改接口 → mvn test -Dopenapi.write=true 更新契约 → npm run gen:api 刷新类型
 * CI 会重新执行本脚本并比对，生成结果与提交内容不一致就失败，所以 schema.ts 不要手改。
 *
 * 只依赖 Node 内置模块，不需要安装任何依赖。
 */
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const here = dirname(fileURLToPath(import.meta.url))
const contractPath = resolve(here, '..', '..', 'docs', 'openapi.json')
const outputPath = resolve(here, '..', 'src', 'api', 'schema.ts')

const contract = JSON.parse(readFileSync(contractPath, 'utf8'))
const schemas = contract.components?.schemas ?? {}

const IDENTIFIER = /^[A-Za-z_$][A-Za-z0-9_$]*$/

/** 联合类型的成员去重后拼接。 */
function union(parts) {
  const types = [...new Set(parts.map((part) => toTs(part)))]
  return types.length === 1 ? types[0] : types.join(' | ')
}

/** 数组元素等位置加括号，避免 `a | b[]` 这种歧义。 */
function wrap(type) {
  return /[ |]/.test(type) ? `(${type})` : type
}

/** OpenAPI schema → TypeScript 类型表达式。 */
function toTs(schema) {
  if (!schema) return 'unknown'
  if (schema.$ref) return schema.$ref.split('/').pop()
  if (schema.oneOf) return union(schema.oneOf)
  if (schema.anyOf) return union(schema.anyOf)
  if (schema.allOf) return schema.allOf.map((part) => wrap(toTs(part))).join(' & ')
  if (Array.isArray(schema.enum))
    return schema.enum.map((value) => JSON.stringify(value)).join(' | ')
  if (Array.isArray(schema.type)) {
    const variants = schema.type.map((type) =>
      type === 'null' ? 'null' : toTs({ ...schema, type })
    )
    return [...new Set(variants)].join(' | ')
  }
  switch (schema.type) {
    case 'string':
      return 'string'
    case 'integer':
    case 'number':
      return 'number'
    case 'boolean':
      return 'boolean'
    case 'array':
      return `${wrap(toTs(schema.items))}[]`
    case 'object':
      return schema.additionalProperties
        ? `Record<string, ${toTs(schema.additionalProperties)}>`
        : 'Record<string, unknown>'
    default:
      return 'unknown'
  }
}

/** 单个 schema → 一条 TypeScript 声明。 */
function declaration(name, schema) {
  if (schema.type === 'object' && schema.properties) {
    const required = new Set(schema.required ?? [])
    const fields = Object.entries(schema.properties).map(([key, value]) => {
      const marker = required.has(key) ? '' : '?'
      const field = IDENTIFIER.test(key) ? key : JSON.stringify(key)
      return `  ${field}${marker}: ${toTs(value)}`
    })
    return `export interface ${name} {\n${fields.join('\n')}\n}`
  }
  return `export type ${name} = ${toTs(schema)}`
}

const names = Object.keys(schemas).sort()
const header = [
  '/**',
  ' * 本文件由 scripts/gen-api-types.mjs 从 docs/openapi.json 自动生成，请勿手动修改。',
  ' * 重新生成：npm run gen:api',
  ' */',
  ''
].join('\n')
const body = names.map((name) => declaration(name, schemas[name])).join('\n\n')

mkdirSync(dirname(outputPath), { recursive: true })
writeFileSync(outputPath, `${header}${body}\n`, 'utf8')
console.log(`已从 ${contractPath} 生成 ${names.length} 个类型到 ${outputPath}`)
