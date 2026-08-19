package com.harriol.blockwiz.common.model;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 扫描范围内的方块类型计数（按注册表 ID）。
 *
 * @author Harriol
 */
public final class BlockCounts {

    private final Map<String, Integer> counts = new HashMap<>();

    /** 记录一个方块。 */
    public void add(String blockId) {
        counts.merge(blockId, 1, Integer::sum);
    }

    /** 某个方块类型的数量。 */
    public int get(String blockId) {
        return counts.getOrDefault(blockId, 0);
    }

    /** 全部方块总数。 */
    public int total() {
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * 数量 Top-N，数量相同按 ID 字典序稳定排序。
     *
     * @param limit 最多返回条数（≤0 返回空列表）
     * @return 排序后的统计列表
     */
    public List<BlockCount> top(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return counts.entrySet().stream()
                .map(entry -> new BlockCount(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(BlockCount::count).reversed()
                        .thenComparing(BlockCount::id))
                .limit(limit)
                .toList();
    }
}
