import axios from 'axios'
import { ElMessage } from 'element-plus'

const SUCCESS_CODE = 200

const request = axios.create({
  baseURL: '/api',
  // 数据导入（CSV/Excel，最大 50MB）可能耗时较久，超时时间放宽到 120s
  timeout: 120000
})

// HTTP 状态码兜底提示：后端未给出 message 时使用
const HTTP_STATUS_MESSAGES = {
  400: '请求参数不合法',
  401: '未登录或登录已过期',
  403: '操作被安全策略拦截',
  404: '请求的资源不存在',
  409: '数据冲突，请刷新后重试',
  500: '服务器内部错误，请查看后端日志',
  502: '网关错误',
  503: '服务暂不可用'
}

function businessError(message, extra = {}) {
  const error = new Error(message)
  Object.assign(error, extra, { handled: true })
  return error
}

// 请求拦截器
request.interceptors.request.use(
  config => config,
  error => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器：同时兼容"HTTP 200 + success=false"和"语义化 HTTP 状态码"两种失败形态
request.interceptors.response.use(
  response => {
    const res = response.data

    // 后端统一响应结构：success=false 或 code !== 200 都视为业务失败
    if (res && typeof res === 'object' && ('success' in res || 'code' in res)) {
      if (res.success === false || (res.code !== undefined && res.code !== SUCCESS_CODE)) {
        const message = res.message || '请求失败'
        ElMessage.error(message)
        console.error('API Error:', { code: res.code, message, httpStatus: response.status })
        return Promise.reject(businessError(message, {
          code: res.code,
          httpStatus: response.status
        }))
      }
      return res
    }

    return res
  },
  error => {
    const data = error.response?.data
    const status = error.response?.status
    const message = data?.message || HTTP_STATUS_MESSAGES[status] || error.message || '网络错误'
    ElMessage.error(message)
    console.error('Response error:', { httpStatus: status, code: data?.code, message })
    return Promise.reject(Object.assign(error, {
      handled: true,
      code: data?.code,
      httpStatus: status,
      message
    }))
  }
)

export default request
