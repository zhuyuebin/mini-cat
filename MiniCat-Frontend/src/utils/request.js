import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    return config
  },
  error => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    
    // 检查success字段或code字段
    if (res.success === false || (res.code && res.code !== 200)) {
      const errorMsg = res.message || '请求失败'
      ElMessage.error(errorMsg)
      console.error('API Error:', res)
      return Promise.reject(new Error(errorMsg))
    }
    
    return res
  },
  error => {
    console.error('Response error:', error)
    // 显示详细的错误信息
    const errorMsg = error.response?.data?.message || error.message || '网络错误'
    ElMessage.error(errorMsg)
    return Promise.reject(error)
  }
)

export default request
