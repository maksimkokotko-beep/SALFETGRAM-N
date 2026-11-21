package org.telegram.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SeekBarView;

import java.util.ArrayList;

import xyz.nextalone.nagram.VibeGramStyle;

/**
 * VibeGram: редактор стиля. Ползунки для цветов, прозрачности, радиусов и отступов,
 * режим перетаскивания на экране профиля, пресеты и обмен настройками через JSON.
 */
public class VibeGramCustomizeActivity extends BaseFragment {

    private static final int COLOR_DEFAULT = 0;
    private static final int[] CHIP_COLORS = new int[]{
            COLOR_DEFAULT, 0xFF3B82F6, 0xFF8B5CF6, 0xFF22D3EE, 0xFFF472B6, 0xFFFFB74D
    };

    private final ArrayList<SliderRow> sliderRows = new ArrayList<>();
    private final ArrayList<View> chipViews = new ArrayList<>();

    private TextCell editorCell;
    private TextCell[] presetCells = new TextCell[3];
    private TextView statusView;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Редактор VibeGram");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        final FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        final ScrollView scroll = new ScrollView(context);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, 0, 0, AndroidUtilities.dp(24));
        root.addView(scroll, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        final LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(column, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        addHeader(column, "Общее");
        editorCell = addTextCell(column, "Режим редактора", "таскать элементы на экране профиля", false);
        editorCell.setOnClickListener(v -> {
            VibeGramStyle.setEditor(!VibeGramStyle.editor());
            refreshAll();
            setStatus(VibeGramStyle.editor()
                    ? "Режим включён. Открой свой профиль и тащи FX авы или кнопки пальцем."
                    : "Режим выключен.");
        });
        addInfo(column, "В режиме редактора: тянешь FX аватара и круглые кнопки — позиция сохраняется. Пинч на аватаре меняет масштаб FX.");

        addHeader(column, "Список чатов");
        addSlider(column, "Радиус карточек", VibeGramStyle.CHAT_RADIUS, 0, 40, false, 18, " dp");
        addSlider(column, "Отступ по бокам", VibeGramStyle.CHAT_INSET_H, 0, 24, false, 6, " dp");
        addSlider(column, "Отступ сверху и снизу", VibeGramStyle.CHAT_INSET_V, 0, 16, false, 3, " dp");
        addSlider(column, "Заливка стекла", VibeGramStyle.CHAT_FILL, 0, 100, true, 18, "%");
        addSlider(column, "Обводка", VibeGramStyle.CHAT_RIM, 0, 100, true, 38, "%");
        addSlider(column, "Толщина обводки", VibeGramStyle.CHAT_RIM_W, 0, 5, false, 1, " dp");
        addSlider(column, "Верхняя кромка", VibeGramStyle.CHAT_TOP, 0, 100, true, 90, "%");
        addSlider(column, "Сияние списка", VibeGramStyle.CHAT_GLOW, 0, 150, true, 100, "%");

        addHeader(column, "Цвет сияния");
        addColorChips(column);

        addHeader(column, "Аватар: сияние, NFT, блики");
        addSlider(column, "Северное сияние", VibeGramStyle.FX_AURORA, 0, 200, true, 100, "%");
        addSlider(column, "Блик по фото", VibeGramStyle.FX_GLARE, 0, 200, true, 100, "%");
        addSlider(column, "Стеклянных шариков", VibeGramStyle.FX_ORBS, 0, 12, true, 7, " шт");
        addSlider(column, "Масштаб FX", VibeGramStyle.FX_SCALE, 50, 200, true, 100, "%");

        addHeader(column, "Круглые кнопки профиля");
        addSlider(column, "Размер кнопок", VibeGramStyle.ACTIONS_SIZE, 28, 64, true, 46, " dp");
        addSlider(column, "Прозрачность", VibeGramStyle.ACTIONS_ALPHA, 20, 100, true, 100, "%");

        addHeader(column, "Переходы вкладок");
        addSlider(column, "Уменьшение второй вкладки", VibeGramStyle.TABS_SCALE, 0, 40, true, 14, "%");
        addSlider(column, "Длительность", VibeGramStyle.TABS_DURATION, 200, 900, true, 540, " мс");

        addHeader(column, "Пресеты");
        for (int i = 0; i < 3; i++) {
            final int slot = i;
            final TextCell cell = addTextCell(column, "Пресет " + (i + 1), "", true);
            cell.setOnClickListener(v -> {
                if (VibeGramStyle.hasPreset(slot)) {
                    VibeGramStyle.loadPreset(slot);
                    refreshAll();
                    setStatus("Пресет " + (slot + 1) + " загружен");
                } else {
                    VibeGramStyle.savePreset(slot);
                    refreshAll();
                    setStatus("Пресет " + (slot + 1) + " сохранён");
                }
            });
            cell.setOnLongClickListener(v -> {
                VibeGramStyle.savePreset(slot);
                refreshAll();
                setStatus("Пресет " + (slot + 1) + " перезаписан");
                return true;
            });
            presetCells[i] = cell;
        }
        addInfo(column, "Нажми на пустой пресет — сохранить, на заполненный — загрузить. Удержание — перезаписать.");

        addHeader(column, "Обмен настройками");
        TextCell exportCell = addTextCell(column, "Скопировать настройки (JSON)", "", true);
        exportCell.setOnClickListener(v -> {
            AndroidUtilities.addToClipboard(VibeGramStyle.exportString());
            setStatus("Настройки скопированы в буфер обмена");
        });
        TextCell importCell = addTextCell(column, "Вставить настройки из буфера", "", true);
        importCell.setOnClickListener(v -> {
            final String raw = readClipboard();
            if (raw == null || raw.trim().isEmpty()) {
                setStatus("Буфер обмена пуст");
            } else if (VibeGramStyle.importString(raw)) {
                refreshAll();
                setStatus("Настройки применены");
            } else {
                setStatus("Не получилось: в буфере не JSON настроек");
            }
        });
        TextCell resetCell = addTextCell(column, "Сбросить всё", "", true);
        resetCell.setOnClickListener(v -> {
            VibeGramStyle.resetAll();
            refreshAll();
            setStatus("Сброшено к значениям по умолчанию");
        });

        statusView = new TextView(context);
        statusView.setTextSize(14);
        statusView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        statusView.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(12), AndroidUtilities.dp(18), AndroidUtilities.dp(12));
        column.addView(statusView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        refreshAll();
        fragmentView = root;
        return fragmentView;
    }

    private void refreshAll() {
        for (SliderRow row : sliderRows) {
            row.refresh();
        }
        if (editorCell != null) {
            editorCell.setTextAndValue("Режим редактора", VibeGramStyle.editor() ? "включён" : "выключен", true);
        }
        for (int i = 0; i < presetCells.length; i++) {
            presetCells[i].setTextAndValue("Пресет " + (i + 1), VibeGramStyle.hasPreset(i) ? "сохранён" : "пусто", true);
        }
        refreshChips();
    }

    private void setStatus(CharSequence text) {
        if (statusView != null) {
            statusView.setText(text);
        }
    }

    private String readClipboard() {
        try {
            final ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null || !clipboard.hasPrimaryClip() || clipboard.getPrimaryClip() == null) {
                return null;
            }
            final ClipData clip = clipboard.getPrimaryClip();
            if (clip.getItemCount() <= 0) {
                return null;
            }
            final CharSequence text = clip.getItemAt(0).coerceToText(getContext());
            return text == null ? null : text.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private void addHeader(LinearLayout column, String text) {
        final TextView header = new TextView(getContext());
        header.setText(text);
        header.setTextSize(14);
        header.setTypeface(AndroidUtilities.bold());
        header.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        header.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(16), AndroidUtilities.dp(18), AndroidUtilities.dp(6));
        column.addView(header, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void addInfo(LinearLayout column, String text) {
        final TextView info = new TextView(getContext());
        info.setText(text);
        info.setTextSize(13);
        info.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        info.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(4), AndroidUtilities.dp(18), AndroidUtilities.dp(8));
        column.addView(info, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private TextCell addTextCell(LinearLayout column, String title, String value, boolean divider) {
        final TextCell cell = new TextCell(getContext(), getResourceProvider());
        cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        cell.setTextAndValue(title, value, divider);
        column.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 50));
        return cell;
    }

    private void addSlider(LinearLayout column, String title, String key, float min, float max, boolean integer, float def, String suffix) {
        final SliderRow row = new SliderRow(getContext(), title, key, min, max, integer, def, suffix);
        sliderRows.add(row);
        column.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void addColorChips(LinearLayout column) {
        final LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(6), AndroidUtilities.dp(18), AndroidUtilities.dp(10));

        chipViews.clear();
        for (final int color : CHIP_COLORS) {
            final View chip = new View(getContext());
            chip.setContentDescription(color == COLOR_DEFAULT ? "Цвет темы" : "Цвет сияния");
            chip.setOnClickListener(v -> {
                VibeGramStyle.setInt(VibeGramStyle.CHAT_GLOW_COLOR, color);
                refreshChips();
                setStatus(color == COLOR_DEFAULT ? "Сияние снова в цвете темы" : "Цвет сияния изменён");
            });
            row.addView(chip, LayoutHelper.createLinear(42, 42, Gravity.CENTER_VERTICAL, 4, 0, 4, 0));
            chipViews.add(chip);
        }
        column.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void refreshChips() {
        if (chipViews.size() != CHIP_COLORS.length) {
            return;
        }
        final int selected = VibeGramStyle.glowColor();
        for (int i = 0; i < chipViews.size(); i++) {
            final int color = CHIP_COLORS[i];
            final GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(color == COLOR_DEFAULT ? Theme.getColor(Theme.key_windowBackgroundWhiteBlueText) : color);
            drawable.setStroke(AndroidUtilities.dp(color == selected ? 3 : 1),
                    Theme.getColor(color == selected ? Theme.key_windowBackgroundWhiteBlackText : Theme.key_divider));
            chipViews.get(i).setBackground(drawable);
        }
    }

    private class SliderRow extends LinearLayout {

        private final String key;
        private final float min;
        private final float max;
        private final boolean integer;
        private final float def;
        private final String suffix;
        private final TextView valueView;
        private final SeekBarView seekBar;

        SliderRow(Context context, String title, String key, float min, float max, boolean integer, float def, String suffix) {
            super(context);
            this.key = key;
            this.min = min;
            this.max = max;
            this.integer = integer;
            this.def = def;
            this.suffix = suffix;

            setOrientation(VERTICAL);
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(6), AndroidUtilities.dp(18), 0);

            final LinearLayout titleRow = new LinearLayout(getContext());
            titleRow.setOrientation(HORIZONTAL);

            final TextView titleView = new TextView(getContext());
            titleView.setText(title);
            titleView.setTextSize(15);
            titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            titleRow.addView(titleView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL));

            valueView = new TextView(getContext());
            valueView.setTextSize(15);
            valueView.setGravity(Gravity.END);
            valueView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
            titleRow.addView(valueView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

            addView(titleRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            seekBar = new SeekBarView(getContext());
            seekBar.setDelegate((stop, progress) -> {
                float value = min + progress * (max - min);
                if (integer) {
                    value = Math.round(value);
                }
                valueView.setText(format(value) + suffix);
                if (integer) {
                    VibeGramStyle.setInt(key, Math.round(value));
                } else {
                    VibeGramStyle.setFloat(key, value);
                }
            });
            addView(seekBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 44));
        }

        void refresh() {
            final float value = integer ? VibeGramStyle.getInt(key, Math.round(def)) : VibeGramStyle.getFloat(key, def);
            valueView.setText(format(value) + suffix);
            final float progress = (value - min) / (max - min);
            seekBar.setProgress(Math.max(0f, Math.min(1f, progress)));
        }

        private String format(float value) {
            return integer ? String.valueOf(Math.round(value)) : String.valueOf(Math.round(value * 10f) / 10f);
        }
    }
}