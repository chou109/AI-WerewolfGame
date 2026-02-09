#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试ModelScope API的Python脚本
根据用户要求，使用官方Python代码测试API是否可用
"""

import requests
import json

# 用户提供的API密钥
API_KEY = "ms-5df5ab49-ddfd-4f5e-8ee9-0f094be0da70"

# 测试端点
BASE_URL = "https://api-inference.modelscope.cn/v1"
CHAT_COMPLETIONS_URL = f"{BASE_URL}/chat/completions"

print("=== ModelScope API 测试 ===")
print(f"API Key: {API_KEY[:15]}...")
print(f"Base URL: {BASE_URL}")
print(f"Chat Completions URL: {CHAT_COMPLETIONS_URL}")
print()

# 测试1: 检查API端点是否存在
print("测试1: 检查API端点是否存在")
try:
    response = requests.get(CHAT_COMPLETIONS_URL, timeout=10)
    print(f"状态码: {response.status_code}")
    print(f"响应: {response.text}")
except Exception as e:
    print(f"错误: {e}")
print()

# 测试2: 使用OpenAI兼容API测试
print("测试2: 使用OpenAI兼容API测试")
headers = {
    "Content-Type": "application/json",
    "Authorization": f"Bearer {API_KEY}"
}

data = {
    "model": "Qwen/Qwen2.5-0.5B-Instruct",
    "messages": [
        {
            "role": "user", 
            "content": "9.9和9.11谁大"
        }
    ],
    "stream": False
}

try:
    response = requests.post(CHAT_COMPLETIONS_URL, headers=headers, json=data, timeout=30)
    print(f"状态码: {response.status_code}")
    print(f"响应头: {dict(response.headers)}")
    try:
        response_json = response.json()
        print(f"响应数据: {json.dumps(response_json, indent=2, ensure_ascii=False)}")
    except:
        print(f"响应文本: {response.text}")
except Exception as e:
    print(f"错误: {e}")
print()

# 测试3: 测试不同的模型
print("测试3: 测试不同的模型")
models_to_test = [
    "Qwen/Qwen2.5-0.5B-Instruct",
    "deepseek-ai/DeepSeek-V3.2",
    "meta-llama/Llama-3.2-1B-Instruct"
]

for model in models_to_test:
    print(f"\n测试模型: {model}")
    data["model"] = model
    try:
        response = requests.post(CHAT_COMPLETIONS_URL, headers=headers, json=data, timeout=30)
        print(f"状态码: {response.status_code}")
        try:
            response_json = response.json()
            if "error" in response_json:
                print(f"错误: {response_json['error']}")
            else:
                print(f"成功: 模型可用")
        except:
            print(f"响应文本: {response.text}")
    except Exception as e:
        print(f"错误: {e}")
print()

print("=== 测试完成 ===")
