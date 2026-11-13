package com.host1syan.claudecn.ui;

public class Palette {
    public final int accent;
    public final int accentSoft;
    public final int bg;
    public final int border;
    public final int danger;
    public final int dangerSoft;
    public final int divider;
    public final int hint;
    public final int onAccent;
    public final int success;
    public final int surface;
    public final int surfaceAlt;
    public final int tabActiveBg;
    public final int tabBarBg;
    public final int textMuted;
    public final int textPrimary;
    public final int textSecondary;
    public final int warning;

    private Palette(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10, int i11, int i12, int i13, int i14, int i15, int i16, int i17, int i18) {
        this.bg = i;
        this.surface = i2;
        this.surfaceAlt = i3;
        this.tabBarBg = i4;
        this.tabActiveBg = i5;
        this.border = i6;
        this.textPrimary = i7;
        this.textSecondary = i8;
        this.textMuted = i9;
        this.hint = i10;
        this.accent = i11;
        this.accentSoft = i12;
        this.onAccent = i13;
        this.danger = i14;
        this.dangerSoft = i15;
        this.divider = i16;
        this.success = i17;
        this.warning = i18;
    }

    public static Palette dark() {
        return new Palette(-15263718, -14737116, -14013135, -15263718, -14013135, -13420740, -855051, -6248786, -9538178, -10722452, -9721601, -14796193, -16051680, -889491, -12967388, -14013135, -12724596, -1002455);
    }

    public static Palette light() {
        return new Palette(-526086, -1, -986379, -1, -1511170, -1841430, -15329252, -10525072, -7695973, -5722442, -14326805, -1511170, -1, -2349530, -72990, -1446673, -15293622, -2525434);
    }
}
