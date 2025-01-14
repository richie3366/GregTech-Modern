package com.gregtechceu.gtceu.api.item.component;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;

import org.jetbrains.annotations.NotNull;

/**
 * @author KilaBash
 * @date 2023/2/24
 * @implNote ICustomRenderer
 */
public interface ICustomRenderer extends IItemComponent {
    @NotNull
    IRenderer getRenderer();
}
