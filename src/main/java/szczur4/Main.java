package szczur4;
import szczur4.client.ClientCore;
import szczur4.server.ServerCore;
import java.io.File;
import java.io.IOException;
public class Main{
	void main()throws IOException,InterruptedException{
		File f=new File(System.getProperty("user.dir"),".lock");
		if(f.exists()){
			System.err.println("Lock file exists! Closing");
			return;
		}
		if(!f.createNewFile())throw new IOException("Could not create lock file");
		f.deleteOnExit();
		new ServerCore();
		new ClientCore();
	}
}