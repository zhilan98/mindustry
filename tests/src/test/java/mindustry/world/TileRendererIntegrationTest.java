package mindustry.world;
import arc.util.Buffers;
import mindustry.Vars;
import mindustry.core.World;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TileRendererIntegrationTest {
    private mindustry.core.Renderer renderer;
    private World world;
    private Renderer.Blocks blocks;
    private MockedStatic<Buffers> buffersMock;
    @BeforeEach
    void setup() throws Exception {
        mockStatic(arc.util.Buffers.class);
        when(Buffers.newUnsafeByteBuffer(anyInt())).thenReturn(ByteBuffer.allocate(1024));


        System.setProperty("java.awt.headless", "true");

        TestableRenderer testRenderer = new TestableRenderer();
        blocks = mock(Renderer.Blocks.class);
        testRenderer.setTestBlocks(blocks);

        renderer = testRenderer;
        Vars.renderer = renderer;

        Vars.world = mock(World.class);

        CacheLayer.water.active = false;
        CacheLayer.turret.active = false;
        CacheLayer.walls.active = false;
    }


    @Test
    void whenSetWaterFloor_activateWaterLayer() {

        Floor waterFloor = mock(Floor.class);
        when(waterFloor.isLiquid()).thenReturn(true);
        when(waterFloor.layer()).thenReturn(CacheLayer.water);


        Tile tile = new Tile(0, 0);
        tile.setFloor(waterFloor);

        verify(blocks).addFloorIndex(tile);
        assertTrue(CacheLayer.water.active);
        verifyNoMoreInteractions(blocks);
    }

    @Test
    void whenMultiblockSet_updateMultipleLayers() {

        Block factoryBlock = mock(Block.class);
        when(factoryBlock.isMultiblock()).thenReturn(true);
        when(factoryBlock.size).thenReturn(3);
        when(factoryBlock.layer()).thenReturn(CacheLayer.turret);

        Tile center = new Tile(5, 5);
        center.setBlock(factoryBlock);

        verify(blocks, times(9)).invalidateTile(any());
        assertTrue(CacheLayer.turret.active, "The turret layer should be activated");
        verify(blocks).setBlock(center, factoryBlock);
    }

    @Test
    void whenRemoveBlock_resetLayerState() {
        Block wallBlock = mock(Block.class);
        when(wallBlock.layer()).thenReturn(CacheLayer.walls);

        Tile tile = new Tile(2, 2);
        tile.setBlock(wallBlock);
        tile.setBlock(Blocks.air);

        verify(blocks).recacheWall(tile);
        assertFalse(CacheLayer.walls.active, "should be closed");
        verify(blocks).setBlock(tile, Blocks.air);
    }

    @AfterEach
    void tearDown() {
        CacheLayer.water.active = false;
        CacheLayer.turret.active = false;
        CacheLayer.walls.active = false;
    }
}
