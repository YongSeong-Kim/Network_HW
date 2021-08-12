package ftp_workspace;
import java.io.*; 
import java.net.*;
import java.util.TimerTask;

public class FTPServer {

	public static void main(String[] args) throws Exception
	{

		ServerSocket welcomeServerSocket;
		int controlPort,dataPort;
		 
		if(args.length == 0) // 기본 2020 2021포트를 사용.
		{
			controlPort = 2020;
			dataPort = 2021;
		}
		else// argument로 받은 것을 포트로 사용.
		{
			controlPort = Integer.parseInt(args[0]);
			dataPort = Integer.parseInt(args[1]);
		}
		  
		System.out.println("ServerSocket open!");
		 
		 welcomeServerSocket = new ServerSocket(controlPort); //server의 welcomesocket을 열엇음.
		 
		 
		 while(true) {
			 Socket soc = welcomeServerSocket.accept(); // server의 소켓을 열었음.
             System.out.println("Incoming connection from " + soc.getRemoteSocketAddress());
             FTPSession fobj = new FTPSession(soc, dataPort);
             fobj.start();
		 }
	}
}

class FTPSession {

    // Path information
	private int dataPort;
    private String currDir;

    // Command Connection
    private Socket cSock;
    private PrintWriter outToClient;
    private BufferedReader inFromClient;

    // Data Connection
    private Socket dataSock;
    private OutputStream dataOut;
    private InputStream dataIn;


    FTPSession(Socket soc, int dataPort) {
    	this.dataPort = dataPort;
        this.cSock = soc; // server소켓이랑 command connection
        this.currDir = System.getProperty("user.dir");//처음 만들때 현재 디렉토리를 가르킴.
    }

    
    void list(String[] args)
    {
    	File fcheck = new File(args[1]);
    	String[] temp = args[1].split("/");
		File f = null;
		String listPath;
		
		if(fcheck.isAbsolute()) // argument가 절대경로일때.
		{
			listPath = temp[0];
    		for(int i = 1; i < temp.length; i++)
    		{
    			listPath = listPath + "/" + temp[i];
    			
    			f = new File(listPath);
    			
    			if(!f.isDirectory())// directory가 아니면 종료.
    			{
    				this.outToClient.println("Line 1 : 501 Failed – directory name is invalid");
    				System.out.println("Response : 501 Failed – directory name is invalid");
    				return; // 종료.
    			}
    		}
		}
		else
		{
    		listPath = this.currDir;
    		for(int i = 0; i < temp.length; i++)
    		{
    			listPath = listPath + "/" + temp[i];// directory인지 확인하기 위해 경로를 계속 만듬. 	
    			f = new File(listPath);
    			if(!f.isDirectory())// directory가 아니면 종료.
    			{
    				this.outToClient.println("Line 1 : 501 Failed – directory name is invalid");
    				System.out.println("Response : 501 Failed – directory name is invalid");
    				return; // 종료.
    			}
    		}
		}
		
		File[] lFile = f.listFiles();
		String list = "";
		
		for(int i = 0; i < lFile.length; i++)
		{
			if(lFile[i].isDirectory())
			{
				list += lFile[i].getName() + ",- ";
			}
			else if(lFile[i].isFile())
			{
				list += lFile[i].getName() + ","+lFile[i].length()+" ";
			}
		}


		
		if(lFile.length == 1) 
		{	
			System.out.println("Response : 200 Comprising " + lFile.length +" entry.");
			this.outToClient.println("Line1 : 200 Comprising " + lFile.length +" entry." );
			this.outToClient.println("Line 2 : " + list);
		
		}
		else
		{
			System.out.println("Response : 200 Comprising " + lFile.length +" entries.");
			this.outToClient.println("Line 1 : 200 Comprising " + lFile.length +" entries.");
			this.outToClient.println("Line 2 :" + list);
		}
    }
    
    
    void changeDirectory(String[] args) throws IOException
    {
    	String[] temp;
    	if(args.length == 1) // 현재 디렉토리 위치를 넘겨줌.
    	{
    		String p = "200 Moved to " + this.currDir;
    		System.out.println("Response : " + p);
    		this.outToClient.println("Line 1 : " + p);
    	}
    	else 
    	{
    		File fcheck = new File(args[1]);
    		temp = args[1].split("/");
    		File f = null;
    		String path;
    		
    		if(fcheck.isAbsolute()) // argument가 절대경로일때.
    		{
    			path = temp[0];
        		for(int i = 1; i < temp.length; i++)
        		{
        			path = path + "/" + temp[i];
        			
        			f = new File(path);
        			
        			if(!f.isDirectory())// directory가 아니면 종료.
        			{
        				this.outToClient.println("Line 1 : 501 Failed – directory name is invalid");
        				System.out.println("Response : 501 Failed – directory name is invalid");
        				return; // 종료.
        			}
        		}
        		
    		}
    		else // 상대경로일때.
    		{
        		path = this.currDir;
        		for(int i = 0; i < temp.length; i++)
        		{
        			path = path + "/" + temp[i];// directory인지 확인하기 위해 경로를 계속 만듬. 
        			f = new File(path);
        			if(!f.isDirectory())// directory가 아니면 종료.
        			{
        				this.outToClient.println("Line 1 : 501 Failed – directory name is invalid");
        				System.out.println("Response : 501 Failed – directory name is invalid");
        				return; // 종료.
        			}
        		}
    		}
    		this.currDir = f.getCanonicalPath() ; //현재 경로는 path를 가르키도록 함.
    		System.out.println("Response : 200 Moved to " + f.getCanonicalPath());
    		this.outToClient.println("Line 1 : 200 Moved to " + f.getCanonicalPath());
    	}
    	
    }
    
   void fileDownload(String[] args) throws IOException 
   {   
	   //args[1] 는 파일의 위치.
	   
	   File inFile = new File(args[1]);
	   
	   if(!inFile.isAbsolute())// 절대경로인지 상대경로인지 파악.
	   {
		   inFile = null;
		   inFile = new File(this.currDir +"/"+args[1]);
	   }

	   if(!inFile.exists()) // 존재하지 않으면.
	   {
		   System.out.println("Response : 401 Failed – No such file exists");
		   this.outToClient.println("Line 1 : 401 Failed – No such file exists");
	   }
	   else
	   {
		   System.out.println("Response : 200 Containing " + inFile.length() + " bytes in total");
		   this.outToClient.println("Line 1 : 200 Containing " + inFile.length() + " bytes in total");
		   
		   ServerSocket s = new ServerSocket(dataPort);
		   dataSock = s.accept();
		   dataOut = dataSock.getOutputStream();
		   
			InputStream fileStream = new FileInputStream(inFile);
			
		   byte[] buff = new byte[1005];
		   int recv;
		   int seqNo = 0;
			while ((recv = fileStream.read(buff, 5, 1000)) > 0) {
					if(seqNo == 16) seqNo = 0;
					buff[0] = (byte) seqNo++;//seqNo
					buff[1] = 0x00;
					buff[2] = 0x00;// checksum
					short size = (short) recv;
					buff[3] = (byte)(size>>8);
					buff[4] = (byte) size;//size
 				dataOut.write(buff,0,recv + 5);//파일 전송.
			}
			
			dataOut.flush();
			fileStream.close();
			dataSock.close();
			dataOut.close();
			s.close();
	   }   
   }
   
   void fileUpload(String[] args) throws IOException
   {
	   String clientResponse = inFromClient.readLine();//file 존재한다고 알려줌.
	   
	   if(clientResponse.contains("200")) // 파일이 존재한다면.
	   {
		   ServerSocket s = new ServerSocket(dataPort);
		   dataSock = s.accept();
		   dataIn = dataSock.getInputStream();
		   
		   String sizeString = inFromClient.readLine();
		   System.out.println("Resquest : " +sizeString);
		   FileOutputStream fileOutputStream = new FileOutputStream(this.currDir+"/"+args[1]);
			
			long size = Integer.parseInt(sizeString);
			byte[] buff = new byte[1005];
			System.out.println("Response : 200 Ready to receive");
			this.outToClient.println("Line 1 : 200 Ready to receive");
			
			int totalSize = (int) Math.ceil((double) size / 1000); // chunk사이즈가 1000이므로 총 바이트의 1000으로 나누면 값이 나옴.
			int[] marker = new int [totalSize+1];//문제있으면, 1 문제없으면, 2
			int recvBase = 0;
			byte[][] totalMessage = new byte[totalSize+1][1000];
			int[] totalPacketSize = new int[totalSize+1];
			
			
			while(true)
			{
				if(recvBase >= totalSize) break;
				
				dataIn.read(buff,0,buff.length);
				int seq = (int) buff[0]; // seq받은것.
				
				int tempSeq = seq;
					
				for(int i = recvBase; i <recvBase+5; i++)
				{
					if(i % 16 == tempSeq % 16)
					{
						tempSeq = i;
						break;
					}
				}
				
				if(buff[1] == (byte) 0xff && buff[2] == (byte) 0xff) // BITERROR
				{
					marker[tempSeq] = 1;//1은 BITERROR.
				}
				else 
				{
					marker[tempSeq] = 2; //2는 receive good
					byte[] control = new byte [3];
					control[0] = (byte) (seq + 33);
					control[1] = 0;
					control[2] = 0;
					String controlMessage = new String(control);
					totalPacketSize[tempSeq] = (((int)buff[3] & 0xff) << 8) | ((int)buff[4] & 0xff);
					System.arraycopy(buff, 5, totalMessage[tempSeq], 0, 1000);
					this.outToClient.println(controlMessage);

					for(int i = recvBase; i < totalSize; i++)//base를 움직여주는 역할.
					{
						if(marker[i] == 2) recvBase++;
						else break;
					}
				}
			}
			
			for(int i = 0; i < totalSize; i++) 
			{
				fileOutputStream.write(totalMessage[i],0,totalPacketSize[i]);
			}
			s.close();
			dataIn.close();
 			dataSock.close();
 			fileOutputStream.close();
	   }
	   else
	   {   
		   System.out.println("Response : " + clientResponse.substring(9));
	   }
   }

    public void cmdProcessor(String cmd) throws IOException {

        String[] args = cmd.split(" ");// userInput
        //CD PUT LIST GET
        switch (args[0]) {
        
        	case "CD":
        		changeDirectory(args);
        		break;
        		
        	case "LIST":
        		list(args);
        		break;
        		
        	case "PUT":
        		fileUpload(args);
        		break;
        		
        	case "GET":
        		fileDownload(args);
        		break;
        }
    }

    public void start() {
    	
        this.inFromClient = null; //command
        this.outToClient = null;//command
        
        try {
            this.inFromClient = new BufferedReader(new InputStreamReader(cSock.getInputStream()));
            this.outToClient =  new PrintWriter(cSock.getOutputStream(),true);    
            
            boolean connected = true;
            String clientCmd = null;
            while (connected) {
            		
            	clientCmd = inFromClient.readLine();
                 
                if (clientCmd != null) {
                    System.out.println("Request : "+ clientCmd);
                    if (clientCmd.equals("QUIT")) {
                        connected = false;
                    }
                    else{
                        cmdProcessor(clientCmd);
                    }
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally{
        	
            try {
                if (inFromClient != null)
                    inFromClient.close();
                if (outToClient != null)
                    outToClient.close();
                if (cSock != null)
                    cSock.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
   
    
    
}
