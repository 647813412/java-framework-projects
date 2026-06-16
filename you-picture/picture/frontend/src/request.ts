import axios from 'axios'
import { message } from 'ant-design-vue'

const request = axios.create({
  baseURL: '',
  timeout: 10000,
  withCredentials: true,
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const data = response.data
    // 成功
    if (data.code === 0) {
      return data
    }
    // 未登录 — 静默拒绝，由路由守卫或业务层决定是否跳转
    if (data.code === 40100) {
      return Promise.reject(new Error('未登录'))
    }
    // 其他业务错误
    message.error(data.message || '请求失败')
    return Promise.reject(new Error(data.message || '请求失败'))
  },
  (error) => {
    message.error('网络异常，请稍后重试')
    return Promise.reject(error)
  }
)

export default request
