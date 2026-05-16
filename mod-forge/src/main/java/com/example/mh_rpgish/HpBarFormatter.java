package com.example.mh_rpgish;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class HpBarFormatter {
    private HpBarFormatter() {}

    private static final String[] PARTIAL_FILLED = {"", "▎", "▌", "▊"};
    private static final String[] PARTIAL_EMPTY  = {"", "▊", "▌", "▎"};
    private static final char FULL_BLOCK = '█';
    private static final String FULL_BAR_TEXT = "██████████";

    public static Component bar(float hp, float maxHp) {
        if (maxHp <= 0f) return Component.empty();

        int ratio = (int) (hp * 40f / maxHp);
        if (ratio < 0) ratio = 0;
        if (ratio > 40) ratio = 40;

        if (ratio >= 40) {
            return Component.literal(FULL_BAR_TEXT).withStyle(ChatFormatting.GREEN);
        }

        int filledCells = ratio / 4;
        int partial = ratio % 4;
        int emptyCells = 10 - filledCells - (partial > 0 ? 1 : 0);

        StringBuilder filled = new StringBuilder();
        for (int i = 0; i < filledCells; i++) filled.append(FULL_BLOCK);
        filled.append(PARTIAL_FILLED[partial]);

        StringBuilder empty = new StringBuilder();
        empty.append(PARTIAL_EMPTY[partial]);
        for (int i = 0; i < emptyCells; i++) empty.append(FULL_BLOCK);

        ChatFormatting filledColor;
        if (ratio <= 4)       filledColor = ChatFormatting.DARK_RED;
        else if (ratio <= 8)  filledColor = ChatFormatting.RED;
        else if (ratio <= 20) filledColor = ChatFormatting.YELLOW;
        else                  filledColor = ChatFormatting.GREEN;

        MutableComponent result = Component.empty();
        if (filled.length() > 0) {
            result.append(Component.literal(filled.toString()).withStyle(filledColor));
        }
        if (empty.length() > 0) {
            result.append(Component.literal(empty.toString()).withStyle(ChatFormatting.DARK_GRAY));
        }
        return result;
    }
}
