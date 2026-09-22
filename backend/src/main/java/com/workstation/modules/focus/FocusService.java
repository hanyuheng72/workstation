package com.workstation.modules.focus;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.workstation.common.exception.BusinessException;
import com.workstation.modules.focus.dto.DailyFocusRow;
import com.workstation.modules.focus.dto.DailyFocusVO;
import com.workstation.modules.focus.dto.FocusSessionVO;
import com.workstation.modules.focus.dto.FocusStartRequest;
import com.workstation.modules.focus.dto.FocusStatsVO;
import com.workstation.modules.focus.dto.FocusTotalsRow;
import com.workstation.modules.focus.entity.FailReason;
import com.workstation.modules.focus.entity.FocusSession;
import com.workstation.modules.focus.entity.FocusStatus;
import com.workstation.modules.focus.mapper.FocusSessionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 自习室。
 *
 * 倒计时的权威在服务端：started_at 落库之后，剩余时间一律由「真实时钟」推算，
 * 客户端只是把服务端给的秒数画出来。所以改手机时间、直接调接口都篡改不了结果，
 * complete 也会拒绝「时间还没走满」的请求。
 *
 * 规则（与用户确认过的）：
 *   - 每场只有一次暂停机会。第一次按暂停就是普通的暂停；
 *     机会用掉之后再按暂停，不再提示，直接整场判失败。
 *   - 暂停期间倒计时不走，累计时长记在 paused_seconds，截止时刻相应后移。
 *   - 切到别的 App / 锁屏 / 关掉页面都不算中断，时间照走——这条是宽的一面，
 *     代价是「开始之后立刻关掉，过一小时再打开」也会记成成功。单人自用可接受。
 *   - 提前学完可以主动结束，一样记成功，但至少要学满 MIN_EARLY_SECONDS。
 *
 * 时长统计一律用 actual_seconds（真正专注的秒数），不用 planned_minutes：
 * 提前结束的场次只学了十几分钟却按设定的 45 分钟记账，那这个数字就是假的。
 */
@Service
public class FocusService {

    /** 每日统计与趋势的回溯窗口 */
    private static final int TREND_DAYS = 30;

    /** 提前结束的门槛。低于它不给结束，否则「开一下就算成功」会架空这条规则 */
    private static final int MIN_EARLY_SECONDS = 60;

    private final FocusSessionMapper focusSessionMapper;

    public FocusService(FocusSessionMapper focusSessionMapper) {
        this.focusSessionMapper = focusSessionMapper;
    }

    /** 进行中的那一场；没有则返回 null */
    public FocusSessionVO active() {
        FocusSession session = findActive();
        return session == null ? null : toVO(session);
    }

    @Transactional
    public FocusSessionVO start(FocusStartRequest request) {
        settleElapsedActive();

        if (findActive() != null) {
            throw BusinessException.conflict("还有一场专注没结束，先把它结束再开新的");
        }

        FocusSession session = new FocusSession();
        session.setSessionDate(LocalDate.now());
        session.setSubject(request.subject().trim());
        session.setPlannedMinutes(request.plannedMinutes());
        session.setStatus(FocusStatus.RUNNING);
        session.setStartedAt(LocalDateTime.now());
        session.setPauseUsed(false);
        session.setPausedSeconds(0);
        session.setEndedEarly(false);
        session.setActualSeconds(0);
        focusSessionMapper.insert(session);
        return toVO(session);
    }

    /**
     * 暂停。机会只有一次：已经用掉还想再暂停，这一场直接判失败并返回。
     * 前端收到 FAILED 就把界面切成「专注失败」，不需要再弹二次确认。
     */
    @Transactional
    public FocusSessionVO pause(Long id) {
        FocusSession session = requireActive(id);

        if (session.getStatus() == FocusStatus.PAUSED) {
            throw BusinessException.conflict("已经暂停中了");
        }

        if (Boolean.TRUE.equals(session.getPauseUsed())) {
            finish(session, FocusStatus.FAILED, FailReason.PAUSE_EXHAUSTED, false);
            return toVO(session);
        }

        session.setPauseUsed(true);
        session.setPausedAt(LocalDateTime.now());
        session.setStatus(FocusStatus.PAUSED);
        focusSessionMapper.updateById(session);
        return toVO(session);
    }

    /** 继续。把这一轮暂停的时长记进账，截止时刻随之顺延 */
    @Transactional
    public FocusSessionVO resume(Long id) {
        FocusSession session = requireActive(id);

        if (session.getStatus() != FocusStatus.PAUSED) {
            throw BusinessException.conflict("当前不在暂停中");
        }

        session.setPausedSeconds(session.getPausedSeconds() + secondsSincePause(session));
        session.setPausedAt(null);
        session.setStatus(FocusStatus.RUNNING);
        focusSessionMapper.updateById(session);
        return toVO(session);
    }

    /**
     * 走满设定时长后结束。服务端按真实时钟校验，
     * 时间没到就拒绝——这是防止「点一下就说自己学完了」的那道关。
     */
    @Transactional
    public FocusSessionVO complete(Long id) {
        FocusSession session = requireActive(id);

        if (session.getStatus() == FocusStatus.PAUSED) {
            throw BusinessException.conflict("还在暂停中，先继续再结束");
        }
        if (LocalDateTime.now().isBefore(deadlineOf(session))) {
            throw BusinessException.badRequest("还没到时间，再坚持一下");
        }

        finish(session, FocusStatus.SUCCESS, null, false);
        return toVO(session);
    }

    /**
     * 提前学完了。不比设定时长，但和走满一样记成功。
     *
     * 少于 1 分钟不给结束：不然开始之后马上点一下就是一次成功，
     * 「专注成功」这个数字也就没有意义了。暂停中也能提前结束，
     * 暂停的那段不算进 actual_seconds。
     */
    @Transactional
    public FocusSessionVO finishEarly(Long id) {
        FocusSession session = requireActive(id);

        if (focusedSeconds(session) < MIN_EARLY_SECONDS) {
            throw BusinessException.badRequest("至少要学满 1 分钟才能提前结束");
        }

        finish(session, FocusStatus.SUCCESS, null, true);
        return toVO(session);
    }

    @Transactional
    public FocusSessionVO abandon(Long id) {
        FocusSession session = requireActive(id);
        finish(session, FocusStatus.FAILED, FailReason.ABANDONED, false);
        return toVO(session);
    }

    /** 历史列表。默认最近 30 天 */
    public List<FocusSessionVO> list(LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(TREND_DAYS - 1) : from;

        return focusSessionMapper.selectList(
                        Wrappers.lambdaQuery(FocusSession.class)
                                .between(FocusSession::getSessionDate, start, end)
                                .orderByDesc(FocusSession::getStartedAt))
                .stream()
                .map(FocusService::toVO)
                .toList();
    }

    public FocusStatsVO stats() {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(TREND_DAYS - 1);

        Map<LocalDate, DailyFocusRow> rows = focusSessionMapper.selectDaily(from, today).stream()
                .collect(Collectors.toMap(DailyFocusRow::sessionDate, Function.identity()));
        FocusTotalsRow totals = focusSessionMapper.selectTotals();

        DailyFocusRow todayRow = rows.getOrDefault(today, DailyFocusRow.empty(today));
        int totalSuccess = totals.successCount();
        int totalFail = totals.failCount();
        int finished = totalSuccess + totalFail;

        return new FocusStatsVO(
                toMinutes(todayRow.successSeconds()),
                todayRow.successCount(),
                todayRow.failCount(),
                toMinutes(totals.successSeconds()),
                totalSuccess,
                totalFail,
                finished == 0 ? 0 : Math.round(totalSuccess * 100f / finished),
                dailyRows(rows, from, today));
    }

    // ---------------- 内部 ----------------

    /** 把区间里没有记录的日子补成 0，让趋势图连续 */
    private static List<DailyFocusVO> dailyRows(Map<LocalDate, DailyFocusRow> rows,
                                                LocalDate from, LocalDate to) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<DailyFocusVO> daily = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            DailyFocusRow row = rows.getOrDefault(date, DailyFocusRow.empty(date));
            daily.add(new DailyFocusVO(date, toMinutes(row.successSeconds()),
                    row.successCount(), row.failCount()));
        }
        return daily;
    }

    /** 秒 → 分钟。先把整段区间的秒数加起来再换算，逐场取整会积少成多 */
    private static int toMinutes(Integer seconds) {
        return seconds == null ? 0 : Math.round(seconds / 60f);
    }

    private FocusSession findActive() {
        return focusSessionMapper.selectOne(
                Wrappers.lambdaQuery(FocusSession.class)
                        .in(FocusSession::getStatus, FocusStatus.RUNNING, FocusStatus.PAUSED)
                        .orderByDesc(FocusSession::getId)
                        .last("LIMIT 1"));
    }

    /**
     * 倒计时走满之后没人来点结束的场次，在开新一场时补记为成功。
     * 没有这一步，一场忘了结束的专注会把后面所有新场次都堵住。
     * 暂停中的不补——它需要人回来决定继续还是放弃。
     */
    private void settleElapsedActive() {
        FocusSession active = findActive();
        if (active == null || active.getStatus() != FocusStatus.RUNNING) {
            return;
        }
        if (LocalDateTime.now().isBefore(deadlineOf(active))) {
            return;
        }
        finish(active, FocusStatus.SUCCESS, null, false);
    }

    /**
     * 倒计时到点的时刻。暂停期间不算数，所以要把累计暂停时长加回来；
     * 正暂停着的那一轮按「已经暂停了多久」整体后移，这样暂停中剩余时间保持不变。
     */
    private static LocalDateTime deadlineOf(FocusSession session) {
        LocalDateTime deadline = session.getStartedAt()
                .plusMinutes(session.getPlannedMinutes())
                .plusSeconds(session.getPausedSeconds());

        if (session.getStatus() == FocusStatus.PAUSED && session.getPausedAt() != null) {
            deadline = deadline.plusSeconds(Duration.between(session.getPausedAt(), LocalDateTime.now()).getSeconds());
        }
        return deadline;
    }

    private static long remainingSeconds(FocusSession session) {
        if (session.getStatus() == null || !session.getStatus().isActive()) {
            return 0;
        }
        return Math.max(Duration.between(LocalDateTime.now(), deadlineOf(session)).getSeconds(), 0);
    }

    /**
     * 到现在为止真正专注的秒数：墙钟减去累计暂停。正在暂停中的那一轮也要扣掉——
     * 否则「暂停着去点提前结束」会把暂停的那段也算成专注。
     */
    private static int focusedSeconds(FocusSession session) {
        long seconds = Duration.between(session.getStartedAt(), LocalDateTime.now()).getSeconds()
                - (session.getPausedSeconds() == null ? 0 : session.getPausedSeconds());

        if (session.getStatus() == FocusStatus.PAUSED && session.getPausedAt() != null) {
            seconds -= Duration.between(session.getPausedAt(), LocalDateTime.now()).getSeconds();
        }
        return (int) Math.max(seconds, 0);
    }

    private static int secondsSincePause(FocusSession session) {
        if (session.getPausedAt() == null) {
            return 0;
        }
        return (int) Math.max(Duration.between(session.getPausedAt(), LocalDateTime.now()).getSeconds(), 0);
    }

    /**
     * 收尾。actual_seconds 记的是真正专注的秒数，并以设定时长为上限——
     * 时间走满之后人不在、过一阵才回来结算的场次，墙钟早就超了，
     * 但那不叫多学了，不该计进去。
     */
    private void finish(FocusSession session, FocusStatus status, FailReason reason, boolean endedEarly) {
        int planned = session.getPlannedMinutes() * 60;
        session.setActualSeconds(Math.min(focusedSeconds(session), planned));
        session.setEndedEarly(endedEarly);
        session.setStatus(status);
        session.setFailReason(reason);
        session.setEndedAt(LocalDateTime.now());
        session.setPausedAt(null);
        focusSessionMapper.updateById(session);
    }

    private FocusSession requireActive(Long id) {
        FocusSession session = focusSessionMapper.selectById(id);
        if (session == null) {
            throw BusinessException.notFound("找不到这一场专注");
        }
        if (!session.getStatus().isActive()) {
            throw BusinessException.conflict("这一场已经结束了");
        }
        return session;
    }

    private static FocusSessionVO toVO(FocusSession session) {
        return new FocusSessionVO(
                session.getId(),
                session.getSessionDate(),
                session.getSubject(),
                session.getPlannedMinutes(),
                session.getStatus(),
                session.getFailReason(),
                session.getStartedAt(),
                session.getEndedAt(),
                Boolean.TRUE.equals(session.getPauseUsed()),
                session.getPausedSeconds() == null ? 0 : session.getPausedSeconds(),
                Boolean.TRUE.equals(session.getEndedEarly()),
                session.getActualSeconds() == null ? 0 : session.getActualSeconds(),
                remainingSeconds(session));
    }
}
