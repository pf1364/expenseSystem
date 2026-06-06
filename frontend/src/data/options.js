export const companies = [
  { id: '1C54557F1782E000', no: '0407', name: '胜意科技北京分公司' },
  { id: '19218A262C976000', no: '0408', name: '胜意科技上海分公司' },
  { id: '1C61686865DA8000', no: '0409', name: '胜意科技武汉分公司' },
  { id: '1717271D1DA15000', no: '0410', name: '胜意科技杭州分公司' },
  { id: '16AE93CC7EF92002', no: '0411', name: '胜意科技荆州分公司' }
]

export const departments = [
  { id: '13AB8D7B52A9B002', no: '072001', name: '客户成功事业部' },
  { id: '13BFD31C6029A002', no: '072002', name: '企业消费事业部' },
  { id: '14515BB4BFB92003', no: '072003', name: '企业费控事业部' },
  { id: '19206611C47A6000', no: '072004', name: '集采事业部' },
  { id: '19D32F9FE9647000', no: '072005', name: '航旅事业部' },
  { id: '13C7E2BAE0393001', no: '072006', name: '运营事业部' },
  { id: '14055D22BB808001', no: '072007', name: '营销事业部' }
]

export const employees = [
  { id: '13AB3A3F72409002', no: '74541', name: '徐年年' },
  { id: '13AB498CC6409002', no: '74008', name: '郑雨雪' },
  { id: '13AB4A56BB009002', no: '21552', name: '邹薇' },
  { id: '13AB591FE8009002', no: '80681', name: '王成军' },
  { id: '13AB77281A408001', no: '89899', name: '潘展飞' },
  { id: '13AB7925EB808001', no: '10503', name: '姜林' }
]

export const businessTypes = [
  { id: '18F0916A8C2C4000', no: '1001001', name: '员工差旅活动', parentId: 'none', hasChildren: true },
  { id: '18F091913EEC4000', no: '100100101', name: '境内出差', parentId: '18F0916A8C2C4000', hasChildren: true },
  { id: '1B5FEB7DD4396000', no: '10010010101', name: '项目出差', parentId: '18F091913EEC4000', hasChildren: false },
  { id: '1A92E43082EFC000', no: '10010010102', name: '市场拓展出差', parentId: '18F091913EEC4000', hasChildren: false },
  { id: '13AB3A4138008001', no: '100100102', name: '境外出差', parentId: '18F0916A8C2C4000', hasChildren: true },
  { id: '13AB3A4248008002', no: '10010010201', name: '国外考察', parentId: '13AB3A4138008001', hasChildren: false },
  { id: '13AB3A4154008001', no: '10010010202', name: '售后维护出差', parentId: '13AB3A4138008001', hasChildren: false },
  { id: '13AB3A4172008001', no: '1001002', name: '人力资源', parentId: 'none', hasChildren: true },
  { id: '13AB3A418F808001', no: '100100201', name: '个人团队培训', parentId: '13AB3A4172008001', hasChildren: false },
  { id: '13AB3A41AC408001', no: '100100202', name: '招聘会', parentId: '13AB3A4172008001', hasChildren: false },
  { id: '13AB3A41CD808002', no: '1001003', name: '员工福利', parentId: 'none', hasChildren: true },
  { id: '13AB3A41ED408002', no: '100100301', name: '员工旅游', parentId: '13AB3A41CD808002', hasChildren: false },
  { id: '13AB3A420CC08002', no: '100100302', name: '员工团建', parentId: '13AB3A41CD808002', hasChildren: false },
  { id: '13AB3A422A808001', no: '100100303', name: '员工体检', parentId: '13AB3A41CD808002', hasChildren: false }
]

export const businessTypeTree = buildTree(businessTypes)

export const billStatuses = [
  { value: 'DRAFT', label: '草稿' },
  { value: 'SUBMITTED', label: '已提交' },
  { value: 'VOIDED', label: '已作废' }
]

export const fallbackCities = [
  { cityCode: '10119', cityName: '北京', cityLevel: 1, mealStandard: 100, trafficStandard: 40, communicationStandard: 40 },
  { cityCode: '10621', cityName: '上海', cityLevel: 1, mealStandard: 100, trafficStandard: 40, communicationStandard: 40 },
  { cityCode: '10458', cityName: '武汉', cityLevel: 2, mealStandard: 80, trafficStandard: 40, communicationStandard: 40 },
  { cityCode: '10216', cityName: '杭州', cityLevel: 2, mealStandard: 80, trafficStandard: 40, communicationStandard: 40 },
  { cityCode: '10455', cityName: '荆州', cityLevel: 3, mealStandard: 50, trafficStandard: 40, communicationStandard: 40 }
]

export function findBusinessType(id) {
  return businessTypes.find(item => item.id === id)
}

function buildTree(items) {
  const nodeMap = new Map()
  items.forEach(item => {
    nodeMap.set(item.id, {
      value: item.id,
      label: item.name,
      data: item,
      children: []
    })
  })
  const roots = []
  items.forEach(item => {
    const node = nodeMap.get(item.id)
    if (item.parentId === 'none') {
      roots.push(node)
    } else {
      nodeMap.get(item.parentId)?.children.push(node)
    }
  })
  return roots
}
