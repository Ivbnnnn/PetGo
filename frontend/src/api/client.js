import axios from 'axios';

const axiosInstance = axios.create({ baseURL: '/api' });

axiosInstance.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

let refreshing = null;

axiosInstance.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;
    if (error.response?.status === 401 && !original._retry && !original.url.includes('/auth/')) {
      original._retry = true;
      if (!refreshing) {
        refreshing = refreshToken().finally(() => { refreshing = null; });
      }
      try {
        await refreshing;
        return axiosInstance(original);
      } catch {
        clearAuth();
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

async function refreshToken() {
  const refresh = localStorage.getItem('refreshToken');
  if (!refresh) throw new Error('No refresh token');
  const { data } = await axios.post('/api/auth/refresh', { refreshToken: refresh });
  localStorage.setItem('accessToken', data.accessToken);
  localStorage.setItem('refreshToken', data.refreshToken);
  localStorage.setItem('user', JSON.stringify(data.user));
  return data;
}

export function clearAuth() {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');
}

export async function apiFetch(url, options = {}) {
  try {
    const method = options.method || 'GET';
    const isFormData = options.body instanceof FormData;
    const data = (!isFormData && options.body) ? JSON.parse(options.body) : options.body;
    
    const response = await axiosInstance.request({
      url: url.startsWith('/api') ? url.replace('/api', '') : url,
      method,
      data,
      headers: isFormData ? { 'Content-Type': 'multipart/form-data' } : {},
    });
    
    return response.data;
  } catch (error) {
    const message = error.response?.data?.message || error.message;
    const fieldErrors = error.response?.data?.fieldErrors || {};
    const status = error.response?.status;
    const err = new Error(message);
    err.status = status;
    err.fieldErrors = fieldErrors;
    throw err;
  }
}

export async function fetchAllPages(path, filters = {}) {
  const items = [];
  for (let page = 0; ; page++) {
    const params = new URLSearchParams({ ...filters, page: String(page), size: '100' });
    const result = await apiFetch(`${path}?${params}`);
    items.push(...result.content);
    if (page + 1 >= result.totalPages) return items;
  }
}

export const api = axiosInstance;
