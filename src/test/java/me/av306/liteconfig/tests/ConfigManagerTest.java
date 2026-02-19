package me.av306.liteconfig.tests;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.av306.liteconfig.ConfigManager;
import me.av306.liteconfig.annotations.ConfigComment;
import me.av306.liteconfig.annotations.IgnoreConfig;
import me.av306.liteconfig.exceptions.InvalidConfigurationEntryException;

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
        public static double STATIC_DOUBLE = Math.sqrt( 2 );
        public static boolean STATIC_BOOL = true;

        @IgnoreConfig
        public static String IGNORED_FIELD = ":O";

        public static ArrayList<Integer> STATIC_INT_ARRAYLIST
                = new ArrayList<>( Arrays.asList( 2, 4, 6, 8, 10 ) );
        public static ArrayList<String> STATIC_STRING_ARRAYLIST
                = new ArrayList<>( Arrays.asList( "hello", "world" ) );
        
        
        @ConfigComment( "This is a field-level single-line comment." )
        public int instanceInt = 42;

        public short instanceShort = 4;
        public float instanceFloat = 3.14f;
        public double instanceDouble = Math.sqrt( 2 );
        public boolean instanceBool = true;

        @IgnoreConfig
        public String instanceIgnoredField = "hello";

        public ArrayList<Integer> instanceIntArrayList
                = new ArrayList<>( Arrays.asList( 2, 4, 6, 8, 10 ) );
        public ArrayList<String> instanceStringArrayList
                = new ArrayList<>( Arrays.asList( "hello", "beautiful", "world" ) );
    }

    // We need another one because the serialisation test mutates the previous one
    @ConfigComment( "This is a top-level" )
    @ConfigComment( "multiline comment." )
    public static class Configurations2
    {
        @ConfigComment( "This is a field-level single-line comment." )
        public static int STATIC_INT = 42;

        @ConfigComment( "This is a field-level" )
        @ConfigComment( "multi-line comment." )
        public static short STATIC_SHORT = 4;
        public static float STATIC_FLOAT = 3.14f;
        public static double STATIC_DOUBLE = Math.sqrt( 2 );
        public static boolean STATIC_BOOL = true;

        @IgnoreConfig
        public static String IGNORED_FIELD = ":O";

        public static ArrayList<Integer> STATIC_INT_ARRAYLIST
                = new ArrayList<>( Arrays.asList( 2, 4, 6, 8, 10 ) );
        public static ArrayList<String> STATIC_STRING_ARRAYLIST
                = new ArrayList<>( Arrays.asList( "hello", "world" ) );
        
        
        @ConfigComment( "This is a field-level single-line comment." )
        public int instanceInt = 42;

        public short instanceShort = 4;
        public float instanceFloat = 3.14f;
        public double instanceDouble = Math.sqrt( 2 );
        public boolean instanceBool = true;

        @IgnoreConfig
        public String instanceIgnoredField = "hello";

        public ArrayList<Integer> instanceIntArrayList
                = new ArrayList<>( Arrays.asList( 2, 4, 6, 8, 10 ) );
        public ArrayList<String> instanceStringArrayList
                = new ArrayList<>( Arrays.asList( "hello", "beautiful", "world" ) );
    }

    // Another one for the malformed entry tests...
    public static class Configurations3
    {
        public static int STATIC_INT = 42;

        public static short STATIC_SHORT = 4;
        public static float STATIC_FLOAT = 3.14f;
        public static double STATIC_DOUBLE = Math.sqrt( 2 );
        public static boolean STATIC_BOOL = true;

        public static ArrayList<Integer> STATIC_INT_ARRAYLIST
                = new ArrayList<>( Arrays.asList( 2, 4, 6, 8, 10 ) );
        public static ArrayList<String> STATIC_STRING_ARRAYLIST
                = new ArrayList<>( Arrays.asList( "hello", "world" ) );
        
        public int instanceInt = 42;

        public short instanceShort = 4;
        public float instanceFloat = 3.14f;
        public double instanceDouble = Math.sqrt( 2 );
        public boolean instanceBool = true;

        public ArrayList<Integer> instanceIntArrayList
                = new ArrayList<>( Arrays.asList( 2, 4, 6, 8, 10 ) );
        public ArrayList<String> instanceStringArrayList
                = new ArrayList<>( Arrays.asList( "hello", "beautiful", "world" ) );
    }

    private static final Logger LOGGER = LoggerFactory.getLogger( ConfigManagerTest.class );
    private final double epsilon = 0.000001;

    /**
     * This test does the following:
     * 
     * <p><ol>
     *  <li>Checks that a new configuration file is created</li>
     *  <li>Checks the contents of the new configuration file</li>
     *  <li>Checks that modified configuration fields are serialised correctly</li>
     * </ol></p>
     * @result Succeeds if the 3 checks pass
     * @throws IOException
     */
    @Test
    void testStaticConfigurationSerialisation( @TempDir Path tempDir ) throws IOException
    {
        String configFileName = "test_static_configuration_serialisation.properties";
        Path configFilePath = tempDir.resolve( configFileName );
        ConfigManager configManager = new ConfigManager(
            configFilePath,
            Configurations.class,
            null
        );


        // ========== Test that the configuration file was created ==========
        assertTrue( configManager.deserialiseConfigurationFileOrElseCreateNew() );
        assertTrue( configFilePath.toFile().exists(), "Configuration file not created" );


        // ========== Test the contents of the new configuration file ==========
        String contents = readConfigFile( configFilePath );
        // Note: the instance fields (and their comments) will be ignored
        // since we did not provide a configuration class instance
        String expectedOriginalContents = """
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

        assertEquals( expectedOriginalContents, contents, "Original config file contents do not match expected" );
        

        // ========== Test static variable modification ==========
        Configurations.STATIC_INT = 67;
        Configurations.STATIC_SHORT = 0xFF;
        Configurations.STATIC_FLOAT= 2.718f;
        Configurations.STATIC_DOUBLE = Math.sqrt( 5 );
        Configurations.STATIC_BOOL = !Configurations.STATIC_BOOL;
        Configurations.STATIC_INT_ARRAYLIST = new ArrayList<>( Arrays.asList( 4, 2, 5, 12, 56 ) );
        Configurations.STATIC_STRING_ARRAYLIST = new ArrayList<>( Arrays.asList( "a", "rfge", "aebfu" ) );
        configManager.serialiseConfigurationsCompletely();

        String expectedNewContents = """
        # This is a top-level
        # multiline comment.

        # This is a field-level single-line comment.
        STATIC_INT=67
        # This is a field-level
        # multi-line comment.
        STATIC_SHORT=0xFF
        STATIC_FLOAT=2.718000
        STATIC_DOUBLE=2.236068
        STATIC_BOOL=false
        STATIC_INT_ARRAYLIST=[4, 2, 5, 12, 56]
        STATIC_STRING_ARRAYLIST=[a, rfge, aebfu]
        """.trim();
        contents = readConfigFile( configFilePath );

        assertEquals( expectedNewContents, contents, "New config file contents do not match expected" );
    }

    @Test
    void testInstanceConfigurationSerialisation( @TempDir Path tempDir ) throws IOException
    {
        // TODO: this needs to ignore the static field values
        String configFileName = "test_instance_configuration_serialisation.properties";
        Path configFilePath = tempDir.resolve( configFileName );

        Configurations configurationsInstance = new Configurations();
        ConfigManager configManager = new ConfigManager(
            tempDir.resolve( configFileName ),
            Configurations.class,
            configurationsInstance
        );

        // Quickly check that we're not reusing any old configuration file
        boolean newConfigFileCreated = configManager.deserialiseConfigurationFileOrElseCreateNew();
        assertTrue( newConfigFileCreated, "A new configuration file was not created" );

        String contents = readConfigFile( configFilePath, 13 );

        // The instance fields start at line 14
        String expectedOriginalContent = """
        # This is a field-level single-line comment.
        instanceInt=42
        instanceShort=0x04
        instanceFloat=3.140000
        instanceDouble=1.414214
        instanceBool=true
        instanceIntArrayList=[2, 4, 6, 8, 10]
        instanceStringArrayList=[hello, beautiful, world]
        """.trim();

        assertEquals( expectedOriginalContent, contents, "Original configuration file contents do not match expected" );

        configurationsInstance.instanceInt = 214;
        configurationsInstance.instanceShort = 0x20;
        configurationsInstance.instanceFloat = 2.718f;
        configurationsInstance.instanceDouble = Math.sqrt( 3 );
        configurationsInstance.instanceBool = !configurationsInstance.instanceBool;
        configurationsInstance.instanceIntArrayList = new ArrayList<>( Arrays.asList( 6, 35, 35, 725, 7801 ) );
        configurationsInstance.instanceStringArrayList = new ArrayList<>( Arrays.asList( "aeth", "vxcioub", "uhdgx" ) );

        configManager.serialiseConfigurationsCompletely();

        String expectedNewContents = """
        # This is a field-level single-line comment.
        instanceInt=214
        instanceShort=0x20
        instanceFloat=2.718000
        instanceDouble=1.732051
        instanceBool=false
        instanceIntArrayList=[6, 35, 35, 725, 7801]
        instanceStringArrayList=[aeth, vxcioub, uhdgx]
        """.trim();

        contents = readConfigFile( configFilePath, 13 );

        assertEquals( expectedNewContents, contents, "New configuration file contents do not match expected" );
    }

    @Test
    void testStaticConfigurationDeserialisation( @TempDir Path tempDir ) throws IOException
    {
        String configFileName = "test_static_configuration_deserialisation.properties";
        Path configFilePath = tempDir.resolve( configFileName );

        ConfigManager configManager = new ConfigManager(
            configFilePath,
            Configurations2.class,
            null
        );

        // Sanity-check the initial contents
        assertEquals( 42, Configurations2.STATIC_INT );
        assertEquals( 4, Configurations2.STATIC_SHORT );
        assertEquals( 3.14f, Configurations2.STATIC_FLOAT );
        assertEquals( Math.sqrt( 2 ), Configurations2.STATIC_DOUBLE );
        assertEquals( true, Configurations2.STATIC_BOOL );
        assertEquals( new ArrayList<>( Arrays.asList( 2, 4, 6, 8, 10 ) ),
                Configurations2.STATIC_INT_ARRAYLIST );
        assertEquals( new ArrayList<>( Arrays.asList( "hello", "world" ) ),
                Configurations2.STATIC_STRING_ARRAYLIST );
        
        // We won't check serialisation; the two previous tests cover that

        String newContents = """
        # This is a top-level
        # multiline comment.

        # This is a field-level single-line comment.
        STATIC_INT=67
        # This is a field-level
        # multi-line comment.
        STATIC_SHORT=0xFF
        STATIC_FLOAT=2.718000
        STATIC_DOUBLE=2.236068
        STATIC_BOOL=false
        STATIC_INT_ARRAYLIST=[4, 2, 5, 12, 56]
        STATIC_STRING_ARRAYLIST=[a, rfge, aebfu]
        """.trim();

        writeConfigFile( configFilePath, newContents );

        // Sanity check the file contents
        assertEquals( readConfigFile( configFilePath ), newContents );

        assertFalse( configManager.deserialiseConfigurationFileOrElseCreateNew(), "A new configuration file was created despite one already existing" );

        // Sanity check the file contents, again
        // TODO: can we check the modified date?
        assertEquals( readConfigFile( configFilePath ), newContents,
                "Configuration file was modified after deserialise" );

        // Check the new configuration values
        assertEquals( 67, Configurations2.STATIC_INT );
        assertEquals( 0xFF, Configurations2.STATIC_SHORT );
        assertEquals( 2.718f, Configurations2.STATIC_FLOAT );
        assertEquals( Math.sqrt( 5 ), Configurations2.STATIC_DOUBLE, epsilon );
        assertEquals( false, Configurations2.STATIC_BOOL );
        assertEquals( new ArrayList<>( Arrays.asList( 4, 2, 5, 12, 56 ) ),
                Configurations2.STATIC_INT_ARRAYLIST );
        assertEquals( new ArrayList<>( Arrays.asList( "a", "rfge", "aebfu" ) ),
                Configurations2.STATIC_STRING_ARRAYLIST );
    }

    @Test
    void testInstanceConfigurationDeserialisation( @TempDir Path tempDir ) throws IOException
    {
        String configFileName = "test_instance_configuration_deserialisation.properties";
        Path configFilePath = tempDir.resolve( configFileName );
        Configurations2 instance = new Configurations2();

        ConfigManager configManager = new ConfigManager(
            configFilePath,
            Configurations2.class,
            instance
        );

        // Sanity-check the initial contents
        assertEquals( 42, instance.instanceInt );
        assertEquals( 4, instance.instanceShort );
        assertEquals( 3.14f, instance.instanceFloat );
        assertEquals( Math.sqrt( 2 ), instance.instanceDouble );
        assertEquals( true, instance.instanceBool );
        assertEquals( new ArrayList<>( Arrays.asList( 2, 4, 6, 8, 10 ) ),
                instance.instanceIntArrayList );
        assertEquals( new ArrayList<>( Arrays.asList( "hello", "beautiful", "world" ) ),
                instance.instanceStringArrayList );
        
        // We won't check serialisation; the two previous tests cover that

        String newContents = """
        # This is a field-level single-line comment.
        instanceInt=214
        instanceShort=0x20
        instanceFloat=2.718000
        instanceDouble=1.732051
        instanceBool=false
        instanceIntArrayList=[6, 35, 335, 725, 7801]
        instanceStringArrayList=[aeth, vxcioub, uhdgx]
        """.trim();

        writeConfigFile( configFilePath, newContents );

        // Sanity check the file contents
        assertEquals( readConfigFile( configFilePath ), newContents );

        assertFalse( configManager.deserialiseConfigurationFileOrElseCreateNew(), "A new configuration file was created despite one already existing" );

        // Sanity check the file contents, again
        // TODO: can we check the modified date?
        assertEquals( readConfigFile( configFilePath ), newContents,
                "Configuration file was modified after deserialise" );

        // Check the new configuration values
        assertEquals( 214, instance.instanceInt );
        assertEquals( 0x20, instance.instanceShort );
        assertEquals( 2.718f, instance.instanceFloat );
        assertEquals( Math.sqrt( 3 ), instance.instanceDouble, epsilon );
        assertEquals( false, instance.instanceBool );
        assertEquals( new ArrayList<>( Arrays.asList( 6, 35, 335, 725, 7801 ) ),
                instance.instanceIntArrayList );
        assertEquals( new ArrayList<>( Arrays.asList( "aeth", "vxcioub", "uhdgx" ) ),
                instance.instanceStringArrayList );
    }

    @Test
    void testStaticConfigurationDeserialisationMalformedNumber( @TempDir Path tempDir ) throws IOException
    {
        String configFileName = "test_static_configuration_deserialisation_malformed_number.properties";
        Path configFilePath = tempDir.resolve( configFileName );

        ConfigManager configManager = new ConfigManager(
            configFilePath,
            Configurations3.class,
            null
        );

        // Sanity-check the initial contents
        assertEquals( 42, Configurations3.STATIC_INT );
        assertEquals( 4, Configurations3.STATIC_SHORT );
        assertEquals( 3.14f, Configurations3.STATIC_FLOAT );
        assertEquals( Math.sqrt( 2 ), Configurations3.STATIC_DOUBLE );
        assertEquals( true, Configurations3.STATIC_BOOL );
        assertEquals( new ArrayList<>( Arrays.asList( 2, 4, 6, 8, 10 ) ),
                Configurations3.STATIC_INT_ARRAYLIST );
        assertEquals( new ArrayList<>( Arrays.asList( "hello", "world" ) ),
                Configurations3.STATIC_STRING_ARRAYLIST );
        
        // We won't check serialisation; the two previous tests cover that

        String newContents = """
        # This is a top-level
        # multiline comment.

        # This is a field-level single-line comment.
        STATIC_INT=42
        # This is a field-level
        # multi-line comment.
        STATIC_SHORT=notanumber
        STATIC_FLOAT=2.718000
        STATIC_DOUBLE=2.236068
        STATIC_BOOL=false
        STATIC_INT_ARRAYLIST=[4, 2, 5, 12, 56]
        STATIC_STRING_ARRAYLIST=[a, rfge, aebfu]
        """.trim();

        writeConfigFile( configFilePath, newContents );

        // Sanity check the file contents
        assertEquals( readConfigFile( configFilePath ), newContents );

        assertThrows( NumberFormatException.class, configManager::deserialiseConfigurationFile );

        // Check the new configuration values weren't modified
        assertEquals( 42, Configurations3.STATIC_INT );
        assertEquals( 4, Configurations3.STATIC_SHORT );
        assertEquals( 3.14f, Configurations3.STATIC_FLOAT );
        assertEquals( Math.sqrt( 2 ), Configurations3.STATIC_DOUBLE );
        assertEquals( true, Configurations3.STATIC_BOOL );
        assertEquals( new ArrayList<>( Arrays.asList( 2, 4, 6, 8, 10 ) ),
                Configurations3.STATIC_INT_ARRAYLIST );
        assertEquals( new ArrayList<>( Arrays.asList( "hello", "world" ) ),
                Configurations3.STATIC_STRING_ARRAYLIST );        
    }

    // TODO: we need to find a better way to isolate the static fields without having one class for each deserialisation test.
    // TODO: also tests need to be updated when we make configmanager rollback changes
    @Test
    void testStaticConfigurationDeserialisationMalformedLine( @TempDir Path tempDir ) throws IOException
    {
        String configFileName = "test_static_configuration_deserialisation_malformed.properties";
        Path configFilePath = tempDir.resolve( configFileName );

        ConfigManager configManager = new ConfigManager(
            configFilePath,
            Configurations3.class,
            null
        );

        // Sanity-check the initial contents
        assertEquals( 42, Configurations3.STATIC_INT );
        assertEquals( 4, Configurations3.STATIC_SHORT );
        assertEquals( 3.14f, Configurations3.STATIC_FLOAT );
        assertEquals( Math.sqrt( 2 ), Configurations3.STATIC_DOUBLE );
        assertEquals( true, Configurations3.STATIC_BOOL );
        assertEquals( new ArrayList<>( Arrays.asList( 2, 4, 6, 8, 10 ) ),
                Configurations3.STATIC_INT_ARRAYLIST );
        assertEquals( new ArrayList<>( Arrays.asList( "hello", "world" ) ),
                Configurations3.STATIC_STRING_ARRAYLIST );
        
        String newContents = """
        STATIC_INT=42
        # This is a field-level
        # multi-line comment.
        STATIC_SHORT=
        STATIC_FLOAT=2.718000
        STATIC_DOUBLE=2.236068
        STATIC_BOOL=false
        STATIC_INT_ARRAYLIST=[4, 2, 5, 12, 56]
        STATIC_STRING_ARRAYLIST=[a, rfge, aebfu]
        """.trim();

        writeConfigFile( configFilePath, newContents );

        // Sanity check the file contents
        assertEquals( readConfigFile( configFilePath ), newContents );

        assertThrows( InvalidConfigurationEntryException.class, configManager::deserialiseConfigurationFile );

        assertEquals( 42, Configurations3.STATIC_INT );
        assertEquals( 4, Configurations3.STATIC_SHORT );
        assertEquals( 3.14f, Configurations3.STATIC_FLOAT );
        assertEquals( Math.sqrt( 2 ), Configurations3.STATIC_DOUBLE );
        assertEquals( true, Configurations3.STATIC_BOOL );
        assertEquals( new ArrayList<>( Arrays.asList( 2, 4, 6, 8, 10 ) ),
                Configurations3.STATIC_INT_ARRAYLIST );
        assertEquals( new ArrayList<>( Arrays.asList( "hello", "world" ) ),
                Configurations3.STATIC_STRING_ARRAYLIST );
    }

    @Test
    void testPrintAllConfigs()
    {
        // TODO
    }


    String readConfigFile( Path path ) throws IOException
    {
        try ( BufferedReader reader = Files.newBufferedReader( path ) )
        {
            return reader.lines().collect( Collectors.joining( "\n" ) );
        }
        catch ( IOException e )
        {
            throw e;
        }
    }

    String readConfigFile( Path path, long linesToSkip ) throws IOException
    {
        try ( BufferedReader reader = Files.newBufferedReader( path ) )
        {
            return reader.lines().skip( linesToSkip )
                    .collect( Collectors.joining( "\n" ) );
        }
        catch ( IOException e )
        {
            throw e;
        }
    }

    void writeConfigFile( Path path, String contents ) throws IOException
    {
        try ( BufferedWriter writer = Files.newBufferedWriter( path, StandardOpenOption.CREATE ) )
        {
            writer.write( contents );
        }
        catch ( IOException ioe )
        {
            throw ioe;
        }
    }
}
