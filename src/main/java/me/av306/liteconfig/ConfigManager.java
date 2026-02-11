package me.av306.liteconfig;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.av306.liteconfig.annotations.ConfigComment;
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
     * True if there were errors when deserialising the config file.
     */
    private boolean deserialisationErrorFlag = false;

    /**
     * TODO
     * @return
     */
    public boolean hasDeserialisationErrors() { return this.deserialisationErrorFlag; }
    

    
    /**
     * Creates a new configuration manager.
     *
     * @param configFilePath Path to the config file
     * @param configurableClass {java.lang.Class} object that holds the configurable fields (use NameOfClass.class or classInstance.getClass())
     * @param configurableClassInstance Instance of the previous configurable object, if instance fields are used. Pass NULL here if static fields are used
     */
    public ConfigManager(
            Path configFilePath,
            Class<?> configurableClass,
            @Nullable Object configurableClassInstance
    )
    {
        this( "LiteConfig(" + configFilePath.getFileName().toString() + ")",
                configFilePath, configurableClass, configurableClassInstance );
    }

    /**
     * Create a new configuration manager.
     * TODO: javadocs
     * @param name
     * @param configFilePath
     * @param configurableClass
     * @param configurableClassInstance
     */
    public ConfigManager(
        String name,
        Path configFilePath,
        Class<?> configurableClass,
        @Nullable Object configurableClassInstance
    )
    {
        this.configFilePath = configFilePath;
        this.configurableClass = configurableClass;
        this.configurableClassInstance = configurableClassInstance;

        this.LOGGER = LoggerFactory.getLogger( name );
    }

    /**
     * Check for the existence of a config file, creating a new one if needed.
     * @return true if a new config file was created, false otherwise
     * @throws IOException (TODO)
     */
    public boolean deserialiseConfigFileOrElseCreate() throws IOException
    {
        if ( !this.configFilePath.toFile().exists() )
        {
            this.LOGGER.info( "Configuration file does not exist, will create a new one at {}", this.configFilePath.toString() );
            this.serialiseConfigurations();

            this.LOGGER.info( "Created new configuration file at {}", this.configFilePath.toString() );
            return true;
        }
        else
        {
            this.LOGGER.info( "Config file already exists; will read configs from {}",
                    this.configFilePath.toString() );
            this.deserialiseConfigFile();
            return false;
        }
    }

    /**
     * Read configs from the config file. Sets hasCustomData if invalid config statements were read.
     * <br>
     * NOTE: entries in the config file MUST match field names EXACTLY (case-SENSITIVE)
     * @throws IOException If one occurred during deserialisaation
     * @throws UnsupportedOperationException If an unsupported data type is encountered during deserialisation
     */
    public void deserialiseConfigFile() throws IOException, UnsupportedOperationException
    {
        // TODO: next step: bounds checking with annotations?
        // Reset error flag
        this.deserialisationErrorFlag = false;

        try ( BufferedReader reader = Files.newBufferedReader( this.configFilePath ) )
        {
            // Iterate over each line in the file
            reader.lines().parallel().forEach( line ->
            {
                line = line.trim();
                // Skip comments and blank lines
                if ( line.startsWith( "#" ) || line.isBlank() ) return;

                try
                {
                    deserialiseConfigurationLine( line );
                }
                catch ( NoSuchFieldException nsfe )
                {
                    // TODO: list of errors?
                    this.LOGGER.error( "No matching field found for config entry: {}", line );
                    this.deserialisationErrorFlag = true;
                }
                catch ( IllegalAccessException illegal )
                {
                    this.LOGGER.error( "Could not set field involved in: {}", line );
                    this.deserialisationErrorFlag = true;
                }
                catch ( ArrayIndexOutOfBoundsException oobe )
                {
                    this.LOGGER.error( "Malformed config entry: {}", line );
                    this.deserialisationErrorFlag = true;
                }
                catch ( NumberFormatException nfe )
                {
                    this.LOGGER.error( "Malformed number in config entry: {}", line );
                    this.deserialisationErrorFlag = true;
                }
            } );
        }
        catch ( IOException ioe )
        {
            this.LOGGER.error( "IOException while reading config file: {}", ioe.getMessage() );
            throw ioe;
        }
        
        this.LOGGER.info( "Finished reading config file!" );
    }

    private void deserialiseConfigurationLine( @NotNull String line )
            throws ArrayIndexOutOfBoundsException, NoSuchFieldException,
                   IllegalAccessException, NumberFormatException, UnsupportedOperationException
    {
        String[] entry = line.split( "=" );

        // Trim lines so you can have spaces around the equals ("prop = val" as opposed to "prop=val")
        String name = entry[0].trim();
        String value = entry[1].trim();

        // Set fields in configurable class
        Field field = this.configurableClass.getDeclaredField( name );
        Class<?> fieldTypeClass = field.getType();

        // Parse the string according to its target field type
        if ( fieldTypeClass.isAssignableFrom( short.class ) )
        {
            if ( value.startsWith( "0x" ) )
            {
                field.setShort( this.configurableClassInstance,
                        Short.parseShort( value.replace( "0x", "" ), 16 ) );
            }
            else
            {
                field.setShort( this.configurableClassInstance, Short.parseShort( value ) );
            }
        }
        else if ( fieldTypeClass.isAssignableFrom( int.class ) )
        {
            if ( entry[1].startsWith( "0x" ) )
            {
                // Hex literal
                field.setInt(
                        this.configurableClassInstance,
                        Integer.parseInt( value.replace( "0x", "" ), 16 )
                );
            }
            else field.setInt(
                this.configurableClassInstance,
                Integer.parseInt( value )
            );
        }
        else if ( fieldTypeClass.isAssignableFrom( float.class ) )
        {
            field.setFloat( this.configurableClassInstance, Float.parseFloat( value ) );
        }
        else if ( fieldTypeClass.isAssignableFrom( double.class ) )
        {
            field.setDouble( this.configurableClassInstance, Double.parseDouble( value ) );
        }
        else if ( fieldTypeClass.isAssignableFrom( boolean.class ) )
        {
            field.setBoolean( this.configurableClassInstance, Boolean.parseBoolean( value ) );
        }
        else if ( fieldTypeClass.isAssignableFrom( ArrayList.class ) )
        {
            Type[] actualTypeArguments = ((ParameterizedType) field.getGenericType()).getActualTypeArguments();

            //if ( actualTypeArguments.length != 1 ) throw new UnexpectedException( "Invalid number of ArrayList type arguments: expected 1, received " + actualTypeArguments.length );

            Class<?> arrayTypeParameter = (Class<?>) actualTypeArguments[0];

            // Unfortunately, we can't fully dynamically set the ArrayList's type parameter
            // (https://stackoverflow.com/questions/14670839/how-to-set-the-generic-type-of-an-arraylist-at-runtime-in-java)

            // Remove all square brackets and spaces, then split by commas
            String[] arrayValues = entry[1].replaceAll( "[\\[\\]\\s]+", "" ).split( "," );

            if ( arrayTypeParameter.isAssignableFrom( Integer.class ) )
            {
                ArrayList<Integer> list = new ArrayList<>();

                for ( String e : arrayValues ) list.add( Integer.parseInt( e ) );
                field.set( this.configurableClassInstance, list );
            }
            if ( arrayTypeParameter.isAssignableFrom( String.class ) )
            {

                ArrayList<String> list = new ArrayList<>( Arrays.asList( arrayValues ) );

                field.set( this.configurableClassInstance, list );
            }
            if ( arrayTypeParameter.isAssignableFrom( Float.class ) )
            {
                ArrayList<Float> list = new ArrayList<>();

                for ( String e : arrayValues ) list.add( Float.parseFloat( e ) );

                field.set( this.configurableClassInstance, list );
            }
            // TODO: more types...
            else
            {
                this.LOGGER.error( "Unsupported ArrayList type {} for config field {}",
                        arrayTypeParameter.getName(), name );
                this.deserialisationErrorFlag = true;
            }
        }
        else
        {
            this.LOGGER.error( "Unsupported data type {} for config entry: {}", fieldTypeClass.getName(), line );
            throw new UnsupportedOperationException( "Unsupported data type " + fieldTypeClass.getName()
                    + " for field " + field.getName() );
        }
    }

    /**
     * Serialise the values from the configuration class into the config file.
     * @throws IOException If one occurred while writing
     */
    public void serialiseConfigurations() throws IOException
    {   
        try ( BufferedWriter writer = Files.newBufferedWriter( this.configFilePath, StandardOpenOption.CREATE ) )
        {
            // Write the top-level comments, if any
            // Length may be 0 if there are no annotations
            ConfigComment[] topLevelComments = this.configurableClass.getAnnotationsByType( ConfigComment.class );
            if ( topLevelComments.length > 0 )
            {
                writer.write( processComments( topLevelComments ) );
                writer.write( System.lineSeparator() );
            }

            // For each line in the config file, retrieve the field and write its default value and comment (if any)
            // Note: this gets us instance fields too
            for ( Field field : this.configurableClass.getDeclaredFields() )
            {
                // Ignore fields with the IgnoreConfig annotation
                if ( field.isAnnotationPresent( IgnoreConfig.class ) ) continue;

                // Ignore instance fields if the configuration class instance is null
                // TODO: Annotation defining whether to ignore static and/or instance fields
                if ( !Modifier.isStatic( field.getModifiers() ) && this.configurableClassInstance == null )
                    continue;

                // Length may be 0
                ConfigComment[] comments = field.getAnnotationsByType( ConfigComment.class );
                if ( comments.length > 0 )
                {
                    writer.write( processComments( comments ) );
                }

                try
                {
                    writer.write( String.format(
                            "%s=%s%s",
                            field.getName(),
                            serialiseField( field, this.configurableClassInstance ),
                            System.lineSeparator()
                    ) );
                }
                catch ( IllegalAccessException illegal )
                {
                    // This is thrown if the field is private
                    this.LOGGER.error( "Could not access field {} while creating config file", field.getName() );
                    //illegal.printStackTrace();
                }
                catch ( NullPointerException npe )
                {
                    // This is thrown if the field is an instance field but we haven't been
                    // given an instance.
                    this.LOGGER.warn( "Failed to access instance field {}: no instance was provided.", field.getName() );
                }
            }
        }
        catch ( IOException ioe )
        {
            this.LOGGER.error( "IOException while creating config file: {}", ioe.getMessage() );
            throw ioe;
        }
    }

    /**
     * TODO
     * @param comments
     * @return
     */
    private static String processComments( ConfigComment... comments )
    {
        StringBuilder commentsString = new StringBuilder();
        for ( var comment : comments )
        {
            if ( !comment.value().isBlank() )
            {
                commentsString.append( "# " ).append( comment.value() );
            }
            commentsString.append( System.lineSeparator() );
        }

        return commentsString.toString();
    }

    /**
     * Serialise a given {java.lang.reflect.Field}.
     * @param field The target field
     * @param instance The instance of the object containing the field
     * @return A string representation of the field's value
     * @throws IllegalAccessException If the field cannot be accessed, e.g. it is private
     * @throws NullPointerException If instance is null and the field is an instance field
     */
    protected static String serialiseField( @NotNull Field field, @Nullable Object instance )
            throws IllegalAccessException, NullPointerException
    {
        // FIXME: I'm not sure what's the importance of the field.getX calls,
        // given that in the next overload we're just passing the values as Objects
        Class<?> fieldTypeClass = field.getType();

        if ( fieldTypeClass.isAssignableFrom( ArrayList.class ) )
        {
            Type[] actualTypeArguments = ((ParameterizedType) field.getGenericType()).getActualTypeArguments();
            //if ( actualTypeArguments.length != 1 ) throw new UnexpectedException( "Invalid number of ArrayList type arguments: expected 1, received " + actualTypeArguments.length );
            Class<?> arrayTypeParameter = (Class<?>) actualTypeArguments[0];

            // Treat everything as Objects
            ArrayList<?> array = (ArrayList<?>) field.get( instance );

            StringBuilder arrayString = new StringBuilder( "[" );
            for ( int i = 0; i < array.size(); i++ )
            {
                arrayString.append( serialiseObject( array.get( i ), arrayTypeParameter ) );
                if ( i < array.size() - 1 ) arrayString.append( ", " );
            }

            arrayString.append( "]" );

            return arrayString.toString();
        }
        else if ( fieldTypeClass.isAssignableFrom( short.class ) )
        {
            return String.format( "0x%02X", field.getShort( instance ) );
        }
        else if ( fieldTypeClass.isAssignableFrom( int.class ) )
        {
            return String.format( "%d", field.getInt( instance ) );
        }
        else if ( fieldTypeClass.isAssignableFrom( float.class ) )
        {
            return String.format( "%f", field.getFloat( instance ) );
        }
        else if ( fieldTypeClass.isAssignableFrom( double.class ) )
        {
            return String.format( "%f", field.getDouble( instance ) );
        }
        else if ( fieldTypeClass.isAssignableFrom( boolean.class ) )
        {
            return String.format( "%b", field.getBoolean( instance ) );
        }
        else return field.get( instance ).toString();
    }

    /**
     * Serialise a given object.
     * @param object The object to be serialised
     * @param fieldTypeClass The type of the object
     * @return A String representation of the object
     */
    protected static String serialiseObject( Object object, Class<?> fieldTypeClass )
    {
        if ( fieldTypeClass.isAssignableFrom( short.class ) )
        {
            return String.format( "0x%02X", object );
        }
        else if ( fieldTypeClass.isAssignableFrom( int.class ) )
        {
            return String.format( "%d", object );
        }
        else if ( fieldTypeClass.isAssignableFrom( float.class ) )
        {
            return String.format( "%f", object );
        }
        else if ( fieldTypeClass.isAssignableFrom( double.class ) )
        {
            return String.format( "%f", object );
        }
        else if ( fieldTypeClass.isAssignableFrom( boolean.class ) )
        {
            return String.format( "%b", object );
        }
        else return object.toString();
    }

    /**
     * Print the values of all the configuration fields accessible to this
     * ConfigManager.
     */
    public void printAllConfigs()
    {
        this.LOGGER.info( "All configs:" );
        for ( var f : this.configurableClass.getDeclaredFields() )
        {
            // TODO: ignore static/instance fields based on annotation (see above)
            if ( !Modifier.isStatic( f.getModifiers() ) && this.configurableClassInstance == null )
                continue;

            try { this.LOGGER.info( "\t{}: {}", f.getName(), f.get( this.configurableClassInstance ) ); }
            catch ( IllegalAccessException | NullPointerException ignored ) {}
        }
    }
}