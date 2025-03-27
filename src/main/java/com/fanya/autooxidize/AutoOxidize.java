package com.fanya.autooxidize;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;

public class AutoOxidize implements ClientModInitializer {

    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    private static CopperStage targetStage = null;
    private static BlockPos currentPos = null;
    private static boolean isProcessing = false;
    private static boolean useWaxedBlocks = false;

    private static int placeDelay = 10;
    private static int scrapeDelay = 6;
    private static int mineDelay = 1;
    private static int cycleDelay = 20;

    private static int tickCounter = 0;
    private static int miningTickCounter = 0;
    private static int miningAttempts = 0;
    private static final int MAX_MINING_ATTEMPTS = 500;

    private enum ProcessState {
        IDLE,
        PLACING,
        WAITING_AFTER_PLACE,
        WAITING_AFTER_WAX,
        SCRAPING,
        WAITING_AFTER_SCRAPE,
        MINING,
        WAITING_AFTER_MINE
    }

    private static ProcessState currentState = ProcessState.IDLE;
    private static int scrapeStepsRemaining = 0;

    @Override
    public void onInitializeClient() {
        registerCommands();
        registerTickEvent();
    }

    private void registerTickEvent() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!isProcessing || client.player == null || client.world == null) return;

            tickCounter++;

            switch (currentState) {
                case IDLE:
                    currentPos = getPositionInFrontOfPlayer();
                    if (currentPos != null) {
                        currentState = ProcessState.PLACING;
                    }
                    break;

                case PLACING:
                    placeCopperBlock();
                    tickCounter = 0;
                    currentState = ProcessState.WAITING_AFTER_PLACE;
                    break;

                case WAITING_AFTER_WAX:
                    if (tickCounter >= scrapeDelay) {
                        Block currentBlock = client.world.getBlockState(currentPos).getBlock();
                        if (!isCopperBlock(currentBlock)) {
                            client.player.sendMessage(Text.translatable("autooxidize.error.no_copper_after_wax"), true);
                            currentState = ProcessState.IDLE;
                            return;
                        }

                        scrapeStepsRemaining = PlayerActionSimulator.getScrapingStepsNeeded(targetStage);

                        if (scrapeStepsRemaining > 0) {
                            currentState = ProcessState.SCRAPING;
                        } else {
                            currentState = ProcessState.MINING;
                            miningTickCounter = 0;
                            miningAttempts = 0;

                            if (!PlayerActionSimulator.selectPickaxe()) {
                                client.player.sendMessage(Text.translatable("autooxidize.error.no_pickaxe"), false);
                                isProcessing = false;
                                currentState = ProcessState.IDLE;
                            } else {
                                PlayerActionSimulator.startMiningBlock(currentPos);
                            }
                        }

                        tickCounter = 0;
                    }
                    break;

                case WAITING_AFTER_PLACE:
                    if (tickCounter >= placeDelay) {
                        Block currentBlock = client.world.getBlockState(currentPos).getBlock();
                        if (!isCopperBlock(currentBlock)) {
                            client.player.sendMessage(Text.translatable("autooxidize.error.no_copper_block"), true);
                            currentState = ProcessState.IDLE;
                            return;
                        }

                        if (useWaxedBlocks) {
                            if (!PlayerActionSimulator.selectAxe()) {
                                client.player.sendMessage(Text.translatable("autooxidize.error.no_axe"), false);
                                isProcessing = false;
                                currentState = ProcessState.IDLE;
                                return;
                            }
                            PlayerActionSimulator.scrapeWaxFromBlock(currentPos);
                            tickCounter = 0;

                            currentState = ProcessState.WAITING_AFTER_WAX;
                        } else {
                            scrapeStepsRemaining = PlayerActionSimulator.getScrapingStepsNeeded(targetStage);
                            if (scrapeStepsRemaining > 0) {
                                currentState = ProcessState.SCRAPING;
                            } else {
                                currentState = ProcessState.MINING;
                                miningTickCounter = 0;
                                miningAttempts = 0;
                            }
                            tickCounter = 0;
                        }
                    }
                    break;

                case SCRAPING:
                    if (tickCounter >= scrapeDelay) {
                        if (scrapeStepsRemaining > 0) {
                            Block currentBlock = client.world.getBlockState(currentPos).getBlock();
                            if (!isCopperBlock(currentBlock)) {
                                client.player.sendMessage(Text.translatable("autooxidize.error.no_copper_scraping"), false);
                                currentState = ProcessState.IDLE;
                                return;
                            }

                            if (!PlayerActionSimulator.selectAxe()) {
                                client.player.sendMessage(Text.translatable("autooxidize.error.no_axe"), false);
                                isProcessing = false;
                                currentState = ProcessState.IDLE;
                                return;
                            }

                            PlayerActionSimulator.useAxeOnBlock(currentPos);
                            scrapeStepsRemaining--;
                        }

                        if (scrapeStepsRemaining <= 0) {
                            currentState = ProcessState.WAITING_AFTER_SCRAPE;
                        }

                        tickCounter = 0;
                    }
                    break;

                case WAITING_AFTER_SCRAPE:
                    if (tickCounter >= placeDelay) {
                        Block currentBlock = client.world.getBlockState(currentPos).getBlock();
                        if (!isCopperBlock(currentBlock)) {
                            client.player.sendMessage(Text.translatable("autooxidize.error.no_copper_after_scrape"), true);
                            currentState = ProcessState.IDLE;
                            return;
                        }

                        currentState = ProcessState.MINING;
                        miningTickCounter = 0;
                        miningAttempts = 0;
                        tickCounter = 0;

                        // Начинаем процесс разрушения
                        if (!PlayerActionSimulator.selectPickaxe()) {
                            client.player.sendMessage(Text.translatable("autooxidize.error.no_pickaxe"), false);
                            isProcessing = false;
                            currentState = ProcessState.IDLE;
                        } else {
                            PlayerActionSimulator.startMiningBlock(currentPos);
                        }
                    }
                    break;
                case MINING:
                    miningTickCounter++;
                    if (miningTickCounter >= mineDelay) {
                        miningTickCounter = 0;
                        miningAttempts++;

                        Block currentBlock = client.world.getBlockState(currentPos).getBlock();
                        if (isCopperBlock(currentBlock)) {
                            if (miningAttempts < MAX_MINING_ATTEMPTS) {
                                PlayerActionSimulator.continueMiningBlock(currentPos);
                            } else {
                                client.player.sendMessage(Text.translatable("autooxidize.error.mining_failed", MAX_MINING_ATTEMPTS), false);
                                currentState = ProcessState.WAITING_AFTER_MINE;
                                tickCounter = 0;
                            }
                        } else {
                            currentState = ProcessState.WAITING_AFTER_MINE;
                            tickCounter = 0;
                        }
                    }
                    break;

                case WAITING_AFTER_MINE:
                    if (tickCounter >= cycleDelay) {
                        currentState = ProcessState.IDLE;
                        tickCounter = 0;
                    }
                    break;
            }
        });
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("copper")
                    .then(argument("stage", StringArgumentType.string())
                            .then(argument("waxed", BoolArgumentType.bool())
                                    .executes(context -> {
                                        String stageStr = getString(context, "stage");
                                        useWaxedBlocks = getBool(context, "waxed");
                                        try {
                                            targetStage = CopperStage.valueOf(stageStr.toUpperCase());
                                            if (CLIENT.player != null) {
                                                CLIENT.player.sendMessage(Text.translatable("autooxidize.command.start",
                                                        Text.translatable(useWaxedBlocks ? "autooxidize.block_type.waxed" : "autooxidize.block_type.normal"),
                                                        targetStage.name()), false);
                                                placeDelay = 10;
                                                scrapeDelay = 6;
                                                mineDelay = 1;
                                                cycleDelay = 20;
                                                startProcess();
                                            }
                                        } catch (IllegalArgumentException e) {
                                            if (CLIENT.player != null) {
                                                CLIENT.player.sendMessage(Text.translatable("autooxidize.command.invalid_stage"), false);
                                            }
                                        }
                                        return 1;
                                    })
                                    .then(argument("placeDelay", IntegerArgumentType.integer(2, 100))
                                            .then(argument("scrapeDelay", IntegerArgumentType.integer(2, 100))
                                                    .then(argument("mineDelay", IntegerArgumentType.integer(1, 20))
                                                            .then(argument("cycleDelay", IntegerArgumentType.integer(2, 200))
                                                                    .executes(context -> {
                                                                        String stageStr = getString(context, "stage");
                                                                        useWaxedBlocks = getBool(context, "waxed");
                                                                        try {
                                                                            targetStage = CopperStage.valueOf(stageStr.toUpperCase());

                                                                            placeDelay = getInteger(context, "placeDelay");
                                                                            scrapeDelay = getInteger(context, "scrapeDelay");
                                                                            mineDelay = getInteger(context, "mineDelay");
                                                                            cycleDelay = getInteger(context, "cycleDelay");

                                                                            if (CLIENT.player != null) {
                                                                                CLIENT.player.sendMessage(Text.translatable("autooxidize.command.start_custom",
                                                                                        Text.translatable(useWaxedBlocks ? "autooxidize.block_type.waxed" : "autooxidize.block_type.normal"),
                                                                                        targetStage.name(), placeDelay, scrapeDelay, mineDelay, cycleDelay), false);
                                                                                startProcess();
                                                                            }
                                                                        } catch (IllegalArgumentException e) {
                                                                            if (CLIENT.player != null) {
                                                                                CLIENT.player.sendMessage(Text.translatable("autooxidize.command.invalid_stage"), false);
                                                                            }
                                                                        }
                                                                        return 1;
                                                                    })
                                                            )
                                                    )
                                            )
                                    )
                            )
                            .executes(context -> {
                                String stageStr = getString(context, "stage");
                                try {
                                    targetStage = CopperStage.valueOf(stageStr.toUpperCase());
                                    useWaxedBlocks = false;
                                    if (CLIENT.player != null) {
                                        CLIENT.player.sendMessage(Text.translatable("autooxidize.command.start",
                                                Text.translatable("autooxidize.block_type.normal"),
                                                targetStage.name()), false);

                                        placeDelay = 10;
                                        scrapeDelay = 6;
                                        mineDelay = 1;
                                        cycleDelay = 20;
                                        startProcess();
                                    }
                                } catch (IllegalArgumentException e) {
                                    if (CLIENT.player != null) {
                                        CLIENT.player.sendMessage(Text.translatable("autooxidize.command.invalid_stage"), false);
                                    }
                                }
                                return 1;
                            })
                    )
                    .then(literal("stop")
                            .executes(context -> {
                                isProcessing = false;
                                currentState = ProcessState.IDLE;
                                if (CLIENT.player != null) {
                                    CLIENT.player.sendMessage(Text.translatable("autooxidize.command.stopped"), false);
                                }
                                return 1;
                            })
                    )
                    .then(literal("help")
                            .executes(context -> {
                                if (CLIENT.player != null) {
                                    CLIENT.player.sendMessage(Text.translatable("autooxidize.help.title"), false);
                                    CLIENT.player.sendMessage(Text.translatable("autooxidize.help.copper_stage"), false);
                                    CLIENT.player.sendMessage(Text.translatable("autooxidize.help.copper_stage_waxed"), false);
                                    CLIENT.player.sendMessage(Text.translatable("autooxidize.help.copper_full"), false);
                                    CLIENT.player.sendMessage(Text.translatable("autooxidize.help.copper_stop"), false);
                                    CLIENT.player.sendMessage(Text.translatable("autooxidize.help.copper_help"), false);
                                    CLIENT.player.sendMessage(Text.translatable("autooxidize.help.args_title"), false);
                                    CLIENT.player.sendMessage(Text.translatable("autooxidize.help.stages"), false);
                                    CLIENT.player.sendMessage(Text.translatable("autooxidize.help.block_type"), false);
                                    CLIENT.player.sendMessage(Text.translatable("autooxidize.help.block_type_normal"), false);
                                    CLIENT.player.sendMessage(Text.translatable("autooxidize.help.block_type_waxed"), false);
                                    CLIENT.player.sendMessage(Text.translatable("autooxidize.help.delays"), false);
                                    CLIENT.player.sendMessage(Text.translatable("autooxidize.help.place_delay"), false);
                                    CLIENT.player.sendMessage(Text.translatable("autooxidize.help.scrape_delay"), false);
                                    CLIENT.player.sendMessage(Text.translatable("autooxidize.help.mine_delay"), false);
                                    CLIENT.player.sendMessage(Text.translatable("autooxidize.help.cycle_delay"), false);
                                }
                                return 1;
                            })
                    )
            );
        });
    }

    private void startProcess() {
        if (CLIENT.player == null || CLIENT.world == null) return;

        isProcessing = true;
        currentState = ProcessState.IDLE;
        tickCounter = 0;
        scrapeStepsRemaining = 0;
    }

    private BlockPos getPositionInFrontOfPlayer() {
        if (CLIENT.player != null) {
            double yaw = Math.toRadians(CLIENT.player.getYaw());
            double pitch = Math.toRadians(CLIENT.player.getPitch());

            double distance = 2.0;

            double x = -Math.sin(yaw) * Math.cos(pitch) * distance;
            double z = Math.cos(yaw) * Math.cos(pitch) * distance;

            BlockPos playerPos = CLIENT.player.getBlockPos();
            return new BlockPos(
                    playerPos.getX() + (int)Math.round(x),
                    playerPos.getY(),
                    playerPos.getZ() + (int)Math.round(z)
            );
        }
        return null;
    }

    private void placeCopperBlock() {
        if (currentPos != null && CLIENT.player != null) {
            Block blockToPlace;
            if (useWaxedBlocks) {
                blockToPlace = Blocks.WAXED_OXIDIZED_COPPER;
            } else {
                blockToPlace = Blocks.OXIDIZED_COPPER;
            }

            if (!PlayerActionSimulator.hasItemInInventory(blockToPlace.asItem())) {
                CLIENT.player.sendMessage(Text.translatable("autooxidize.error.no_block"), false);
                isProcessing = false;
                currentState = ProcessState.IDLE;
                return;
            }

            boolean success = PlayerActionSimulator.placeBlock(currentPos, blockToPlace);
            if (!success) {
                CLIENT.player.sendMessage(Text.translatable("autooxidize.error.place_failed"), false);
            }
        }
    }

    private boolean isCopperBlock(Block block) {
        return block == Blocks.COPPER_BLOCK ||
                block == Blocks.EXPOSED_COPPER ||
                block == Blocks.WEATHERED_COPPER ||
                block == Blocks.OXIDIZED_COPPER ||
                block == Blocks.WAXED_COPPER_BLOCK ||
                block == Blocks.WAXED_EXPOSED_COPPER ||
                block == Blocks.WAXED_WEATHERED_COPPER ||
                block == Blocks.WAXED_OXIDIZED_COPPER ||
                block.getTranslationKey().contains("copper");
    }

    public enum CopperStage {
        NORMAL,
        EXPOSED,
        WEATHERED,
        OXIDIZED
    }
}
