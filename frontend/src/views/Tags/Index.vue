<template>
  <div class="tag-page">
    <div class="page-toolbar">
      <h2>标签管理</h2>
      <el-button type="primary" @click="showDialog = true; form.name = ''">
        <el-icon><Plus /></el-icon> 新建标签
      </el-button>
    </div>

    <el-card class="list-card">
      <div class="tag-cloud" v-if="tags.length > 0">
        <el-tag
          v-for="t in tags"
          :key="t.id"
          closable
          size="large"
          effect="plain"
          @close="handleDelete(t.id!)"
          class="tag-item"
        >
          {{ t.name }}
        </el-tag>
      </div>
      <el-empty v-else description="暂无标签" />
    </el-card>

    <el-dialog v-model="showDialog" title="新建标签" width="400px" destroy-on-close>
      <el-form label-width="60px">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="标签名称" @keydown.enter="handleSave" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getTags, createTag, deleteTag } from '@/api/tags'
import { ElMessage, ElMessageBox } from 'element-plus'

const tags = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const showDialog = ref(false)
const form = ref({ name: '' })

async function load() {
  loading.value = true
  try {
    const res: any = await getTags()
    tags.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!form.value.name.trim()) {
    ElMessage.warning('请输入标签名称')
    return
  }
  saving.value = true
  try {
    await createTag(form.value)
    ElMessage.success('已创建')
    showDialog.value = false
    form.value.name = ''
    await load()
  } finally {
    saving.value = false
  }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定删除该标签？', '确认', { type: 'warning' })
  await deleteTag(id)
  ElMessage.success('已删除')
  await load()
}

onMounted(() => load())
</script>

<style scoped>
.tag-page { padding: 0; }
.page-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: var(--space-4);
}
.page-toolbar h2 { padding-left: 8px; font-family: var(--font-display); font-size: var(--text-xl); font-weight: 700; }
.list-card { border-radius: var(--radius-lg); box-shadow: var(--shadow-card); }
.tag-cloud { min-height: 180px; align-content: flex-start; display: flex; flex-wrap: wrap; gap: var(--space-3); padding: 8px; background-image: radial-gradient(rgba(49,94,251,.11) .7px,transparent .7px); background-size: 20px 20px; }
.tag-item { cursor: default; padding: 9px 17px; font-size: var(--text-sm); background: rgba(255,255,255,.82); }
</style>
