package io.github.lukebemish.groovyduvet.wrapper.qsl.extension.client

import net.minecraft.client.renderer.RenderType
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.material.Fluid
import org.quiltmc.qsl.block.extensions.api.client.BlockRenderLayerMap

class RenderExtension {
    static void putAt(RenderType layer, Block block) {
        BlockRenderLayerMap.put(layer, block)
    }

    static void putAt(RenderType layer, Fluid fluid) {
        BlockRenderLayerMap.put(layer, fluid)
    }
}