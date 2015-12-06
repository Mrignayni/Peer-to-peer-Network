import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.*;
public class Server implements Runnable {

	static int sPort = 8000;    //The server will be listening on this port number
	static ServerSocket sSocket;   //serversocket used to lisen on port number 8000
	Socket connection = null; //socket for the connection with the client
	String message;    //message received from the client
	ObjectOutputStream out;  //stream write to the socket
	ObjectInputStream in;    //stream read from the socket
	Socket csocket;
	public static ArrayList<File> chunks = new ArrayList<File>();;
	public static int count = 0;
	static int clientCount = 0;
	static String Extension = "";
	public Server(Socket csocket)
	{
		this.csocket = csocket;
	}




	public static void main(String args[]) throws Exception
	{
		new File("Server/").mkdirs();
		File f = new File(args[0]);
		fileSplit(f);
		Extension = args[0].split("\\.")[1];

		sSocket = new ServerSocket(sPort, 10);
		while (true)
		{
			Socket sock = sSocket.accept();
			new Thread(new Server(sock)).start();
		}
	}




	//send a message to the output stream

	@Override
	public void run() 
	{
		try
		{
			//create a serversocket

			//Wait for connection
			//accept a connection from the client
			out = new ObjectOutputStream(csocket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(csocket.getInputStream());
			message = (String)in.readObject();
			System.out.println("Client "+ message+" connected");
			out.writeObject(Extension);
			try
			{
				while(count<chunks.size())
				{
					sendChunk(chunks.get(count++));
					Thread.sleep(6000);
				}
			}
			catch(Exception classnot)
			{
				System.err.println("Error in thread sleep");
			}

		}
		catch(IOException | ClassNotFoundException ioException)
		{
			ioException.printStackTrace();
		}
		finally
		{
			//Close connections
			try
			{
				in.close();
				out.close();
			}
			catch(IOException ioException)
			{
				ioException.printStackTrace();
			}
		}



	}
	
	public static void fileSplit(File f)
	{
		try
		{
		int fSize = 102400;
		byte[] buffer = new byte[fSize];
		int partNumber = 0;
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
		String fBuffer = f.getName();
		int tmp = 0;
		while ((tmp = bis.read(buffer)) > 0) 
		{
			File newFile = new File(f.getParent(), "Server/"+fBuffer + (partNumber++));
			if(!chunks.contains(newFile))
				chunks.add(newFile);
			FileOutputStream out = new FileOutputStream(newFile);
			out.write(buffer, 0, tmp);
		}
		}catch(Exception e){
			System.out.println("Error in file split...");
		}
	}
	
	public void print()
	{
		for(int i = 0;i<count;i++)
		{
			System.out.print(chunks.get(i)+"  ");
		}
	}

	void sendChunk(File obj)
	{
		try
		{
			out.writeObject(chunks.size()+"");
			out.flush();
			out.writeObject((count-1)+"");
			out.flush();
			byte[] content = Files.readAllBytes(obj.toPath());
			out.writeObject(content);
			System.out.println("Sent chunk "+(count-1)+" to "+message);
			out.flush();
		}
		catch(IOException ioException)
		{
			ioException.printStackTrace();
		}
	}

	
}


