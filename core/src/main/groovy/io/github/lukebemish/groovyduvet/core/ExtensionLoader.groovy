/*
 * Copyright (C) 2022 Luke Bemish
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, see <https://www.gnu.org/licenses/>.
 */

package io.github.lukebemish.groovyduvet.core

import groovy.transform.CompileStatic
import net.fabricmc.api.EnvType
import org.codehaus.groovy.reflection.CachedClass
import org.codehaus.groovy.runtime.m12n.ExtensionModule
import org.codehaus.groovy.runtime.m12n.ExtensionModuleScanner
import org.codehaus.groovy.runtime.metaclass.MetaClassRegistryImpl
import org.quiltmc.loader.api.ModContainer
import org.quiltmc.loader.api.QuiltLoader
import org.quiltmc.loader.api.entrypoint.PreLaunchEntrypoint
import org.quiltmc.loader.api.minecraft.MinecraftQuiltLoader

import java.nio.file.Files
import java.nio.file.Path

@CompileStatic
class ExtensionLoader implements PreLaunchEntrypoint {
    public static final String CLIENT_FILE = "META-INF/groovyduvet/client.extensions";
    public static final String COMMON_FILE = "META-INF/groovyduvet/common.extensions";
    public static final String SERVER_FILE = "META-INF/groovyduvet/server.extensions";

    @Override
    void onPreLaunch(ModContainer mod) {
        if (GroovySystem.metaClassRegistry instanceof MetaClassRegistryImpl) {
            MetaClassRegistryImpl registry = GroovySystem.metaClassRegistry as MetaClassRegistryImpl
            Map<CachedClass, List<MetaMethod>> map = [:]
            ExtensionModuleScanner scanner = new ExtensionModuleScanner((ExtensionModule module) -> {
                List<MetaMethod> metaMethods = module.getMetaMethods()
                if (!registry.moduleRegistry.hasModule(module.getName())) {
                    registry.moduleRegistry.addModule(module)
                    for (MetaMethod metaMethod : metaMethods) {
                        if (metaMethod.isStatic()) {
                            registry.staticMethods.add(metaMethod)
                        } else {
                            registry.instanceMethods.add(metaMethod)
                        }
                    }
                }
                for (MetaMethod metaMethod : metaMethods) {
                    CachedClass cachedClass = metaMethod.getDeclaringClass()
                    List<MetaMethod> methods = map.computeIfAbsent(cachedClass, k -> new ArrayList<MetaMethod>(4))
                    methods.add(metaMethod)
                }
            }, ExtensionLoader.classLoader)
            QuiltLoader.allMods.each {
                loadFromPath(ExtensionModuleScanner.MODULE_META_INF_FILE, it, scanner, registry, map)
                loadFromPath(COMMON_FILE, it, scanner, registry, map)
                if (MinecraftQuiltLoader.environmentType == EnvType.CLIENT) {
                    loadFromPath(CLIENT_FILE, it, scanner, registry, map)
                } else if (MinecraftQuiltLoader.environmentType == EnvType.SERVER) {
                    loadFromPath(SERVER_FILE, it, scanner, registry, map)
                }
            }
            map.each {key, value ->
                try {
                    key.addNewMopMethods(value)
                } catch (Exception ignored) {}
            }
        }
    }

    private void loadFromPath(String resource, ModContainer it, ExtensionModuleScanner scanner, MetaClassRegistryImpl registry, LinkedHashMap<CachedClass, List<MetaMethod>> map) {
        Path path = it.getPath(resource)
        if (Files.exists(path)) {
            Properties properties = new Properties()
            properties.load(path.newReader())
            scanner.scanExtensionModuleFromProperties(properties)
            registry.registerExtensionModuleFromProperties(properties, ExtensionLoader.classLoader, map)
        }
    }
}