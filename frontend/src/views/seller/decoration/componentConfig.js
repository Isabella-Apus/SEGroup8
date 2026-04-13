// 所有可用的组件类型定义
export const COMPONENT_TYPES = {
  BANNER: 'banner',
  PRODUCT_GRID: 'product_grid',
  TEXT_BLOCK: 'text_block',
  DIVIDER: 'divider',
  ANNOUNCEMENT: 'announcement',
  IMAGE_TEXT: 'image_text',
  COUNTDOWN: 'countdown'
}

export const COMPONENT_TEMPLATES = [
  {
    type: COMPONENT_TYPES.BANNER,
    label: 'Banner 图片',
    icon: '🖼️',
    description: '大图横幅，适合展示活动促销',
    defaultProps: {
      imageUrl: '',
      linkUrl: '',
      height: 200,
      borderRadius: 8
    }
  },
  {
    type: COMPONENT_TYPES.PRODUCT_GRID,
    label: '商品展示',
    icon: '🛍️',
    description: '展示精选商品，2或4列布局',
    defaultProps: {
      title: '精选商品',
      columns: 2,
      productIds: [],
      products: []
    }
  },
  {
    type: COMPONENT_TYPES.TEXT_BLOCK,
    label: '文字内容',
    icon: '📝',
    description: '自定义文字，支持标题和正文',
    defaultProps: {
      title: '',
      content: '',
      align: 'left',
      titleSize: 16,
      contentSize: 14
    }
  },
  {
    type: COMPONENT_TYPES.ANNOUNCEMENT,
    label: '公告栏',
    icon: '📢',
    description: '店铺公告或促销信息',
    defaultProps: {
      text: '欢迎光临本店！',
      bgColor: '#fffbeb',
      textColor: '#b45309',
      icon: '📢'
    }
  },
  {
    type: COMPONENT_TYPES.IMAGE_TEXT,
    label: '图文组合',
    icon: '🖼️',
    description: '左图右文或左文右图布局',
    defaultProps: {
      imageUrl: '',
      title: '标题',
      content: '描述内容',
      layout: 'image-left',
      imageWidth: 40
    }
  },
  {
    type: COMPONENT_TYPES.DIVIDER,
    label: '分割线',
    icon: '➖',
    description: '用于分隔不同内容区块',
    defaultProps: {
      style: 'solid',
      color: '#e5e7eb',
      margin: 16
    }
  },
  {
    type: COMPONENT_TYPES.COUNTDOWN,
    label: '倒计时',
    icon: '⏱️',
    description: '活动倒计时，制造紧迫感',
    defaultProps: {
      title: '限时特惠',
      endTime: '',
      bgColor: '#ef4444',
      textColor: '#ffffff'
    }
  }
]

export function createComponent(type) {
  const template = COMPONENT_TEMPLATES.find(t => t.type === type)
  if (!template) return null
  return {
    id: Date.now() + Math.random(),
    type,
    props: { ...template.defaultProps }
  }
}