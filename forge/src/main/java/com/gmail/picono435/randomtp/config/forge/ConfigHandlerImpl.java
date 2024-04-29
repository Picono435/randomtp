package com.gmail.picono435.randomtp.config.forge;

import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public class ConfigHandlerImpl.javaConfigHandlerImpl {

	public static Path getConfigDirectory() {
		return FMLPaths.CONFIGDIR.get();
	}

}