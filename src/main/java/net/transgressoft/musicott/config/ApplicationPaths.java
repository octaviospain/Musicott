package net.transgressoft.musicott.config;

import java.nio.file.Path;

public record ApplicationPaths(Path audioItemsPath, Path playlistsPath, Path waveformsPath) {
}