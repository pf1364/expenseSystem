import request from './request'

// 本文件集中封装报销业务接口，页面不直接拼接 Axios 请求。

/** 分页查询报销单。 */
export function pageReimbursements(params) {
  return request.get('/reimbursements/page', { params })
}

/** 查询报销单完整详情。 */
export function getReimbursement(reimNo) {
  return request.get(`/reimbursements/${reimNo}`)
}

/** 创建草稿。 */
export function createDraft(data) {
  return request.post('/reimbursements/draft', data)
}

/** 创建并立即提交。 */
export function createAndSubmit(data) {
  return request.post('/reimbursements/submit', data)
}

/** 更新已有报销单。 */
export function updateReimbursement(reimNo, data) {
  return request.put(`/reimbursements/${reimNo}`, data)
}

/** 提交数据库中已有的草稿。 */
export function submitDraft(reimNo) {
  return request.post(`/reimbursements/${reimNo}/submit`)
}

/** 深度复制报销单为新草稿。 */
export function copyReimbursement(reimNo) {
  return request.post(`/reimbursements/${reimNo}/copy`)
}

/** 删除草稿。 */
export function deleteDraft(reimNo) {
  return request.delete(`/reimbursements/${reimNo}`)
}

/** 作废非草稿单据。 */
export function voidBill(reimNo) {
  return request.post(`/reimbursements/${reimNo}/void`)
}

/** 根据行程日期和目的地生成每日补助。 */
export function generateAllowanceDays(data) {
  return request.post('/reimbursements/allowance-days/generate', data)
}

/** 查询城市补助标准。 */
export function listCityAllowances() {
  return request.get('/city-allowances')
}

/** 查询个人报销统计。 */
export function personalStatistics(params) {
  return request.get('/statistics/personal', { params })
}
