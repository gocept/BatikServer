/*
 BatikServer - ImageConversionServer for SVG-Images
 Copyright (C) 2003-2006  Michael Howitz, gocept gmbh & co. kg

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

*/
// $Id$

import java.net.*;
import java.io.*;
import java.text.*;
import java.util.*;

import org.apache.batik.apps.rasterizer.*;


public class batikServer extends Thread 
{
    private Socket theConnection;
    private PrintStream os;
    private BufferedReader is;
    private static SVGConverter theConverter = new SVGConverter();
    

    /**
     * Static map containing all the mime types understood by the 
     * rasterizer
     */
    protected static Map mimeTypeMap = new Hashtable();


    /**
     * Static map containing all the command line options understood by
     * batikServer
     */
    protected static Map cmdLineOptionsMap = new Hashtable();
    

    /**
     * Static properties sheet for all settings
     */
    protected static Properties theProperties;
    protected static Properties theConfigProperties;
    protected static Properties theDefaultProperties = new Properties();


    /**
     * Static initializer: 
     * - adds all the option handlers to the map of option handlers.
     * - sets default properties
     * - sets the konwn command line options
     */
    static 
    {
        mimeTypeMap.put("image/jpg", DestinationType.JPEG);
        mimeTypeMap.put("image/jpeg", DestinationType.JPEG);
        mimeTypeMap.put("image/jpe", DestinationType.JPEG);
        mimeTypeMap.put("image/png", DestinationType.PNG);
        mimeTypeMap.put("application/pdf", DestinationType.PDF);
        mimeTypeMap.put("image/tiff", DestinationType.TIFF);
        theDefaultProperties.setProperty("port", "54822");
        theDefaultProperties.setProperty("configFile", ""); //batikServer.config
        theDefaultProperties.setProperty("logFile", ""); // stdout
        theConfigProperties = new Properties(theDefaultProperties);
        theProperties = new Properties(theConfigProperties);
        cmdLineOptionsMap.put("-p", "port");
        cmdLineOptionsMap.put("-l", "logFile");
        cmdLineOptionsMap.put("-c", "configFile");
    }


    /**
     * constructor
     */
    public batikServer(Socket s)
    {
        theConnection = s;
    }


    public static void main(String[] args)
    {
        ServerSocket ss;

        batikServer.parseCmdLineArgs(args);
        batikServer.loadConfigFile();
        batikServer.openLogFile();
        batikServer.checkPortNumber();
        batikServer.printLicense();
        
        try {
            ss = new ServerSocket(Integer.parseInt(theProperties.getProperty("port")));
            System.out.println("Accepting connections on port " 
                               + ss.getLocalPort() + ".");
            while(true) {
                batikServer bs = new batikServer(ss.accept());
                bs.start();
            }
        } catch(IOException e) {
            System.err.println("Server aborted prematurely: " + e);
            System.exit(-1);
        }
    }


    public void run()
    {
        try {
            long startTime = System.currentTimeMillis();
            os = new PrintStream(theConnection.getOutputStream());
            is = new BufferedReader(new InputStreamReader(theConnection.getInputStream()));
            String source, mimetype, destination;
            long takenMillis;

            try {
                String get = is.readLine();
                System.out.println(get+" from "+theConnection.getInetAddress()+":"+theConnection.getPort());
                System.out.print("  ");
                StringTokenizer st = new StringTokenizer(get);
                if (st.nextToken().equals("HELLO")) {
                    System.out.println("  BatikServer answers: Nice to meet you.");
                    sendErrorMessage();
                    return;
                }
                source = URLDecoder.decode(st.nextToken(), "UTF-8");
                st.nextToken();
                destination= URLDecoder.decode(st.nextToken(), "UTF-8");
                st.nextToken();
                mimetype = st.nextToken();
                st.nextToken();
                String version  = st.nextToken();
                
                // loop through the rest of the input lines 
                while ((get = is.readLine()) != null)
                    {
                        if (get.trim().equals(""))
                            break;
                    }

                if (!version.equals("1.0")) {
                    System.err.println("Error: unsupported version ("+version+") in request!");
                    sendErrorMessage();
                    return;
                }
            }
            catch (NoSuchElementException nsee) {
                System.err.println("Error: Not enough tokens in request!");
                sendErrorMessage();
                return;
            }
            
            try {
                DestinationType dstType = (DestinationType)mimeTypeMap.get(mimetype);
                if (dstType == null) {
                    throw new IllegalArgumentException();
                }

                String[] s = {source};
                
                theConverter.setDestinationType(dstType);
                theConverter.setSources(s);
                theConverter.setDst(new File(destination));
                theConverter.execute();
                os.print(1); // success
            }
            catch (SVGConverterException ce) {
                System.err.println("Error: ConverterException" + ce.getMessage());
                sendErrorMessage();
                return;
            }
            catch (IllegalArgumentException iae) {
                System.err.println("Error: Unsupported MimeType '"+mimetype+"'!");
                sendErrorMessage();
                return;
            }
            
            close();
            System.out.println("  Success in "+(System.currentTimeMillis()-startTime)+" ms.");
        }
        catch (IOException e) {
            System.err.println("I/O exception occurred!: " + e);
        }
    }
    

    private void sendErrorMessage()
    {
        os.print(0);
        close();
    }


    private void close()
    {
        try {
            os.flush();
            os.close();
            theConnection.close();
        }
        catch (IOException e) {
            System.err.println("I/O exception occurred on close!: " + e);
        }
    }


    private static void printLicense()
    {
        System.out.println("BatikServer version 0.3.1 Copyright (C) 2003-2006  Michael Howitz, gocept gmbh & co. kg");
        System.out.println("BatikServer comes with ABSOLUTELY NO WARRANTY.");
        System.out.println("This is free software, and you are welcome to redistribute it under certain conditions.");
        System.out.println("See LICENSE.txt.");
        System.out.println("");
        System.out.println("Default port number is 54822.");
    }


    private static void printOptionHelp()
    {
        batikServer.printLicense();
        System.err.println("");
        System.err.println("Usage: batikServer [options]");
        System.err.println("");
        System.err.println("possible options:");
        System.err.println(" -p <port> ... set port to listen on besides default");
        System.err.println(" -l <logfile> ... send output to logfile instead of stdout");
        System.err.println(" -c <configfile> ... configfile for setting options default: batikServer.config in $PWD or $HOME.");
        System.exit(-1);
    }


    private static void loadConfigFile()
    {
        FileInputStream configFile = null;
        String configfilename = theProperties.getProperty("configFile");
        String path;
        String file_separator;
        if (configfilename.length() > 0)
            { // set value on cmd line
                try {
                    configFile = new FileInputStream(configfilename);
                }
                catch (FileNotFoundException fnfe) {
                    System.err.println("Error: Can't read configFile '"+configfilename+"'. ("+fnfe+")");
                    System.exit(-1);
                }
            }
        else
            {
                configfilename = "batikServer.config";
                file_separator = System.getProperty("file.separator");
                try {
                    path = System.getProperty("user.dir");
                    configFile = new FileInputStream(path+file_separator+configfilename);
                }
                catch (FileNotFoundException fnfe) {
                    try {
                        path = System.getProperty("user.home");
                        configFile = new FileInputStream(path+file_separator+configfilename);
                    }
                    catch (FileNotFoundException fnfe2) {
                        System.err.println("no config file found");
                    }
                }
            }
        if (configFile != null)
            {
                try {
                    theConfigProperties.load(configFile);
                }
                catch (IOException ioe)
                    {
                        System.err.println("Error on reading configFile ("+configfilename+"): "+ioe);
                        System.exit(-1);
                    }
            }
    }


    private static void checkPortNumber()
    {
        String thePort = theProperties.getProperty("port");
        String defaultPort = theDefaultProperties.getProperty("port");
        int thePortInt;
        try {
            thePortInt = Integer.parseInt(thePort);
            if (thePortInt < 0 || thePortInt > 65535)
                {
                    System.err.println("Port number '"+thePort+"' is invalid, it must be an integer between 0 and 65535");
                    System.exit(-1);
                }
        }
        catch (Exception e) {
            System.err.println("Port number '"+thePort+"' is invalid, it must be an integer between 0 and 65535");
            System.exit(-1);
        }
    }

    private static void openLogFile()
    {
        FileOutputStream logfile;
        PrintStream logstream;
        DateFormat df;
        String logfilename = theProperties.getProperty("logFile");

        if (logfilename.length() > 0)
            { // otherwise System.err and System.out are used
                try {
                    logfile = new FileOutputStream(logfilename, true); // mit append
                    logstream = new PrintStream(logfile, true); // mit autoflush
                    System.setOut(logstream);
                    System.setErr(logstream);
                    df = DateFormat.getDateTimeInstance();
                    System.out.println("");
                    System.out.println("---------------------------------------------------------------------------------------");
                    System.out.println(df.format(new Date()));
                    System.out.println("---------------------------------------------------------------------------------------");
                }
                catch (FileNotFoundException fnfe) {
                    batikServer.printLicense();
                    System.err.println("Can't open specified log-file: " + fnfe);
                    System.exit(-1);
                }
            }
    }


    private static void parseCmdLineArgs(String[] args)
    {
        float options = (float)args.length / 2;
        if (options != (int)options)
            { // uneven number of cmd-line parameters
                batikServer.printOptionHelp(); // does System.exit(-1)
            }
        for (int i=0;i<args.length;i+=2)
            {
                if (! cmdLineOptionsMap.containsKey(args[i]))
                    {
                        batikServer.printOptionHelp(); // does System.exit(-1)
                    }
                theProperties.setProperty((String)cmdLineOptionsMap.get(args[i]),
                                          args[i+1]);
            }
    }

}
