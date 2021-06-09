package pj;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * 
 * @author Ashim Gautam Upadhaya
 * UPDATED: December 1 2018
 * A simple TCP client for directory download with an expected typical tcp payload size of 1000 bytes
 */

public class TCPClientFile 
 {
   private Socket socket = null;
   private DataInputStream inStream = null;
   private DataOutputStream outStream = null;
   private int port;
   private String backupDir;
/**
 * Constructor  
 * @param port - port number
 * @param backupDir - destination directory
 */
public TCPClientFile(int port , String backupDir ) 
 {
   this.port = port;
   this.backupDir = backupDir;
   File f = new File(backupDir);/*Making a backup directory*/
   f.mkdir();
 }
/**
 *A method that Creates socket
 *Fetches input and output Stream
 */

public void createSocket()
 {
   try 
    {
      /*connect to localHost at given port*/
      socket = new Socket("192.168.1.93", port);
      System.out.println("Connected");
      /*fetch the streams*/
      inStream = new DataInputStream(socket.getInputStream());
      outStream = new DataOutputStream(socket.getOutputStream());
      
    } 
    catch (Exception u) 
     {
       u.printStackTrace();
     } 
  }
 
/**
  *A method that receives directory structure
  *It creates the received directory structure in the backup-dir    
  */
public void receiveDirStruct() 
 {
   int count = 0;
        
   try 
    {
      int fileNo = inStream.read();
      
      while (count < 6)
       {
          int flag = inStream.read();
          if (flag == 1)
           {
            flag = 0;
            String absPath = inStream.readUTF();
            System.out.println("Relative path of dir to be created: " + absPath);
            File newDir = new File(backupDir , absPath);
            newDir.mkdir();
            System.out.println("Directory created: " + newDir.getAbsolutePath() + "\n");
           }
          count++;
        }
     }
    catch(Exception e)
     {
        e.printStackTrace();
     }
    
   }
    
   
public void receiveFile()
  {
	reinitConn();
	final int MAX_BUFFER = 1000;
	byte [] data = null;
    int fileNo = 0;
    /*decide the max buffer size in bytes
    a typical value for a tcp payload is 1000 bytes, this is because of
    the common MTU of the underlying ethernet of 1500 bytes
    HOWEVER their is no optimal value for tcp payload, just a best guess i.e. 1000 bytes*/
    try 
     {
    	fileNo = inStream.readInt();
        System.out.println("Receiving No of Files:" + fileNo + "\n");
     } 
    catch (IOException e1) 
     {
        e1.printStackTrace();
     }
        
     
     for (int i = 0 ; i < fileNo ; i++)
      {
        
    	 try
         {
           
           String received = inStream.readUTF();
          
           File newFile = new File (backupDir , received);/*create a new file object with the destination dir 
                                                            and relative*/
           long fileSize = inStream.readLong();/*Size of incoming file*/
           int bufferSize=0; 
            
           /*decide the data reading bufferSize*/
            if(fileSize > MAX_BUFFER)
                bufferSize = MAX_BUFFER;
            else
                bufferSize = (int)fileSize;
            
            data = new byte[bufferSize];
            
            /*insert the path/name of your target file*/
            FileOutputStream fileOut = new FileOutputStream(newFile.getAbsolutePath(),true);        
            /*now read the file coming from Server & save it onto disk*/
                
            long totalBytesRead = 0;
                
            while(true)
             {
                /*read bufferSize number of bytes from Server*/
                int readBytes = inStream.read(data,0,bufferSize);
                byte[] arrayBytes = new byte[readBytes];
                System.arraycopy(data, 0, arrayBytes, 0, readBytes);
                totalBytesRead = totalBytesRead + readBytes;
                fileOut.write(data);
                    
                if(readBytes > 0 )
                 {
                    /*write the data to the file*/
                    fileOut.write(arrayBytes);
                    fileOut.flush();
                 }

                /*stop if fileSize number of bytes are read*/
                if(totalBytesRead == fileSize)
                    break;
                
                /*update fileSize for the last remaining block of data*/
                if((fileSize-totalBytesRead) < MAX_BUFFER)
                    bufferSize = (int) (fileSize-totalBytesRead);
                
                 /*reinitialize the data buffer*/
                 data = new byte[bufferSize];
               }
              System.out.println(newFile.getAbsolutePath() + " is received");
              fileOut.close();
            
           }
        catch(Exception e)
         {
            e.printStackTrace();
         }
      }
        
        /*closing the streams because all files are received*/
     try
      {
        inStream.close();
        outStream.close();
        socket.close();
      }
     catch (Exception e)
      {
        e.getStackTrace();
      }
        
   }
/**
 * A void method to reinitiate connection
 */
private void reinitConn()
{
	try
	{
		inStream.close();
		outStream.close();
		socket.close();
		socket = new Socket("192.168.1.93" , port);
		outStream = new DataOutputStream(socket.getOutputStream());
		inStream = new DataInputStream(socket.getInputStream());
	}
	catch (IOException e)
	{
		e.printStackTrace();
	}
}
    
  
   /*Main method for the client*/
public static void main(String[] args) throws Exception 
  {
   /*sending destination port and backUpdirPath*/
    TCPClientFile fileClient = new TCPClientFile(6002, "C:\\Users\\ExavierFrost\\Desktop\\home-backup");
    fileClient.createSocket();
    fileClient.receiveDirStruct();
    fileClient.receiveFile();
  }
}