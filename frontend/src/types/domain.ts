export type TransactionType = 'EXPENSE' | 'INCOME'
/** 任务分类。原先还有 WEEKLY，实测用不上已移除；「每周重复」是 RecurrenceType 的事 */
export type TaskType = 'TODAY' | 'LONG_TERM'
export type TaskStatus = 'PENDING' | 'DONE' | 'CANCELLED'
export type RecurrenceType = 'NONE' | 'DAILY' | 'WEEKLY'

// ---------- 体重 ----------

export interface WeightVO {
  id: number
  recordDate: string
  weightKg: number
  note: string | null
}

export interface WeightUpsertRequest {
  recordDate?: string | null
  weightKg: number
  note?: string | null
}

export interface TrendPointVO {
  key: string
  label: string
  value: number
}

export interface WeightStatsVO {
  current: number | null
  currentDate: string | null
  max: number | null
  min: number | null
  average: number | null
  latestDelta: number | null
  recordCount: number
}

export interface BmiBandVO {
  code: string
  label: string
  min: number
  max: number
}

export interface BmiVO {
  bmi: number | null
  category: string | null
  heightCm: number | null
  weightKg: number | null
  scaleMin: number | null
  scaleMax: number | null
  healthyMinKg: number | null
  healthyMaxKg: number | null
  bands: BmiBandVO[]
}

// ---------- 档案 ----------

export interface ProfileVO {
  id: number
  nickname: string
  heightCm: number
  aiEnabled: boolean
  aiModel: string
}

export interface ProfileUpdateRequest {
  nickname?: string | null
  heightCm?: number | null
  aiEnabled?: boolean | null
  aiModel?: string | null
}

// ---------- 健身 ----------

export interface ExercisePartVO {
  id: number
  code: string
  name: string
  sortOrder: number
  cardio: boolean
  sessionCount: number
  exerciseCount: number
}

export interface ExerciseVO {
  id: number
  partId: number
  name: string
  isDefault: boolean
}

export interface WorkoutSetVO {
  id: number
  setIndex: number
  weightKg: number | null
  reps: number | null
  durationMin: number | null
  distanceKm: number | null
}

export interface WorkoutVO {
  id: number
  recordDate: string
  partId: number
  partName: string
  partCode: string
  cardio: boolean
  exerciseId: number
  exerciseName: string
  note: string | null
  sets: WorkoutSetVO[]
  totalVolume: number
  maxWeight: number | null
  totalReps: number
}

export interface WorkoutSetRequest {
  weightKg?: number | null
  reps?: number | null
  durationMin?: number | null
  distanceKm?: number | null
}

export interface WorkoutRequest {
  recordDate?: string | null
  exerciseId: number
  sets: WorkoutSetRequest[]
  note?: string | null
}

export interface WorkoutComparisonVO {
  hasPrevious: boolean
  previousDate: string | null
  previousMaxWeight: number | null
  currentMaxWeight: number | null
  weightDelta: number | null
  previousVolume: number | null
  currentVolume: number | null
  volumeDelta: number | null
  message: string | null
}

export interface CountPointVO {
  key: string
  label: string
  value: number
}

export interface PartCountVO {
  partId: number
  partName: string
  partCode: string
  count: number
}

export interface WorkoutFrequencyVO {
  sessions: CountPointVO[]
  byPart: PartCountVO[]
  totalSessions: number
}

export interface WorkoutVolumeVO {
  start: string
  end: string
  totalVolume: number
  totalReps: number
  totalSets: number
  totalRecords: number
}

export interface PersonalBestVO {
  exerciseId: number
  exerciseName: string
  partName: string | null
  maxWeight: number
  reps: number
  achievedDate: string
}

// ---------- 记账 ----------

export interface CategoryVO {
  id: number
  code: string
  name: string
  type: TransactionType
  icon: string | null
  sortOrder: number
  isSystem: boolean
}

export interface TransactionVO {
  id: number
  type: TransactionType
  amount: number
  categoryId: number
  categoryName: string | null
  categoryIcon: string | null
  occurDate: string
  occurTime: string | null
  note: string | null
}

export interface TransactionRequest {
  type: TransactionType
  amount: number
  categoryId: number
  occurDate?: string | null
  occurTime?: string | null
  note?: string | null
}

export interface FinanceOverviewVO {
  date: string
  todayExpense: number
  weekExpense: number
  monthIncome: number
  monthExpense: number
  monthBalance: number
}

export interface CategoryStatVO {
  categoryId: number
  code: string | null
  categoryName: string
  icon: string | null
  amount: number
  percent: number
}

export interface PeriodAmountVO {
  key: string
  label: string
  income: number
  expense: number
}

// ---------- 任务 ----------

export interface TaskVO {
  id: number
  title: string
  description: string | null
  taskType: TaskType
  priority: number
  planDate: string | null
  dueDate: string | null
  recurrenceType: RecurrenceType
  recurrenceInterval: number
  recurrenceEndDate: string | null
  status: TaskStatus
  recurring: boolean
  /** 在日历上落位的日期；长期任务为 null，因为它不占日历 */
  anchorDate: string | null
}

export interface TaskOccurrenceVO {
  occurrenceId: number
  taskId: number
  title: string
  description: string | null
  taskType: TaskType
  recurrenceType: RecurrenceType
  priority: number
  occurDate: string
  status: TaskStatus
  completedAt: string | null
  recurring: boolean
}

export interface TaskRequest {
  title: string
  description?: string | null
  taskType: TaskType
  priority?: number | null
  planDate?: string | null
  dueDate?: string | null
  recurrenceType?: RecurrenceType | null
  recurrenceInterval?: number | null
  recurrenceEndDate?: string | null
}

export interface TodayTasksVO {
  date: string
  items: TaskOccurrenceVO[]
  total: number
  done: number
  completionRate: number
}

export interface CalendarDayVO {
  date: string
  total: number
  done: number
}

export interface CompletionStatsVO {
  start: string
  end: string
  total: number
  done: number
  completionRate: number
}

// ---------- 仪表盘 ----------

export interface DashboardOverviewVO {
  date: string
  tasks: {
    total: number
    done: number
    completionRate: number
    pending: { taskId: number; title: string; occurDate: string }[]
  }
  weight: {
    recordDate: string | null
    weightKg: number | null
    delta: number | null
    bmi: number | null
    bmiCategory: string | null
  }
  workout: {
    exerciseCount: number
    exerciseNames: string[]
    totalVolume: number
  }
  finance: {
    todayExpense: number
    weekExpense: number
    monthIncome: number
    monthExpense: number
    monthBalance: number
  }
}

// ---------- AI ----------

export type ActionStatus = 'NONE' | 'PENDING' | 'CONFIRMED' | 'REJECTED' | 'EXECUTED' | 'FAILED'

export interface AiStatusVO {
  configured: boolean
  model: string
}

export interface AiMessageVO {
  id: number
  role: 'USER' | 'ASSISTANT' | 'SYSTEM'
  content: string
  intent: string | null
  actionStatus: ActionStatus
  /** 待确认草稿的可读预览，落库前给用户看清楚要记什么 */
  draftPreview: string | null
  createdAt: string | null
}

export interface ChatResponse {
  conversationId: number
  userMessage: AiMessageVO
  assistantMessage: AiMessageVO
}

export interface AiSummaryVO {
  date: string
  content: string
  generatedAt: string | null
  /** true 表示读的是缓存，没有重新调用模型 */
  cached: boolean
}

export interface ConversationVO {
  id: number
  title: string
  updatedAt: string | null
}

export interface AnalysisVO {
  content: string
}

/** AI 对我的长期画像记忆。永久保留，只有我确认过的才会存进来 */
export interface AiMemoryVO {
  id: number
  content: string
  /** PROFILE 基本资料 / GOAL 目标 / PREFERENCE 偏好 / HABIT 习惯 / OTHER */
  category: string
  source: string
  createdAt: string | null
}
