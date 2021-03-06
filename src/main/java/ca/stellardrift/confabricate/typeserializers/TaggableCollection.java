/*
 * Copyright 2020 zml
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.stellardrift.confabricate.typeserializers;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.EntityTypeTags;
import net.minecraft.tag.FluidTags;
import net.minecraft.tag.ItemTags;
import net.minecraft.tag.Tag;
import net.minecraft.tag.TagContainer;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.Set;

public interface TaggableCollection<T> extends Iterable<T> {

    static TaggableCollection<Block> ofBlocks(Set<Block> blocks, Set<Tag<Block>> blockTags) {
        return new TaggableCollectionImpl<>(Registry.BLOCK, BlockTags.getContainer(), blocks, blockTags);
    }

    static TaggableCollection<EntityType<?>> ofEntityTypes(Set<EntityType<?>> single, Set<Tag<EntityType<?>>> tags) {
        return new TaggableCollectionImpl<>(Registry.ENTITY_TYPE, EntityTypeTags.getContainer(), single, tags);
    }

    static TaggableCollection<Fluid> ofFluids(Set<Fluid> single, Set<Tag<Fluid>> tags) {
        return new TaggableCollectionImpl<>(Registry.FLUID, FluidTags.getContainer(), single, tags);
    }

    static TaggableCollection<Item> ofItems(Set<Item> single, Set<Tag<Item>> tags) {
        return new TaggableCollectionImpl<>(Registry.ITEM, ItemTags.getContainer(), single, tags);
    }

    static <T> TaggableCollection<T> of(Registry<T> registry, TagContainer<T> tagRegistry, Set<T> single, Set<Tag<T>> tags) {
        return new TaggableCollectionImpl<>(registry, tagRegistry, single, tags);
    }

    Registry<T> getContainingRegistry();
    TagContainer<T> getTagContainer();

    Set<T> getSpecificElements();
    Set<Tag<T>> getTaggedElements();

    TaggableCollection<T> addingSingle(Identifier ident);
    TaggableCollection<T> addingTag(Identifier tag);

    TaggableCollection<T> removingSingle(Identifier ident);
    TaggableCollection<T> removingTag(Identifier tag);
}
