package com.host1syan.claudecn;

/** 报告进程实际加载的 native 库所属 ABI；库缺失时降级为「未加载」，不影响界面使用。 */
public final class NativeAbi {
    private static final boolean LOADED;
    private static final String ABI;

    static {
        boolean ok;
        String value = "未加载";
        try {
            System.loadLibrary("claudecn");
            value = nativeAbi();
            ok = true;
        } catch (Throwable t) {
            ok = false;
        }
        LOADED = ok;
        ABI = value;
    }

    private NativeAbi() {
    }

    private static native String nativeAbi();

    public static boolean isLoaded() {
        return LOADED;
    }

    public static String abi() {
        return ABI;
    }
}
