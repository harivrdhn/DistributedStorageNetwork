package poke.client;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.jboss.netty.handler.codec.base64.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.client.util.ClientUtil;
import poke.demo.Jab;
import eye.Comm.Header;
import eye.Comm.Request;

/**
 * example listener that an application would use to receive events.
 * 
 * @author gash
 * 
 */
public class ClientPrintListener implements ClientListener {
	protected static Logger logger = LoggerFactory.getLogger("client");
	
	byte[] bytes = null;

	private String id;

	public ClientPrintListener(String id) {
		this.id = id;
	}

	@Override
	public String getListenerID() {
		return id;
	}

	@Override
	public void onMessage(eye.Comm.Response msg) {
        System.out.println("&&&&&&&&&&&&&&&&&&  in clientprintlistener  &&&&&&&&&&&&&&&");
       
        //if (logger.isDebugEnabled())
            //ClientUtil.printHeader(msg.getHeader());
            ClientUtil.printMessage(msg.getHeader(), msg.getBody());
            System.out.println("&&&&&&&&&&&&&&&&&&  Response to " + msg.getHeader().getTag() + "  from Node "+ msg.getHeader().getOriginator() + "   is " + msg.getHeader().getReplyCode() + msg.getHeader().getReplyMsg());
        if (msg.getHeader().getRoutingId() == Header.Routing.FINGER){
            ClientUtil.printFinger(msg.getBody().getFinger());
      		Jab.num=msg.getBody().getFinger().getNumber();
        }
        else {
            //ClientUtil.printFinger(msg.getBody().getFile());
        	System.out.println("************ In else on onMessage.. what you want");
        	if(msg.getHeader().getTag().equalsIgnoreCase("DOCFIND")){
        		try{
        		
            	bytes = msg.getBody().getStats().getChunkContent().toByteArray();
            	System.out.println(msg.getBody().getStats().getChunkContent().toByteArray().length);
            	ClientConnection.chunkArray.add(bytes);
            	String fileName = msg.getBody().getStats().getDocName();
            	String nameSpace = msg.getBody().getSpaces(0).getName();
            	System.out.println(nameSpace);
            	String Filepath = ("/home/sandeep/Documents/"+fileName);
            	System.out.println("****** chunkArray size" + ClientConnection.chunkArray.size());
            	File pdfFile = new File(Filepath);
            	FileOutputStream out = new FileOutputStream(pdfFile);
            	if(ClientConnection.chunkArray.size() == msg.getBody().getStats().getTotalChunk()){
            		for(int i=0 ;i< ClientConnection.chunkArray.size();i++)
            		{
                	    bytes = ClientConnection.chunkArray.get(i);
                	    System.out.println("Byte "+(i+1)+" size "+bytes.length);
                	    out.write(bytes);
                	 //   System.out.println("sandeep"+bytes);
                        /*
                         * 2. How to convert byte array back to an image file?
                         */
                	} 
            		System.out.println("bytes size "+bytes.length);
            		out.flush();          		
            		out.close();
                    System.out.println(" FILE  "+ fileName +"  DOWNLOADED SUCCESFULLY TO  "+ Filepath);
            	    // use the data in some way here       
            	
            	  }
        		}catch(Exception e){
        			e.printStackTrace();
        		}
            	
        	}
            /*for (int i = 0, I = msg.getBody().getDocsCount(); i < I; i++)   // CHUNKING NEED TO EXECUTED ACCORDING TO getDocsCount
                ClientUtil.printDocument(msg.getBody().getStats().(i));*/
        }
    }
	
	/*public static byte[] decodeImage(String imageDataString) {
        return Base64.decode(imageDataString);
    }
*/
	@Override
	public void onMessage(Request msg) {
		System.out.println("&&&&&&&&&&&&&&&&&&  in clientprintlistener  &&&&&&&&&&&&&&&");
		
		//if (logger.isDebugEnabled())
			ClientUtil.printHeader(msg.getHeader());
			System.out.println("&&&&&&&&&&&&&&&&&&  Response to " + msg.getHeader().getTag() + "  from Node "+ msg.getHeader().getOriginator() + "   is " + msg.getHeader().getReplyCode() + msg.getHeader().getReplyMsg());
		if (msg.getHeader().getRoutingId() == Header.Routing.FINGER)
			ClientUtil.printFinger(msg.getBody().getFinger());
		/*else {
			//ClientUtil.printFinger(msg.getBody().getFile());
			for (int i = 0, I = msg.getBody().getDocsCount(); i < I; i++)   // CHUNKING NEED TO EXECUTED ACCORDING TO getDocsCount
				ClientUtil.printDocument(msg.getBody().getDocs(i));
		}*/		
	}
}
