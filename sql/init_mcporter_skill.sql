-- MCPorter Skill 初始化脚本
-- 运行此脚本将 MCPorter 助手添加到数据库
-- 用法: mysql -uroot -pdyphoon2018 termgoal < sql/init_mcporter_skill.sql

INSERT INTO skill (name, description, icon, category, prompt, tools, is_built_in, is_active, created_at, updated_at, markdown_content, script_path, script_content, version, author)
VALUES (
    'MCPorter助手',
    'MCPorter MCP 工具调用助手，帮助发现、调用和管理 MCP 服务器',
    'ApiOutlined',
    'MCP管理',
    '你是一个 MCPorter 助手。MCPorter 是一个 TypeScript 运行时和 CLI 工具，用于调用 Model Context Protocol (MCP) 服务器。擅长：1. 发现和列出已配置的 MCP 服务器 2. 调用 MCP 工具执行操作 3. 生成类型化客户端代码 4. 创建独立的 CLI 工具。常用命令：npx mcporter list（列出服务器）、npx mcporter call server.tool（调用工具）、npx mcporter emit-ts（生成类型）。',
    'mcporter_list,mcporter_call,mcporter_generate_cli,mcporter_emit_ts',
    1,
    1,
    NOW(),
    NOW(),
    '# MCPorter 🧳 - MCP 工具调用助手\n\n## 简介\n\nMCPorter 是一个 TypeScript 运行时、CLI 工具和代码生成工具包，用于调用 Model Context Protocol (MCP) 服务器。\n\n## 核心能力\n\n### 1. MCP 服务器发现与调用\n- **零配置发现**: 自动合并多种 MCP 配置\n- **一键 CLI 生成**: `mcporter generate-cli` 将任何 MCP 服务器定义转换为可运行的 CLI\n- **类型化工具客户端**: `mcporter emit-ts` 发出类型定义或客户端包装器\n\n### 2. 友好的组合 API\n- `createServerProxy()` 将工具公开为符合人体工程学的 camelCase 方法\n- 自动应用 JSON-schema 默认值、验证必填参数\n- 返回 `CallResult` 对象，支持 `.text()`、`.markdown()`、`.json()` 等方法\n\n## 常用命令\n\n### 列出所有 MCP 服务器\n```bash\nnpx mcporter list\nnpx mcporter list context7 --schema\n```\n\n### 调用 MCP 工具\n```bash\n# 冒号分隔格式\nnpx mcporter call linear.create_comment issueId:ENG-123 body:''Looks good!''\n\n# 函数调用风格\nnpx mcporter call ''linear.create_comment(issueId: "ENG-123", body: "Looks good!")''\n```\n\n### 生成类型化客户端\n```bash\nnpx mcporter emit-ts linear --out types/linear-tools.d.ts\n```',
    NULL,
    NULL,
    '1.0.0',
    'System'
)
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    icon = VALUES(icon),
    category = VALUES(category),
    prompt = VALUES(prompt),
    tools = VALUES(tools),
    markdown_content = VALUES(markdown_content),
    updated_at = NOW();
