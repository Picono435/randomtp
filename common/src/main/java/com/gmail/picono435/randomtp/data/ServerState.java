package com.gmail.picono435.randomtp.data;

import com.gmail.picono435.randomtp.RandomTPMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.apache.logging.log4j.core.jmx.Server;

import java.util.HashMap;
import java.util.UUID;

public class ServerState extends SavedData {

    public HashMap<UUID, PlayerState> players = new HashMap<>();

    @Override
    public CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider provider) {
        CompoundTag playersNbtCompound = new CompoundTag();
        players.forEach((UUID, playerSate) -> {
            CompoundTag playerStateNbt = new CompoundTag();

            playerStateNbt.putBoolean("hasJoined", playerSate.hasJoined);

            playersNbtCompound.put(String.valueOf(UUID), playerStateNbt);
        });
        compoundTag.put("players", playersNbtCompound);
        return compoundTag;
    }

    public static ServerState createFromNbt(CompoundTag compoundTag, HolderLookup.Provider provider) {
        ServerState serverState = new ServerState();

        CompoundTag playersTag = compoundTag.getCompound("players");
        playersTag.getAllKeys().forEach(key -> {
            PlayerState playerState = new PlayerState();

            playerState.hasJoined = playersTag.getCompound(key).getBoolean("hasJoined");

            UUID uuid = UUID.fromString(key);
            serverState.players.put(uuid, playerState);
        });

        return serverState;
    }

    public static ServerState getServerState(MinecraftServer server) {
        DimensionDataStorage persistentStateManager = server
                .getLevel(Level.OVERWORLD).getDataStorage();

        ServerState serverState = persistentStateManager.computeIfAbsent(
                new SavedData.Factory<>(ServerState::new, ServerState::createFromNbt, null),
                RandomTPMod.MOD_ID);

        serverState.setDirty();

        return serverState;
    }

    public static PlayerState getPlayerState(LivingEntity player) {
        ServerState serverState = getServerState(player.getServer());

        PlayerState playerState = serverState.players.computeIfAbsent(player.getUUID(), uuid -> new PlayerState());

        return playerState;
    }
}
