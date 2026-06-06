import request from './request'

export function pageReimbursements(params) {
  return request.get('/reimbursements/page', { params })
}

export function getReimbursement(reimNo) {
  return request.get(`/reimbursements/${reimNo}`)
}

export function createDraft(data) {
  return request.post('/reimbursements/draft', data)
}

export function createAndSubmit(data) {
  return request.post('/reimbursements/submit', data)
}

export function updateReimbursement(reimNo, data) {
  return request.put(`/reimbursements/${reimNo}`, data)
}

export function submitDraft(reimNo) {
  return request.post(`/reimbursements/${reimNo}/submit`)
}

export function copyReimbursement(reimNo) {
  return request.post(`/reimbursements/${reimNo}/copy`)
}

export function deleteDraft(reimNo) {
  return request.delete(`/reimbursements/${reimNo}`)
}

export function voidBill(reimNo) {
  return request.post(`/reimbursements/${reimNo}/void`)
}

export function generateAllowanceDays(data) {
  return request.post('/reimbursements/allowance-days/generate', data)
}

export function listCityAllowances() {
  return request.get('/city-allowances')
}

export function personalStatistics(params) {
  return request.get('/statistics/personal', { params })
}
