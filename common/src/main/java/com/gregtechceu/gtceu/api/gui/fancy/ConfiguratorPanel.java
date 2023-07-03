package com.gregtechceu.gtceu.api.gui.fancy;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.lowdragmc.lowdraglib.client.utils.RenderUtils;
import com.lowdragmc.lowdraglib.gui.animation.Animation;
import com.lowdragmc.lowdraglib.gui.animation.Transform;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import com.lowdragmc.lowdraglib.utils.interpolate.Eases;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author KilaBash
 * @date 2023/6/27
 * @implNote ConfiguratorPanel
 */
public class ConfiguratorPanel extends WidgetGroup {

    @Getter
    protected List<Tab> tabs = new ArrayList<>();
    @Getter @Nullable
    protected Tab expanded;
    @Setter
    protected int border = 4;
    @Setter
    protected IGuiTexture texture = GuiTextures.BACKGROUND;

    public ConfiguratorPanel() {
        super(-(24 + 2), 2, 24, 0);
    }

    public void clear() {
        clearAllWidgets();
        tabs.clear();
        expanded = null;
    }

    public int getTabSize() {
        return getSize().width;
    }

    public void attachConfigurators(IFancyConfigurator... fancyConfigurators) {
        for (IFancyConfigurator fancyConfigurator : fancyConfigurators) {
            var tab = new Tab(fancyConfigurator);
            tab.setBackground(texture);
            tabs.add(tab);
            addWidgetAnima(tab, (Transform) new Transform()
                    .scale(0)
                    .duration(500)
                    .ease(Eases.EaseQuadOut));
        }
        setSize(new Size(getSize().width, Math.max(0, tabs.size() * (getTabSize() + 2) - 2)));
    }

    public void expandTab(Tab tab) {
        tab.expand();
        int i = 0;
        for (Tab otherTab : tabs) {
            if (otherTab != tab) {
                otherTab.collapseTo(0, i++ * (getTabSize() + 2));
            }
        }
        expanded = tab;
    }

    public void collapseTab() {
        if (expanded != null) {
            for (int i = 0; i < tabs.size(); i++) {
                tabs.get(i).collapseTo(0, i * (getTabSize() + 2));
            }
        }
        expanded = null;
    }

    public class Tab extends WidgetGroup {
        private final IFancyConfigurator configurator;
        private final ButtonWidget button;
        private final WidgetGroup view;

        public Tab(IFancyConfigurator configurator) {
            super(0, tabs.size() * (getTabSize() + 2), getTabSize(), getTabSize());
            this.configurator = configurator;
            this.button = new ButtonWidget(0, 0, getTabSize(), getTabSize(), null, this::onClick);
            var widget = configurator.createConfigurator();
            widget.setSelfPosition(new Position(border, getTabSize()));

            this.view = new WidgetGroup(0, 0, 0, 0) {
                @Override
                protected void onChildSizeUpdate(Widget child) {
                    super.onChildSizeUpdate(child);
                    if (widget == child) {
                        this.setSize(new Size(widget.getSize().width + border * 2, widget.getSize().height + getTabSize() + border));
                    }
                }
            };

            this.view.setVisible(false);
            this.view.setActive(false);
            this.view.setSize(new Size(widget.getSize().width + border * 2, widget.getSize().height + getTabSize() + border));
            this.view.addWidget(widget);
            this.view.addWidget(new ImageWidget(border + 5, border, widget.getSize().width - getTabSize() - 5, getTabSize() - border,
                    new TextTexture(configurator.getTitle())
                            .setType(TextTexture.TextType.LEFT_HIDE)
                            .setWidth(widget.getSize().width - getTabSize())));
            this.addWidget(button);
            this.addWidget(view);
        }

        @Override
        public void detectAndSendChanges() {
            super.detectAndSendChanges();
            configurator.detectAndSendChange((id, sender) -> writeUpdateInfo(0, buf -> {
                buf.writeVarInt(id);
                sender.accept(buf);
            }));
        }

        @Override
        public void readUpdateInfo(int id, FriendlyByteBuf buffer) {
            if (id == 0) {
                configurator.readUpdateInfo(buffer.readVarInt(), buffer);
            } else {
                super.readUpdateInfo(id, buffer);
            }
        }

        @Override
        protected void onChildSizeUpdate(Widget child) {
            if (this.view == child) {
                if (expanded == this) {
                    expandTab(this);
                }
            }
        }

        private void onClick(ClickData clickData) {
            if (expanded == this) {
                collapseTab();
            } else {
                expandTab(this);
            }
        }

        @Override
        public void setSize(Size size) {
            super.setSize(size);
            button.setSelfPosition(new Position(size.width - getTabSize(), 0));
        }

        private void expand() {
            var size = view.getSize();
            animation(new Animation()
                    .duration(500)
                    .position(new Position(- size.width + (tabs.size() > 1 ?  - 2 : getTabSize()), 0))
                    .size(size)
                    .ease(Eases.EaseQuadOut)
                    .onFinish(() -> {
                        view.setVisible(true);
                        view.setActive(true);
                    }));
        }

        private void collapseTo(int x, int y) {
            view.setVisible(false);
            view.setActive(false);
            animation(new Animation()
                    .duration(500)
                    .position(new Position(x, y))
                    .size(new Size(getTabSize(), getTabSize()))
                    .ease(Eases.EaseQuadOut));
        }

        @Override
        @Environment(EnvType.CLIENT)
        public void drawInBackground(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
            drawBackgroundTexture(poseStack, mouseX, mouseY);
            var position = getPosition();
            var size = getSize();
            if (inAnimate()) {
                RenderUtils.useScissor(poseStack, position.x + border - 1, position.y + border - 1, size.width - (border - 1) * 2, size.height - (border - 1) * 2, () -> {
                    drawWidgetsBackground(poseStack, mouseX, mouseY, partialTicks);
                });
            } else {
                drawWidgetsBackground(poseStack, mouseX, mouseY, partialTicks);
            }
            configurator.getIcon().draw(poseStack, mouseX, mouseY, position.x + size.width - 20, position.y + 4, 16, 16);
        }

        @Override
        @Environment(EnvType.CLIENT)
        public void drawInForeground(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
            super.drawInForeground(poseStack, mouseX, mouseY, partialTicks);
            if (isMouseOver(getPosition().x + getSize().width - 20, getPosition().y + 4, 16, 16, mouseX, mouseY) && gui != null && gui.getModularUIGui() != null) {
                gui.getModularUIGui().setHoverTooltip(configurator.getTooltips(), ItemStack.EMPTY, null, null);
            }
        }
    }
}