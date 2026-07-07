import { createRouter, createWebHistory } from 'vue-router'

import Login from '../views/Login.vue'
import Home from '../views/Home.vue'
import Versions from '../views/Versions.vue'
import Editor from '../views/Editor.vue'
import ModelFullView from '../views/ModelFullView.vue'
import Community from '../views/Community.vue'
import CommunityDetail from '../views/CommunityDetail.vue'

const routes = [
  {
    path: '/',
    component: Login
  },
  {
    path: '/home',
    component: Home
  },
  {
    path: '/versions/:characterId',
    component: Versions
  },
  {
    path: '/editor/:characterId',
    component: Editor
  },
  {
    path: '/model-view/:versionId',
    component: ModelFullView
  },
  {
    path: '/community',
    component: Community,
    meta: {
      transition: 'page-slide'
    }
  },
  {
    path: '/community/:id',
    component: CommunityDetail,
    meta: {
      transition: 'page-flip'
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router