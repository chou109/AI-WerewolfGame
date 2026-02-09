#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
详细的ModelScope API测试脚本
完全模拟前端代码的调用方式
"""

import requests
import json

# 用户提供的API密钥
API_KEY = "ms-5df5ab49-ddfd-4f5e-8ee9-0f094be0da70"

# 测试端点
BASE_URL = "https://api-inference.modelscope.cn/v1"
CHAT_COMPLETIONS_URL = f"{BASE_URL}/chat/completions"

print("=== 详细ModelScope API测试 ===")
print(f"API Key: {API_KEY}")
print(f"API Key长度: {len(API_KEY)}")
print(f"API Key前缀: {API_KEY.startswith('ms-')}")
print(f"Base URL: {BASE_URL}")
print(f"Chat Completions URL: {CHAT_COMPLETIONS_URL}")
print(f"URL长度: {len(CHAT_COMPLETIONS_URL)}")
print()

# 详细检查URL字符
print("URL字符详情:")
for i, char in enumerate(CHAT_COMPLETIONS_URL[:20]):
    code = ord(char)
    print(f"  {i}: '{char}' ({code})")
print()

# 测试请求
print("=== 测试请求 ===")
headers = {
    "Content-Type": "application/json",
    "Authorization": f"Bearer {API_KEY}"
}

data = {
    "model": "deepseek-ai/DeepSeek-V3.2",
    "messages": [
        {
            "role": "user",
            "content": "9.9和9.11谁大"
        }
    ],
    "stream": False
}

print(f"请求头: {json.dumps(headers, indent=2, ensure_ascii=False)}")
print(f"请求体: {json.dumps(data, indent=2, ensure_ascii=False)}")
print()

try:
    response = requests.post(CHAT_COMPLETIONS_URL, headers=headers, json=data, timeout=30)
    print(f"状态码: {response.status_code}")
    print(f"响应头: {dict(response.headers)}")
    
    if response.status_code == 200:
        print("✅ 成功！")
        response_json = response.json()
        print(f"响应数据: {json.dumps(response_json, indent=2, ensure_ascii=False)}")
    else:
        print(f"❌ 失败！状态码: {response.status_code}")
        try:
            response_json = response.json()
            print(f"错误数据: {json.dumps(response_json, indent=2, ensure_ascii=False)}")
        except:
            print(f"错误文本: {response.text}")
            
except Exception as e:
    print(f"❌ 异常: {e}")
    import traceback
    traceback.print_exc()

print()
print("=== 测试完成 ===")
