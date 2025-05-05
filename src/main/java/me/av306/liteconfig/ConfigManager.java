package me.av306.liteconfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.text.Format;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Handles reading/saving config file, and setting fields in confugurable class.
 * 
 * Note: when you use this, you should change a few things:
 * - logging proxy methods (uses System.out/err by default; doesn't support format strings)
 * - logging messages (e.g. adding identifiers)
 * 
*/
public class ConfigManager
{
    private final String name; /** Application name, used only for logging */
    private final Path configFileDirectory; /** Directory containing the config file */
    private final String configFileName; /** Name of the config file */
    private final Class<?> configurableClass; /** The Class object holding the config fields */
    private Object configurableClassInstance; /** The Class instance on which to set the config fields (if instance fields are used) */

    /** A File object representing the config file, guaranteed to exist after checkConfigFile() is run */
    private File configFile;

    /** True if there were errors when reading the config file */
    public boolean errorFlag = false;

    /** Logging proxy method; replace contents with your logging method */
    private static void logInfo( String format, Object... params )
    {
    }
    System.out.println( "[INFO] " + format );

    /** Logging proxy method; replace contents with your logging method */
    private static void logWarning( String format, Object... params )
    {
        System.out.println( "[WARN] " + format );
    }

    /** Logging proxy method; replace contents with your logging method */
    private static void logError( String format, Object... params )
    {
        System.err.println( format );
    }
    
    /**
     * Constructor that tries to find a default config file in the JAR resources section.
     * This constructor calls checkConfigFileExists() and readConfigFile().
     *
     * @param name: Name of the application, used in logging statements
     * @param configFilePath: Path to the config file
     * @param configFileName: Name of the config file (with extension, e.g. "app_config.properties") (this will be used both to name the newly created one, and to find the embedded default one)
     * @param configurableClass: {java.lang.Class} object that holds the configurable fields (use NameOfClass.class or classInstance.getClass())
     * @param configurableClassInstance: Instance of the previous configurable object, if instance fields are used. Pass NULL here if static fields are used
     */
    public ConfigManager(
        String name, Path configFilePath, String configFileName,
        Class<?> configurableClass,
        Object configurableClassInstance
    ) throws IOException
    {
        this.name = name;
        this.configFileDirectory = configFilePath;
        this.configFileName = configFileName;
        this.configurableClass = configurableClass;
        this.configurableClassInstance = configurableClassInstance;

        this.checkConfigFileExists();
        this.readConfigFile();
    }

    /**
     * Check for the existence of a config file, and copy the one on the classpath over if needed
    */
    public void checkConfigFileExists() throws IOException
    {
        // TODO: I'm not too sure about how to handle closing all the streams, any help from more experienced devs would be much appreciated
        // https://stackoverflow.com/questions/38698182/close-java-8-stream about closing streams?
        // or https://stackoverflow.com/questions/76815547/if-an-ioexception-occurs-while-invoking-close-is-the-stream-closed-anyway

        this.configFile = this.configFileDirectory.resolve( this.configFileName ).toFile();

        if ( !this.configFile.exists() )
        {
            // Config file doesn't exist - create it by copying the template from resources
            try (
                InputStream defaultConfigFileInputStream = this.getClass().getResourceAsStream( "/" + this.configFileName );
                FileOutputStream fos = new FileOutputStream( this.configFile )
            )
            {
                this.configFile.createNewFile();
                
                logWarning( "{} config file not found, copying default config file", this.name );
                defaultConfigFileInputStream.transferTo( fos );
            }
            catch ( IOException ioe ) 
            {
                logError( "IOException while copying default config file!" );
                ioe.printStackTrace();
                throw ioe; // Re-throw for user app to handle exception
            }
        }

        logInfo( "Config file exists!" );
    }

    /**
     * Read configs from the config file. Sets hasCustomData if invalid config statements were read.
    * <br>
    * NOTE: entries in the config file MUST match field names EXACTLY (case-insensitive)
    */
    public void readConfigFile() throws IOException
    {
        // Reset error flag
        this.errorFlag = false;

        try ( BufferedReader reader = new BufferedReader( new FileReader( this.configFile ) ) )
        {
            // Iterate over each line in the file
            for ( String line : reader.lines().toArray( String[]::new ) )
            {
                // Skip comments and blank lines
                if ( line.trim().startsWith( "#" ) || line.isBlank() ) continue;
                
                // Split it by the equals sign (.properties format)
                String[] entry = line.split( "=" );

                try
                {
                    // Trim lines so you can have spaces around the equals ("prop = val" as opposed to "prop=val")
                    entry[0] = entry[0].trim();
                    entry[1] = entry[1].trim();

                    // Set fields in configurable class
                    Field f = this.configurableClass.getDeclaredField( entry[0].toUpperCase( Locale.getDefault() ) );
                    Class<?> fieldTypeClass = f.getType();
                    
                    //System.out.println( f.getType().getName() );
                    if ( fieldTypeClass.isAssignableFrom( short.class ) )
                    {
                        // Short value (0x??)
                        f.setShort( this.configurableClassInstance, Short.parseShort(
                                entry[1].replace( "0x", "" ),
                                16 )
                        );
                    }
                    else if ( fieldTypeClass.isAssignableFrom( int.class ) )
                    {
                        // Integer value
                        if ( entry[1].startsWith( "0x" ) )
                        {
                            // Hex literal
                            Integer.parseInt(
                                    entry[1].replace( "0x", "" ),
                                    16
                            );
                        }
                        else f.setInt(
                            this.configurableClassInstance,
                            Integer.parseInt( entry[1] )
                        );
                    }
                    else if ( fieldTypeClass.isAssignableFrom( float.class ) )
                    {
                        f.setFloat(
                            this.configurableClassInstance,
                            Float.parseFloat( entry[1] )
                        );
                    }
                    else if ( fieldTypeClass.isAssignableFrom( boolean.class ) )
                    {
                        f.setBoolean(
                            this.configurableClassInstance,
                            Boolean.parseBoolean( entry[1] )
                        );
                    }
                    else if ( fieldTypeClass.isAssignableFrom( ArrayList.class ) )
                    {
                        // I HATE TYPE ERASURE GRRR
                        // Fck this i'm kicking the can down the road
                        // only supports int lists
                        // FIXME: someone help me with the stupid type thing

                        // Remove opening square brackets and commas
                        ArrayList<Integer> list = new ArrayList<>();
                                
                        for ( String e : entry[1].replaceAll( "[\\[\\]\\s]+", "" ).split( "," ) )
                            list.add( Integer.parseInt( e ) );
                        
                        //list.forEach( e -> logInfo( "{}",e ) );

                        f.set( this.configurableClassInstance, list );

                        //String typeParamName = ((Class<?>) ((ParameterizedType) fieldTypeClass.getGenericSuperclass()).getActualTypeArguments()[0]).getName();
                        //logInfo( "FOund ArrayList of type {}", typeParamName );
                        /*switch( typeParamName )
                        {
                            case "java.lang.Integer" ->
                            {
                                // Remove opening square brackets and commas
                                ArrayList<Integer> list = new ArrayList<>();
                                
                                for ( String e : entry[1].replaceAll( "[],", "" ).split( " " ) )
                                    list.add( Integer.parseInt( e ) );
                                
                                logInfo( list.toString() );

                                f.set( this.configurableClassInstance, list );
                            }

                            case "java.lang.String" ->
                            {
                                f.set(
                                    this.configurableClassInstance,
                                    new ArrayList<>( Arrays.asList( entry[1].replaceAll( "[],", "" ).split( " " ) ) )
                                );
                            }

                            default ->
                            {
                                logError( "Unsupported array type {} for config entry {}", typeParamName, line );
                            }
                        }*/
                    }
                    else
                    {
                        logError( "Unrecognised data type for config entry {}", line );
                    }
                }
                catch ( NoSuchFieldException nsfe )
                {
                    logError( "No matching field found for config entry: {}", entry[0] );
                    this.errorFlag = true;
                }
                catch ( IllegalAccessException illegal )
                {
                    logError( "Could not set field involved in: {}", line );
                    this.errorFlag = true;
                    illegal.printStackTrace();
                }
                catch ( /*ArrayIndexOutOfBoundsException | NumberFormatException*/ Exception e )
                {
                    logError( "Malformed config entry: {}", line );
                    this.errorFlag = true;
                }

                //System.out.printf( "Set config %s to %s%n", entry[0], entry[1] );
            }
        }
        catch ( IOException ioe )
        {
            logError( "IOException while reading config file: {}", ioe.getMessage() );
            throw ioe;
        }

        logInfo( "Finished reading config file!" );
    }

    /**
     * Print all configuration variables and values.
     */
    public void printAllConfigs()
    {
        logInfo( "Dumping configs:" );
        for ( var f : this.configurableClass.getDeclaredFields() )
        {
            try { logInfo( "\t{}: {}", f.getName(), f.get( this.configurableClassInstance ) ); }
            catch ( IllegalAccessException | NullPointerException ignored ) {}
        }
    }
}