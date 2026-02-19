package me.av306.liteconfig;

import me.av306.liteconfig.annotations.ConfigComment;
import me.av306.liteconfig.annotations.IgnoreConfig;
import me.av306.liteconfig.exceptions.InvalidConfigurationEntryException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * Provides serialisation and deserialisation to/from a single file, for the
 * associated class/object.
 * <br>
 * Does not support concurrent access to the same configuration file.
 */
public class ConfigManager
{
    /** Holds the {java.nio.file.Path} to the configuration file */
    private final Path configFilePath;

    /** Holds the Class object with the config fields */
    private final Class<?> configurableClass;

    /** Holds the instance of the class holding the config fields, if any. */
    private @Nullable Object configurableClassInstance;

    /** The internal logger */
    private final Logger LOGGER;

    
    /**
     * Creates a new configuration manager for the given class and the given file path.
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
     * Create a new configuration manager for the given class and file path, with the specified logger name.
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
     * Create a new configuration file if one does not already exist, and then deserialise its contents.
     * @throws InvalidConfigurationEntryException See {deserialiseConfigurationFile()}
     * @throws NumberFormatException See {deserialiseConfigurationFIle()}
     * @throws IOException If one occurred while reading or writing the configuration file
     * @return True if a new configuration file was created, false otherwise
     */
    public boolean deserialiseConfigurationFileOrElseCreateNew()
            throws InvalidConfigurationEntryException, NumberFormatException, IOException
    {
        if ( !this.configFilePath.toFile().exists() )
        {
            this.LOGGER.info( "Configuration file does not exist, will create a new one at {}", this.configFilePath.toString() );
            this.serialiseConfigurationsCompletely();

            this.LOGGER.info( "Created new configuration file at {}", this.configFilePath.toString() );
            return true;
        }
        else
        {
            this.LOGGER.info( "Config file already exists; will read configs from {}",
                    this.configFilePath.toString() );
            this.deserialiseConfigurationFile();
            return false;
        }
    }

    /**
     * Read configs from the config file, updating the configuration class. Will stop and throw an exception if malformed input is encountered,
     * so the user can be notified. NOTE: Changes made to the configuration class before an exception is thrown will not be rolled back.
     * Partial application of configurations may result.
     * <br>
     * This does not modify the configuration file.
     * <br>
     * NOTE: entries in the config file MUST match field names EXACTLY (case-SENSITIVE)
     * @throws InvalidConfigurationEntryException If an invalid configuration entry is encountered
     * @throws NumberFormatException If a configuration entry contains an invalid number
     * @throws IOException If one occurred while reading the configuration file
     * @throws RuntimeException (unchecked, wrapped) Reflection exceptions e.g. {java.lang.NoSuchFIeldException}
     * or {java.lang.IllegalAccessException} are thrown by the underlying {deserialiseConfigurationLine()‘ method
     * for programmer error arising from improper declarations in the configuration class.
     * They are wrapped in RuntimeException to make them unchecked.
     * @throws UnsupportedOperationException (unchecked) If an unsupported data type is encountered during deserialisation
     * @deprecated Because you probably don't want to use this. See {deserialiseConfigurationFileCompletely()} instead --
     * it deserialises the whole file and doesn't just stop when it hits an error
     */
    // TODO: any way to make it collect all exceptions first then throw them all, like a compiler?
    // FIXME: partial application of configs
    @Deprecated( forRemoval = true )
    public void deserialiseConfigurationFile()
            throws InvalidConfigurationEntryException, NumberFormatException, IOException
    {
        // TODO: next step: bounds checking with annotations?

        try ( BufferedReader reader = Files.newBufferedReader( this.configFilePath ) )
        {
            // Iterate over each line in the file
            // TODO: benchmark .parallel()
            reader.lines().forEach( line ->
            {
                line = line.trim();

                // Skip comments and blank lines
                if ( line.startsWith( "#" ) || line.isBlank() ) return;

                try
                {
                    deserialiseConfigurationLine( line );
                }
                // FIXME: should error messages be printed inside the deserialisation method?
                catch ( InvalidConfigurationEntryException icee )
                {
                    // This exception is reasonably handled by callers,
                    // e.g. giving the user an error message. Therefore it is checked.
                    this.LOGGER.error( "Invalid configuration entry: {}", line );
                    throw icee;
                }
                catch ( NumberFormatException nfe )
                {
                    this.LOGGER.error( "Invalid number in config entry: {}", line );
                    throw nfe;
                }
                catch ( NoSuchFieldException nsfe )
                {
                    // The (checked) NoSuchFieldException cannot be reasonably handled by callers,
                    // since it arises from a programming error in the confoguratioun class.
                    // Therefore we wrap it in IllegalStateException so that it becomes unchecked.
                    this.LOGGER.error( "No matching field found for config entry: {}", line );
                    throw new RuntimeException( nsfe );
                }
                catch ( IllegalAccessException iae )
                {
                    this.LOGGER.error( "Failed to set field for: {}", line );
                    throw new RuntimeException( iae );
                }
                catch ( UnsupportedOperationException uoe )
                {
                    // The message is constructed in the underlying method, which has the field and type name
                    this.LOGGER.error( uoe.getLocalizedMessage() );
                    throw uoe;
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

    /**
     * Read configs from the config file, updating the configuration class.
     * Will NOT throw exceptions if malformed input is encountered.
     * All configuration entries will be processed; only valid entries will have
     * their corresponding fields updated.
     * <br>
     * This does not modify the configuration file.
     * <br>
     * NOTE: entries in the config file MUST match field names EXACTLY (case-SENSITIVE)
     * @throws IOException If one occurred while reading the configuration file
     * @throws RuntimeException (unchecked, wrapped) Reflection exceptions e.g. {java.lang.NoSuchFIeldException}
     * or {java.lang.IllegalAccessException} are thrown by the underlying {deserialiseConfigurationLine()‘ method
     * for programmer error arising from improper declarations in the configuration class.
     * They are wrapped in RuntimeException to make them unchecked.
     * @throws UnsupportedOperationException (unchecked) If an unsupported data type is encountered during deserialisation
     */
    public void deserialiseConfigurationFileCompletely() throws IOException
    {
        // TODO: next step: bounds checking with annotations?

        try ( BufferedReader reader = Files.newBufferedReader( this.configFilePath ) )
        {
            // Iterate over each line in the file
            // TODO: benchmark .parallel()
            reader.lines().forEach( line ->
            {
                line = line.trim();

                // Skip comments and blank lines
                if ( line.startsWith( "#" ) || line.isBlank() ) return;

                try
                {
                    deserialiseConfigurationLine( line );
                }
                // FIXME: should error messages be printed inside the deserialisation method?
                catch ( InvalidConfigurationEntryException icee )
                {
                    // This exception is reasonably handled by callers,
                    // e.g. giving the user an error message. Therefore it is checked.
                    this.LOGGER.error( "Invalid configuration entry: {}", line );
                }
                catch ( NumberFormatException nfe )
                {
                    this.LOGGER.error( "Invalid number in config entry: {}", line );
                }
                catch ( NoSuchFieldException nsfe )
                {
                    // The (checked) NoSuchFieldException cannot be reasonably handled by callers,
                    // since it arises from a programming error in the confoguratioun class.
                    // Therefore we wrap it in IllegalStateException so that it becomes unchecked.
                    this.LOGGER.error( "No matching field found for config entry: {}", line );
                    throw new RuntimeException( nsfe );
                }
                catch ( IllegalAccessException iae )
                {
                    this.LOGGER.error( "Failed to set field for: {}", line );
                    throw new RuntimeException( iae );
                }
                catch ( UnsupportedOperationException uoe )
                {
                    // The message is constructed in the underlying method, which has the field and type name
                    this.LOGGER.error( uoe.getLocalizedMessage() );
                    throw uoe;
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

    /**
     * Deserialise the given "prop=val" configuration entry and set
     * the associated field in the configuration class.
     * @param line The configuration entry in "prop=val" format
     * @throws InvalidConfigurationEntryException If the configuration entry is invalid, i.e. missing one or more of `prop` or `val`.
     * @throws NumberFormatException If the field matching `prop` is a number type and `val` is not a valid number
     * @throws NoSuchFieldException If the field matching `prop` cannot be found in the configuration class (this should be handled in the caller)
     * @throws IllegalAccessException If the field matching `prop` in the configuration class cannot be accessed
     * @throws UnsupportedOperationException (Unchecked) If the field matching `prop` is of an unsupported type
     */
    protected void deserialiseConfigurationLine( @NotNull String line )
            throws InvalidConfigurationEntryException, NumberFormatException,
            NoSuchFieldException, IllegalAccessException
    {
        String[] entry = line.split( "=" );

        // Trim lines so you can have spaces around the equals ("prop = val" as opposed to "prop=val")
        String name, value;
        try
        {
            name = entry[0].trim();
            value = entry[1].trim();
        }
        catch ( ArrayIndexOutOfBoundsException oobe )
        {
            // Rethrow as InvalidCnfigurationEntryException
            throw new InvalidConfigurationEntryException( "Invalid configuration entry \""
                    + line + "\": ", oobe );
        }

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
            if ( value.startsWith( "0x" ) )
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
            else if ( arrayTypeParameter.isAssignableFrom( String.class ) )
            {

                ArrayList<String> list = new ArrayList<>( Arrays.asList( arrayValues ) );

                field.set( this.configurableClassInstance, list );
            }
            else if ( arrayTypeParameter.isAssignableFrom( Float.class ) )
            {
                ArrayList<Float> list = new ArrayList<>();

                for ( String e : arrayValues ) list.add( Float.parseFloat( e ) );

                field.set( this.configurableClassInstance, list );
            }
            // TODO: more types...
            else
            {
                throw new UnsupportedOperationException( "Unsupported data type " + arrayTypeParameter.getName()
                    + " for ArrayList field " + field.getName() );
            }
        }
        else
        {
            throw new UnsupportedOperationException( "Unsupported data type " + fieldTypeClass.getName()
                    + " for field " + field.getName() );
        }
    }

    /**
     * Serialise all values (as possible) from the configuration class into the config file.
     * Does NOT stop when an error is encountered.
     * @throws RuntimeException (unchecked, wrapped {IllegalAccessException}) If a configuration field cannot be accessed
     * @throws NullPointerException (unchecked) If an instance field is encountered but the configuration object instance is {null}
     * @throws IOException If one occurred while writing to the configuration file
     */
    public void serialiseConfigurationsCompletely() throws IOException
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
                    throw new RuntimeException( illegal );
                }
                catch ( NullPointerException npe )
                {
                    // This is thrown if the field is an instance field but we haven't been
                    // given an instance.
                    this.LOGGER.warn( "Failed to access instance field {} as ano instance was provided.", field.getName() );
                    throw npe;
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
     * Concatenate the values of the provided comment annotations into a single string,
     * with the contents of each annotation on a separate line and the appropriate comment prefix ("#") beginning each line.
     * Blank comments become newlines.
     * @param comments The comment annotations
     * @return A String containing the contents of each annotation, concatenated as described previously
     */
    protected static String processComments( ConfigComment... comments )
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
     * Serialise a given {java.lang.reflect.Field} into a string representation.
     * @param field The field to serialise
     * @param instance The object instance containing the field
     * @return A string representation of the field's value
     * @throws IllegalAccessException If the field cannot be accessed
     * @throws NullPointerException} If the field is an instance field and the provided instance was {null}.
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
            ArrayList<?> array;
            array = (ArrayList<?>) field.get( instance );

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
     * Serialise a given object to a string representation.
     * @param object The object to be serialised
     * @param fieldTypeClass The type of the object
     * @return A string representation of the object
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
     * Print the values of all the configuration fields accessible to this ConfigManager to its logger.
     */
    public void printAllConfigs()
    {
        this.LOGGER.info( "All configs accessible to {}:", this.LOGGER.getName() );
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