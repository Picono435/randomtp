package com.gmail.picono435.randomtp.config.neoforge;

import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public class ConfigHandlerImpl {

	public static Path getConfigDirectory() {
		return FMLPaths.CONFIGDIR.get();
	}

}