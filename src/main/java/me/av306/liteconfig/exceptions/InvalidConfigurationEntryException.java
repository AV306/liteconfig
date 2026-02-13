package me.av306.liteconfig.exceptions;

/**
 * Represents a user-created configuration entry missing either its property name or value.
 * 
 * This should only be used to wrap {java.lang.ArrayIndexOutOfBoundsException}s arising from
 * splitting the configuration entry.
 * This should NOT be used to indicate deserialisation errors
 * arising from programmer error, e.g. NoSuchFieldExceptions.
 */
public class InvalidConfigurationEntryException extends IllegalArgumentException
{
    public InvalidConfigurationEntryException( String message )
    {
        super( message );
    }

    public InvalidConfigurationEntryException( Throwable cause )
    {
        super( cause );
    }

    public InvalidConfigurationEntryException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
