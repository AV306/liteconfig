package me.av306.liteconfig.tests;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.av306.liteconfig.ConfigManager;
import me.av306.liteconfig.annotations.ConfigComment;
import me.av306.liteconfig.annotations.IgnoreConfig;

public class ConfigManagerTest
{
    @ConfigComment( "This is a top-level" )
    @ConfigComment( "multiline comment." )
    public static class Configurations
    {
        @ConfigComment( "This is a field-level single-line comment." )
        public static int STATIC_INT = 42;

        @ConfigComment( "This is a field-level" )
        @ConfigComment( "multi-line comment." )
        public static short STATIC_SHORT = 4;
        public static float STATIC_FLOAT = 3.14f;
        public static double STATIC_DOUBLE = 1.4142135;
        public static boolean STATIC_BOOL = true;

        @IgnoreConfig
        public static String IGNORED_FIELD = ":O";

        public static ArrayList<Integer> STATIC_INT_ARRAYLIST
                = new ArrayList<>( Arrays.asList( new Integer[]{2, 4, 6, 8, 10} ) );
        public static ArrayList<String> STATIC_STRING_ARRAYLIST
                = new ArrayList<>( Arrays.asList( new String[]{"hello", "world"} ) );
    }

    private static @TempDir Path tempDir;
    private static ConfigManager configManager;
    private static final String CONFIG_FILE_NAME = "test_config.properties";
    private static final Logger LOGGER = LoggerFactory.getLogger( ConfigManagerTest.class );


    @BeforeAll
    static void setupConfigManager() throws IOException
    {
        configManager = new ConfigManager(
            tempDir.resolve( CONFIG_FILE_NAME ),
            Configurations.class,
            null
        );
    }

    @Test
    void dummyTest()
    {
        assertTrue( true );
    }

    /**
     * Test that {me.av306.liteconfig.ConfigManager} creates a config file
     * when it does not exist.
     * @result Succeeds if the config file exists
     * @throws IOException
     */
    @Test
    void testConfigFileCreation() throws IOException
    {
        assertTrue( tempDir.resolve( CONFIG_FILE_NAME ).toFile().exists() );
    }

    /**
     * Tests that the config file created matches what is described
     * in the configuration class.
     * @throws IOException
     */
    @Test
    void testNewConfigFileContents() throws IOException
    {
        BufferedReader reader = Files.newBufferedReader(
                tempDir.resolve( CONFIG_FILE_NAME ) );
        String contents = reader.lines().collect( Collectors.joining( "\n" ) );
        reader.close();

        String expectedContents = """
        # This is a top-level
        # multiline comment.

        # This is a field-level single-line comment.
        STATIC_INT=42
        # This is a field-level
        # multi-line comment.
        STATIC_SHORT=0x04
        STATIC_FLOAT=3.140000
        STATIC_DOUBLE=1.414214
        STATIC_BOOL=true
        STATIC_INT_ARRAYLIST=[2, 4, 6, 8, 10]
        STATIC_STRING_ARRAYLIST=[hello, world]
        """.trim();

        assertEquals( expectedContents, contents );
    }

    @Test
    void testConfigFileSerialisation()
    {
        Configurations.STATIC_INT = 67;
        Configurations.STATIC_SHORT = 0xFF;
        Configurations.STATIC_FLOAT= 2.718f;
        Configurations.STATIC_DOUBLE = 1.73205;
        Configurations.STATIC_BOOL = !Configurations.STATIC_BOOL;
        Configurations.STATIC_INT_ARRAYLIST = new ArrayList<>( Arrays.asList( 4, 2, 5, 12, 56 ) );
        Configurations.STATIC_INT_ARRAYLIST = new ArrayList<>( Arrays.asList( "a", "rfge", "aebfu" ) );

        //configManager.
    }

    @Test
    void testConfigFileDeserialisation()
    {

    }
}
