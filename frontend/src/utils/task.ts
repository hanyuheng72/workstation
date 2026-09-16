import type { TaskOccurrenceVO, TaskVO } from '@/types/domain'

/**
 * 把「某天的任务实例」转成编辑表单需要的任务形状。
 *
 * 实例里只有当天那一条的信息，任务的日期/重复规则不在其中，
 * 所以日期取实例所在的那天、重复规则按原样带回——
 * 编辑时必须把这些原值传回去，否则后端会把「字段为空」当成「改成默认值」，
 * 把任务挪到今天、或者把重复任务改成不重复。
 */
export function occurrenceToTask(item: TaskOccurrenceVO): TaskVO {
  return {
    id: item.taskId,
    title: item.title,
    description: item.description,
    taskType: item.taskType,
    priority: item.priority,
    planDate: item.occurDate,
    dueDate: item.taskType === 'LONG_TERM' ? item.occurDate : null,
    recurrenceType: item.recurrenceType,
    recurrenceInterval: 1,
    recurrenceEndDate: null,
    status: item.status,
    recurring: item.recurring,
    anchorDate: item.occurDate,
  }
}
