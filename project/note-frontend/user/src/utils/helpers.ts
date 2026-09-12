/**
 * 验证URL是否为安全协议
 */
export function isSafeUrl(url: string): boolean {
  if (!url) return false
  try {
    const parsed = new URL(url)
    return ['http:', 'https:', 'ftp:'].includes(parsed.protocol)
  } catch {
    return false
  }
}

/**
 * 检测文本是否可能是域名，自动补全http前缀
 */
export function autoCompleteUrl(text: string): string | null {
  // 匹配类似域名的格式: xxx.xxx 或 xxx.xxx/xxx
  const domainRegex = /^[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?(\.[a-zA-Z]{2,})+([/a-zA-Z0-9._~:/?#[\]@!$&'()*+,;=-]*)?$/
  if (domainRegex.test(text)) {
    return `http://${text}`
  }
  return null
}

/**
 * 判断是否是URL
 */
export function isUrl(text: string): boolean {
  return /^https?:\/\//i.test(text)
}

/**
 * 转义HTML特殊字符，防止XSS
 */
export function escapeHtml(text: string): string {
  const div = document.createElement('div')
  div.textContent = text
  return div.innerHTML
}

/**
 * 格式化日期
 */
export function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  // 一分钟内
  if (diff < 60 * 1000) return '刚刚'
  // 一小时内
  if (diff < 60 * 60 * 1000) return `${Math.floor(diff / (60 * 1000))}分钟前`
  // 一天内
  if (diff < 24 * 60 * 60 * 1000) return `${Math.floor(diff / (60 * 60 * 1000))}小时前`
  // 超过一天
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')

  if (year === now.getFullYear()) {
    return `${month}-${day} ${hour}:${minute}`
  }
  return `${year}-${month}-${day} ${hour}:${minute}`
}

/**
 * 移除 Markdown 代码块和行内代码，避免把教学代码误判为 HTML 注入。
 * 代码块会由 Markdown 渲染器按代码文本展示，不应参与 HTML/XSS 校验。
 */
function removeMarkdownCode(text: string): string {
  const visibleLines: string[] = []
  const lines = text.split(/\r?\n/)
  let fenceChar = ''
  let fenceLength = 0

  for (const line of lines) {
    const fence = line.match(/^[ \t]{0,3}(`{3,}|~{3,})(.*)$/)

    if (!fenceChar && fence) {
      // 反引号围栏的信息字符串不能再包含反引号，否则不是合法围栏。
      if (fence[1][0] === '`' && fence[2].includes('`')) {
        visibleLines.push(line)
        continue
      }
      fenceChar = fence[1][0]
      fenceLength = fence[1].length
      continue
    }

    if (fenceChar) {
      const closingFence = new RegExp(`^[ \\t]{0,3}${fenceChar}{${fenceLength},}[ \\t]*$`)
      if (closingFence.test(line)) {
        fenceChar = ''
        fenceLength = 0
      }
      continue
    }

    // Markdown 中缩进四个空格或一个制表符也表示代码块。
    if (/^(?: {4}|\t)/.test(line)) continue
    visibleLines.push(line)
  }

  // 行内代码同样会被渲染器转义为文本。
  return visibleLines.join('\n').replace(/`+[^`\r\n]*`+/g, '')
}

/**
 * 敏感词/XSS 简单校验（前端基础防护）。
 * Markdown 代码示例不参与 XSS 校验，但真实 HTML 注入仍会被拦截。
 */
export function containsSensitiveWords(text: string): boolean {
  if (!text) return false
  const visibleText = removeMarkdownCode(text)
  const sensitivePatterns = [
    /<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/i,
    /javascript\s*:/i,
    /vbscript\s*:/i,
    /data\s*:\s*(?:text\/html|text\/javascript|application\/javascript|application\/x-javascript)/i,
    /on(?:load|error|click|mouseover|mouseenter|focus|blur|submit)\s*=/i,
  ]
  return sensitivePatterns.some(pattern => pattern.test(visibleText))
}
