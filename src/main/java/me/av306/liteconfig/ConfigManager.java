package me.av306.liteconfig;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.av306.liteconfig.annotations.ConfigComment;
import me.av306.liteconfig.annotations.ConfigComments;
import me.av306.liteconfig.annotations.IgnoreConfig;

/**
 * The main class of LiteConfig!
 */
public class ConfigManager
{
    /** {java.nio.file.Path} to the configuration file */
    private final Path configFilePath;

    /** The Class object holding the config fields */
    private final Class<?> configurableClass;

    /** The instance of the class holding the config fields, if any */
    private @Nullable Object configurableClassInstance;

    /** The internal logger */
    private final Logger LOGGER;
    
    /**
     * True if there were errors when reading the config file.
     */
    public boolean errorFlag = false;
    

    
    /**
     * Creates a new configuration manager. 
     * This constructor calls createConfigFileIfNeeded() and readConfigFile().
     *
     * @param configFilePath Path to the config file
     * @param configurableClass {java.lang.Class} object that holds the configurable fields (use NameOfClass.class or classInstance.getClass())
     * @param configurableClassInstance Instance of the previous configurable object, if instance fields are used. Pass NULL here if static fields are used
     */
    public ConfigManager(
        Path configFilePath,
        Class<?> configurableClass,
        @Nullable Object configurableClassInstance
    ) throws IOException
    {
        this( "LiteConfig(" + configFilePath.getFileName().toString() + ")",
                configFilePath, configurableClass, configurableClassInstance );
    }

    /**
     * Create a ConfigManager with the specified logger name;
     */
    public ConfigManager(
        String name,
        Path configFilePath,
        Class<?> configurableClass,
        @Nullable Object configurableClassInstance
    ) throws IOException
    {
        this.configFilePath = configFilePath;
        this.configurableClass = configurableClass;
        this.configurableClassInstance = configurableClassInstance;

        this.LOGGER = LoggerFactory.getLogger( name );

        if ( !this.createConfigFileIfNeeded() )
            this.readConfigFile();
    }

    /**
     * Check for the existence of a config file, creating a new one if needed
     * @return true if a new config file was created, false otherwise
     */
    public final boolean createConfigFileIfNeeded() throws IOException
    {
        if ( !this.configFilePath.toFile().exists() )
        {
            this.serialiseConfigsToFile();            

            this.LOGGER.info( "Created new config file at {}", this.configFilePath.toString() );
            return true;
        }
        else
        {
            this.LOGGER.info( "Config file already exists!" );
            return false;
        }
    }

    /**
     * Read configs from the config file. Sets hasCustomData if invalid config statements were read.
     * <br>
     * NOTE: entries in the config file MUST match field names EXACTLY (case-insensitive)
     */
    public final void readConfigFile() throws IOException
    {
        // Reset error flag
        this.errorFlag = false;

        try ( BufferedReader reader = Files.newBufferedReader( this.configFilePath ) )
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
                    String name = entry[0].trim();
                    String value = entry[1].trim();

                    // Set fields in configurable class
                    Field f = this.configurableClass.getDeclaredField( entry[0] );
                    Class<?> fieldTypeClass = f.getType();
                    
                    if ( fieldTypeClass.isAssignableFrom( short.class ) )
                    {
                        // Short value (0x??)
                        if ( value.startsWith( "0x" ) )
                        {
                            f.setShort( this.configurableClassInstance,
                                    Short.parseShort( value.replace( "0x", "" ), 16 ) );
                        }
                        else
                        {
                            f.setShort( this.configurableClassInstance, Short.parseShort( value ) );
                        }
                    }
                    else if ( fieldTypeClass.isAssignableFrom( int.class ) )
                    {
                        // Integer value
                        if ( entry[1].startsWith( "0x" ) )
                        {
                            // Hex literal
                            f.setInt(
                                    this.configurableClassInstance,
                                    Integer.parseInt( value.replace( "0x", "" ), 16 )
                            );
                        }
                        else f.setInt(
                            this.configurableClassInstance,
                            Integer.parseInt( value )
                        );
                    }
                    else if ( fieldTypeClass.isAssignableFrom( float.class ) )
                    {
                        f.setFloat(
                            this.configurableClassInstance,
                            Float.parseFloat( value )
                        );
                    }
                    else if ( fieldTypeClass.isAssignableFrom( double.class ) )
                    {
                        f.setDouble(
                            this.configurableClassInstance,
                            Double.parseDouble( value )
                        );
                    }
                    else if ( fieldTypeClass.isAssignableFrom( boolean.class ) )
                    {
                        f.setBoolean(
                            this.configurableClassInstance,
                            Boolean.parseBoolean( value )
                        );
                    }
                    else if ( fieldTypeClass.isAssignableFrom( ArrayList.class ) )
                    {
                        Type[] actualTypeArguments = ((ParameterizedType) f.getGenericType()).getActualTypeArguments();

                        if ( actualTypeArguments.length != 1 ) throw new UnexpectedException( "Invalid number of ArrayList type arguments: expected 1, received " + actualTypeArguments.length );

                        Class<?> arrayTypeParameter = (Class<?>) actualTypeArguments[0];

                        // Unfortunately, we can't fully dynamically set the ArrayList's type parameter
                        // (https://stackoverflow.com/questions/14670839/how-to-set-the-generic-type-of-an-arraylist-at-runtime-in-java)

                        // Remove all square brackets and spaces, then split by commas
                        String[] arrayValues = entry[1].replaceAll( "[\\[\\]\\s]+", "" ).split( "," );

                        if ( arrayTypeParameter.isAssignableFrom( Integer.class ) )
                        {
                            ArrayList<Integer> list = new ArrayList<Integer>();

                            for ( String e : arrayValues ) list.add( Integer.parseInt( e ) );
                            f.set( this.configurableClassInstance, list );
                        }
                        if ( arrayTypeParameter.isAssignableFrom( String.class ) )
                        {
                            ArrayList<String> list = new ArrayList<>();
                            
                            for ( String e : arrayValues ) list.add( e );
                            
                            f.set( this.configurableClassInstance, list );   
                        }
                        if ( arrayTypeParameter.isAssignableFrom( Float.class ) )
                        {
                            ArrayList<Float> list = new ArrayList<>();
                            
                            for ( String e : arrayValues ) list.add( Float.parseFloat( e ) );

                            f.set( this.configurableClassInstance, list );
                        }
                        // TODO: more types...
                        else
                        {
                            this.LOGGER.error( "Unsupported ArrayList type {} for config field {}",
                            arrayTypeParameter.getName(), name );
                            this.errorFlag = true;
                        }
                    }
                    else
                    {
                        this.LOGGER.error( "Unrecognised data type {} for config entry: {}",
                                fieldTypeClass.getName(), line );
                    }
                }
                catch ( NoSuchFieldException nsfe )
                {
                    this.LOGGER.error( "No matching field found for config entry: {}", entry[0] );
                    this.errorFlag = true;
                }
                catch ( IllegalAccessException illegal )
                {
                    this.LOGGER.error( "Could not set field involved in: {}", line );
                    this.LOGGER.error( "NOTE: This is not a problem with LiteConfig! The user of this library likely has made the field private and/or final." );
                    this.errorFlag = true;
                }
                catch ( /*ArrayIndexOutOfBoundsException | NumberFormatException*/ Exception e )
                {
                    this.LOGGER.error( "Malformed config entry: {}", line );
                    this.errorFlag = true;
                }
            }
        }
        catch ( IOException ioe )
        {
            this.LOGGER.error( "IOException while reading config file: {}", ioe.getMessage() );
            throw ioe;
        }

        this.LOGGER.info( "Finished reading config file!" );
    }

    public void printAllConfigs()
    {
        this.LOGGER.info( "All configs:" );
        for ( var f : this.configurableClass.getDeclaredFields() )
        {
            try { this.LOGGER.info( "\t{}: {}", f.getName(), f.get( this.configurableClassInstance ) ); }
            catch ( IllegalAccessException | NullPointerException ignored ) {}
        }
    }

    /**
     * Serialise a given field.
     * @param field The target field
     * @return A string representation of the field's value
     * @throws IllegalAccessException If the field cannot be accessed, e.g. it is private
     */
    private String formatFieldValue( Field field ) throws IllegalAccessException
    {
        Class<?> fieldTypeClass = field.getType();
        if ( fieldTypeClass.isAssignableFrom( short.class ) )
        {
            return String.format( "0x%02X", field.getShort( this.configurableClassInstance ) );
        }
        else if ( fieldTypeClass.isAssignableFrom( int.class ) )
        {
            return String.format( "%d", field.getInt( this.configurableClassInstance ) );
        }
        else if ( fieldTypeClass.isAssignableFrom( float.class ) )
        {
            return String.format( "%f", field.getFloat( this.configurableClassInstance ) );
        }
        else if ( fieldTypeClass.isAssignableFrom( double.class ) )
        {
            return String.format( "%f", field.getDouble( this.configurableClassInstance ) );
        }
        else if ( fieldTypeClass.isAssignableFrom( boolean.class ) )
        {
            return String.format( "%b", field.getBoolean( this.configurableClassInstance ) );
        }
        else return field.get( this.configurableClassInstance ).toString();
    }

    private void serialiseConfigsToFile() throws IOException
    {   
        try ( BufferedWriter writer = Files.newBufferedWriter( this.configFilePath, StandardOpenOption.CREATE_NEW ) )
        {
            // Write the top-level comments, if any

            // Length may be 0 if there are no annotations
            ConfigComment[] topLevelComments = this.configurableClass.getAnnotationsByType( ConfigComment.class );
            if ( topLevelComments.length > 0 )
            {
                for ( ConfigComment c : topLevelComments )
                {
                    writer.write( "# " + c.value() + System.lineSeparator() );
                }

                writer.write( System.lineSeparator() );
            }

            // For each line in the config file, retrieve the field and
            // write its default value and comment (if any)
            for ( var field : this.configurableClass.getDeclaredFields() )
            {
                // Ignore fields with the IgnoreConfig annotation
                if ( field.isAnnotationPresent( IgnoreConfig.class ) ) continue;

                // Length may be 0
                ConfigComment[] comments = field.getAnnotationsByType( ConfigComment.class );
                    for ( ConfigComment c : comments )
                        writer.write( "# " + c.value() + System.lineSeparator() );

                // Write the default value
                Class<?> fieldTypeClass = field.getType();
                
                try
                {
                    // We don't need to care about the specific type for serialisation
                    if ( fieldTypeClass.isAssignableFrom( ArrayList.class ) )
                    {
                        ArrayList<? extends Object> array = (ArrayList<? extends Object>) field.get( this.configurableClassInstance );
                        StringBuilder arrayString = new StringBuilder( "[" );
                        for ( int i = 0; i < array.size(); i++ )
                        {
                            arrayString.append( array.get( i ) );
                            if ( i < array.size() - 1 ) arrayString.append( ", " );
                        }
                        arrayString.append( "]" );
                        
                        writer.write( String.format(
                            "%s=%s%s",
                            field.getName().toUpperCase(), 
                            arrayString.toString(),
                            System.lineSeparator()
                        ) );
                    }
                    else
                    {
                        // Non-array type
                        writer.write( String.format(
                            "%s=%s%s",
                            field.getName().toUpperCase(), 
                            this.formatFieldValue( field ),
                            System.lineSeparator()
                        ) );
                    }
                }
                catch ( IllegalAccessException illegal )
                {
                    this.LOGGER.error( "Could not access field {} while creating config file", field.getName() );
                    //illegal.printStackTrace();
                }
            }
        }
        catch ( IOException ioe )
        {
            this.LOGGER.error( "IOException while creating config file: {}", ioe.getMessage() );
            throw ioe;
        }
    }
}