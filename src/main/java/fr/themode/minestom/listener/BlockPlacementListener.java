package fr.themode.minestom.listener;

import fr.themode.minestom.MinecraftServer;
import fr.themode.minestom.data.Data;
import fr.themode.minestom.entity.Entity;
import fr.themode.minestom.entity.GameMode;
import fr.themode.minestom.entity.Player;
import fr.themode.minestom.event.PlayerBlockInteractEvent;
import fr.themode.minestom.event.PlayerBlockPlaceEvent;
import fr.themode.minestom.instance.Chunk;
import fr.themode.minestom.instance.Instance;
import fr.themode.minestom.instance.block.Block;
import fr.themode.minestom.instance.block.BlockManager;
import fr.themode.minestom.instance.block.CustomBlock;
import fr.themode.minestom.instance.block.rule.BlockPlacementRule;
import fr.themode.minestom.inventory.PlayerInventory;
import fr.themode.minestom.item.ItemStack;
import fr.themode.minestom.item.Material;
import fr.themode.minestom.item.StackingRule;
import fr.themode.minestom.net.packet.client.play.ClientPlayerBlockPlacementPacket;
import fr.themode.minestom.net.packet.client.play.ClientPlayerDiggingPacket;
import fr.themode.minestom.utils.BlockPosition;
import fr.themode.minestom.utils.ChunkUtils;

import java.util.Set;

public class BlockPlacementListener {

    private Instance instance;

    public static void listener(ClientPlayerBlockPlacementPacket packet, Player player) {
        PlayerInventory playerInventory = player.getInventory();
        Player.Hand hand = packet.hand;
        ClientPlayerDiggingPacket.BlockFace blockFace = packet.blockFace;
        BlockPosition blockPosition = packet.blockPosition;

        Instance instance = player.getInstance();
        if (instance == null)
            return;

        // Interact at block
        PlayerBlockInteractEvent playerBlockInteractEvent = new PlayerBlockInteractEvent(blockPosition, hand);
        player.callCancellableEvent(PlayerBlockInteractEvent.class, playerBlockInteractEvent, () -> {
            CustomBlock customBlock = instance.getCustomBlock(blockPosition);
            if (customBlock != null) {
                Data data = instance.getBlockData(blockPosition);
                customBlock.onInteract(player, hand, blockPosition, data);
            }
        });

        // Check if item at hand is a block
        ItemStack usedItem = hand == Player.Hand.MAIN ? playerInventory.getItemInMainHand() : playerInventory.getItemInOffHand();
        Material material = Material.fromId(usedItem.getMaterialId());
        if (material != null && !material.isBlock()) {
            return;
        }

        // Get the newly placed block position
        int offsetX = blockFace == ClientPlayerDiggingPacket.BlockFace.WEST ? -1 : blockFace == ClientPlayerDiggingPacket.BlockFace.EAST ? 1 : 0;
        int offsetY = blockFace == ClientPlayerDiggingPacket.BlockFace.BOTTOM ? -1 : blockFace == ClientPlayerDiggingPacket.BlockFace.TOP ? 1 : 0;
        int offsetZ = blockFace == ClientPlayerDiggingPacket.BlockFace.NORTH ? -1 : blockFace == ClientPlayerDiggingPacket.BlockFace.SOUTH ? 1 : 0;

        blockPosition.add(offsetX, offsetY, offsetZ);

        Chunk chunk = instance.getChunkAt(blockPosition);
        Set<Entity> entities = instance.getChunkEntities(chunk);
        boolean intersect = false;
        for (Entity entity : entities) {
            intersect = entity.getBoundingBox().intersect(blockPosition);
            if (intersect)
                break;
        }

        boolean refreshChunk = false;

        if (material.isBlock() && !intersect) {
            PlayerBlockPlaceEvent playerBlockPlaceEvent = new PlayerBlockPlaceEvent((short) 10, blockPosition, packet.hand);
            playerBlockPlaceEvent.consumeBlock(player.getGameMode() != GameMode.CREATIVE);

            // BlockPlacementRule check
            Block block = material.getBlock();
            BlockManager blockManager = MinecraftServer.getBlockManager();
            BlockPlacementRule blockPlacementRule = blockManager.getBlockPlacementRule(block);
            boolean canPlace = true;
            if (blockPlacementRule != null) {
                canPlace = blockPlacementRule.canPlace(instance, blockPosition);
            }

            player.callEvent(PlayerBlockPlaceEvent.class, playerBlockPlaceEvent);
            if (!playerBlockPlaceEvent.isCancelled() && canPlace) {
                instance.setBlock(blockPosition, material.getBlock());
                //instance.setCustomBlock(blockPosition, "updatable");
                if (playerBlockPlaceEvent.doesConsumeBlock()) {

                    StackingRule stackingRule = usedItem.getStackingRule();
                    ItemStack newUsedItem = stackingRule.apply(usedItem, stackingRule.getAmount(usedItem) - 1);

                    if (hand == Player.Hand.OFF) {
                        playerInventory.setItemInOffHand(newUsedItem);
                    } else { // Main
                        playerInventory.setItemInMainHand(newUsedItem);
                    }
                }
            } else {
                refreshChunk = true;
            }
        } else {
            refreshChunk = true;
        }

        // Refresh chunk section if needed
        if (refreshChunk) {
            instance.sendChunkSectionUpdate(chunk, ChunkUtils.getSectionAt(blockPosition.getY()), player);
        }

        player.getInventory().refreshSlot(player.getHeldSlot());
    }

}