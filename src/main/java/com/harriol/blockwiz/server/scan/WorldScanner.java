package com.harriol.blockwiz.server.scan;

import com.harriol.blockwiz.common.model.BlockCounts;
import com.harriol.blockwiz.common.model.Box;
import com.harriol.blockwiz.common.model.Pos;
import com.harriol.blockwiz.common.model.ScanGeometry;
import com.harriol.blockwiz.common.model.ScanSummary;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

/**
 * 服务端环境扫描：以玩家为中心、半径 scanRadius 的方形邻域（PRD §5.2）。
 * 每 tick 至多处理 {@value #SLICE_SIZE} 个位置，避免阻塞主线程；
 * 扫描区域跨越未加载区块时拒绝执行（不自动加载区块）。
 *
 * @author Harriol
 */
public final class WorldScanner {

    /**
     * 每 tick 扫描的位置上限。
     * 1024/tick 时半径 64（129³ ≈ 215 万格）需约 105 秒，过长；4096/tick 下约 26 秒，
     * getBlockState 为主线程毫秒级开销，仍不影响 TPS。
     */
    public static final int SLICE_SIZE = 4096;

    /** 未加载区块错误（i18n 键）。 */
    public static final String ERROR_UNLOADED = "command.scan.unloaded";

    private WorldScanner() {
    }

    /**
     * 启动一次分片扫描（在服务端主线程创建，随后每 tick 调用 {@link ScanSession#tick()}）。
     *
     * @param level  服务端世界
     * @param center 扫描中心（玩家所在方块）
     * @param radius 扫描半径
     * @param facing 玩家朝向（DIRECTION 枚举名）
     * @return 扫描会话
     */
    public static ScanSession start(ServerLevel level, BlockPos center, int radius, String facing) {
        Pos centerPos = new Pos(center.getX(), center.getY(), center.getZ());
        return new ScanSession(level, ScanGeometry.scanBounds(centerPos, radius), centerPos, radius, facing);
    }

    /**
     * 启动一次范围扫描（在服务端主线程创建）：扫描本体为玩家手动指定的范围
     * （先 /blockwiz range 设定范围，再输入自然语言描述时使用）。
     *
     * @param level  服务端世界
     * @param bounds 手动指定范围
     * @param facing 玩家朝向（DIRECTION 枚举名）
     * @return 扫描会话
     */
    public static ScanSession start(ServerLevel level, Box bounds, String facing) {
        return new ScanSession(level, bounds, ScanGeometry.centerOf(bounds),
                Math.max(bounds.edgeX(), Math.max(bounds.edgeY(), bounds.edgeZ())), facing);
    }

    /** 一次扫描会话：持有游标与中间统计，由主线程逐 tick 推进。 */
    public static final class ScanSession {

        private final ServerLevel level;
        private final Pos center;
        private final int radius;
        private final String facing;
        private final Box bounds;
        private final long total;
        private final BlockCounts counts = new BlockCounts();

        private long cursor;
        private int nonAir;
        private int minX = Integer.MAX_VALUE;
        private int minY = Integer.MAX_VALUE;
        private int minZ = Integer.MAX_VALUE;
        private int maxX = Integer.MIN_VALUE;
        private int maxY = Integer.MIN_VALUE;
        private int maxZ = Integer.MIN_VALUE;

        private boolean done;
        private boolean failed;
        private String failReason;
        private ScanSummary summary;
        private Consumer<ScanSummary> onComplete;

        private ScanSession(ServerLevel level, Box raw, Pos center, int radius, String facing) {
            this.level = level;
            this.center = center;
            this.radius = radius;
            this.facing = facing;
            // Y 方向裁剪到世界高度，防止扫描虚空/上限外
            int minY = Math.max(raw.min().y(), level.getMinY());
            int maxY = Math.min(raw.max().y(), level.getMaxY() - 1);
            if (maxY < minY) {
                minY = level.getMinY();
                maxY = level.getMinY();
            }
            this.bounds = Box.fromCorners(raw.min().x(), minY, raw.min().z(), raw.max().x(), maxY, raw.max().z());
            this.total = bounds.volume();
        }

        /**
         * 处理下一个扫描分片。应在服务端主线程逐 tick 调用。
         *
         * @return 会话是否已结束（完成或失败）
         */
        public boolean tick() {
            if (done) {
                return true;
            }
            long end = Math.min(cursor + SLICE_SIZE, total);
            for (long i = cursor; i < end; i++) {
                int[] xyz = ScanGeometry.coordinateAt(bounds, i);
                BlockPos pos = new BlockPos(xyz[0], xyz[1], xyz[2]);
                if (!level.isLoaded(pos)) {
                    failed = true;
                    failReason = ERROR_UNLOADED;
                    done = true;
                    return true;
                }
                BlockState state = level.getBlockState(pos);
                if (!state.isAir()) {
                    counts.add(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
                    nonAir++;
                    minX = Math.min(minX, xyz[0]);
                    minY = Math.min(minY, xyz[1]);
                    minZ = Math.min(minZ, xyz[2]);
                    maxX = Math.max(maxX, xyz[0]);
                    maxY = Math.max(maxY, xyz[1]);
                    maxZ = Math.max(maxZ, xyz[2]);
                }
            }
            cursor = end;
            if (cursor >= total) {
                finish();
            }
            return done;
        }

        private void finish() {
            Box blockBounds = nonAir > 0
                    ? Box.fromCorners(minX, minY, minZ, maxX, maxY, maxZ)
                    : null;
            summary = new ScanSummary(center, radius, bounds, facing, cursor, nonAir, blockBounds, counts.top(10));
            done = true;
            if (onComplete != null) {
                onComplete.accept(summary);
            }
        }

        /** 是否已结束（含失败）。 */
        public boolean isDone() {
            return done;
        }

        /** 是否因未加载区块等原因失败。 */
        public boolean isFailed() {
            return failed;
        }

        /** 失败原因（i18n 键，未失败时为 null）。 */
        public String failReason() {
            return failReason;
        }

        /** 完成的扫描摘要（未完成时为 null）。 */
        public ScanSummary getSummary() {
            return summary;
        }

        /** 扫描范围（已裁剪世界高度）。 */
        public Box bounds() {
            return bounds;
        }

        /** 待扫描位置总数。 */
        public long total() {
            return total;
        }

        /** 已扫描位置数（进度反馈用）。 */
        public long scanned() {
            return cursor;
        }

        /** 注册完成回调（在 tick() 内部同步调用）。 */
        public void onComplete(Consumer<ScanSummary> callback) {
            this.onComplete = callback;
        }
    }
}
