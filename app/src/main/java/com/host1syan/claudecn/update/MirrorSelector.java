package com.host1syan.claudecn.update;

import android.content.Context;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 自动选路：并发探测各加速前缀，按「真能返回安装包」+ 首字节耗时排序。
 *
 * <p>只测状态码和延迟不够：有的代理会对任意地址回 200 却吐一个网页，
 * 所以探测请求带 Range 取文件头 4 字节，必须命中 zip 魔数 PK\3\4 才算可用。</p>
 */
public final class MirrorSelector {
    /**
     * 内置候选池，全部为路径前缀式代理（前缀 + 原始 GitHub 地址）。
     *
     * <p>这 7 条是 40 个候选逐个实测筛出来的：必须国内可直连、且带 Range 请求能返回
     * 真实 zip 魔数。池内顺序只是默认展示顺序，实际用哪条由每次的测速结果决定。</p>
     */
    public static final String[] POOL = {
            "https://gh.monlor.com/",
            "https://gh-proxy.com/",
            "https://ghproxy.imciel.com/",
            "https://ghfast.top/",
            "https://ghproxy.xyz/",
            "https://ghfile.geekertao.top/",
            "https://ghproxy.net/",
    };

    /** 单个探测的超时；并发执行，整体最多等这么久。 */
    private static final int PROBE_TIMEOUT = 6000;
    private static final int PROBE_BYTES = 4;

    /** 一次探测的结果：ms < 0 表示不可用。 */
    public static final class Probe {
        public final String base;
        public final long ms;

        Probe(String base, long ms) {
            this.base = base;
            this.ms = ms;
        }

        public boolean ok() {
            return this.ms >= 0;
        }
    }

    private MirrorSelector() {
    }

    /** 把前缀转成界面展示用的短名字；空串表示官方直连。 */
    public static String label(String base) {
        if (base == null || base.isEmpty()) {
            return "官方直连";
        }
        String value = base.replace("https://", "").replace("http://", "");
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    /**
     * 参与测速的全部候选：内置镜像池 + 用户自定义前缀 + 官方直连（空串）。
     * 官方直连也一起测，才能做到「谁快用谁」，而不只是拿它兜底。
     */
    public static String[] candidates() {
        return candidates("");
    }

    /**
     * 同上，另把用户手填的加速前缀并进候选池。
     *
     * <p>自定义项与内置池同样待遇：一起测速、一起排序，谁快用谁；前缀非法或已在池内
     * 就不重复加入。这样「自定义」在自动选路下也有意义，不必被迫切成手动模式。</p>
     */
    public static String[] candidates(String extra) {
        List<String> all = new ArrayList<String>();
        for (String base : POOL) {
            all.add(base);
        }
        String custom = extraBase(extra);
        if (!custom.isEmpty() && !all.contains(custom)) {
            all.add(custom);
        }
        all.add("");
        return all.toArray(new String[all.size()]);
    }

    /** 候选总数，界面显示「探测 N 条线路」用。 */
    public static int candidateCount() {
        return candidateCount("");
    }

    public static int candidateCount(String extra) {
        return candidates(extra).length;
    }

    /** 自定义前缀先规范化（补尾 /、拒非 https），非法一律当没有。 */
    private static String extraBase(String extra) {
        String value = UpdateSettings.normalizeMirrorBase(extra);
        return value == null ? "" : value;
    }

    /**
     * 并发探测全部候选，可用项按耗时升序排在前面。
     *
     * <p>所有探测线程共用一个绝对截止时间，超时未回的按不可用处理。这样总耗时
     * 恒定为 PROBE_TIMEOUT + 2s，不会随候选池变长而线性叠加。</p>
     */
    public static List<Probe> probeAll(final String url) {
        return probeAll(url, "");
    }

    /**
     * 并发探测「内置池 + 自定义前缀 + 官方直连」，可用项按耗时升序排在前面。
     *
     * <p>所有探测线程共用一个绝对截止时间，超时未回的按不可用处理。这样总耗时
     * 恒定为 PROBE_TIMEOUT + 2s，不会随候选池变长而线性叠加。</p>
     */
    public static List<Probe> probeAll(final String url, String extra) {
        final String[] targets = candidates(extra);
        final Probe[] slots = new Probe[targets.length];
        Thread[] threads = new Thread[targets.length];
        for (int i = 0; i < targets.length; i++) {
            final int index = i;
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    slots[index] = new Probe(targets[index], probe(targets[index], url));
                }
            }, "mirror-probe-" + i);
            threads[i].setDaemon(true);
            threads[i].start();
        }
        final long deadline = System.currentTimeMillis() + PROBE_TIMEOUT + 2000L;
        for (Thread thread : threads) {
            long left = deadline - System.currentTimeMillis();
            if (left <= 0L) {
                break;
            }
            try {
                thread.join(left);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        List<Probe> results = new ArrayList<Probe>();
        for (int i = 0; i < slots.length; i++) {
            results.add(slots[i] == null ? new Probe(targets[i], -1L) : slots[i]);
        }
        Collections.sort(results, new Comparator<Probe>() {
            @Override
            public int compare(Probe left, Probe right) {
                boolean leftOk = left.ok();
                boolean rightOk = right.ok();
                if (leftOk != rightOk) {
                    return leftOk ? -1 : 1;
                }
                if (leftOk) {
                    return left.ms < right.ms ? -1 : (left.ms > right.ms ? 1 : 0);
                }
                return 0;
            }
        });
        return results;
    }

    /**
     * 由探测结果得到下载顺序。官方直连也参与测速，排到它该在的位置；
     * 若它不可用或没被测到，仍然补在末尾兜底——镜像全挂时还能直接连 GitHub。
     */
    public static List<String> rankFrom(List<Probe> probes) {
        List<String> bases = new ArrayList<String>();
        boolean hasOfficial = false;
        for (Probe probe : probes) {
            if (!probe.ok()) {
                continue;
            }
            if (probe.base == null || probe.base.isEmpty()) {
                hasOfficial = true;
            }
            if (!bases.contains(probe.base)) {
                bases.add(probe.base);
            }
        }
        if (!hasOfficial) {
            bases.add("");
        }
        return bases;
    }

    /** 带缓存的排序结果：同一下载地址在 TTL 内复用，过期重新测速。 */
    public static List<String> cachedRank(Context context, String url) {
        return cachedRank(context, url, "");
    }

    /**
     * 同上，并把用户自定义前缀也纳入候选。
     *
     * <p>缓存里没有刚保存的自定义前缀时不能直接复用：那样它要等缓存过期（最长 6 小时）
     * 才会参与测速。命中缓存但不含该前缀，就强制重测一次。</p>
     */
    public static List<String> cachedRank(Context context, String url, String extra) {
        String cached = UpdateSettings.cachedRank(context, System.currentTimeMillis());
        if (cached != null) {
            List<String> bases = decodeRank(cached);
            String custom = extraBase(extra);
            if (custom.isEmpty() || bases.contains(custom)) {
                return bases;
            }
        }
        return refreshRank(context, url, extra);
    }

    /** 忽略缓存强制重新测速，并把结果写回缓存。界面「重新测速」用这个。 */
    public static List<String> refreshRank(Context context, String url) {
        return refreshRank(context, url, "");
    }

    /** 同上，自定义前缀一起参与测速。 */
    public static List<String> refreshRank(Context context, String url, String extra) {
        List<String> bases = rankFrom(probeAll(url, extra));
        UpdateSettings.setCachedRank(context, encodeRank(bases), System.currentTimeMillis());
        return bases;
    }

    /** 测一次可用的 HTTP 耗时；返回 -1 表示不可用。 */
    private static long probe(String base, String url) {
        HttpURLConnection conn = null;
        long start = System.currentTimeMillis();
        try {
            conn = (HttpURLConnection) new URL(base + url).openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(PROBE_TIMEOUT);
            conn.setReadTimeout(PROBE_TIMEOUT);
            conn.setRequestProperty("Range", "bytes=0-" + (PROBE_BYTES - 1));
            conn.setRequestProperty("Accept-Encoding", "identity");
            conn.setRequestProperty("User-Agent", "ClaudeCN-Android");
            int code = conn.getResponseCode();
            if (code != 200 && code != 206) {
                return -1L;
            }
            byte[] head = new byte[PROBE_BYTES];
            int read = 0;
            InputStream in = conn.getInputStream();
            try {
                while (read < PROBE_BYTES) {
                    int n = in.read(head, read, PROBE_BYTES - read);
                    if (n <= 0) {
                        break;
                    }
                    read += n;
                }
            } finally {
                in.close();
            }
            if (read < PROBE_BYTES
                    || head[0] != 0x50 || head[1] != 0x4B
                    || head[2] != 0x03 || head[3] != 0x04) {
                return -1L;
            }
            return System.currentTimeMillis() - start;
        } catch (Exception e) {
            return -1L;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /** 把有序线路列表序列化为缓存字符串（'\n' 分隔，可含空串表示官方直连）。 */
    public static String encodeRank(List<String> bases) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bases.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(bases.get(i));
        }
        return sb.toString();
    }

    /** encodeRank 的反向操作；空内容解出一个「官方直连」项。 */
    public static List<String> decodeRank(String value) {
        List<String> bases = new ArrayList<String>();
        if (value == null) {
            bases.add("");
            return bases;
        }
        int start = 0;
        while (start <= value.length()) {
            int end = value.indexOf('\n', start);
            if (end < 0) {
                end = value.length();
            }
            bases.add(value.substring(start, end));
            start = end + 1;
        }
        return bases;
    }
}
