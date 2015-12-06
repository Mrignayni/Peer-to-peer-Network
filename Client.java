import java.io.*;
import java.util.*;
import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.Files;
import java.util.*;

public class Client implements Runnable {
	static int sPort = 0;    //The server will be listening on this port number
	static ServerSocket serverSocket;
	Socket reqSocket;           //socket connect to the server
	ObjectOutputStream oStream;         //stream write to the socket
	ObjectInputStream iStream;          //stream read from the socket
	int fSize;
	String message;                //message send to the server
	String MESSAGE;                //capitalized message read from the server
	static File[] chunkArray;
	static String fName;
	int threadType;
	Socket socket;
	boolean flag = false;
	static String name = "";
	static int downloadNeighbor = 0;
	public Client(int type) 
	{
		this.threadType = type;
	}

	public static void main(String args[]) throws Exception
	{
		Properties properties = new Properties();		
		InputStream in = new FileInputStream("config.properties");
		// load a properties file
		properties.load(in);
		String inputs = properties.getProperty(args[0]);
		String[] inputArr = inputs.split(",");
		name = inputArr[0];
		sPort = Integer.parseInt(inputArr[1]);
		downloadNeighbor = Integer.parseInt(inputArr[2]);

		new File(name+"/chunks").mkdirs();
		new File(name+"/FinalFile").mkdirs();
		serverSocket = new ServerSocket(sPort, 10);
		Client fileOwnerClientObj = new Client(1);		
		Client downloadClient = new Client(2);
		Client uploadClient = new Client(3);
		Scanner s = new Scanner(System.in);
		System.out.println("Press any key to start.");
		String st = s.next();		
		new Thread(fileOwnerClientObj).start();
		Thread.sleep(1000);
		new Thread(downloadClient).start();
		new Thread(uploadClient).start();

	}


	public void run()
	{
		switch(threadType){
		case 1:
		{
			try{
				reqSocket = new Socket("localhost", 8000);
				System.out.println("Connected to File Owner (Port 1000)");
				oStream = new ObjectOutputStream(reqSocket.getOutputStream());
				oStream.flush();
				iStream = new ObjectInputStream(reqSocket.getInputStream());
				oStream.writeObject(name);
				oStream.flush();
				String fileExtension = (String)iStream.readObject();
				fName = name+"File."+fileExtension;
				//get Input from standard input
				while(true)
				{				
					getMessage();
					
					if(flag || isFull())
					{
						break;
					}
					
					Thread.sleep(500);
				}
			}
			catch (ConnectException e)
			{
				System.err.println("Connection declined. Please initialize server first.");
			} 
			catch ( ClassNotFoundException e )
			{
				System.err.println("Class not found");
			} 
			catch(UnknownHostException unknownHost)
			{
				System.err.println("Unknown Host Exception");
			}
			catch(IOException ioException)
			{
				ioException.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally
			{
				//Close connections
				try
				{
					iStream.close();
					oStream.close();
			
				}
				catch(IOException ioException){
					ioException.printStackTrace();
				}
			}
		break;
		}

		case 2:
		{
			try
			{
					reqSocket = new Socket("localhost", downloadNeighbor);
					oStream = new ObjectOutputStream(reqSocket.getOutputStream());
					oStream.flush();
					iStream = new ObjectInputStream(reqSocket.getInputStream());
				while(true)
				{
					if(chunkArray!=null)
					{
					if(isFull())
					{
						mergeFiles(chunkArray,new File(name+"/FinalFile/"+fName));
						break;
						
					}
					else
					{
						
						String required="";
						for(int i = 0;i<chunkArray.length;i++)
						{
							if(chunkArray[i]==null)
							{
								required+=""+i+",";
							}
						}
						System.out.println("Chunk Request List : "+required);
						oStream.writeObject(required);
						oStream.flush();
						getPeerMessage();
					}
					}
					Thread.sleep(1000);
				}

			}
			catch (IOException e)
			{
				e.printStackTrace();
			} 
			catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		break;
		}

		case 3:
		{
			try {
				socket = serverSocket.accept();
				oStream = new ObjectOutputStream(socket.getOutputStream());
				oStream.flush();
				iStream = new ObjectInputStream(socket.getInputStream());
				while(true)
				{
					message = (String)iStream.readObject();
					String[] requested = message.split(",");
					boolean flag = true;
					while(flag)
					{
					for(int i = 0;i < requested.length; i++)
					{
						if(chunkArray!=null && chunkArray[Integer.parseInt(requested[i])] != null)
						{
							sendMessage(chunkArray[Integer.parseInt(requested[i])],Integer.parseInt(requested[i]));
							flag = false;
							break;
						}
					}
					Thread.sleep(1000);
					}
					Thread.sleep(1000);
				}
			} catch (IOException e) 
			{
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;

		}
		}
	}

	void sendMessage(File obj, int count)
	{
		try
		{
			oStream.writeObject(count+"");
			System.out.println("Sending chunk "+count+": to peer");
			oStream.flush();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		try
		{
			byte[] content = Files.readAllBytes(obj.toPath());
			oStream.writeObject(content);
			oStream.flush();
		}
		catch(IOException ioException)
		{
			ioException.printStackTrace();
		}
	}


	void getMessage() throws Exception, ClassNotFoundException
	{
		try
		{
			int temp = Integer.parseInt((String)iStream.readObject());
			if(fSize == 0)
			{
				fSize = temp;
				chunkArray = new File[fSize];
				System.out.println("Number of Chunks: " + fSize);
			}
			int fileNumber = Integer.parseInt((String)iStream.readObject());
			String chunkName = "chunk."+name+"."+fileNumber;
			File f = new File(name+"/chunks/"+chunkName);
			byte[] content = (byte[]) iStream.readObject();
			Files.write(f.toPath(), content);
			System.out.println("File Owner: Chunk "+fileNumber);
			chunkArray[fileNumber] = f;
		}
		catch(IOException e){
				flag = true;
				System.out.println("Server aborted!");
			}
	}

	void getPeerMessage()
	{
		try
		{
		int fileNumber = Integer.parseInt((String)iStream.readObject());
		String chunkName = "File."+name+fileNumber;
		File f = new File(name+"/chunks/"+chunkName);
		byte[] content = (byte[]) iStream.readObject();
		Files.write(f.toPath(), content);
		System.out.println("Chunk "+fileNumber+" recieved from neighbour at port: "+downloadNeighbor);
		chunkArray[fileNumber] = f;
		}
		catch(ClassNotFoundException cnfe)
		{
			System.out.println("Class not found in getPeerMessage");
		}
		catch(IOException ioe)
		{
			System.out.println("I/O Exception in getPeerMessage");
		}
		catch(Exception e)
		{
			System.out.println("Exception in getPeerMessage");
		}
		
	}

	

	public void mergeFiles(File[] files, File into){
		
		try{
		BufferedOutputStream mergingStream = new BufferedOutputStream(new FileOutputStream(into)); 
		{

			for (File f : files) 
			{
				Files.copy(f.toPath(), mergingStream);
				mergingStream.flush();
			}
			
		}
		System.out.println("Files Merged!");
		}
		catch(Exception e)
		{
			System.out.println("Exception in mergeFile");
		}
	}

	boolean isFull()
	{
		for(int i = 0;i<chunkArray.length;i++)
		{
			if(chunkArray[i] == null)
				return false;
		}
		return true;
	}

}
