
import java.io.*;

import java.io.ObjectOutputStream.PutField;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 *@author Ashim Gautam Upadhaya
 *UPDATED: December 1 2018
 *A TCP server that sends a directory
*/  
 

public class TCPServerFile 
 {
   private ServerSocket serverSocket = null;
   private Socket socket = null;
   private DataInputStream inStream = null;
   private DataOutputStream outStream = null;
   private ArrayList<File> toTransfer = new ArrayList<File>(); /*ArrayList of files to be transferred*/
   private String dirName;
   private int port;
   private int count;
/**
 *   
 * @param port - port of server
 * @param dir - source directory to be sent
 * Constructor
 */
public TCPServerFile(int port , String dir) 
  {
    this.port = port;
    this.dirName = dir;
    this.getFile(dirName);
    System.out.println("Contents count: " + count);
    
  }

/**
 * A method to create socket and fetch input and out streams
 */
public void createSocket() 
  {
    try 
     {
       /*create Server and start listening*/
    	serverSocket = new ServerSocket(port);
        /*accept the connection*/
         socket = serverSocket.accept();
         /*fetch the streams*/
         inStream = new DataInputStream(socket.getInputStream());
         outStream = new DataOutputStream(socket.getOutputStream());
         System.out.println("Connected");
         System.out.println("Dir to sync: " + dirName);
      }
     catch (IOException io) 
       {
            io.printStackTrace();
       }
    }

/**
 *A method to send directory structure to the client
 *Calls a helper method to iterate through the source directory
 */ 
public void sendDirStruct()
  {
    File src = new File(dirName);
    String child[] = src.list();
    try
    {
    	outStream.write(count);
    	outStream.flush();
    }
    catch(IOException h)
    {
    	h.printStackTrace();
    }
    
    	
    for(int i = 0 ; i < child.length ; i++)
      {
    	iterate(new File(src , child[i]));
      }
  }

/**A helper iterate method to iterate through directories.
  *Checks if the contents inside a the source directory is file or directory
  *If is a file stores it in ArrayList file to transfer
  *If is a directory sends its relative path to the client    
  * @param dir is the directories and sub directories to iterate through
  */
private void iterate(File dir)
  {
    try 
     {
    	
    	if (dir.isDirectory())
    	 { 
    		outStream.write(1);
    		outStream.flush();
    		String srcRelative = dir.getAbsolutePath().substring((dir.getAbsolutePath().indexOf(dirName) + dirName.length()+1));
    		System.out.println("Relative path of directory to be sent: " + srcRelative);
    		outStream.writeUTF(srcRelative);
			outStream.flush();
			String child [] = dir.list();
			for(int i = 0 ; i < child.length ; i++)
			 {
				iterate(new File(dir , child[i]));
			 }
		 }
    	 else
    	  {
    	    toTransfer.add(dir);
    	  }
       }
      catch (IOException e) 
       {
			e.printStackTrace();
	   }
    }
/** A void method to give the contents count in the directory
 * 
 * @param dirName-Name of the dir to be counted
 */
private void getFile(String dirName) 
{
    File f = new File(dirName);
    File[] files = f.listFiles();
    
    if (files != null)
    for (int i = 0; i < files.length; i++) 
    {
        
        File file = files[i];
        count++;
        if (file.isDirectory()) 
        {   
             getFile(file.getAbsolutePath()); 
        }
    }
}
    	
 /**
  * A sendFile method that sends the file along and relative paths   
  */
 public void sendFile()
   {
    
	  reinitConn();
	final int MAX_BUFFER = 1000;
    byte [] data = null;
    int bufferSize = 0;
    
    	
    try 
     {
       //socket.shutdownOutput();
       
       outStream = new DataOutputStream(socket.getOutputStream());
       
       System.out.println("\nSending No of files:" + toTransfer.size() + "\n");
       int size = toTransfer.size();
       outStream.writeInt(size);
	   outStream.flush();
	 } 
    catch (IOException e1) 
      {
		e1.printStackTrace();
	  }
    	
     for (int i = 0; i < toTransfer.size(); i++)
       {
    	 try
    	  {
    		
    		 /*write the filename below in the File constructor*/
    		String s = (toTransfer.get(i).getAbsolutePath().substring((toTransfer.get(i).getAbsolutePath().indexOf(dirName) + dirName.length()+1)));
    		System.out.println("Relative paths to be sent: " + s);
    		
    		outStream.writeUTF(s);
    		outStream.flush();
    		
    		FileInputStream fileInput = new FileInputStream(toTransfer.get(i));
    		
    		/*get the file length*/
    		long fileSize = toTransfer.get(i).length();
    		System.out.println("File size at server is: " + fileSize + " bytes");
    		
    		/*first send the size of the file to the client*/
    		outStream.writeLong(fileSize);
    		outStream.flush();

    		/*Now send the file contents*/
    		if(fileSize > MAX_BUFFER)
    		  bufferSize = MAX_BUFFER;
    		else 
    		  bufferSize = (int)fileSize;
    	
    		data = new byte[bufferSize];
    		
    		long totalBytesRead = 0;
    		while(true)
    		 {
    			/*read upto MAX_BUFFER number of bytes from file*/
    			int readBytes = fileInput.read(data);
    			/*send readBytes number of bytes to the client*/
    			outStream.write(data);
    			outStream.flush();
    			/*stop if EOF*/
    			if(readBytes == -1)//EOF
    				break;
    			
    			totalBytesRead = totalBytesRead + readBytes;
    			/*stop if fileLength number of bytes are read*/
    			if(totalBytesRead == fileSize)
    				break;
    			/*update fileSize for the last remaining block of data*/
    			if((fileSize-totalBytesRead) < MAX_BUFFER)
    				bufferSize = (int) (fileSize-totalBytesRead);
    			
    			/*reinitialize the data buffer*/
    			data = new byte[bufferSize];
    		  }
    		  System.out.println(toTransfer.get(i) + " is Sent " + "\n");
    		  fileInput.close();
    		    		
    	   }
    	   catch(Exception e)
    		{
    		 e.printStackTrace();
    		}
    	}
    	try
		 {
		   inStream.close();
		   outStream.close();
		   serverSocket.close();
		   socket.close();
		 }
		catch (Exception e)
		{
			e.getStackTrace();
		}
    }
 /**
  * A void method to re-initate the connection and get new Socket streams
  */
 private void reinitConn()
  {
	try
	 {
		inStream.close();
		outStream.close();
		socket.close();
		socket = serverSocket.accept();
		outStream = new DataOutputStream(socket.getOutputStream());
		inStream = new DataInputStream(socket.getInputStream());
	 }
	catch (IOException e)
	{
		e.printStackTrace();
	}
}
/**
 * Main method   
 * 
 */
public static void main(String[] args)
  {
    TCPServerFile fileServer = new TCPServerFile(6002 , "C:\\Users\\admin\\Desktop\\home");
    fileServer.createSocket();
    fileServer.sendDirStruct();
    fileServer.sendFile();
       
    }
}