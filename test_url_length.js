// 测试URL长度
const url = 'https://api-inference.modelscope.cn/v1/chat/completions'
console.log('URL:', url)
console.log('URL长度:', url.length)
console.log('标准URL长度:', 'https://api-inference.modelscope.cn/v1/chat/completions'.length)

// 检查每个字符
console.log('URL字符详情:')
for (let i = 0; i < url.length; i++) {
  const char = url[i]
  const code = char.charCodeAt(0)
  console.log(`  ${i}: '${char}' (${code})`)
}

// 检查是否有特殊字符
const hasSpecialChars = url.split('').some(char => {
  const code = char.charCodeAt(0)
  return code < 32 || code > 126
})
console.log('是否有特殊字符:', hasSpecialChars)
