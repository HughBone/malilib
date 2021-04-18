package fi.dy.masa.malilib.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.lwjgl.input.Keyboard;
import fi.dy.masa.malilib.MaLiLibConfigs;
import fi.dy.masa.malilib.action.ActionContext;
import fi.dy.masa.malilib.action.ActionRegistry;
import fi.dy.masa.malilib.action.NamedAction;
import fi.dy.masa.malilib.gui.icon.DefaultIcons;
import fi.dy.masa.malilib.gui.util.GuiUtils;
import fi.dy.masa.malilib.gui.widget.BaseTextFieldWidget;
import fi.dy.masa.malilib.gui.widget.CheckBoxWidget;
import fi.dy.masa.malilib.gui.widget.list.DataListWidget;
import fi.dy.masa.malilib.gui.widget.list.entry.ActionPromptNamedActionEntryWidget;
import fi.dy.masa.malilib.input.ActionResult;
import fi.dy.masa.malilib.util.StringUtils;

public class ActionPromptScreen extends BaseListScreen<DataListWidget<NamedAction>>
{
    protected final List<NamedAction> filteredActions = new ArrayList<>();
    protected final BaseTextFieldWidget searchTextField;
    protected final CheckBoxWidget fuzzySearchCheckBoxWidget;
    protected final CheckBoxWidget rememberSearchCheckBoxWidget;

    public ActionPromptScreen()
    {
        super(0, 16, 0, 16);

        this.setScreenWidthAndHeight(320, 132);
        this.setPosition(4, GuiUtils.getScaledWindowHeight() - this.screenHeight - 4);

        int x = this.x + this.screenWidth - DefaultIcons.CHECKMARK_OFF.getWidth();
        String label = "malilib.gui.label.action_prompt_screen.remember_search";
        String hoverKey = "malilib.gui.hover.action_prompt_screen.remember_search_text";
        this.rememberSearchCheckBoxWidget = new CheckBoxWidget(x, this.y - 6, label, hoverKey);
        this.rememberSearchCheckBoxWidget.setBooleanStorage(MaLiLibConfigs.Generic.ACTION_PROMPT_REMEMBER_SEARCH);

        label = "malilib.gui.label.action_prompt_screen.fuzzy_search";
        hoverKey = "malilib.gui.hover.action_prompt_screen.use_fuzzy_search";
        this.fuzzySearchCheckBoxWidget = new CheckBoxWidget(x, this.y + 6, label, hoverKey);
        this.fuzzySearchCheckBoxWidget.setBooleanStorage(MaLiLibConfigs.Generic.ACTION_PROMPT_FUZZY_SEARCH);

        this.searchTextField = new BaseTextFieldWidget(this.x, this.y, this.screenWidth - 12, 16);
        this.searchTextField.setListener(this::updateFilteredList);
        this.searchTextField.setUpdateListenerAlways(true);
    }

    @Override
    protected void initScreen()
    {
        super.initScreen();

        this.addWidget(this.searchTextField);
        this.addWidget(this.rememberSearchCheckBoxWidget);
        this.addWidget(this.fuzzySearchCheckBoxWidget);

        this.searchTextField.setFocused(true);

        if (MaLiLibConfigs.Generic.ACTION_PROMPT_REMEMBER_SEARCH.getBooleanValue())
        {
            this.searchTextField.setText(MaLiLibConfigs.Internal.ACTION_PROMPT_SEARCH_TEXT.getStringValue());
        }

        this.updateFilteredList(this.searchTextField.getText());
    }

    @Override
    public void onGuiClosed()
    {
        if (MaLiLibConfigs.Generic.ACTION_PROMPT_REMEMBER_SEARCH.getBooleanValue())
        {
            MaLiLibConfigs.Internal.ACTION_PROMPT_SEARCH_TEXT.setValue(this.searchTextField.getText());
        }

        super.onGuiClosed();
    }

    @Override
    public boolean onKeyTyped(int keyCode, int scanCode, int modifiers)
    {
        if (keyCode == Keyboard.KEY_RETURN)
        {
            // Close the screen before running the action, in case the action opens another screen
            this.closeScreen(false);

            NamedAction action = this.getListWidget().getLastSelectedEntry();

            if (action != null)
            {
                action.getAction().execute(new ActionContext());
            }
        }
        else if (keyCode == Keyboard.KEY_ESCAPE)
        {
            this.closeScreen(false);
        }

        return super.onKeyTyped(keyCode, scanCode, modifiers);
    }

    protected List<NamedAction> getFilteredActions()
    {
        return this.filteredActions;
    }

    @Override
    protected DataListWidget<NamedAction> createListWidget(int listX, int listY, int listWidth, int listHeight)
    {
        DataListWidget<NamedAction> listWidget = new DataListWidget<>(listX, listY, listWidth, listHeight, this::getFilteredActions);
        listWidget.setBackgroundEnabled(true);
        listWidget.setBackgroundColor(0xC0000000);
        listWidget.setAllowKeyboardNavigation(true);
        listWidget.setListEntryWidgetFixedHeight(12);
        listWidget.setFetchFromSupplierOnRefresh(true);
        listWidget.getEntrySelectionHandler().setAllowSelection(true);
        listWidget.setEntryWidgetFactory(ActionPromptNamedActionEntryWidget::new);
        return listWidget;
    }

    protected boolean stringMatchesSearch(String searchTerm, String text)
    {
        if (this.fuzzySearchCheckBoxWidget.isSelected())
        {
            return StringUtils.containsOrderedCharacters(searchTerm, text);
        }

        return text.contains(searchTerm);
    }

    protected void updateFilteredList(String searchText)
    {
        this.filteredActions.clear();

        if (org.apache.commons.lang3.StringUtils.isBlank(searchText))
        {
            this.filteredActions.addAll(ActionRegistry.INSTANCE.getAllActions());
        }
        else
        {
            searchText = searchText.toLowerCase(Locale.ROOT);

            for (NamedAction action : ActionRegistry.INSTANCE.getAllActions())
            {
                if (this.stringMatchesSearch(searchText, action.getName().toLowerCase(Locale.ROOT)) ||
                    this.stringMatchesSearch(searchText, action.getDisplayName().toLowerCase(Locale.ROOT)))
                {
                    this.filteredActions.add(action);
                }
            }
        }

        this.getListWidget().refreshEntries();

        if (this.filteredActions.isEmpty() == false)
        {
            this.getListWidget().setLastSelectedEntry(-1);
            this.getListWidget().setLastSelectedEntry(0);
        }
    }

    public static ActionResult openActionPromptScreen(ActionContext ctx)
    {
        BaseScreen.openScreen(new ActionPromptScreen());
        return ActionResult.SUCCESS;
    }
}