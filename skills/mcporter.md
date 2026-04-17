# MCPorter 🧳 - MCP 工具调用助手

## 简介

MCPorter 是一个 TypeScript 运行时、CLI 工具和代码生成工具包，用于调用 Model Context Protocol (MCP) 服务器。

## 核心能力

### 1. MCP 服务器发现与调用
- **零配置发现**: 自动合并 `~/.mcporter/mcporter.jsonc`、`config/mcporter.json` 以及 Cursor/Claude/Codex/Windsurf/OpenCode/VS Code 的 MCP 配置
- **一键 CLI 生成**: `mcporter generate-cli` 将任何 MCP 服务器定义转换为可运行的 CLI
- **类型化工具客户端**: `mcporter emit-ts` 发出 `.d.ts` 接口或可直接运行的客户端包装器

### 2. 友好的组合 API
- `createServerProxy()` 将工具公开为符合人体工程学的 camelCase 方法
- 自动应用 JSON-schema 默认值、验证必填参数
- 返回 `CallResult` 对象，支持 `.text()`、`.markdown()`、`.json()`、`.images()`、`.content()` 等方法

### 3. 支持多种传输协议
- HTTP/HTTPS
- SSE (Server-Sent Events)
- STDIO
- OAuth 认证

## 常用命令

### 列出所有 MCP 服务器
```bash
npx mcporter list
npx mcporter list context7 --schema
npx mcporter list --http-url https://mcp.linear.app/mcp --all-parameters
```

### 调用 MCP 工具
```bash
# 冒号分隔格式（shell 友好）
npx mcporter call linear.create_comment issueId:ENG-123 body:'Looks good!'

# 函数调用风格
npx mcporter call 'linear.create_comment(issueId: "ENG-123", body: "Looks good!")'

# Context7 示例
npx mcporter call context7.resolve-library-id libraryName:react
npx mcporter call context7.get-library-docs context7CompatibleLibraryID:/websites/react_dev topic:hooks
```

### 生成独立的 CLI
```bash
npx mcporter generate-cli --command https://mcp.context7.com/mcp

# 生成 TypeScript 模板
#   context7.ts        (TypeScript 模板，包含嵌入的 schemas)
#   context7.js        (通过 Rolldown 或 Bun 打包的 CLI)
```

### 生成类型化客户端
```bash
# 仅类型接口
npx mcporter emit-ts linear --out types/linear-tools.d.ts

# 客户端包装器
npx mcporter emit-ts linear --mode client --out clients/linear.ts
```

### 管理 MCP 配置
```bash
mcporter config list
mcporter config add <name> <url>
mcporter config remove <name>
mcporter config import cursor --copy
```

## 工具列表

MCPorter 可调用的 MCP 服务器包括但不限于：

| 服务器 | 描述 |
|--------|------|
| `context7` | Context7 文档 MCP，获取最新库文档 |
| `linear` | Linear MCP，问题追踪和工作流 |
| `firecrawl` | 网站抓取为 Markdown |
| `chrome-devtools` | Chrome 开发者工具 |
| `shadcn.io` | shadcn/ui 组件查询 |
| `vercel` | Vercel 部署和文档 |
| `xcode-build` | Xcode 构建工具 |

## AI 助手可用工具

本项目的 AI 助手已内置以下 MCPorter 相关工具，无需安装：

### 直接调用工具

| 工具名称 | 功能 | 示例 |
|----------|------|------|
| `mcporter_list` | 列出所有 MCP 服务器 | `mcporter_list(null)` 或 `mcporter_list("context7")` |
| `mcporter_call` | 调用 MCP 工具 | `mcporter_call("context7.resolve_library_id libraryName:react")` |
| `mcporter_generate_cli` | 生成独立 CLI | `mcporter_generate_cli("https://mcp.example.com/mcp", null)` |
| `mcporter_emit_ts` | 生成 TypeScript 类型 | `mcporter_emit_ts("context7", "types/context7.d.ts")` |
| `execute_command` | 执行任意 shell 命令 | `execute_command("npx mcporter list")` |

### 使用示例

```
# 列出所有 MCP 服务器
mcporter_list(null)

# 查看特定服务器的工具
mcporter_list("context7")

# 调用 MCP 工具
mcporter_call("context7.resolve_library_id libraryName:react")

# 函数调用风格
mcporter_call("context7.get_library_docs context7CompatibleLibraryID:/websites/react_dev topic:hooks")
```

### 自托管 MCP 服务器

如果需要让 AI 助手调用本项目的 MCP 服务，可以在 MCPorter 配置中添加：

```json
{
  "mcpServers": {
    "orgperformance": {
      "description": "组织绩效管理系统 MCP",
      "baseUrl": "http://localhost:8080/mcp"
    }
  }
}
```

然后 AI 可以用 `mcporter_call("orgperformance.list_systems")` 调用本系统的 MCP 工具。

### 安装
```bash
pnpm add mcporter
# 或
npm install mcporter
```

### 在代码中使用
```typescript
import { callOnce } from "mcporter";

const result = await callOnce({
    server: "context7",
    toolName: "resolve-library-id",
    args: { libraryName: "react" },
});
```

### 使用 Runtime 组合自动化
```typescript
import { createRuntime, createServerProxy } from "mcporter";

const runtime = await createRuntime();
const context7 = createServerProxy(runtime, "context7");

const docs = await context7.get_library_docs({
    context7CompatibleLibraryID: "/websites/react_dev",
    topic: "hooks"
});
console.log(docs.markdown());

await runtime.close();
```

## 配置文件格式

```json
{
    "mcpServers": {
        "context7": {
            "description": "Context7 docs MCP",
            "baseUrl": "https://mcp.context7.com/mcp",
            "headers": {
                "Authorization": "$env:CONTEXT7_API_KEY"
            }
        },
        "chrome-devtools": {
            "command": "npx",
            "args": ["-y", "chrome-devtools-mcp@latest"],
            "env": { "npm_config_loglevel": "error" }
        }
    },
    "imports": ["cursor", "claude-code", "claude-desktop", "codex", "windsurf", "opencode", "vscode"]
}
```

## 最佳实践

1. **使用函数调用风格**: 对于复杂参数，使用 `'server.tool(arg1: "value1", arg2: "value2")'` 格式
2. **利用自动补全**: MCPorter 会自动修正拼写错误，提供 "Did you mean...?" 提示
3. **配置保持**: 对于临时测试的 MCP 服务器，使用 `--persist` 标志保存配置
4. **调试挂起**: 使用 `tmux` 运行长时间命令，配合 `MCPORTER_DEBUG_HANG=1` 获取详细诊断

## 参考链接

- GitHub: https://github.com/steipete/mcporter
- 文档: https://github.com/steipete/mcporter/blob/main/README.md
- MCP 规范: https://github.com/modelcontextprotocol/specification
