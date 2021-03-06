package com.norcode.bukkit.griefprevention.tasks;

import org.bukkit.Chunk;

public class RestoreChunksExecutionTask implements Runnable {

    private Chunk[] chunksregen = null;

    public RestoreChunksExecutionTask(Chunk regenerate) {
        this(new Chunk[]{regenerate});
    }

    public RestoreChunksExecutionTask(Chunk[] regenerate) {

    }

    @Override
    public void run() {
        if (chunksregen == null) return;
        for (Chunk iterate : chunksregen) {
            iterate.getWorld().regenerateChunk(iterate.getX(), iterate.getZ());
        }
    }
}
