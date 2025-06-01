
package mindustry.graphics;
import arc.Core;
import arc.Settings;
import arc.graphics.Color;
import arc.graphics.gl.Shader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class CacheLayerTest {

    @BeforeEach
    void setUp() {
        // Initialize the CacheLayer.all array before tests
        CacheLayer.init();
    }

    @Test
    void testInitializationCreatesLayers() {
        assertNotNull(CacheLayer.all, "CacheLayer.all should be initialized");
        assertTrue(CacheLayer.all.length > 0, "CacheLayer.all should contain layers");
        assertNotNull(CacheLayer.normal, "Normal layer should be initialized");
        assertNotNull(CacheLayer.water, "Water layer should be initialized");
    }

    @Test
    void testLayerIdsAreAssigned() {
        for (int i = 0; i < CacheLayer.all.length; i++) {
            assertEquals(i, CacheLayer.all[i].id, "Layer ID should match its index in the array");
        }
    }

    @Test
    void testAddLayerAtPosition() {
        int originalLength = CacheLayer.all.length;
        CacheLayer newLayer = new CacheLayer();
        int insertPosition = 3;

        CacheLayer.add(insertPosition, newLayer);

        assertEquals(originalLength + 1, CacheLayer.all.length, "Array length should increase by 1");
        assertSame(newLayer, CacheLayer.all[insertPosition], "New layer should be at the specified position");
        assertEquals(insertPosition, newLayer.id, "New layer should have the correct ID");

        // Check that subsequent IDs were updated
        for (int i = 0; i < CacheLayer.all.length; i++) {
            assertEquals(i, CacheLayer.all[i].id, "Layer ID should match its new index");
        }
    }

    @Test
    void testAddLayerDefaultPosition() {
        int originalLength = CacheLayer.all.length;
        CacheLayer newLayer = new CacheLayer();

        CacheLayer.add(newLayer);

        assertEquals(originalLength + 1, CacheLayer.all.length, "Array length should increase by 1");

        // Check that all IDs are correctly assigned
        for (int i = 0; i < CacheLayer.all.length; i++) {
            assertEquals(i, CacheLayer.all[i].id, "Layer ID should match its index");
        }
    }

    @Test
    void testAddLastLayer() {
        int originalLength = CacheLayer.all.length;
        CacheLayer newLayer = new CacheLayer();

        CacheLayer.addLast(newLayer);

        assertEquals(originalLength + 1, CacheLayer.all.length, "Array length should increase by 1");
        assertSame(newLayer, CacheLayer.all[CacheLayer.all.length - 1], "New layer should be at the last position");
        assertEquals(CacheLayer.all.length - 1, newLayer.id, "New layer should have the correct ID");
    }

    @Test
    void testShaderLayerBeginAndEnd() {

        Settings original = Core.settings;
        try {

            Settings mockSettings = mock(Settings.class);

            Core.settings = mockSettings;

            when(mockSettings.getBool(eq("animatedwater"), anyBoolean())).thenReturn(false);

            Shader mockShader = mock(Shader.class);
            CacheLayer.ShaderLayer shaderLayer = new CacheLayer.ShaderLayer(mockShader);


            assertDoesNotThrow(shaderLayer::begin, "ShaderLayer.begin() no excepetion");
            assertDoesNotThrow(shaderLayer::end, "ShaderLayer.end() no excepetion");
        } finally {

            Core.settings = original;
        }
    }

    @Test
    void testLiquidFlagIsSet() {
        assertTrue(CacheLayer.water.liquid, "Water layer should be marked as liquid");
        assertTrue(CacheLayer.mud.liquid, "Mud layer should be marked as liquid");
        assertTrue(CacheLayer.tar.liquid, "Tar layer should be marked as liquid");
        assertTrue(CacheLayer.slag.liquid, "Slag layer should be marked as liquid");
        assertTrue(CacheLayer.cryofluid.liquid, "Cryofluid layer should be marked as liquid");
        assertTrue(CacheLayer.arkycite.liquid, "Arkycite layer should be marked as liquid");
        assertFalse(CacheLayer.space.liquid, "Space layer should not be marked as liquid");
    }
}