<template>
  <div class="category-page">
    <div class="page-toolbar">
      <h2>分类管理</h2>
      <el-button type="primary" @click="showDialog = true; editingId = null; form.name = ''; form.color = '#10B981'">
        <el-icon><Plus /></el-icon> 新建分类
      </el-button>
    </div>

    <el-card class="list-card">
      <el-table :data="categories" stripe v-loading="loading">
        <el-table-column label="颜色" width="80">
          <template #default="{ row }">
            <div class="color-dot" :style="{ background: row.color }"></div>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="分类名称" min-width="200" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id!)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && categories.length === 0" description="暂无分类" />
    </el-card>

    <el-dialog v-model="showDialog" :title="editingId ? '编辑分类' : '新建分类'" width="400px" destroy-on-close>
      <el-form label-width="60px">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="分类名称" />
        </el-form-item>
        <el-form-item label="颜色">
          <el-color-picker v-model="form.color" show-alpha />
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
import { getCategories, createCategory, updateCategory, deleteCategory } from '@/api/categories'
import { ElMessage, ElMessageBox } from 'element-plus'

const categories = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const showDialog = ref(false)
const editingId = ref<number | null>(null)
const form = ref({ name: '', color: '#10B981' })

async function load() {
  loading.value = true
  try {
    const res: any = await getCategories()
    categories.value = res.data || []
  } finally {
    loading.value = false
  }
}

function openEdit(row: any) {
  editingId.value = row.id
  form.value = { name: row.name, color: row.color }
  showDialog.value = true
}

async function handleSave() {
  if (!form.value.name.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateCategory(editingId.value, form.value)
      ElMessage.success('已更新')
    } else {
      await createCategory(form.value)
      ElMessage.success('已创建')
    }
    showDialog.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定删除该分类？关联的笔记不会删除。', '确认', { type: 'warning' })
  await deleteCategory(id)
  ElMessage.success('已删除')
  await load()
}

onMounted(() => load())
</script>

<style scoped>
.category-page { padding: 0; }
.page-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: var(--space-4);
}
.page-toolbar h2 { padding-left: 8px; font-family: var(--font-display); font-size: var(--text-xl); font-weight: 700; }
.list-card { border-radius: var(--radius-lg); box-shadow: var(--shadow-card); }
.color-dot {
  width: 24px; height: 24px; border-radius: 8px 8px 8px 3px;
  border: 3px solid rgba(255,255,255,.75); box-shadow: 0 0 0 1px var(--border-medium);
}
</style>
