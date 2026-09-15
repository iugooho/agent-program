import axios, { type AxiosInstance } from 'axios'

/**
 * 统一的 axios 实例：组件不要直接调用 axios，一律通过 src/api 下的模块调用。
 * baseURL 走环境变量，开发环境默认 '/api'，由 Vite 代理到后端 8080。
 */
export const http: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => Promise.reject(error)
)
