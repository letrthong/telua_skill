<!--
  Copyright (C) 2026 letrthong@gmail.com
  Created & Maintained by: letrthong@gmail.com
  Generated & Refactored by: Gemini 3.6 Pro (Google DeepMind)
  Licensed under the Apache License, Version 2.0
-->

# Model Context Protocol (MCP) Setup & Integration Guide (`tools/mcp_config_guide.md`)

This document defines configuration instructions, recommended open-source MCP servers, and execution rules for setting up Model Context Protocol (MCP) servers in Visual Studio Code (VS Code) and AI Extensions for Java, Android, and Android Automotive OS (AAOS) development.

---

## 1. Overview of MCP Benefits

* **Direct Raw Code Access:** Fetches clean JSON/raw source code directly via GitHub REST/GraphQL APIs instead of parsing HTML web pages.
* **Autonomous Repository Search:** Enables AI tools like `github.search_code`, `github.get_file_contents`, and `github.list_commits`.
* **AAOS & VHAL Automation:** Enables direct query and validation of Vehicle HAL (VHAL) properties and `dumpsys car_service`.

---

## 2. Visual Studio Code (VS Code) Integration Steps

### Step 1: Create Workspace MCP Configuration File
In your project workspace, create a file at `.vscode/mcp.json`:

```
d:\code\telua_skill\.vscode\mcp.json
```

### Step 2: Add Configuration JSON
Paste the following standard configuration:

```json
{
  "mcpServers": {
    "github": {
      "command": "npx",
      "args": [
        "-y",
        "@modelcontextprotocol/server-github"
      ],
      "env": {
        "GITHUB_PERSONAL_ACCESS_TOKEN": "ghp_YOUR_GITHUB_PERSONAL_ACCESS_TOKEN_HERE"
      }
    },
    "adb-android": {
      "command": "npx",
      "args": [
        "-y",
        "adb-mcp"
      ]
    },
    "vhal-automotive": {
      "command": "npx",
      "args": [
        "-y",
        "vhal-mcp-server"
      ]
    }
  }
}
```

### Step 3: Acquire GitHub Personal Access Token
1. Navigate to **GitHub Settings** -> **Developer Settings** -> **Personal Access Tokens** -> **Fine-grained tokens**.
2. Click **Generate new token**.
3. Grant **Repository permissions** -> **Contents: Read-only**.
4. Paste the generated token into `"GITHUB_PERSONAL_ACCESS_TOKEN"`.

### Step 4: Reload VS Code
1. Press `Ctrl + Shift + P` (or `Cmd + Shift + P` on macOS).
2. Type and select `Developer: Reload Window`.
3. Verify that the AI Assistant detects the active `github`, `adb-android`, and `vhal-automotive` MCP server tools.

---

## 3. Recommended Open-Source MCP Servers for Java, Android & AAOS

The following top-rated open-source MCP repositories on GitHub enhance AI capabilities for Java, Android, and Android Automotive OS (AAOS) engineering:

### 1. Android Automotive OS (AAOS) & VHAL MCP Servers
* 🚗 **`vhal-mcp-server`**: Specialized MCP server for Android Automotive **Vehicle Hardware Abstraction Layer (VHAL)**. Allows AI to validate VHAL properties (`PERF_VEHICLE_SPEED`, `HVAC_TEMPERATURE_SET`, `TIRE_PRESSURE`), inspect AOSP Automotive docs, and auto-generate VHAL vendor property bindings.
* 🚗 **`aaos-car-service-mcp`**: Enables AI to execute `dumpsys car_service`, inject mock VHAL events (`cmd car_service inject-vhal-event`), and inspect `CarAudioService` display zone audio routing.

### 2. Android Device & ADB Automation MCP Servers
* 📱 **[srmorete/adb-mcp](https://github.com/srmorete/adb-mcp)**: MCP server wrapping ADB. Enables AI to list connected emulators/devices, execute shell commands, install/uninstall APKs, and inspect UI hierarchy.
* 📱 **[jiantao88/android-mcp-server](https://github.com/jiantao88/android-mcp-server)**: Grants AI direct tools to run `adb logcat`, fetch system logs, and trigger Intents directly from prompt instructions.

### 3. Official Model Context Protocol Java/Kotlin SDKs
* 📦 **[modelcontextprotocol/java-sdk](https://github.com/modelcontextprotocol/java-sdk)**: Official Java SDK for creating custom Java MCP servers with Spring Boot, Gradle, or Maven.
* 📦 **[modelcontextprotocol/kotlin-sdk](https://github.com/modelcontextprotocol/kotlin-sdk)**: Official Kotlin SDK for building idiomatic Kotlin MCP servers.

### 4. Repository & Code Search MCP Servers
* 🐙 **[@modelcontextprotocol/server-github](https://github.com/modelcontextprotocol/servers/tree/main/src/github)**: Official GitHub MCP server for raw code search, commit inspection, and file content retrieval.

---

## 4. AI Execution Rules for MCP Tools

Whenever external GitHub repository URLs, ADB commands, or AAOS VHAL properties are referenced:
1. **Tool Invocation:** Prefer calling `github.get_file_contents`, `adb-mcp`, or `vhal-automotive` tools over raw web page scraping or manual terminal executions.
2. **Workspace Adaptation:** Refactor fetched source code to comply with all 17 `Java_Android` rules (`m`/`s` prefixes, null safety, timeout boundaries).
3. **Registry Logging:** Log the repository source URL, imports, and dependencies into a markdown note inside `docs/`.

---

## 5. Verification Checklist

1. [ ] Is Node.js (`npx`) installed and available in system PATH? -> **Must be Yes**.
2. [ ] Is `.vscode/mcp.json` configured with a valid GitHub Personal Access Token? -> **Must be Yes**.
3. [ ] Did you execute `Developer: Reload Window` in VS Code? -> **Must be Yes**.
4. [ ] Does the AI invoke `github`, `adb`, and `vhal-automotive` MCP tools when referencing external repositories or AAOS properties? -> **Must be Yes**.
