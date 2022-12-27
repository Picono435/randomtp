package com.gmail.picono435.randomtp.api;

import com.gmail.picono435.randomtp.RandomTP;
import com.gmail.picono435.randomtp.config.Config;
import com.gmail.picono435.randomtp.config.Messages;
import com.mojang.datafixers.util.Pair;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;

import java.util.Map;
import java.util.Random;

public class RandomTPAPI {

    public static void randomTeleport(ServerPlayer player, ServerLevel world) {
        randomTeleport(player, world, null);
    }

    public static void randomTeleport(ServerPlayer player, ServerLevel world, ResourceKey<Biome> biomeResourceKey) {
        try  {
            Random random = new Random();
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
            if(biomeResourceKey == null) {
                Pair<Integer, Integer> coordinates = generateCoordinates(world, player, random);
                int x = coordinates.getFirst();
                int z = coordinates.getSecond();

                mutableBlockPos.setX(x);
                mutableBlockPos.setY(50);
                mutableBlockPos.setZ(z);
            } else {
                BlockPos biomePos = world.findNearestBiome(world.getServer().registryAccess().registry(Registry.BIOME_REGISTRY).get().get(biomeResourceKey), new BlockPos(player.getX(), player.getY(), player.getZ()), 6400, 8);
                if(biomePos == null) {
                    TextComponent msg = new TextComponent(Messages.getMaxTries().replaceAll("\\{playerName\\}", player.getName().getString()).replaceAll("&", "§"));
                    player.sendMessage(msg, player.getUUID());
                    return;
                }
                mutableBlockPos.setX(biomePos.getX());
                mutableBlockPos.setY(50);
                mutableBlockPos.setZ(biomePos.getZ());
                if(!world.getWorldBorder().isWithinBounds(mutableBlockPos)) {
                    TextComponent msg = new TextComponent(Messages.getMaxTries().replaceAll("\\{playerName\\}", player.getName().getString()).replaceAll("&", "§"));
                    player.sendMessage(msg, player.getUUID());
                    return;
                }
            }
            int maxTries = Config.getMaxTries();
            int y = mutableBlockPos.getY();
            while (!isSafe(world, mutableBlockPos) && (maxTries == -1 || maxTries > 0)) {
                y++;
                mutableBlockPos.setY(y);
                if(mutableBlockPos.getY() >= 200 || !isInBiomeWhitelist(world.getServer().registryAccess().registry(Registry.BIOME_REGISTRY).get().getKey(world.getBiome(mutableBlockPos.immutable())))) {
                    if(biomeResourceKey != null) {
                        BlockPos biomePos = world.findNearestBiome(world.getServer().registryAccess().registry(Registry.BIOME_REGISTRY).get().get(biomeResourceKey), new BlockPos(player.getX(), player.getY(), player.getZ()), 6400, 8);
                        if(biomePos == null) {
                            TextComponent msg = new TextComponent(Messages.getMaxTries().replaceAll("\\{playerName\\}", player.getName().getString()).replaceAll("&", "§"));
                            player.sendMessage(msg, player.getUUID());
                            return;
                        }
                        mutableBlockPos.setX(biomePos.getX());
                        mutableBlockPos.setY(50);
                        mutableBlockPos.setZ(biomePos.getZ());
                        if(!world.getWorldBorder().isWithinBounds(mutableBlockPos)) {
                            TextComponent msg = new TextComponent(Messages.getMaxTries().replaceAll("\\{playerName\\}", player.getName().getString()).replaceAll("&", "§"));
                            player.sendMessage(msg, player.getUUID());
                            return;
                        }
                        continue;
                    }
                    Pair<Integer, Integer> coordinates = generateCoordinates(world, player, random);
                    int x = coordinates.getFirst();
                    int z = coordinates.getSecond();

                    mutableBlockPos.setX(x);
                    mutableBlockPos.setY(50);
                    mutableBlockPos.setZ(z);
                    continue;
                }
                if(maxTries > 0){
                    maxTries--;
                }
                if(maxTries == 0) {
                    TextComponent msg = new TextComponent(Messages.getMaxTries().replaceAll("\\{playerName\\}", player.getName().getString()).replaceAll("&", "§"));
                    player.sendMessage(msg, player.getUUID());
                    return;
                }
            }

            player.teleportTo(world, mutableBlockPos.getX(), mutableBlockPos.getY(), mutableBlockPos.getZ(), player.xRot, player.yRot);
            TextComponent successful = new TextComponent(Messages.getSuccessful().replaceAll("\\{playerName\\}", player.getName().getString()).replaceAll("\\{blockX\\}", "" + (int)player.position().x).replaceAll("\\{blockY\\}", "" + (int)player.position().y).replaceAll("\\{blockZ\\}", "" + (int)player.position().z).replaceAll("&", "§"));
            player.sendMessage(successful, player.getUUID());
        } catch(Exception ex) {
            RandomTP.getLogger().info("Error executing command.");
            ex.printStackTrace();
        }
    }

    private static Pair<Integer, Integer> generateCoordinates(ServerLevel world, Player player, Random random) {
        // Calculating X coordinates
        int x;
        if(random.nextInt(2) == 1) {
            // Calculating X coordinates from min to max
            int maxDistance = Config.getMaxDistance() == 0 ? (int) world.getWorldBorder().getMinX() : (int) (player.getX() + Config.getMaxDistance());
            if(maxDistance < world.getWorldBorder().getMinX()) maxDistance = (int) world.getWorldBorder().getMinX();
            int minDistance = (int) (player.getX() - Config.getMinDistance());
            if(minDistance < world.getWorldBorder().getMinX()) minDistance = (int) (world.getWorldBorder().getMinX() + 10);
            if(maxDistance < minDistance) maxDistance = maxDistance ^ minDistance ^ (minDistance = maxDistance);
            if(maxDistance == minDistance) minDistance = minDistance - 1;
            x = random.ints(minDistance, maxDistance).findAny().getAsInt();
        } else {
            // Calculating X coordinates from max to min
            int maxDistance = Config.getMaxDistance() == 0 ? (int) world.getWorldBorder().getMaxX() : (int) (player.getX() - Config.getMaxDistance());
            if(maxDistance > world.getWorldBorder().getMaxX()) maxDistance = (int) world.getWorldBorder().getMaxX();
            int minDistance = (int) (player.getX() + Config.getMinDistance());
            if(minDistance > world.getWorldBorder().getMaxX()) minDistance = (int) (world.getWorldBorder().getMaxX() - 10);
            if(maxDistance < minDistance) maxDistance = maxDistance ^ minDistance ^ (minDistance = maxDistance);
            if(maxDistance == minDistance) minDistance = minDistance - 1;
            x = random.ints(minDistance, maxDistance).findAny().getAsInt();
        }
        int z;
        if(random.nextInt(2) == 1) {
            // Calculating Z coordinates from min to max
            int maxDistance = Config.getMaxDistance() == 0 ? (int) world.getWorldBorder().getMinZ() : (int) (player.getZ() + Config.getMaxDistance());
            if(maxDistance < world.getWorldBorder().getMinZ()) maxDistance = (int) world.getWorldBorder().getMinZ();
            int minDistance = (int) (player.getZ() - Config.getMinDistance());
            if(minDistance < world.getWorldBorder().getMinZ()) minDistance = (int) (world.getWorldBorder().getMinZ() + 10);
            if(maxDistance < minDistance) maxDistance = maxDistance ^ minDistance ^ (minDistance = maxDistance);
            if(maxDistance == minDistance) minDistance = minDistance - 1;
            z = random.ints(minDistance, maxDistance).findAny().getAsInt();
        } else {
            // Calculating Z coordinates from max to min
            int maxDistance = Config.getMaxDistance() == 0 ? (int) world.getWorldBorder().getMaxZ() : (int) (player.getZ() - Config.getMaxDistance());
            if(maxDistance > world.getWorldBorder().getMaxZ()) maxDistance = (int) world.getWorldBorder().getMaxZ();
            int minDistance = (int) (player.getZ() + Config.getMinDistance());
            if(minDistance > world.getWorldBorder().getMaxZ()) minDistance = (int) (world.getWorldBorder().getMaxZ() - 10);
            if(maxDistance < minDistance) maxDistance = maxDistance ^ minDistance ^ (minDistance = maxDistance);
            if(maxDistance == minDistance) minDistance = minDistance - 1;
            z = random.ints(minDistance, maxDistance).findAny().getAsInt();
        }
        return new Pair<>(x, z);
    }

    public static ServerLevel getWorld(String world, MinecraftServer server) {
        try {
            ResourceLocation resourcelocation = ResourceLocation.tryParse(world);
            ResourceKey<Level> registrykey = ResourceKey.create(Registry.DIMENSION_REGISTRY, resourcelocation);
            ServerLevel worldTo = server.getLevel(registrykey);
            return worldTo;
        } catch(Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static boolean checkCooldown(ServerPlayer player, Map<String, Long> cooldowns) {
        int cooldownTime = Config.getCooldown();
        if(cooldowns.containsKey(player.getName().getString())) {
            long secondsLeft = ((cooldowns.get(player.getName().getString())/1000)+cooldownTime) - (System.currentTimeMillis()/1000);
            if(secondsLeft > 0) {
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    public static long getCooldownLeft(ServerPlayer player, Map<String, Long> cooldowns) {
        int cooldownTime = Config.getCooldown();
        long secondsLeft = ((cooldowns.get(player.getName().getString())/1000)+cooldownTime) - (System.currentTimeMillis()/1000);
        return secondsLeft;
    }

    @ExpectPlatform
    public static boolean hasPermission(ServerPlayer player, String permission) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean hasPermission(CommandSourceStack source, String permission) {
        throw new AssertionError();
    }

    public static boolean isSafe(ServerLevel world, BlockPos.MutableBlockPos mutableBlockPos) {
        if (isEmpty(world, mutableBlockPos) && !isDangerBlocks(world, mutableBlockPos)) {
            return true;
        }
        return false;
    }

    public static boolean isEmpty(ServerLevel world, BlockPos.MutableBlockPos mutableBlockPos) {
        if (world.isEmptyBlock(mutableBlockPos.offset(0, 1, 0)) && world.isEmptyBlock(mutableBlockPos)) {
            return true;
        }
        return false;
    }

    public static boolean isDangerBlocks(ServerLevel world, BlockPos.MutableBlockPos mutableBlockPos) {
        if(isDangerBlock(world, mutableBlockPos) && isDangerBlock(world, mutableBlockPos.offset(0, 1, 0)) &&
                isDangerBlock(world, mutableBlockPos.offset(0, -1, 0))) {
            return true;
        }
        if(world.getBlockState(mutableBlockPos.offset(0, -1, 0)).getBlock() != Blocks.AIR) {
            return false;
        }
        return true;
    }

    public static boolean isDangerBlock(ServerLevel world, BlockPos mutableBlockPos) {
        return world.getBlockState(mutableBlockPos).getBlock() instanceof LiquidBlock;
    }

    private static boolean isInBiomeWhitelist(ResourceLocation biome) {
        //WHITELIST
        if(Config.useBiomeWhitelist()) {
            if(biome == null) {
                return false;
            }
            return Config.getAllowedBiomes().contains(biome.toString());
            //BLACKLIST
        } else {
            if(biome == null) {
                return true;
            }
            return !Config.getAllowedBiomes().contains(biome.toString());
        }
    }
}