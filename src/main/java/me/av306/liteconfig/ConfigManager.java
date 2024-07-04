package me.av306.liteconfig;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration manager. Handles reading/saving config file, and setting fields in confugurable class.
 */
public class ConfigManager
{
    private final String name; /** Application name, used to locate config file */
    private final Path configFileDirectory; /** Directory containing the config file */
    private final String configFileName; /** Name of the config file */
    private final Class<?> configurableClass; /** The Class object holding the config fields */
    private Configurable configurableClassInstance; /** The instance on which to set the config fields (if instance fields are used) */

    private File configFile; /** A {@Link java.io.File} object representing the config file, guaranteed to exist after {@Link #checkConfigFile} is run */
    
    /**
     * Full constructor for a config manager.
     * @param name: Name of the application, used in logging statements
     * @param configFilePath: Path to the config file
     * @param configFileName: Name of the config file (with extension, e.g. "app_config.properties")
     * @param configurableClass: {@Link java.lang.Class} object that holds the configurable fields (use NameOfClass.class or classInstance.getClass())
     * @param configurableClassInstance: Instance of the previous configurable object, if instance fields are used. Pass NULL here if static fields are used
     */
    public ConfigManager(
        String name, Path configFilePath, String configFileName,
        Class<?> configurableClass,
        Configurable configurableClassInstance )
    {
        this.name = name;
        this.configFileDirectory = configFilePath;
        this.configFileName = configFileName;
        this.configurableClass = configurableClass;
        this.configurableClassInstance = configurableClassInstance;

        this.checkConfigFile();
        this.readConfigFile();
    }

    /**
     * Check for the existence of a config file, and copy the bundled one over if needed
     */
    private void checkConfigFile()
    {
        this.configFile = this.configFileDirectory.resolve( this.configFileName ).toFile();

        if ( !this.configFile.exists() )
        {
            try ( FileOutputStream fos = new FileOutputStream( this.configFile ); )
            {
                this.configFile.createNewFile();
                
                System.err.printf( "%s config file not found, copying embedded file%n", this.name );
                fos.write( this.getClass().getResourceAsStream( "/" + this.configFileName ).readAllBytes() );
            }
            catch ( IOException ioe )
            {
                System.err.println( "IOException while copying default config file!" );
                ioe.printStackTrace();
            }
        }

        System.out.println( "Finished checking config file!" );
    }

    /**
     * Read configs from the config file.
     * 
     * NOTE: entries in the config file MUST match field names EXACTLY.
     */
    public void readConfigFile()
    {
        try ( BufferedReader reader = new BufferedReader( new FileReader( this.configFile ) ) )
        {
            // Iterate over each line in the file
            for ( String line : reader.lines().toArray( String[]::new ) )
            {
                // Skip comments and blank lines
                if ( line.startsWith( "#" ) || line.isBlank() ) continue;
                
                // Split it by the equals sign (.properties format)
                String[] entry = line.split( "=" );

                // Trim lines so you can have spaces around the equals ("prop = val" as opposed to "prop=val")
                entry[0] = entry[0].trim();
                entry[1] = entry[1].trim();

                // Set fields in configurable class
                try
                {
                    Field f = this.configurableClass.getDeclaredField( entry[0] );
                    
                    //System.out.println( f.getType().getName() );
                    if ( f.getType().isAssignableFrom( short.class ) )
                    {
                        // Short value (0x??)
                        f.setShort( this.configurableClassInstance, Short.parseShort(
                                entry[1].replace( "0x", "" ),
                                16 )
                        );
                    }
                    else if ( f.getType().isAssignableFrom( int.class ) )
                    {
                        // Integer value
                        f.setInt(
                            this.configurableClassInstance,
                            Integer.parseInt( entry[1] )
                        );
                    }
                    else if ( f.getType().isAssignableFrom( float.class ) )
                    {
                        f.setFloat(
                            this.configurableClassInstance,
                            Float.parseFloat( entry[1] )
                        );
                    }
                    else if ( f.getType().isAssignableFrom( boolean.class ) )
                    {
                        f.setBoolean(
                            this.configurableClassInstance,
                            Boolean.parseBoolean( entry[1] )
                        );
                    }
                    else
                    {
                        System.err.printf( "Unrecognised data type for config entry %s%n", line );
                    }
                }
                catch ( ArrayIndexOutOfBoundsException | NumberFormatException e )
                {
                    System.err.printf( "Malformed config entry: %s%n", line );
                }
                catch ( NoSuchFieldException nsfe )
                {
                    System.err.printf( "No matching field found for config entry: %s%n", line );
                }
                catch ( IllegalAccessException illegal )
                {
                    System.err.printf( "Could not set field involved in: %s%m", line );
                    illegal.printStackTrace();
                }
            }
        }
        catch ( IOException ioe )
        {
            System.err.printf( "IOException while reading config file: %s%n", ioe.getMessage() );
        }

        System.out.println( "Finished reading config file!" );
    }

    /**
     * Save the modified configs into the config file.
     */
    public void saveConfigFile()
    {
        // Check old config file
        // POV: user deleted config file partway through execution
        this.checkConfigFile();

        // Create temporary config file
        File tempConfigFile;
        try
        {
            tempConfigFile = File.createTempFile( this.configFileName, ".tmp" );
        }
        catch ( IOException ioe )
        {
            System.err.println( "IOException while creating temporary config file; not saving configs" );
            ioe.printStackTrace();
            return;
        }

        // Scan through each line in the config file
        try (
            BufferedReader reader = new BufferedReader( new FileReader( this.configFile ) );
            BufferedWriter writer = new BufferedWriter( new FileWriter( tempConfigFile ) )
        )
        {
            reader.lines().forEach( line ->
            {
                try
                {
                    // Copy comments and blank lines, then continue
                    if ( line.startsWith( "#" ) || line.isBlank() )
                    {
                        writer.write( line );
                        return;
                    }

                    // Split line
                    String[] entry = line.trim().split( "=" );
                    entry[0] = entry[0].trim();
                    //entry[1] = entry[1].trim();
                    
                    // Serialise config value from field
                    // Catch problems here and continue, to ensure other configs are written
                    try
                    {
                        Field f = this.configurableClass.getDeclaredField( entry[0] );

                        if ( f.getType().isAssignableFrom( short.class ) )
                        {
                            // Short value (0x??)
                            entry[1] = "0x" + f.getShort( this.configurableClassInstance );
                        }
                        else if ( f.getType().isAssignableFrom( int.class ) )
                        {
                            // Integer value
                            entry[1] = String.valueOf( f.getInt( this.configurableClassInstance ) );
                        }
                        else if ( f.getType().isAssignableFrom( float.class ) )
                        {
                            entry[1] = String.valueOf( f.getFloat( this.configurableClassInstance ) );
                        }
                        else if ( f.getType().isAssignableFrom( boolean.class ) )
                        {
                            entry[1] = String.valueOf( f.getBoolean( this.configurableClassInstance ) );
                        }
                        else
                        {
                            System.err.printf( "Unrecognised data type for config entry %s%n", line );
                        }
                    }
                    catch ( ArrayIndexOutOfBoundsException oobe )
                    {
                        // Malformed config line
                        System.out.printf( "Malformed config line: %s%n", line );
                    }
                    catch ( NoSuchFieldException nsfe )
                    {
                        // Invalid config key
                        System.err.printf( "No matching field found for config entry: %s%n", entry[0] );
                    }
                    catch ( IllegalAccessException illegal )
                    {
                        // Illegal field access
                        System.err.printf( "Illegal access on field %s%n", entry[0] );
                    }

                    // Write modified line to temp file
                    writer.write( entry[0] + "=" + entry[1] );
                }
                catch ( IOException ioe )
                {
                    // IOException when writing line
                    System.err.printf( "IOException while saving config line: %s%n", line );
                    ioe.printStackTrace();
                }
            } );

            // Backup old file
            Files.move(
                this.configFile.toPath(),
                this.configFile.toPath().resolveSibling( this.configFileName + ".bak" )
            );

            // Move temp file over
            Files.move(
                tempConfigFile.toPath(),
                this.configFileDirectory.resolve( this.configFileName )
            );
        }
        catch ( IOException ioe )
        {
            // IOException somewhere else (ugh)
            System.err.printf( "IOException while saving config file" );
            ioe.printStackTrace();
        }

        System.out.println( "Finished saving config file!" );
    }
}