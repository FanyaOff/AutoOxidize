package com.fanya.autooxidize;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class PlayerActionSimulator {
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();

    public static boolean hasItemInInventory(Item item) {
        ClientPlayerEntity player = CLIENT.player;
        if (player == null) return false;

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                return true;
            }
        }
        return false;
    }

    public static boolean selectItemInHotbar(Item item) {
        ClientPlayerEntity player = CLIENT.player;
        if (player == null) return false;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                player.getInventory().selectedSlot = i;
                return true;
            }
        }
        return false;
    }

    public static boolean placeBlock(BlockPos pos, Block block) {
        ClientPlayerEntity player = CLIENT.player;
        ClientPlayerInteractionManager interactionManager = CLIENT.interactionManager;

        if (player == null || interactionManager == null) return false;

        if (!selectItemInHotbar(block.asItem())) {
            return false;
        }

        BlockHitResult hitResult = new BlockHitResult(
                new Vec3d(pos.getX(), pos.getY() - 1, pos.getZ()),
                Direction.UP,
                pos.down(),
                false
        );

        interactionManager.interactBlock(player, Hand.MAIN_HAND, hitResult);
        return true;
    }

    public static boolean useAxeOnBlock(BlockPos pos) {
        ClientPlayerEntity player = CLIENT.player;
        ClientPlayerInteractionManager interactionManager = CLIENT.interactionManager;

        if (player == null || interactionManager == null) return false;

        if (!selectAxe()) {
            return false;
        }

        BlockHitResult hitResult = new BlockHitResult(
                new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5),
                Direction.UP,
                pos,
                false
        );

        interactionManager.interactBlock(player, Hand.MAIN_HAND, hitResult);
        return true;
    }

    public static boolean scrapeWaxFromBlock(BlockPos pos) {
        return useAxeOnBlock(pos);
    }

    public static boolean startMiningBlock(BlockPos pos) {
        ClientPlayerEntity player = CLIENT.player;
        ClientPlayerInteractionManager interactionManager = CLIENT.interactionManager;

        if (player == null || interactionManager == null) return false;

        if (!selectPickaxe()) {
            return false;
        }

        interactionManager.attackBlock(pos, Direction.UP);

        return true;
    }

    public static boolean continueMiningBlock(BlockPos pos) {
        ClientPlayerEntity player = CLIENT.player;
        ClientPlayerInteractionManager interactionManager = CLIENT.interactionManager;

        if (player == null || interactionManager == null) return false;

        interactionManager.updateBlockBreakingProgress(pos, Direction.UP);

        return true;
    }

    public static boolean selectAxe() {
        return selectItemInHotbar(Items.NETHERITE_AXE) ||
                selectItemInHotbar(Items.DIAMOND_AXE) ||
                selectItemInHotbar(Items.IRON_AXE) ||
                selectItemInHotbar(Items.GOLDEN_AXE) ||
                selectItemInHotbar(Items.STONE_AXE) ||
                selectItemInHotbar(Items.WOODEN_AXE);
    }

    public static boolean selectPickaxe() {
        return selectItemInHotbar(Items.NETHERITE_PICKAXE) ||
                selectItemInHotbar(Items.DIAMOND_PICKAXE) ||
                selectItemInHotbar(Items.IRON_PICKAXE) ||
                selectItemInHotbar(Items.GOLDEN_PICKAXE) ||
                selectItemInHotbar(Items.STONE_PICKAXE) ||
                selectItemInHotbar(Items.WOODEN_PICKAXE);
    }

    public static int getScrapingStepsNeeded(AutoOxidize.CopperStage targetStage) {
        return switch (targetStage) {
            case NORMAL -> 3;
            case EXPOSED -> 2;
            case WEATHERED -> 1;
            default -> 0;
        };
    }
}
