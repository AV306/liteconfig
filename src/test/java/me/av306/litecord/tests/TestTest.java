package me.av306.litecord.tests;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;

import me.av306.liteconfig.ConfigManager;

public class TestTest {
    public static boolean settingA = false;

    @Test
    void testTest() throws IOException
    {
        //System.out.println( System.getProperty( "user.home" ) );
        ConfigManager cm = new ConfigManager(
            "LiteConfig_Test", Path.of( System.getProperty( "user.dir" ) ), "config.properties",
            this.getClass(), null
        );

        /*File test = new File( "/file.txt" );
        try
        {
            test.createNewFile();
        }
        catch ( IOException ioe )
        {
            ioe.printStackTrace();
        }*/

        // Check that the external config file was successfully created
        File externalConfigFile = Path.of( System.getProperty( "user.dir" ), "config.properties" ).toFile();
        Assertions.assertTrue( externalConfigFile.exists() );

        // Check that the content of the external config file is identical to the embedded one
        /*boolean identical = true;
        try (
            FileInputStream externalConfigFileInput = new FileInputStream( externalConfigFile );
            InputStream embeddedConfigFileInput = this.getClass().getResourceAsStream( "/config.properties" );
        )
        {
            byte[] external = externalConfigFileInput.readAllBytes();
            byte[] embedded = embeddedConfigFileInput.readAllBytes();

            // Check length, first
            Assertions.assertEquals( external.length, embedded.length );

            // Check each byte
            for ( var i = 0; i < external.length; i++ )
                Assertions.assertEquals( embedded[i], external[i] );
        }
        catch ( IOException ioe )
        {
            System.err.println( "IOException while testing" );
            ioe.printStackTrace();
        }*/

        // Check that the config was set
        //Assertions.assertTrue( settingA );
    }

}
