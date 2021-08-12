package ftp_workspace;

import java.io.*; 
import java.net.*;
import java.util.Timer;
import java.util.TimerTask;

public class FTPClient {

	static BufferedReader inFromUser;
	
	static Socket clientControlSocket;
	static DataOutputStream outToServer;
	static BufferedReader inFromServer;
	
	static Socket clientDataSocket;
    static OutputStream dataOut;
    static InputStream dataIn;
    
    static boolean bitError;
    static boolean timeOut;
    static boolean drop;
    static String[] error;
    
    static int[] marker;
    static Timer timeOutTimer;
    static int errorNum;
    
	public static void main(String[] args) throws Exception
	{
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
		 
		 String[] input = userInput.split(" "); // input[1]에 경로가 존재. 
		 
		 if(input[0].equals("QUIT"))
		 {
			 done = true;
			 return done;
		 }
		 else if(input[0].startsWith("BITERROR") || input[0].startsWith("DROP") || input[0].startsWith("TIMEOUT"))
		 {
			 
		 }
		 else
		 {
			 outToServer.writeBytes(userInput+'\n');// 명령어 전송.
			 outToServer.flush();
		 }
		 
		 String serverResponse = null;
		 
		 if(userInput.startsWith("GET") || userInput.startsWith("LIST") || userInput.startsWith("CD"))
		 {
			 serverResponse = inFromServer.readLine(); // server로 부터 명령어를 읽음. 
		 }
		 
		 
		if(userInput.startsWith("BITERROR"))
		{
			error = userInput.substring(9).split(",");
			
			for(int i = 0; i <error.length; i++)
			{
				error[i] = error[i].substring(1);
			}
			bitError = true;
		}
		else if(userInput.startsWith("DROP"))
		{
			error = userInput.substring(5).split(",");
			for(int i = 0; i <error.length; i++)
			{
				error[i] = error[i].substring(1);
			}
			drop = true;
		}
		else if(userInput.startsWith("TIMEOUT"))
		{
			error = userInput.substring(8).split(",");
			for(int i = 0; i <error.length; i++)
			{
				error[i] = error[i].substring(1);
			}
			timeOut = true;
		}
		else if(userInput.startsWith("CD"))
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
	 			String[] list = serverFile.substring(8).split(" ");
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
	 			
	 			while ((readBytes = dataIn.read(buff,0,buff.length)) != -1) {
	 		        
					if(buff[1] == 0 && buff[2] == 0) {//checksum이 문제가 없을경우.
						if(readBytes == 1005) System.out.print( (int)buff[0] + " ");
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
	 			System.out.println("Failed – No such file exists");
	 			outToServer.writeBytes("Line 1 : 501 Failed – No such file exists\n");
	 		}
	 		else
	 		{
	 			outToServer.writeBytes("Line 1 : 200 File exists\n");
	 			
	 			clientDataSocket = new Socket(hostname, dataPort);
	 			dataOut = clientDataSocket.getOutputStream();
	 			InputStream fileStream = new FileInputStream(inFile);//내가 입력한 파일을 읽어오기위해.
	 			
	 			outToServer.writeBytes(inFile.length()+"\n"); // 크기.
	 			System.out.println(inFile.getName()+" transferred / " + inFile.length()+" bytes");
	 			String check = inFromServer.readLine();
	 			
	 			int totalSize = (int) Math.ceil((double) inFile.length() / 1000); // chunk사이즈가 1000이므로 총 바이트의 1000으로 나누면 값이 나옴.
	 			
	 			byte[][] totalMessage = new byte[totalSize+1][1000];//  한행마다 데이터를 저장.
	 			
	 			int[] totalPacketSize = new int[totalSize+1];//packet사이즈를 저장하는 변수.
	 			
	 			int k = 0;
	 			int recv;
	 			while ((recv = fileStream.read(totalMessage[k], 0, 1000)) > 0) {
	 				totalPacketSize[k] = recv;//size를 저장.
 					k++;
	 			}//chunk division 수행.
	 			
	 			timeOutTimer = new Timer(true);
	 			errorNum = 0;
	 			marker = new int [totalSize+1];
					
	 			
	 			if(check.contains("200"))
	 			{
	 				if(bitError == true)
	 				{
	 					int m = 0;
	 					int sendBase = 0;//window의 가장첫번째.
	 					
	 					for(m = sendBase; m < 5; m++)//5는 window size.
	 					{
	 						if(m >= totalSize) break;
			 				sendBitErrorPacket(m, totalMessage[m], totalPacketSize[m]);		
	 					}
	 					
	 					while( true )
	 					{

	 						if(sendBase >= totalSize) break;
	 						
	 						String ack = inFromServer.readLine();//dataIn으로 받아야할듯 seq Ack이므로.

		 					byte[] ackMessage = ack.getBytes();
		 					
		 					int seqNo = ((int) ackMessage[0]) -33;
		 					
		 					int tempSeq = seqNo;
		 					
		 					for(int i = sendBase; i <sendBase+5; i++)
		 					{
		 						if(i%16 == tempSeq%16)
		 						{
		 							tempSeq = i;
		 							break;
		 						}
		 					}
		 					
	 						for(int i = tempSeq; i < tempSeq + 5; i++){
	 							
	 							if(marker[tempSeq] == 1)
	 							{
 									System.out.print(seqNo + "acked ");
				 					marker[tempSeq] = 2;
	 							}
				            }
	 						
	 						for(int i = sendBase; i < totalSize; i++)//base를 움직여주는 역할.
	 						{
	 							if(marker[i] == 2) sendBase++;
	 							else break;
	 						}
	 						
	 						for(int i = sendBase; i < sendBase + 5; i++)//base를 기준으로 안보낸 패킷을 보내주는 역할.
	 						{
	 							if(i >= totalSize) break;
	 							
	 							if(marker[i] == 0 ) 
								{
	 								sendBitErrorPacket(i, totalMessage[i], totalPacketSize[i]);
								}
	 						}
	 						
	 					}
	 				}
	 				else if(drop == true)
	 				{
	 					int m = 0;
	 					int sendBase = 0;//window의 가장첫번째.
	 					
	 					for(m = sendBase; m < 5; m++)//5는 window size.
	 					{
	 						if(m >= totalSize) break;
			 				sendDropPacket(m, totalMessage[m], totalPacketSize[m]);		
	 					}
	 					
	 					while( true )
	 					{

	 						if(sendBase >= totalSize) break;
	 						
	 						String ack = inFromServer.readLine();//dataIn으로 받아야할듯 seq Ack이므로.

		 					byte[] ackMessage = ack.getBytes();
		 					
		 					int seqNo = ((int) ackMessage[0]) -33;
		 					
		 					int tempSeq = seqNo;
		 					
		 					for(int i = sendBase; i < sendBase +5; i++)
		 					{
		 						if(i % 16 == tempSeq % 16)
		 						{
		 							tempSeq = i;
		 							break;
		 						}
		 					}
		 					
	 						for(int i = tempSeq; i < tempSeq + 5; i++){
	 							
	 							if(marker[tempSeq] == 1)
	 							{	
 									System.out.print(seqNo + "acked ");
				 					marker[tempSeq] = 2;
	 							}
				            }
	 						
	 						for(int i = sendBase; i < totalSize; i++)//base를 움직여주는 역할.
	 						{
	 							if(marker[i] == 2) sendBase++;
	 							else break;
	 						}
	 						
	 						for(int i = sendBase; i < sendBase + 5; i++)//base를 기준으로 안보낸 패킷을 보내주는 역할.
	 						{
	 							if(i >= totalSize) break;
	 							
	 							if( marker[i] == 0 ) 
								{
	 								sendDropPacket(i, totalMessage[i], totalPacketSize[i]);
								}
	 						}
	 						
	 					}
	 				}
	 				else if(timeOut == true)
	 				{
	 					int m = 0;
	 					int sendBase = 0;//window의 가장첫번째.
	 					
	 					for(m = sendBase; m < 5; m++)//5는 window size.
	 					{
	 						if(m >= totalSize) break;
			 				sendTimeOutPacket(m, totalMessage[m], totalPacketSize[m]);		
	 					}
	 					
	 					while( true )
	 					{

	 						if(sendBase >= totalSize) break;
	 						
	 						String ack = inFromServer.readLine();
		 					byte[] ackMessage = ack.getBytes();
		 					int seqNo = ((int) ackMessage[0]) -33;
		 					int tempSeq = seqNo;
		 					
		 					for(int i = sendBase; i < sendBase + 5; i++)
		 					{
		 						if(i % 16 == tempSeq % 16)
		 						{
		 							tempSeq = i;
		 							break;
		 						}
		 					}
		 					
	 						for(int i = tempSeq; i < tempSeq + 5; i++){
	 							
	 							if(marker[tempSeq] == 1)
	 							{
	 								
 									System.out.print(seqNo + "acked ");
				 					marker[tempSeq] = 2;
	 							}
				            }
	 						
	 						for(int i = sendBase; i < totalSize; i++)//base를 움직여주는 역할.
	 						{
	 							if(marker[i] == 2) sendBase++;
	 							else break;
	 						}
	 						
	 						for(int i = sendBase; i < sendBase + 5; i++)//base를 기준으로 안보낸 패킷을 보내주는 역할.
	 						{
	 							if(i >= totalSize) break;
	 							
	 							if( marker[i] == 0 ) 
								{
	 								sendTimeOutPacket(i, totalMessage[i], totalPacketSize[i]);
								}
	 						}
	 					}
	 					
	 					
	 				}
	 				else
	 				{
	 					
	 					int m = 0;
	 					int sendBase = 0;//window의 가장첫번째.
	 					
	 					for(m = sendBase; m < 5; m++)//5는 window size.
	 					{
	 						if(m >= totalSize) break;
			 				sendPacket(m, totalMessage[m], totalPacketSize[m]);		
	 					}
	 					
	 					while( true )
	 					{

	 						if(sendBase >= totalSize) break;
	 						
	 						String ack = inFromServer.readLine();//dataIn으로 받아야할듯 seq Ack이므로.
		 					byte[] ackMessage = ack.getBytes();
		 					
		 					int seqNo = ((int) ackMessage[0]) -33;

		 					int tempSeq = seqNo;
		 					for(int i = sendBase; i <sendBase+5; i++)
		 					{
		 						if(i%16 == tempSeq%16)
		 						{
		 							tempSeq = i;
		 							break;
		 						}
		 					}
		 					
		 					for(int i = tempSeq; i < tempSeq + 5; i++){	
	 							if(marker[tempSeq] == 1)
	 							{
 									System.out.print(seqNo + "acked ");	
				 					marker[tempSeq] = 2;
	 							}
				            }
	 						
	 						for(int i = sendBase; i < totalSize; i++)//base를 움직여주는 역할.
	 						{
	 							if(marker[i] == 2) sendBase++;
	 							else break;
	 						}
	 						
	 						for(int i = sendBase; i < sendBase + 5; i++)//base를 기준으로 안보낸 패킷을 보내주는 역할.
	 						{
	 							if(i >= totalSize) break;
	 							
	 							if( marker[i] == 0 ) 
								{
	 								sendPacket(i, totalMessage[i], totalPacketSize[i]);
								}
	 						}	
	 					}
	 				}
	 			}
	 			System.out.println(" Completed...");
	 			dataOut.flush();
	 			fileStream.close();
	 			dataOut.close();
	 			clientDataSocket.close();		
	 		}
	 		
	 		error = null;
	 		bitError = false;
	 		drop = false;
	 		timeOut = false;
	 	}
	 	
		return done;
	 }
		 
	 
	 static void sendBitErrorPacket(int seq, byte[] totalMessage, int totalPacketSize) throws IOException
	 {
		 
		byte[] buff = new byte [1005];
		int tempSeq = seq;
		while(tempSeq > 15)
		{
			tempSeq -= 16;
		}
		
		buff[0] = (byte) tempSeq;
		
		if( errorNum < error.length && (Integer.parseInt(error[errorNum]) == seq + 1) )//error나는것.
		{
			buff[1] = (byte) 0xff;
			buff[2] = (byte) 0xff;// checksum
			errorNum++;
		}
		else
		{
			buff[1] = 0x00;
			buff[2] = 0x00;// checksum
		}
		
		int size = totalPacketSize;
		buff[3] = (byte)(size>>8);
		buff[4] = (byte) size;//size
		
		System.arraycopy(totalMessage, 0, buff, 5, 1000);
		dataOut.write(buff,0,1005);//파일 전송.
		marker[seq] = 1;
		
		System.out.print(tempSeq + " ");
		
		timeOutTimer.schedule(new PacketTimeout(seq,totalMessage,totalPacketSize), 1000);
	 }
	 
	 static void sendTimeOutPacket(int seq, byte[] totalMessage, int totalPacketSize) throws IOException
	 {
		 
		byte[] buff = new byte [1005];
		
		int tempSeq = seq;
		
		while(tempSeq>15)
		{
			tempSeq -=16;
		}
		
		buff[0] = (byte) tempSeq;
		buff[1] = 0x00;
		buff[2] = 0x00;// checksum
		int size = totalPacketSize;
		buff[3] = (byte)(size>>8);
		buff[4] = (byte) size;//size
		System.arraycopy(totalMessage, 0, buff, 5, 1000);
		
		
		System.out.print( tempSeq + " " );

		
		marker[seq] = 1;
		
		if( errorNum < error.length && (Integer.parseInt(error[errorNum]) == seq + 1) )//error나는것.
		{
			errorNum++;
			timeOutTimer.schedule(new PacketTimeout(seq,totalMessage,totalPacketSize,true), 2000);
		}
		else
		{		
			dataOut.write(buff,0,1005);//파일 전송.
		}
		
		timeOutTimer.schedule(new PacketTimeout(seq,totalMessage,totalPacketSize), 1000);
	 }
	 
	 
	 
	 
	 static void sendPacket(int seq, byte[] totalMessage, int totalPacketSize) throws IOException
	 {
		 
		byte[] buff = new byte [1005];
		int tempSeq = seq;
		
		while(tempSeq > 15)
		{
			tempSeq -= 16;
		}
		 
		buff[0] = (byte) tempSeq;
		
		buff[1] = 0x00;
		buff[2] = 0x00;// checksum
		int size = totalPacketSize;
		buff[3] = (byte)(size>>8);
		buff[4] = (byte) size;//size
		System.arraycopy(totalMessage, 0, buff, 5, 1000);
		
		dataOut.write(buff,0,1005);//파일 전송.
		marker[seq] = 1;
		
		System.out.print(tempSeq + " ");
		
		
		timeOutTimer.schedule(new PacketTimeout(seq,totalMessage,totalPacketSize), 1000);
	 }
	 
	 static void sendDropPacket(int seq, byte[] totalMessage, int totalPacketSize) throws IOException
	 {
		 
		byte[] buff = new byte [1005];
		int tempSeq = seq;
		
		while(tempSeq>15)
		{
			tempSeq -=16;
		}
		
		buff[0] = (byte) tempSeq;
		if( errorNum < error.length && (Integer.parseInt(error[errorNum]) == seq + 1) )//error나는것.
		{
			marker[seq] = -1; // -1은 drop을 의미.
			errorNum++;
		}
		else
		{
			buff[1] = 0x00;
			buff[2] = 0x00;// checksum
			int size = totalPacketSize;
			buff[3] = (byte)(size>>8);
			buff[4] = (byte) size;//size
			System.arraycopy(totalMessage, 0, buff, 5, 1000);
			
			dataOut.write(buff,0,1005);//파일 전송.
			marker[seq] = 1;
		}
		
		
		System.out.print(tempSeq + " ");
		
		timeOutTimer.schedule(new PacketTimeout(seq,totalMessage,totalPacketSize), 1000);
	 }
	 
	 
	 
	 private static class PacketTimeout extends TimerTask{
	        private int seq;
	        private byte[] message;
	     	private int size;
	     	private boolean timeout;
	        public PacketTimeout(int seq, byte[] message, int size) {
	           this.seq = seq;
	           this.message = message;
	           this.size = size;
	           this.timeout = false;
	        }
	        
	        public PacketTimeout(int seq, byte[] message, int size, boolean timeout){
		           this.seq = seq;
		           this.message = message;
		           this.size = size;
		           this.timeout = timeout;
		        }
	     	
	        public void run(){
	        	if(timeout == false) // timeout 발생하지 않을때,
	        	{ 
	        		if(marker[seq] == 1 || marker[seq] == -1){ // 보내주지 않거나 보냈는데 응답이 없는것에 대하여 다시보냄.
		        	   
		        	   if(seq > 15)
		        	   {
		        		   System.out.println( seq % 16 + " timeout & retransmitted" );
		        	   }
		        	   else
		        	   {
		        		   System.out.println( seq + " timeout & retransmitted" );
		        	   }
		              
		              try{
		            	  if(bitError == true)
		            	  {
		 	                 sendBitErrorPacket(seq, message, size);
		            	  }
		            	  else if(drop == true)
		            	  {
		            		  sendDropPacket(seq, message, size); 
		            	  }
		            	  else if(timeOut == true)
		            	  {
		            		  sendTimeOutPacket(seq, message, size); 
		            	  }

		              }
	                 catch (Exception e){}
		           }
	        		
	        	}
	        	else if(this.timeout == true && !clientDataSocket.isClosed()) //2초 대기후에 socket이 열려있지 않으면.
	        	{

	        		byte[] buff = new byte [1005];
	        		int tempSeq = seq;
	        		
	        		while(tempSeq > 15)
	        		{
	        			tempSeq -= 16;
	        		}
	        		 
	        		buff[0] = (byte) tempSeq;
	        		
	        		buff[1] = 0x00;
	        		buff[2] = 0x00;// checksum
	        		int tempSize = this.size;
	        		buff[3] = (byte)(tempSize>>8);
	        		buff[4] = (byte) tempSize;//size
	        		System.arraycopy(message, 0, buff, 5, 1000);
	        		
	        		try {
						dataOut.write(buff,0,1005);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}//파일 전송.
	        		
	        		
	        		
	        	}
	        }
	     }
}
