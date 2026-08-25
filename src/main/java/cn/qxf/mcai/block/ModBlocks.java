package cn.qxf.mcai.block;

import cn.qxf.mcai.QxfMcAi;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, QxfMcAi.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, QxfMcAi.MOD_ID);
    public static final RegistryObject<Block> DRAGON_GAME_BOARD = BLOCKS.register("dragon_game_board", GameBoardBlock::new);
    public static final RegistryObject<Item> DRAGON_GAME_BOARD_ITEM = ITEMS.register("dragon_game_board",
        () -> new BlockItem(DRAGON_GAME_BOARD.get(), new Item.Properties().stacksTo(1)));

    private ModBlocks() {}

    @Mod.EventBusSubscriber(modid = QxfMcAi.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class CreativeTab {
        @SubscribeEvent
        public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
            if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(DRAGON_GAME_BOARD_ITEM.get());
        }
    }
}
