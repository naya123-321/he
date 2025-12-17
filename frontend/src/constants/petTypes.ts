export type PetTypeOption = {
  label: string;
  value: string;
  emoji?: string;
};

export type PetTypeGroup = {
  label: string;
  options: PetTypeOption[];
};

/**
 * 全项目统一的"宠物类型"下拉框选项（与预约服务页一致：常见宠物 + 其他宠物）
 * 包含所有预约服务页（BookService.vue）中的完整选项
 */
export const PET_TYPE_GROUPS: PetTypeGroup[] = [
  {
    label: "常见宠物",
    options: [
      { label: "猫", value: "cat", emoji: "🐱" },
      { label: "狗", value: "dog", emoji: "🐶" },
      { label: "兔子", value: "rabbit", emoji: "🐰" },
      { label: "仓鼠", value: "hamster", emoji: "🐹" },
      { label: "鸟", value: "bird", emoji: "🐦" },
    ],
  },
  {
    label: "其他宠物",
    options: [
      { label: "龙猫", value: "chinchilla" },
      { label: "豚鼠", value: "guinea-pig" },
      { label: "刺猬", value: "hedgehog", emoji: "🦔" },
      { label: "荷兰猪", value: "guinea-pig-2" },
      { label: "雪貂", value: "ferret" },
      { label: "爬行动物", value: "reptile", emoji: "🦎" },
      { label: "鱼类", value: "fish", emoji: "🐠" },
      { label: "其他", value: "other" },
    ],
  },
];

export function getPetTypeLabel(value?: string) {
  if (!value) return "";
  for (const g of PET_TYPE_GROUPS) {
    const hit = g.options.find(o => o.value === value);
    if (hit) return hit.label;
  }
  return value;
}


