<template>
  <div class="shell">
    <header class="topbar">
      <div class="brand" @click="$router.push('/')">SEGroup8 · 好物市场</div>
      <div class="search-fake">重构版前端 · 淘宝/闲鱼风格商品流</div>
    </header>
    <nav class="nav-wrap">
      <el-scrollbar>
        <div class="nav-row">
          <el-button size="small" @click="$router.push('/')">首页</el-button>
          <el-button size="small" @click="$router.push('/product')">商品</el-button>
          <el-button size="small" @click="$router.push('/cart')">购物车</el-button>
          <el-button size="small" @click="$router.push('/order')">我的订单</el-button>
          <el-button size="small" @click="$router.push('/secondhand')">二手</el-button>
          <el-button size="small" @click="$router.push('/secondhand/publish')">发布二手</el-button>
          <el-button size="small" @click="$router.push('/profile')">个人资料</el-button>
          <el-button size="small" @click="$router.push('/addresses')">地址管理</el-button>
          <el-button size="small" @click="$router.push('/merchant-apply')">申请成为卖家</el-button>
          <el-button size="small" type="warning" @click="$router.push('/merchant')">进入卖家工作台</el-button>
          <el-button size="small" type="danger" plain @click="$router.push('/admin')">管理后台</el-button>
          <el-button size="small" @click="$router.push('/login')">登录</el-button>
          <el-button size="small" @click="$router.push('/register')">注册</el-button>
        </div>
      </el-scrollbar>
    </nav>
    <main class="main"><router-view /></main>
  </div>
</template>

<style scoped>
.shell {
  min-height: 100vh;
  background: var(--bg-grad);
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 20;
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 22px;
  border-bottom: 1px solid rgba(255, 255, 255, .4);
  background: rgba(255, 255, 255, .72);
}

.brand {
  font-weight: 800;
  font-size: 20px;
  color: #191d23;
  cursor: pointer;
}

.search-fake {
  max-width: 520px;
  flex: 1;
  text-align: right;
  color: #6f7680;
  font-size: 14px;
}

.main {
  width: min(1280px, 100% - 24px);
  margin: 18px auto 26px;
}

.nav-wrap {
  width: min(1280px, 100% - 24px);
  margin: 8px auto 0;
  background: rgba(255, 255, 255, .82);
  border: 1px solid var(--line-soft);
  border-radius: 12px;
  padding: 8px;
}

.nav-row {
  display: flex;
  gap: 8px;
  white-space: nowrap;
}

@media (max-width: 760px) {
  .topbar {
    padding: 12px 14px;
  }

  .brand {
    font-size: 18px;
  }

  .search-fake {
    display: none;
  }

  .main {
    width: calc(100% - 12px);
    margin-top: 10px;
  }

  .nav-wrap {
    width: calc(100% - 12px);
  }
}
</style>
