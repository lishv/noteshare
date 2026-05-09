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
 * 敏感词简单校验（前端基础防护）
 */
export function containsSensitiveWords(text: string): boolean {
  // 这里只做前端基础校验，后端会有完整校验
  const sensitivePatterns = [
    /javascript\s*:/i,
    /vbscript\s*:/i,
    /data\s*:/i,
    /on\w+\s*=/i,
  ]
  return sensitivePatterns.some(pattern => pattern.test(text))
}
