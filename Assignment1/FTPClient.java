package ftp_assignment;

import java.io.*; 
import java.net.*;

public class FTPClient {

	static BufferedReader inFromUser;
	
	static Socket clientControlSocket;
	static DataOutputStream outToServer;
	static BufferedReader inFromServer;
	
	static Socket clientDataSocket;
    static OutputStream dataOut;
    static InputStream dataIn;
    
	
	public static void main(String[] args) throws Exception
	{
		String cmd;
		String modifiedSentence;
		inFromUser = new BufferedReader(new InputStreamReader(System.in));
		
		String hostname = null;
		int controlPort, dataPort;
		
		if(args.length == 0)
		{
			hostname = "127.0.0.1";
			controlPort = 2020;
			dataPort = 2021;
		}
		else
		{
			hostname = args[0];
			controlPort = Integer.parseInt(args[1]);
			dataPort = Integer.parseInt(args[2]);//server의 control data port를 알려주는 것.
		}
		System.out.println("Control socket connect.");
		clientControlSocket = new Socket(hostname, controlPort); // 서버와 소켓을 연결.
		outToServer = new DataOutputStream(clientControlSocket.getOutputStream());// 서버로 보내는 스트림.
		inFromServer = new BufferedReader(new InputStreamReader(clientControlSocket.getInputStream())); // 서버에서 읽어오는 스트림.
		
		processUserCommand(hostname, dataPort);
		
		if(clientControlSocket != null)
        {
            try
            {
            	clientControlSocket.close();
            }
            catch(final IOException e)
            {
                e.printStackTrace();
            }
        }

        if(outToServer != null)
        {
        	outToServer.close();
        }

        if(inFromServer != null)
        {
            try
            {
            	inFromServer.close();
            }
            catch(final IOException e)
            {
                e.printStackTrace();
            }
        }
	}
	
	 private static void processUserCommand(String hostname, int dataPort) throws IOException
	 {
		 BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		 String userInput;
		 boolean done = false;

		 do
		 {
			 System.out.print("FTP command:  ");
			 userInput = null;
	   		try
	   		{
		   //Read user's input from keyboard
	           userInput = stdIn.readLine();
	       }
	       catch (final IOException e)
	       {
	           e.printStackTrace();
	       }
	
	       if(userInput != null)
	       {
	    	   done = processUserCommand(userInput, hostname, dataPort);
	       }
		 }while(!done);
	 }
	 
	 private static boolean processUserCommand(String userInput, String hostname, int dataPort) throws IOException
	 {
		 boolean done = false;
		 String cmd;
		 
		 String[] input = userInput.split(" "); // input[1]에 경로가 존재. 
		 
		 if(input[0].equals("QUIT"))
		 {
			 done = true;
			 return done;
		 }
		 else
		 {
			 outToServer.writeBytes(userInput+'\n');// 명령어 전송.
			 outToServer.flush();
		 }
		 String serverResponse = null;
		 
		 if(!userInput.startsWith("PUT"))
		 {
			 serverResponse = inFromServer.readLine(); // server로 부터 명령어를 읽음. 
		 }
		 
		 
	 	if(userInput.startsWith("CD"))
	 	{
	 		if(serverResponse.contains("200"))
			{
	 			System.out.println(serverResponse.substring(22));
			}
			else
			{
				System.out.println(serverResponse.substring(9));
			}
	 	}
	 	else if(userInput.startsWith("LIST"))
	 	{
	 		if(serverResponse.contains("200"))
			{
	 			
	 			String serverFile = inFromServer.readLine();	
	 			String[] list = serverFile.substring(9).split(" ");
	 			for(int i = 0; i < list.length; i++ )
	 			{
	 				System.out.println(list[i]);
	 			}
			}
			else
			{
				System.out.println(serverResponse.substring(9));
			}
	 	}
	 	else if(userInput.startsWith("GET"))
	 	{
	 		if(serverResponse.contains("200"))
			{
	 			clientDataSocket = new Socket(hostname, dataPort);
	 			dataIn = clientDataSocket.getInputStream();
	 			
	 			String[] fileName = input[input.length-1].split("/");
	 			FileOutputStream fileOutputStream = new FileOutputStream("./"+fileName[fileName.length-1]);
	 			
	 			String[] tmp = serverResponse.substring(24).split(" ");//파일의 크기를 알기 위해서 함.
	 			long size = Integer.parseInt(tmp[0]); //파일의 사이즈를 저장.
	 			byte[] buff = new byte[1005];
	 			
	 			System.out.println("Received " + fileName[fileName.length-1] + " / " + size);
	 			
	 			int readBytes;
	            
//	 			while ((readBytes = dataIn.read(buff, 0, buff.length)) != -1) { // 파일 전달 받아 현재 디렉토리위치에 저장.
//	 				if(readBytes == 1005) System.out.print("#");
//	 				
//	                fileOutputStream.write(buff, 0, readBytes);
//	            }
	 			
	 			while ((readBytes = dataIn.read(buff,0,buff.length)) != -1) {
	 		        
					if(buff[1] == 0 && buff[2] == 0) {//checksum이 문제가 없을경우.
						if(readBytes == 1005) System.out.print("#");
		            	fileOutputStream.write(buff, 5, readBytes-5);
		            }
				}
	 			
	            System.out.println(" Completed...");
	 			dataIn.close();
	 			clientDataSocket.close();
	 			fileOutputStream.close();
			}
			else // 실패했을때,
			{
				System.out.println(serverResponse.substring(13));
			}
	 	}
	 	else if(userInput.startsWith("PUT"))
	 	{
	 		File inFile = new File(input[1]); // 내가 입력한 파일.
	 		
	 		if(!inFile.exists())
	 		{
	 			System.out.println("Line 1 : Failed – No such file exists");
	 			outToServer.writeBytes("Line 1 : 501 Failed – No such file exists\n");
	 				
	 		}
	 		else
	 		{
	 			outToServer.writeBytes("Line 1 : 200 File exists\n");
	 			clientDataSocket = new Socket(hostname, dataPort);
	 			dataOut = clientDataSocket.getOutputStream();
	 			InputStream fileStream = new FileInputStream(inFile);
	 			byte[] buff = new byte[1005];
	 			int recv;
	 			outToServer.writeBytes(inFile.length()+"\n"); // 크기.
	 			System.out.println(inFile.getName()+" transferred / " + inFile.length()+" bytes");
	 			String check = inFromServer.readLine();
	 			
	 			if(check.contains("200"))
	 			{
		 			while ((recv = fileStream.read(buff, 5, 1000)) > 0) {
		 					buff[0] = 0;//seqNo
		 					buff[1] = 0x00;
		 					buff[2] = 0x00;// checksum
		 					short size = (short) recv;
		 					buff[3] = (byte)(size>>8);
		 					buff[4] = (byte) size;//size
			 				if(recv == 1000) System.out.print("#");
			 				dataOut.write(buff,0,recv + 5);//파일 전송.
		 			}
	 			}
	 			System.out.println(" Completed...");
	 			dataOut.flush();
	 			fileStream.close();
	 			dataOut.close();
	 			clientDataSocket.close();		
	 		}
	 	}
	 	
		return done;
	 }
		 
}
