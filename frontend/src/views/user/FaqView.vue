<template>
  <section class="faq-page">
    <header class="faq-hero">
      <div>
        <p>使用帮助</p>
        <h1>常见问题与平台使用说明</h1>
        <span>这里整理了买家、二手交易、卖家入驻、订单售后、优惠券、账户安全等常见操作。</span>
      </div>
    </header>

    <div class="faq-layout">
      <aside class="faq-index" aria-label="常见问题分类">
        <button
          type="button"
          :class="{ active: activeSection === 'all' }"
          @click="activeSection = 'all'"
        >
          全部问题
        </button>
        <button
          v-for="section in sections"
          :key="section.id"
          type="button"
          :class="{ active: activeSection === section.id }"
          @click="activeSection = section.id"
        >
          {{ section.title }}
        </button>
      </aside>

      <main class="faq-content">
        <section
          v-for="section in visibleSections"
          :key="section.id"
          class="faq-section"
        >
          <div class="section-title">
            <p>{{ section.subtitle }}</p>
            <h2>{{ section.title }}</h2>
          </div>

          <el-collapse>
            <el-collapse-item
              v-for="item in section.items"
              :key="item.q"
              :title="item.q"
            >
              <p v-for="line in item.a" :key="line">{{ line }}</p>
            </el-collapse-item>
          </el-collapse>
        </section>
      </main>
    </div>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue';

const activeSection = ref('all');

const sections = [
  {
    id: 'buyer',
    title: '买家购物',
    subtitle: '浏览、下单、支付、收货',
    items: [
      {
        q: 'Q：我可以在平台上买什么？',
        a: [
          '你可以购买平台卖家发布的一手商品，也可以进入二手市场购买用户发布的闲置商品。',
          '商品列表默认展示封面图；进入详情页后可以查看商品介绍、价格、库存、卖家信息和多张商品图片。',
        ],
      },
      {
        q: 'Q：如何搜索或筛选商品？',
        a: [
          '在商城顶部搜索框输入商品关键词后提交，即可进入商品列表查看匹配结果。',
          '商品列表支持按分类、价格等条件查看商品，二手市场也可以按分类查看闲置商品。',
        ],
      },
      {
        q: 'Q：如何加入购物车？',
        a: [
          '在一手商品详情页选择需要购买的商品后，可以加入购物车。',
          '购物车里可以统一查看待购买商品、调整数量、选择结算商品并提交订单。',
        ],
      },
      {
        q: 'Q：如何直接购买商品？',
        a: [
          '在商品详情页点击购买，会进入下单流程。',
          '下单前请确认收货地址、商品数量、价格和优惠信息，确认无误后再提交订单。',
        ],
      },
      {
        q: 'Q：下单前必须先设置收货地址吗？',
        a: [
          '是的。购买商品前需要至少有一个可用收货地址。',
          '你可以在“地址管理”里新增、修改、删除地址，也可以设置默认地址。',
        ],
      },
      {
        q: 'Q：平台支持哪些支付方式？',
        a: [
          '平台支持模拟支付和商城币支付。',
          '如果使用商城币，请确保个人账户余额足够；余额不足时可以先在个人资料页进行模拟充值。',
        ],
      },
      {
        q: 'Q：在哪里查看我的订单？',
        a: [
          '进入“我的订单”可以查看待支付、待发货、待收货、已完成、退款售后等订单。',
          '点击订单详情可以查看商品、金额、物流、支付和售后状态。',
        ],
      },
      {
        q: 'Q：如何确认收货？',
        a: [
          '卖家发货后，订单会进入待收货状态。',
          '你收到商品并确认无误后，可以在订单详情中确认收货。确认后订单会进入后续完成流程。',
        ],
      },
    ],
  },
  {
    id: 'secondhand',
    title: '二手交易',
    subtitle: '发布闲置、购买二手、订单管理',
    items: [
      {
        q: 'Q：谁可以发布二手商品？',
        a: [
          '普通用户、卖家和管理员账号都可以发布二手闲置商品。',
          '发布时需要填写商品名称、图片、原价、售价、分类、成色、是否可议价和商品描述。',
        ],
      },
      {
        q: 'Q：二手商品可以上传多张图片吗？',
        a: [
          '可以。二手商品支持多图上传，最多 9 张。',
          '第一张图片会作为列表封面。你可以拖动图片调整顺序，从而更换封面图。',
        ],
      },
      {
        q: 'Q：二手商品支持哪些分类？',
        a: [
          '二手商品支持大部分商品分类，但不支持食品类目。',
          '发布时需要先选择一级分类，再选择对应的二级分类。',
        ],
      },
      {
        q: 'Q：购买二手商品和购买一手商品有什么区别？',
        a: [
          '二手商品通常由个人用户发布，详情页会显示成色、售价、是否可议价等信息。',
          '购买流程仍然需要确认地址、提交订单并完成支付。',
        ],
      },
      {
        q: 'Q：我发布的二手商品在哪里管理？',
        a: [
          '可以在二手市场相关入口查看你发布、售出或购买的二手订单。',
          '卖家工作台中也提供二手商品相关管理入口，方便统一查看。',
        ],
      },
      {
        q: 'Q：二手交易可以联系对方吗？',
        a: [
          '可以通过消息功能与对方沟通。',
          '建议在平台内沟通商品状态、发货时间和售后问题，方便保留记录。',
        ],
      },
    ],
  },
  {
    id: 'seller',
    title: '卖家与店铺',
    subtitle: '入驻、发布商品、管理店铺',
    items: [
      {
        q: 'Q：如何申请成为卖家？',
        a: [
          '进入“申请成为卖家”，填写店铺名称、主营领域、身份信息、银行卡信息、营业/资质图片、仓库地址和联系人信息。',
          '提交后等待管理员审核。审核通过后，你会拥有卖家工作台权限。',
        ],
      },
      {
        q: 'Q：主营领域有什么作用？',
        a: [
          '主营领域会成为店铺的一手商品一级分类。',
          '卖家发布一手商品时，一级分类固定为注册入驻时选择的主营领域，不能自由更换；你可以自由选择该一级分类下的二级分类。',
        ],
      },
      {
        q: 'Q：如何发布一手商品？',
        a: [
          '进入卖家工作台，选择发布商品或商品管理中的新增商品。',
          '填写商品名称、描述、价格、库存、二级分类并上传图片后即可发布。',
        ],
      },
      {
        q: 'Q：一手商品可以上传多张图片吗？',
        a: [
          '可以。一手商品支持多图上传，最多 9 张。',
          '列表页默认展示第一张图片，详情页可以查看多张图片。拖动图片排序后，第一张会成为新的封面。',
        ],
      },
      {
        q: 'Q：卖家可以编辑商品吗？',
        a: [
          '可以。卖家只能编辑自己店铺下的商品。',
          '编辑时可以修改商品名称、描述、价格、库存、二级分类、图片顺序和上下架状态。',
        ],
      },
      {
        q: 'Q：如何上架或下架商品？',
        a: [
          '在卖家商品列表中可以切换商品状态。',
          '下架后商品不会在商城正常售卖；重新上架后买家可以继续浏览和购买。',
        ],
      },
      {
        q: 'Q：店铺资料可以修改哪些内容？',
        a: [
          '店铺名称、主营领域、联系人和仓库地址等核心资料来自入驻审核信息，通常不可随意修改。',
          '店铺介绍、客服电话、营业时间、头像、横幅、退换货政策、发货说明和公告等展示内容可以在店铺资料中维护。',
        ],
      },
      {
        q: 'Q：卖家在哪里处理订单？',
        a: [
          '进入卖家工作台的订单管理，可以查看买家订单、发货、处理售后和查看订单详情。',
          '建议及时处理发货和售后，避免影响店铺信用。',
        ],
      },
      {
        q: 'Q：卖家可以创建优惠券吗？',
        a: [
          '可以。进入卖家工作台的优惠券管理，可以创建、编辑、关闭或删除店铺优惠券。',
          '优惠券是否可用会根据有效期、库存、订单金额和适用范围判断。',
        ],
      },
    ],
  },
  {
    id: 'order',
    title: '订单、物流与售后',
    subtitle: '退款、评价、物流进度',
    items: [
      {
        q: 'Q：订单有哪些常见状态？',
        a: [
          '常见状态包括待支付、待发货、待收货、已完成、已关闭和退款售后中。',
          '具体状态以订单详情页展示为准。',
        ],
      },
      {
        q: 'Q：如何查看物流？',
        a: [
          '卖家发货后，订单详情页会展示物流状态和物流进度。',
          '如果物流信息暂未更新，可以稍后刷新查看。',
        ],
      },
      {
        q: 'Q：什么时候可以申请售后？',
        a: [
          '订单支付后、符合平台售后条件时，可以在订单或售后入口申请退款/售后。',
          '不同订单状态可申请的售后类型可能不同，例如未发货更适合仅退款，已发货或已收货可能需要退货退款。',
        ],
      },
      {
        q: 'Q：申请售后需要填写什么？',
        a: [
          '需要选择售后原因，并尽量说明问题。',
          '如果涉及商品损坏、错发、少发等问题，建议上传凭证图片，便于卖家和管理员判断。',
        ],
      },
      {
        q: 'Q：卖家不处理售后怎么办？',
        a: [
          '平台有超时处理机制。卖家长时间未处理时，系统或管理员可根据规则介入。',
          '你也可以保留沟通记录和凭证，等待平台处理。',
        ],
      },
      {
        q: 'Q：如何评价商品？',
        a: [
          '订单完成后，可以在订单或评价入口对商品进行评价。',
          '卖家也可以在工作台查看评价并进行回复。',
        ],
      },
    ],
  },
  {
    id: 'wallet',
    title: '优惠券、钱包与账务',
    subtitle: '商城币、优惠券、财务记录',
    items: [
      {
        q: 'Q：商城币是什么？',
        a: [
          '商城币是平台内的模拟余额，可用于订单支付。',
          '你可以在个人资料页查看余额，并进行模拟充值。',
        ],
      },
      {
        q: 'Q：如何领取优惠券？',
        a: [
          '进入领券中心或优惠券入口，可以查看当前可领取的优惠券。',
          '领取后可在“我的优惠券”中查看可用、已用或不可用的优惠券。',
        ],
      },
      {
        q: 'Q：为什么优惠券不能用？',
        a: [
          '常见原因包括优惠券已过期、库存不足、订单金额不满足、商品或店铺不适用、已使用或状态不可用。',
          '下单时系统会根据当前订单自动判断可用优惠券。',
        ],
      },
      {
        q: 'Q：卖家在哪里看收入？',
        a: [
          '卖家可以在工作台财务模块查看经营相关金额和记录。',
          '订单完成、退款、售后等都会影响最终结算结果。',
        ],
      },
    ],
  },
  {
    id: 'account',
    title: '账户、消息与安全',
    subtitle: '登录、资料、信用、通知',
    items: [
      {
        q: 'Q：注册后可以做什么？',
        a: [
          '注册并登录后，可以浏览商品、下单购买、发布二手商品、管理地址、查看订单、领取优惠券、发送消息和查看通知。',
          '如果想经营店铺，可以继续申请成为卖家。',
        ],
      },
      {
        q: 'Q：在哪里修改个人资料？',
        a: [
          '进入“个人资料”可以查看和维护个人信息、头像、联系方式和商城币余额等。',
          '部分与店铺审核相关的信息需要通过店铺资料或入驻流程维护。',
        ],
      },
      {
        q: 'Q：消息功能有什么用？',
        a: [
          '消息功能用于买家、卖家之间沟通商品、订单和售后问题。',
          '从商品详情页联系卖家时，系统会带上商品来源，方便双方识别沟通内容。',
        ],
      },
      {
        q: 'Q：通知中心会显示什么？',
        a: [
          '通知中心会显示订单、售后、卖家审核、系统提醒等平台通知。',
          '卖家和买家可能收到不同类型的通知。',
        ],
      },
      {
        q: 'Q：信用分有什么作用？',
        a: [
          '信用分用于展示账号在买家、二手卖家或店铺经营中的信用情况。',
          '良好的交易、及时发货、合理处理售后和真实评价有助于维持较好的信用表现。',
        ],
      },
      {
        q: 'Q：账号被封禁后还能使用吗？',
        a: [
          '被封禁账号会受到平台限制，具体以页面提示为准。',
          '管理员不能封禁自己的当前账号，避免误操作导致后台无法继续管理。',
        ],
      },
    ],
  },
  {
    id: 'admin',
    title: '平台管理说明',
    subtitle: '审核、用户、订单、报表',
    items: [
      {
        q: 'Q：管理员可以做什么？',
        a: [
          '管理员可以进入后台管理用户、审核卖家入驻申请、查看订单、处理平台报表、查看审计日志和管理优惠券等。',
          '后台功能面向平台运营，不建议普通用户使用。',
        ],
      },
      {
        q: 'Q：卖家入驻由谁审核？',
        a: [
          '卖家提交入驻申请后，由管理员在后台进行审核。',
          '审核通过后，用户会获得卖家权限；审核驳回时会显示驳回原因。',
        ],
      },
      {
        q: 'Q：为什么后台操作会记录日志？',
        a: [
          '平台会记录关键后台操作，便于追踪审核、封禁、订单处理等重要行为。',
          '这有助于平台运营安全和问题追溯。',
        ],
      },
    ],
  },
];

const visibleSections = computed(() => {
  if (activeSection.value === 'all') {
    return sections;
  }
  return sections.filter((section) => section.id === activeSection.value);
});
</script>

<style scoped>
.faq-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.faq-hero {
  border-radius: 18px;
  padding: 28px;
  background: #fff;
  border: 1px solid #eeeeee;
  box-shadow: 0 8px 20px rgba(30, 34, 40, 0.04);
}

.faq-hero p {
  margin: 0 0 6px;
  color: #8a7100;
  font-weight: 800;
}

.faq-hero h1 {
  margin: 0;
  color: #20242d;
  font-size: 30px;
}

.faq-hero span {
  display: block;
  margin-top: 10px;
  color: #60656f;
  line-height: 1.7;
}

.faq-layout {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.faq-index,
.faq-section {
  border: 1px solid #eeeeee;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 20px rgba(30, 34, 40, 0.04);
}

.faq-index {
  position: sticky;
  top: 118px;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.faq-index button {
  height: 38px;
  border: 0;
  border-radius: 10px;
  padding: 0 12px;
  background: transparent;
  color: #30343c;
  cursor: pointer;
  text-align: left;
  font-weight: 700;
}

.faq-index button:hover,
.faq-index button.active {
  background: #fff7c2;
}

.faq-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.faq-section {
  padding: 20px;
}

.section-title {
  margin-bottom: 12px;
}

.section-title p {
  margin: 0 0 4px;
  color: #8a8f99;
  font-size: 13px;
}

.section-title h2 {
  margin: 0;
  color: #20242d;
  font-size: 22px;
}

.faq-section :deep(.el-collapse) {
  border-top: 0;
  border-bottom: 0;
}

.faq-section :deep(.el-collapse-item__header) {
  min-height: 48px;
  color: #20242d;
  font-weight: 800;
  line-height: 1.45;
}

.faq-section :deep(.el-collapse-item__content) {
  padding-bottom: 16px;
  color: #555c66;
  line-height: 1.8;
}

.faq-section p {
  margin: 0 0 8px;
}

@media (max-width: 860px) {
  .faq-layout {
    grid-template-columns: 1fr;
  }

  .faq-index {
    position: static;
    flex-direction: row;
    overflow-x: auto;
  }

  .faq-index button {
    flex: 0 0 auto;
    white-space: nowrap;
  }

  .faq-hero,
  .faq-section {
    padding: 16px;
  }
}
</style>
